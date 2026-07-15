package com.example.Projectly.config.security.authz;

/**
 * Outcome of a single {@code PolicyEngine.decide(...)} call (RBAC_V3 §6.3).
 *
 * <p>An immutable record carrying the effect and the origin chain that produced it.
 * The {@code origin} is non-null even on the default-policy path so the JSON 403 body
 * (when {@code authorization.explain: true}) can show why the request was denied.
 *
 * <p>This is intentionally NOT a Spring class — generated code consumes it directly from
 * {@code PolicyEngine.decide(...)} and {@code StaticPolicyRegistry.snapshot()}.
 */
public record Decision(Effect effect, Origin origin) {

    public boolean isPermit() {
        return effect == Effect.PERMIT;
    }

    public boolean isDeny() {
        return effect == Effect.DENY;
    }

    /** Convenience — true when any non-default deny produced this outcome (used for explain-mode explain). */
    public boolean hasDenyOrigin() {
        return origin != null && origin.decisionStatement() != null
                && origin.decisionStatement().effect() == Effect.DENY;
    }

    public enum Effect { PERMIT, DENY }
}
