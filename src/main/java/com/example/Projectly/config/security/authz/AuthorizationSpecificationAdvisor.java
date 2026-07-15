package com.example.Projectly.config.security.authz;

import com.example.Projectly.config.security.authz.PrincipalContext;
import com.example.Projectly.config.security.authz.grant.Grant;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Row-filtering face of the policy engine (RBAC_V3 §6.4) — replaces the v1
 * {@code buildOwnershipSpec} helper. For list/page/criteria/count queries:
 *
 * <pre>
 * P_permit = OR( toPredicate(s) for every applicable PERMIT statement matching READ )
 *          OR ( EXISTS active grant matching READ )                    [grants module]
 * P_deny   = OR( toPredicate(s) for every applicable DENY  statement matching READ )
 * result   = P_permit AND NOT(P_deny)
 * </pre>
 *
 * ALL-scope permits contribute TRUE (null Specification = no filter); an ALL-scope deny
 * yields zero rows. Statements are pre-filtered by principal applicability (role held /
 * PUBLIC floor / grant-derived) using the SAME rule as {@code PolicyEngine.decide()} —
 * the list you can query and the rows you can touch stay the same set.
 *
 * <p><b>Grant/exclusive-ACL scope note (RBAC_V3 §6.3 step 3, §6.4):</b> the grant-existence
 * predicate below covers both regular and {@code exclusive} grants for VISIBILITY — a shared
 * resource shows up in the grantee's list either way. The "exclusive restricts evaluation to
 * level-0 statements" rule DOES matter for membership-derived permits (§6.1 step 2, level ≥ 1):
 * a row carrying an active exclusive grant for this principal is excluded from the membership
 * permit predicate below, mirroring {@code PolicyEngine.decide()}'s
 * {@code !exclusiveAclApplies} gate on {@code membershipStatementsFor()}. Deny statements are
 * exempt from that gate either way — they pierce exclusivity per §6.3/§10.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationSpecificationAdvisor {

    private final PolicyEngine engine;
    private final StaticPolicyRegistry registry;

    /**
     * Build the row filter for a READ list query on {@code entityType}.
     * Returns {@code null} when no filtering is required (unconditional permit, no denies).
     */
    public <T> Specification<T> forList(Class<T> entityType) {
        PrincipalContext.Snapshot principal = PrincipalContext.currentOrAnonymous();
        String entityKey = entityType.getSimpleName();

        // §14.4 — service principals: allowlist only, all-or-nothing per action.
        if (principal.isServicePrincipal()) {
            boolean allowed = principal.servicePrincipalActions().contains("*")
                    || principal.servicePrincipalActions().contains(entityKey + ":READ");
            return allowed ? null : none();
        }

        List<Statement> permits = new ArrayList<>();
        List<Statement> denies = new ArrayList<>();
        for (Statement s : registry.forEntity(entityKey)) {
            if (!engine.appliesToPrincipal(s, principal)) continue;
            if (!engine.matchesAction(s, entityKey, "READ")) continue;
            (s.effect() == Decision.Effect.PERMIT ? permits : denies).add(s);
        }

        // An unconditional ALL permit (no when-condition) makes the compiled-statement side of
        // P_permit TRUE outright — nothing to OR against it.
        boolean unconditionalPermit = permits.stream()
                .anyMatch(s -> s.scope() == Statement.Scope.ALL
                        && (s.whenJava() == null || s.whenJava().isBlank() || "true".equals(s.whenJava())));

        Specification<T> permitSpec;
        if (unconditionalPermit) {
            permitSpec = null; // null = "no filter" = every row passes the permit side
        } else if (!permits.isEmpty()) {
            permitSpec = orCombine(permits, principal);
        } else {
            // No explicit compiled permit: defaultPolicy decides the floor. "read" ⇒ unfiltered
            // (grants/denies still apply below); "deny" ⇒ grants are the ONLY way in.
            Decision def = engine.defaultFor(entityKey, "READ");
            permitSpec = def.isDeny() ? none() : null;
        }

        // permitSpec == null already means "every row passes" (OR-ing grants into a tautology
        // changes nothing) — only wire grants in when there is a real filter (or an explicit
        // deny-all from none()) for them to widen.
        if (!principal.isAnonymous() && permitSpec != null) {
            permitSpec = permitSpec.or(grantPredicate(entityKey, principal));
        }

        Specification<T> denySpec = denies.isEmpty() ? null : notDenies(denies, principal);
        if (denySpec == null) {
            return permitSpec;
        }
        return permitSpec == null ? denySpec : permitSpec.and(denySpec);
    }

    /**
     * EXISTS predicate over active grants (RBAC_V3 §6.4) — exact-resource or entity-wide
     * ({@code target_id = '*'}) grants naming this principal, matching READ, not expired/revoked.
     * Correlated on {@code root.get("id")}; the id-to-string comparison mirrors how grants are
     * created ({@code targetId} is always the resource id as text, see {@code GrantController}).
     * Uses the HQL {@code str()} function, not {@code root.get("id").as(String.class)} — the
     * latter does not reliably emit an actual SQL cast for a numeric path in this Hibernate
     * version, producing "operator does not exist: character varying = bigint" on Postgres for
     * any entity with a {@code Long} id (i.e. every business entity — the hybrid ID strategy
     * only uses UUID for security entities).
     */
    private <T> Specification<T> grantPredicate(String entityKey, PrincipalContext.Snapshot principal) {
        return (root, query, cb) -> {
            Subquery<String> sub = query.subquery(String.class);
            var g = sub.from(Grant.class);
            sub.select(g.get("id"));
            Predicate matchTarget = cb.or(
                    cb.equal(g.get("targetId"), cb.function("str", String.class, root.get("id"))),
                    cb.equal(g.get("targetId"), "*"));
            Predicate matchAction = cb.like(
                    cb.concat(cb.concat(cb.literal(","), g.get("actions")), ","), "%,READ,%");
            sub.where(cb.and(
                    cb.equal(g.get("granteeId"), principal.user().getId()),
                    cb.equal(g.get("targetType"), entityKey),
                    matchTarget,
                    matchAction,
                    cb.isNull(g.get("revokedAt")),
                    cb.or(cb.isNull(g.get("expiresAt")),
                            cb.greaterThan(g.get("expiresAt"), cb.literal(java.time.LocalDateTime.now())))));
            return cb.exists(sub);
        };
    }



    /** OR of every permit statement's criteria predicate. */
    private <T> Specification<T> orCombine(List<Statement> permits, PrincipalContext.Snapshot principal) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            for (Statement s : permits) {
                preds.add(s.criteriaCheck().build(root, cb, principal));
            }
            return cb.or(preds.toArray(new Predicate[0]));
        };
    }

    /** NOT(OR of every deny statement's predicate). ALL-scope deny ⇒ conjunction ⇒ NOT(true) ⇒ zero rows. */
    private <T> Specification<T> notDenies(List<Statement> denies, PrincipalContext.Snapshot principal) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            for (Statement s : denies) {
                preds.add(s.criteriaCheck().build(root, cb, principal));
            }
            return cb.not(cb.or(preds.toArray(new Predicate[0])));
        };
    }

    private <T> Specification<T> none() {
        return (root, query, cb) -> cb.disjunction();
    }
}
