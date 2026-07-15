package com.example.Projectly.config.security.authz.scope;

import com.example.Projectly.config.security.authz.PrincipalContext;
import org.springframework.stereotype.Component;

/**
 * {@code EXPRESSION} scope strategy (RBAC_V3 §6.2, §7).
 *
 * <p>The strategy is the runtime face of a compiled {@code when:} condition. The generator
 * emits one instance per {@code when} expression; the {@code match} lambda body is the
 * precomputed {@code ConditionCompiler}-emitted Java expression (single source of truth —
 * the same expression feeds {@code AuthorizationSpecificationAdvisor.ifPredicate()}).
 *
 * <p>Because each EXPRESSION statement has its own scoped expression, this is an abstract
 * base — generated subclasses (created by FreeMarker {@code <#macro>}) override
 * {@link #matchesExpression(Object, PrincipalContext.Snapshot)} with the precomputed
 * expression body. There is no runtime interpreter beyond the expression itself.
 */
@Component("expressionScopeStrategy")
public abstract class ExpressionScope implements ScopeStrategy {

    @Override
    public final boolean matches(Object resource, PrincipalContext.Snapshot principal) {
        if (resource == null) return false;
        return matchesExpression(resource, principal);
    }

    /**
     * Subclasses implement the body of the {@code when} expression here. The body is the
     * precomputed Java boolean expression emitted by the generator (e.g. {@code resource.X == user.Y
     * && resource.Z > 5}). Resource is non-null (guarded above) and may be cast to the entity
     * type without an {@code instanceof} check — generated code emits the cast.
     */
    protected abstract boolean matchesExpression(Object resource, PrincipalContext.Snapshot principal);
}
