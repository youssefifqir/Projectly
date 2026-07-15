package com.example.Projectly.config.security.authz;

import com.example.Projectly.config.security.authz.scope.AllScope;
import com.example.Projectly.config.security.authz.scope.ScopeStrategy;
import com.example.Projectly.config.security.authz.grant.Grant;
import com.example.Projectly.config.security.authz.grant.GrantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The single decision point for v3 authorization (RBAC_V3 §6.3 — deny-overrides).
 *
 * <p>{@code decide(principal, action, resource)} is invoked by:
 * <ul>
 *   <li>{@code @Authorize} aspect for point checks (single-resource gets).</li>
 *   <li>{@code AuthorizationSpecificationAdvisor} for list/criteria filtering (resource == null).</li>
 *   <li>The generated {@code PermissionMatrixTest} to verify the matrix end-to-end.</li>
 * </ul>
 *
 * <p>Combining algorithm ({@link #decide}):
 * <ol>
 *   <li>Walk the entity's compiled statement list in declared order.</li>
 *   <li>A statement matches when its action set contains the requested action
 *       ({@code wildcard=true} matches anything except sensitive actions).</li>
 *   <li>If any matched statement carries {@link Decision.Effect#DENY}, deny-overrides short-circuits
 *       and DENY wins — the discovered chaining is preserved on the returned {@link Origin}.</li>
 *   <li>If any matched statement carries PERMIT, the engine returns PERMIT plus a list of
 *       candidate scopes to OR-combine. The caller decides which scopes the resource satisfies
 *       (single-resource invocation: check the resource; list invocation: row-by-row in the advisor).</li>
 *   <li>If no statement matched: {@code defaultPolicy} from {@code authorization:}.</li>
 * </ol>
 *
 * <p>Sensitive actions are matched by exact string, never by wildcards ({@link Statement#wildcard()}
 * returns false on those).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyEngine {

    private final StaticPolicyRegistry registry;
    private final AllScope allScope;
    private final GrantRepository grantRepository;

    /**
     * Single-point decision. {@code resource} may be null (list/criteria invocation) — the engine
     * returns the matching PERMITs without resolving scope; the caller applies each scope's
     * row filter via {@code AuthorizationSpecificationAdvisor}. Entity key is derived from
     * {@code resource}'s class — null resolves to the registry wildcard, matching nothing.
     */
    public Decision decide(PrincipalContext.Snapshot principal, String action, Object resource) {
        return decide(principal, action, resource, entityKeyFor(resource));
    }

    /**
     * Point-check with an explicit entity-key override. Used by the {@code @Authorize.Permit}
     * aspect: when the annotated method only received an id (not the entity instance, e.g.
     * {@code findById(Long)}), {@code resource} is null and {@code entityKeyFor(null)} would
     * silently resolve to the registry wildcard {@code "*"} — which has no compiled statements —
     * falling through to {@code defaultPolicy} regardless of the annotation's declared entity.
     * The annotation's {@code "<Entity>:<ACTION>"} value is always accurate (code-generated), so
     * passing it here beats guessing from a possibly-absent resource.
     */
    public Decision decide(PrincipalContext.Snapshot principal, String action, Object resource, String entityKeyHint) {
        Decision result = decideInternal(principal, action, resource, entityKeyHint);
        return result;
    }

    private Decision decideInternal(PrincipalContext.Snapshot principal, String action, Object resource, String entityKey) {
        if (principal == null) {
            log.warn("decide() invoked with no principal — denying request.");
            return new Decision(Decision.Effect.DENY, Origin.defaultDeny(entityKey, action));
        }
        // RBAC_V3 §14.4 — service principals bypass everything else: no ownership, no
        // memberships, no grants, just the key's own allowlist.
        if (principal.isServicePrincipal()) {
            boolean allowed = principal.servicePrincipalActions().contains("*")
                    || principal.servicePrincipalActions().contains(entityKey + ":" + action);
            return new Decision(allowed ? Decision.Effect.PERMIT : Decision.Effect.DENY,
                    allowed ? Origin.defaultAllow(entityKey, action) : Origin.defaultDeny(entityKey, action));
        }
        // Resolution path: no-arg resourceIsNull() calls below treat resource as the wildcard slot,
        // matching "any resource of this entity" criteria-side. We never throw at this layer.
        List<Statement> statements = new ArrayList<>(registry.forEntity(entityKey));
        boolean exclusiveAclApplies = false;
        // RBAC_V3 §6.1 step 3 — active grants on this exact resource. Point-check only (resource !=
        // null); list/criteria invocations don't get grant-derived rows in Phase 2 (under-inclusive,
        // never over-inclusive — a safe default, not a security gap).
        if (resource != null && !principal.isAnonymous()) {
            statements.addAll(grantStatementsFor(principal, entityKey, resource));
            exclusiveAclApplies = hasExclusiveGrant(principal, entityKey, resource);
        }
        // Coarse widening for the id-only point-check case (findById(Long), findByRef(String),
        // deleteById(Long)) — same shape and reasoning as the membership widening below:
        // grantStatementsFor() above only runs when resource != null (it needs the resource's own
        // id to query grants against), so a grantee with NO role and NO membership — access
        // entirely through a Grant — was denied here before the row-filtered fetch (which DOES
        // evaluate the real per-resource grant predicate via AuthorizationSpecificationAdvisor)
        // ever ran. This only admits candidates; it does not itself decide anything.
        if (resource == null && !principal.isAnonymous()) {
            statements.addAll(coarseGrantStatementsFor(principal, entityKey));
        }
        if (statements.isEmpty()) {
            return defaultFor(entityKey, action);
        }
        List<Statement> candidates = new ArrayList<>();
        Statement denyWins = null;
        for (Statement s : statements) {
            // A role-authored statement only applies when the principal actually HOLDS that role
            // (role == null means grant-derived / synthetic — already principal-specific).
            // Without this check every user matches every role's statements — privilege escalation.
            if (!appliesToPrincipal(s, principal)) {
                continue;
            }
            if (!matchesAction(s, entityKey, action)) {
                continue;
            }
            // Point check (resource != null): the statement must also satisfy its scope + when
            // (RBAC_V3 §6.2 "scope check passes"). List/criteria invocation (resource == null)
            // defers row-level scoping to AuthorizationSpecificationAdvisor — every action-matching
            // statement is a candidate there, not just the ones whose scope trivially holds.
            if (resource != null && !s.matches().test(resource, principal)) {
                continue;
            }
            candidates.add(s);
            if (s.effect() == Decision.Effect.DENY) {
                denyWins = s;
                break; // deny-overrides short-circuit
            }
        }
        if (denyWins != null) {
            return new Decision(Decision.Effect.DENY,
                    new Origin(entityKey, action, candidates, denyWins, false));
        }
        if (candidates.isEmpty()) {
            return defaultFor(entityKey, action);
        }
        // Return PERMIT with the FIRST matching PERMIT as the decision statement.
        Statement permitWins = candidates.get(0);
        return new Decision(Decision.Effect.PERMIT,
                new Origin(entityKey, action, candidates, permitWins, false));
    }

    /**
     * RBAC_V3 §6.1 step 1 — statement applicability by principal:
     * <ul>
     *   <li>{@code role == null} — grant-derived/synthetic, already principal-specific → applies;</li>
     *   <li>{@code role == "PUBLIC"} — the anonymous pseudo-role's statements are the floor for
     *       EVERY principal (an authenticated user is never worse off than anonymous);</li>
     *   <li>otherwise — the principal must hold the role.</li>
     * </ul>
     */
    boolean appliesToPrincipal(Statement s, PrincipalContext.Snapshot principal) {
        if (s.role() == null || "PUBLIC".equalsIgnoreCase(s.role())) {
            return true;
        }
        return principal.roleNames() != null && principal.roleNames().stream()
                .anyMatch(r -> r.equalsIgnoreCase(s.role()) || ("ROLE_" + s.role()).equalsIgnoreCase(r));
    }

    /**
     * True iff {@code action} matches any in {@code s.actions()} (wildcard-aware).
     *
     * <p>RBAC_V3 §14.2: sensitivity is a property of the requested action (declared globally in
     * {@code authorization.sensitive}), not of the statement — a wildcard statement must reject
     * whichever specific sensitive action is being asked about, even though the SAME statement
     * matches every other (non-sensitive) action fine. {@code s.sensitive()} (statement-level) is
     * a coarser signal used only for audit sampling, not for this check.
     */
    boolean matchesAction(Statement s, String entityKey, String action) {
        if (action == null) return false;
        if (s.wildcard()) {
            return !isSensitiveAction(entityKey, action);
        }
        for (String a : s.actions()) {
            if (a.equalsIgnoreCase(action)) {
                return true;
            }
        }
        return false;
    }

    /** Best-effort entity resolution when the resource is non-null. The registry ignores unknown keys. */
    String entityKeyFor(Object resource) {
        return resource == null ? REGISTRY_WILDCARD : resource.getClass().getSimpleName();
    }

    /** {@code authorization.sensitive} — declared once, checked on every wildcard match (RBAC_V3 §14.2). */
    private static final java.util.Set<String> SENSITIVE_ACTIONS = java.util.Set.of(
            "PROJECT:DELETE"
    );

    private boolean isSensitiveAction(String entityKey, String action) {
        return SENSITIVE_ACTIONS.contains((entityKey + ":" + action).toUpperCase())
                || SENSITIVE_ACTIONS.contains("*:" + action.toUpperCase());
    }

    /**
     * One synthesized PERMIT Statement per active grant — deny-overrides still applies on top of
     * these. Includes wildcard ({@code target_id = '*'}) grants — whole-entity-type access, e.g.
     * a break-glass elevation session (RBAC_V3 §14.1) — alongside exact-resource grants.
     */
    private List<Statement> grantStatementsFor(PrincipalContext.Snapshot principal, String entityKey, Object resource) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<Grant> grants = new ArrayList<>(grantRepository.findActiveWildcardGrants(principal.user().getId(), entityKey, now));
        String resourceId = resourceIdOf(resource);
        if (resourceId != null) {
            grants.addAll(grantRepository.findActiveGrants(principal.user().getId(), entityKey, resourceId, now));
        }
        List<Statement> out = new ArrayList<>();
        for (Grant grant : grants) {
            out.add(new Statement(
                    Decision.Effect.PERMIT,
                    grant.getActions(),
                    Statement.Scope.GRANT,
                    "true",
                    null,
                    null,
                    false,
                    false,
                    (r, p) -> true,
                    (root, cb, p) -> cb.disjunction()));
        }
        return out;
    }

    /** RBAC_V3 §6.3 step 3 — does an active EXCLUSIVE grant exist for this principal on this exact resource? */
    private boolean hasExclusiveGrant(PrincipalContext.Snapshot principal, String entityKey, Object resource) {
        String resourceId = resourceIdOf(resource);
        if (resourceId == null) return false;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return grantRepository.findActiveGrants(principal.user().getId(), entityKey, resourceId, now)
                .stream().anyMatch(Grant::isExclusive);
    }

    /**
     * Coarse, entity-type-wide grant statements — used ONLY when {@code resource} is null (id-only
     * point-check methods). Unlike {@link #grantStatementsFor}, this can't correlate to one exact
     * resource, so it must not be trusted as the real decision: it only lets a legitimate grantee's
     * request reach the row-filtered fetch, which evaluates the actual per-resource grant predicate
     * (the same one {@code AuthorizationSpecificationAdvisor} uses for list queries).
     */
    private List<Statement> coarseGrantStatementsFor(PrincipalContext.Snapshot principal, String entityKey) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<Grant> grants = grantRepository.findActiveGrantsForType(principal.user().getId(), entityKey, now);
        List<Statement> out = new ArrayList<>();
        for (Grant grant : grants) {
            out.add(new Statement(
                    Decision.Effect.PERMIT,
                    grant.getActions(),
                    Statement.Scope.GRANT,
                    "true",
                    null,
                    null,
                    false,
                    false,
                    (r, p) -> true,
                    (root, cb, p) -> cb.disjunction()));
        }
        return out;
    }


    /** Reflective {@code getId()} + {@code toString()} — every generated entity has one. Null-safe. */
    private String resourceIdOf(Object resource) {
        if (resource == null) return null;
        try {
            Object id = resource.getClass().getMethod("getId").invoke(resource);
            return id == null ? null : id.toString();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }



    Decision defaultFor(String entityKey, String action) {
        // RBAC_V3 §5: defaultPolicy is "deny" or "read" only — "allow" is not a valid value
        // (explicitly rejected by design). §6.3 step 5: "read" only ever permits the READ action.
        StaticPolicyRegistry.DefaultEffect de = registry.defaultDecision(entityKey);
        if ("read".equalsIgnoreCase(de.policy()) && "READ".equalsIgnoreCase(action)) {
            return new Decision(Decision.Effect.PERMIT, Origin.defaultAllow(entityKey, action));
        }
        return new Decision(Decision.Effect.DENY, Origin.defaultDeny(entityKey, action));
    }

    /** Singleton key for list-wide decisions; the registry's {@code getDefault()} is consulted. */
    public static final String REGISTRY_WILDCARD = "*";
}
