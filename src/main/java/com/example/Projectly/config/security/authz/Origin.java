package com.example.Projectly.config.security.authz;

import java.util.List;

/**
 * Origin trace for a {@link Decision} (RBAC_V3 §6.3 + §13 "explain mode").
 *
 * <p>An {@code Origin} lists the candidate statements the engine considered and indicates
 * which one won the deny-overrides combining algorithm. The full chain is preserved so
 * debug-mode 403 responses can show the analyst every statement that matched (with the
 * effect each carried) before the engine reduced them.
 *
 * <p>Default-policy decisions carry an origin with an empty list and {@code decisionStatement == null}.
 */
public record Origin(
        String entityName,
        String action,
        List<Statement> candidates,
        Statement decisionStatement,
        boolean fromDefaultPolicy) {

    /** True iff there is a non-default decision (i.e. some authored rule matched). */
    public boolean hasDecisionStatement() {
        return decisionStatement != null;
    }

    public static Origin defaultDeny(String entityName, String action) {
        return new Origin(entityName, action, List.of(), null, true);
    }

    public static Origin defaultAllow(String entityName, String action) {
        return new Origin(entityName, action, List.of(), null, true);
    }
}
