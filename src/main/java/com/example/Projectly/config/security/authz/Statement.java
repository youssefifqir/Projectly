package com.example.Projectly.config.security.authz;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Compiled policy statement — emitted into every generated project (RBAC_V3 §6.3).
 *
 * <p>One instance per YAML statement. The static {@link StaticPolicyRegistry} builds an
 * immutable list of these at boot via {@code builders} so generated source never holds
 * scattered collection literals (which would fall foul of the {@code Map.of} 10-entry cap
 * fixed in Phase 0 B3).
 *
 * <p>{@link #whenJava}, {@link #scopeJava} and {@link #scopeCriteria} are the human-readable
 * compiled-expression strings (dev/staging "explain" mode, matrix-test assertions) — they are
 * DATA, never executed. The actual enforcement is {@link #matches} and {@link #criteriaCheck}:
 * real lambdas whose bodies are the SAME compiled expression text, embedded as literal Java
 * source at generation time by {@code StaticPolicyRegistry.ftl} (RBAC_V3 §7: "no runtime
 * expression interpreter" — a lambda compiled by javac when the generated project builds
 * satisfies that; a string field alone cannot).
 */
public record Statement(
        Decision.Effect effect,
        List<String> actions,
        Scope scope,
        String whenJava,
        String scopeJava,
        String scopeCriteria,
        boolean wildcard,
        boolean sensitive,
        BiPredicate<Object, PrincipalContext.Snapshot> matches,
        CriteriaCheck criteriaCheck,
        String role) {

    /** Non-role-authored statements (grants, defaults) pass {@code null} for {@link #role}. */
    public Statement(Decision.Effect effect, List<String> actions, Scope scope, String whenJava, String scopeJava,
                      String scopeCriteria, boolean wildcard, boolean sensitive,
                      BiPredicate<Object, PrincipalContext.Snapshot> matches, CriteriaCheck criteriaCheck) {
        this(effect, actions, scope, whenJava, scopeJava, scopeCriteria, wildcard, sensitive, matches, criteriaCheck, null);
    }

    /** Scope strategy enum. ALL/OWN/EXPRESSION are implemented in Phase 1; CONTAINER/GRANT are rejected by the validator. */
    public enum Scope { ALL, OWN, EXPRESSION, CONTAINER, GRANT }

    /** Row-filtering face of {@link #matches} — builds a Criteria predicate for list queries. */
    @FunctionalInterface
    public interface CriteriaCheck {
        Predicate build(Root<?> root, CriteriaBuilder cb, PrincipalContext.Snapshot principal);
    }
}
