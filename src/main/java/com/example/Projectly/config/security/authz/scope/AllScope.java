package com.example.Projectly.config.security.authz.scope;

import com.example.Projectly.config.security.authz.PrincipalContext;
import org.springframework.stereotype.Component;

/**
 * {@code ALL} scope strategy (RBAC_V3 §6.2). Accepts every resource for every principal —
 * used as the default when a statement carries no {@code scope:} and no {@code when:}.
 *
 * <p>Bean-named {@code allScopeStrategy} so the engine resolves it without reflection.
 */
@Component("allScopeStrategy")
public class AllScope implements ScopeStrategy {

    @Override
    public boolean matches(Object resource, PrincipalContext.Snapshot principal) {
        return true;
    }

    @Override
    public boolean matchesAll() {
        return true;
    }
}
