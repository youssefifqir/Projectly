package com.example.Projectly.config.security.authz;

import com.example.Projectly.bean.core.user.User;

import java.util.Set;

/**
 * Request-scoped principal context (RBAC_V3 §6.3 step 1).
 *
 * <p>Populated once per request by {@code AnonymousPrincipalSupport} or the security
 * filter chain and consumed by {@code PolicyEngine.decide(...)} and {@code ScopeStrategy}
 * implementations. Method-name contracts must match what {@code ConditionCompiler}
 * emits — the {@code user} and {@code envTenantId} / {@code envRequestIp} / {@code nowMs()}
 * identifiers are passed through here verbatim.
 *
 * <p>Thread-local by design: generated code is request-handling only; async invocation
 * must explicitly propagate via {@link #bindFor(Object, Runnable)}.
 */
public final class PrincipalContext {

    private static final ThreadLocal<Snapshot> CURRENT = new ThreadLocal<>();

    private PrincipalContext() {
    }

    public record Snapshot(User user, Set<String> roleNames, String tenantId, String requestIp,
                            Set<String> servicePrincipalActions, Set<String> containerRoleNames) {
        /** Existing 4-arg call sites are unaffected — {@code servicePrincipalActions}/{@code containerRoleNames} default empty. */
        public Snapshot(User user, Set<String> roleNames, String tenantId, String requestIp) {
            this(user, roleNames, tenantId, requestIp, null, Set.of());
        }

        /** Existing 5-arg call sites (service principals) are unaffected — {@code containerRoleNames} defaults empty. */
        public Snapshot(User user, Set<String> roleNames, String tenantId, String requestIp,
                         Set<String> servicePrincipalActions) {
            this(user, roleNames, tenantId, requestIp, servicePrincipalActions, Set.of());
        }

        public boolean isAnonymous() {
            return user == null;
        }

        /** Null-safe: {@code containerRoleNames} may be {@code null} from an older call site. */
        public Set<String> containerRoleNames() {
            return containerRoleNames == null ? Set.of() : containerRoleNames;
        }

        /** RBAC_V3 §14.4 — API-key principal with a scoped action allowlist. No ownership, no memberships, no grants. */
        public boolean isServicePrincipal() {
            return servicePrincipalActions != null;
        }
    }

    /** Binds a snapshot for the current thread; restored automatically by {@code try-with-resources}. */
    public static Scope bind(Snapshot snapshot) {
        Snapshot previous = CURRENT.get();
        CURRENT.set(snapshot);
        return previous == null ? new Scope(null) : new Scope(previous);
    }

    /** Convenience: snapshot read, or null when nothing is bound (test contexts, scheduled jobs). */
    public static Snapshot currentOrNull() {
        return CURRENT.get();
    }

    /** Convenience: snapshot read, or anonymous/empty when nothing is bound. */
    public static Snapshot currentOrAnonymous() {
        Snapshot s = CURRENT.get();
        return s != null ? s : new Snapshot(null, Set.of(), null, null);
    }

    /** AutoCloseable handle returned by {@link #bind(Snapshot)} so callers can {@code try}-restore. */
    public static final class Scope implements AutoCloseable {
        private final Snapshot previous;

        Scope(Snapshot previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    /** Run {@code body} in a {@code PrincipalContext} bound to {@code snapshot}. */
    public static void bindFor(Snapshot snapshot, Runnable body) {
        try (Scope scope = bind(snapshot)) {
            body.run();
        }
    }
}
