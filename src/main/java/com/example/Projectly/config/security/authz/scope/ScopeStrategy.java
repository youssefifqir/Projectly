package com.example.Projectly.config.security.authz.scope;

import com.example.Projectly.config.security.authz.PrincipalContext;

/**
 * Single-faced scope strategy — one runtime predicate (RBAC_V3 §6.3 step 2).
 *
 * <p>For list/criteria row filtering the {@code AuthorizationSpecificationAdvisor} emits
 * the equivalent {@code Specification} directly, derived from the precomputed
 * {@code scopeCriteria} string on the {@code Statement} record. Strategies therefore
 * answer only the point-check question — there is no per-scope {@code toPredicate()}
 * duplication that must stay in sync.
 *
 * <p>Resource is typed {@code Object} so a single strategy covers every entity type
 * without per-entity subclasses. Implementations cast safely.
 *
 * <p>This interface is generated exactly once per project; the generator's
 * {@code AuthzValidator} guarantees no strategy is referenced that isn't on the
 * allow-list (ALL/OWN/EXPRESSION in Phase 1).
 */
public interface ScopeStrategy {

    /** True iff this strategy accepts the resource for the active principal. */
    boolean matches(Object resource, PrincipalContext.Snapshot principal);

    /** True iff this strategy accepts ALL resources the query returns (e.g. ALL scope). */
    default boolean matchesAll() {
        return false;
    }
}
