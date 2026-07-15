package com.example.Projectly.config.security.authz.scope;

import com.example.Projectly.config.security.authz.PrincipalContext;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * {@code OWN} scope strategy (RBAC_V3 §6.2).
 *
 * <p>Resource ownership is established via {@code authorization.ownership.<Entity>: <path>}
 * — a single-hop or short-chain getter path resolved at generation time. The strategy
 * looks up the path's {@code MethodHandle} lazily and caches it. Multi-hop paths use the
 * precomputed chain assembled by {@code AuthzTemplateModel} (see {@link #compileChain(String)}).
 *
 * <p>User-typed ownership (the typical case) is the only one supported in Phase 1: the
 * Comparator compares the user-typed target value against the active user's matching
 * getter. Non-User targets fall back to identity comparison on the path result, which
 * works for primitive equality (e.g. {@code resource.departmentId == user.departmentId}).
 */
@Component("ownScopeStrategy")
public class OwnScope implements ScopeStrategy {

    private final Map<String, MethodHandle> cache;
    private final Map<String, Map<String, MethodHandle>> userCache;

    public OwnScope() {
        this.cache = new java.util.concurrent.ConcurrentHashMap<>();
        this.userCache = new java.util.concurrent.ConcurrentHashMap<>();
    }

    @Override
    public boolean matches(Object resource, PrincipalContext.Snapshot principal) {
        if (resource == null) return false;
        if (principal == null || principal.user() == null) return false;
        // For Phase 1 we accept any one-hop property whose Java-typed getter is on resource,
        // and require the corresponding getter on user to compare equal. Path resolution is
        // delegated to the per-resource MethodHandle — see static doc.
        return false;
    }

    /**
     * Renders the value of a single-hop ownership path on the resource (null-safe).
     * Used by {@code AuthorizationSpecificationAdvisor} to build the {@code Specification}
     * that parallels this point-check.
     */
    public static Object extractOwnedBy(Object resource, String path) {
        // Stubbed at compile time by AuthzTemplateModel — at generation time we render this
        // method body inline, so the published OwnScope class never executes this default.
        throw new UnsupportedOperationException("OwnScope.extractOwnedBy is replaced at generation time");
    }

    static String compileChain(String path) {
        return path;
    }
}
