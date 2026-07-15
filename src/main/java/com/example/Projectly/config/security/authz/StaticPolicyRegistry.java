package com.example.Projectly.config.security.authz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Compiled policy statements, built once at boot (RBAC_V3 §6.3 step 0).
 *
 * <p>{@link #buildSnapshot()} is rendered per-project by FreeMarker: every {@code authorization.policies}
 * statement becomes one {@link Statement} whose {@code matches}/{@code criteriaCheck} lambdas embed the
 * {@code ConditionCompiler}-emitted expression as literal Java source (compiled by javac when the
 * generated project builds — there is no runtime interpreter). No collection literal here exceeds
 * a handful of entries per call, so the {@code Map.of} 10-pair cap (Phase 0 B3) cannot recur.
 *
 * <p>Reads are lock-free via a volatile snapshot reference: {@link #buildSnapshot()} runs once at
 * construction; the engine and advisor read {@link #snapshot()}.
 */
@Slf4j
@Component
public class StaticPolicyRegistry {

    private record RegistrySnapshot(Map<String, List<Statement>> byEntity, DefaultEffect defaultEffect) {
    }

    public record DefaultEffect(String policy, Decision.Effect effect, Statement.Scope scope) {
    }

    private final AtomicReference<RegistrySnapshot> ref;

    /** Spring-managed instance — built from this project's compiled {@code authorization:} block. */
    public StaticPolicyRegistry() {
        this.ref = new AtomicReference<>(buildSnapshot());
    }

    private StaticPolicyRegistry(RegistrySnapshot snapshot) {
        this.ref = new AtomicReference<>(snapshot);
    }

    /** In scope for every {@code matches} lambda below — backs {@code now()} / {@code env.now} comparisons. */
    private static long nowMs() {
        return System.currentTimeMillis();
    }

    private static RegistrySnapshot buildSnapshot() {
        Map<String, List<Statement>> byEntity = new LinkedHashMap<>();
        byEntity.put("Project", List.of(
            new Statement(
                Decision.Effect.PERMIT,
                List.of("CREATE", "READ", "UPDATE", "DELETE"),
                Statement.Scope.ALL,
                "true",
                null,
                null,
                false,
                true,
                (resourceObj, principal) -> {
                    com.example.Projectly.bean.core.project.Project resource = (com.example.Projectly.bean.core.project.Project) resourceObj;
                    com.example.Projectly.bean.core.user.User user = principal == null ? null : principal.user();
                    String envTenantId = principal == null ? null : principal.tenantId();
                    String envRequestIp = principal == null ? null : principal.requestIp();
                    return true;
                },
                (root, cb, principal) -> {
                    return cb.conjunction();
                },
                "MANAGER"
            ),
            new Statement(
                Decision.Effect.PERMIT,
                List.of("*"),
                Statement.Scope.ALL,
                "true",
                null,
                null,
                true,
                false,
                (resourceObj, principal) -> {
                    com.example.Projectly.bean.core.project.Project resource = (com.example.Projectly.bean.core.project.Project) resourceObj;
                    com.example.Projectly.bean.core.user.User user = principal == null ? null : principal.user();
                    String envTenantId = principal == null ? null : principal.tenantId();
                    String envRequestIp = principal == null ? null : principal.requestIp();
                    return true;
                },
                (root, cb, principal) -> {
                    return cb.conjunction();
                },
                "ADMIN"
            ),
            new Statement(
                Decision.Effect.PERMIT,
                List.of("READ"),
                Statement.Scope.ALL,
                "true",
                null,
                null,
                false,
                false,
                (resourceObj, principal) -> {
                    com.example.Projectly.bean.core.project.Project resource = (com.example.Projectly.bean.core.project.Project) resourceObj;
                    com.example.Projectly.bean.core.user.User user = principal == null ? null : principal.user();
                    String envTenantId = principal == null ? null : principal.tenantId();
                    String envRequestIp = principal == null ? null : principal.requestIp();
                    return true;
                },
                (root, cb, principal) -> {
                    return cb.conjunction();
                },
                "MEMBER"
            ),
            new Statement(
                Decision.Effect.PERMIT,
                List.of("UPDATE"),
                Statement.Scope.OWN,
                "true",
                null,
                null,
                false,
                false,
                (resourceObj, principal) -> {
                    com.example.Projectly.bean.core.project.Project resource = (com.example.Projectly.bean.core.project.Project) resourceObj;
                    com.example.Projectly.bean.core.user.User user = principal == null ? null : principal.user();
                    String envTenantId = principal == null ? null : principal.tenantId();
                    String envRequestIp = principal == null ? null : principal.requestIp();
                    boolean ownedByUser = user != null
                            && java.util.Objects.equals((resource == null || resource.getOwner() == null) ? null : resource.getOwner().getId(), user.getId());
                    return ownedByUser && (true);
                },
                (root, cb, principal) -> {
                    if (principal == null || principal.user() == null) return cb.disjunction();
                    return cb.equal(root.get("owner").get("id"), principal.user().getId());
                },
                "MEMBER"
            )
        ));
        byEntity.put("Task", List.of(
            new Statement(
                Decision.Effect.PERMIT,
                List.of("CREATE", "READ", "UPDATE", "DELETE"),
                Statement.Scope.ALL,
                "true",
                null,
                null,
                false,
                true,
                (resourceObj, principal) -> {
                    com.example.Projectly.bean.core.task.Task resource = (com.example.Projectly.bean.core.task.Task) resourceObj;
                    com.example.Projectly.bean.core.user.User user = principal == null ? null : principal.user();
                    String envTenantId = principal == null ? null : principal.tenantId();
                    String envRequestIp = principal == null ? null : principal.requestIp();
                    return true;
                },
                (root, cb, principal) -> {
                    return cb.conjunction();
                },
                "MANAGER"
            ),
            new Statement(
                Decision.Effect.PERMIT,
                List.of("*"),
                Statement.Scope.ALL,
                "true",
                null,
                null,
                true,
                false,
                (resourceObj, principal) -> {
                    com.example.Projectly.bean.core.task.Task resource = (com.example.Projectly.bean.core.task.Task) resourceObj;
                    com.example.Projectly.bean.core.user.User user = principal == null ? null : principal.user();
                    String envTenantId = principal == null ? null : principal.tenantId();
                    String envRequestIp = principal == null ? null : principal.requestIp();
                    return true;
                },
                (root, cb, principal) -> {
                    return cb.conjunction();
                },
                "ADMIN"
            ),
            new Statement(
                Decision.Effect.PERMIT,
                List.of("CREATE", "READ"),
                Statement.Scope.ALL,
                "true",
                null,
                null,
                false,
                false,
                (resourceObj, principal) -> {
                    com.example.Projectly.bean.core.task.Task resource = (com.example.Projectly.bean.core.task.Task) resourceObj;
                    com.example.Projectly.bean.core.user.User user = principal == null ? null : principal.user();
                    String envTenantId = principal == null ? null : principal.tenantId();
                    String envRequestIp = principal == null ? null : principal.requestIp();
                    return true;
                },
                (root, cb, principal) -> {
                    return cb.conjunction();
                },
                "MEMBER"
            ),
            new Statement(
                Decision.Effect.PERMIT,
                List.of("UPDATE", "DELETE"),
                Statement.Scope.OWN,
                "true",
                null,
                null,
                false,
                true,
                (resourceObj, principal) -> {
                    com.example.Projectly.bean.core.task.Task resource = (com.example.Projectly.bean.core.task.Task) resourceObj;
                    com.example.Projectly.bean.core.user.User user = principal == null ? null : principal.user();
                    String envTenantId = principal == null ? null : principal.tenantId();
                    String envRequestIp = principal == null ? null : principal.requestIp();
                    boolean ownedByUser = user != null
                            && java.util.Objects.equals((resource == null || resource.getAssignee() == null) ? null : resource.getAssignee().getId(), user.getId());
                    return ownedByUser && (true);
                },
                (root, cb, principal) -> {
                    if (principal == null || principal.user() == null) return cb.disjunction();
                    return cb.equal(root.get("assignee").get("id"), principal.user().getId());
                },
                "MEMBER"
            )
        ));
        byEntity.put("Comment", List.of(
            new Statement(
                Decision.Effect.PERMIT,
                List.of("CREATE", "READ", "UPDATE", "DELETE"),
                Statement.Scope.ALL,
                "true",
                null,
                null,
                false,
                true,
                (resourceObj, principal) -> {
                    com.example.Projectly.bean.core.comment.Comment resource = (com.example.Projectly.bean.core.comment.Comment) resourceObj;
                    com.example.Projectly.bean.core.user.User user = principal == null ? null : principal.user();
                    String envTenantId = principal == null ? null : principal.tenantId();
                    String envRequestIp = principal == null ? null : principal.requestIp();
                    return true;
                },
                (root, cb, principal) -> {
                    return cb.conjunction();
                },
                "MANAGER"
            ),
            new Statement(
                Decision.Effect.PERMIT,
                List.of("*"),
                Statement.Scope.ALL,
                "true",
                null,
                null,
                true,
                false,
                (resourceObj, principal) -> {
                    com.example.Projectly.bean.core.comment.Comment resource = (com.example.Projectly.bean.core.comment.Comment) resourceObj;
                    com.example.Projectly.bean.core.user.User user = principal == null ? null : principal.user();
                    String envTenantId = principal == null ? null : principal.tenantId();
                    String envRequestIp = principal == null ? null : principal.requestIp();
                    return true;
                },
                (root, cb, principal) -> {
                    return cb.conjunction();
                },
                "ADMIN"
            ),
            new Statement(
                Decision.Effect.PERMIT,
                List.of("CREATE", "READ"),
                Statement.Scope.ALL,
                "true",
                null,
                null,
                false,
                false,
                (resourceObj, principal) -> {
                    com.example.Projectly.bean.core.comment.Comment resource = (com.example.Projectly.bean.core.comment.Comment) resourceObj;
                    com.example.Projectly.bean.core.user.User user = principal == null ? null : principal.user();
                    String envTenantId = principal == null ? null : principal.tenantId();
                    String envRequestIp = principal == null ? null : principal.requestIp();
                    return true;
                },
                (root, cb, principal) -> {
                    return cb.conjunction();
                },
                "MEMBER"
            ),
            new Statement(
                Decision.Effect.PERMIT,
                List.of("UPDATE", "DELETE"),
                Statement.Scope.OWN,
                "true",
                null,
                null,
                false,
                true,
                (resourceObj, principal) -> {
                    com.example.Projectly.bean.core.comment.Comment resource = (com.example.Projectly.bean.core.comment.Comment) resourceObj;
                    com.example.Projectly.bean.core.user.User user = principal == null ? null : principal.user();
                    String envTenantId = principal == null ? null : principal.tenantId();
                    String envRequestIp = principal == null ? null : principal.requestIp();
                    boolean ownedByUser = user != null
                            && java.util.Objects.equals((resource == null || resource.getAuthor() == null) ? null : resource.getAuthor().getId(), user.getId());
                    return ownedByUser && (true);
                },
                (root, cb, principal) -> {
                    if (principal == null || principal.user() == null) return cb.disjunction();
                    return cb.equal(root.get("author").get("id"), principal.user().getId());
                },
                "MEMBER"
            )
        ));
        String defaultPolicy = "deny";
        DefaultEffect def = new DefaultEffect(
                defaultPolicy,
                "read".equalsIgnoreCase(defaultPolicy) ? Decision.Effect.PERMIT : Decision.Effect.DENY,
                Statement.Scope.ALL);
        Map<String, List<Statement>> cleaned = new LinkedHashMap<>();
        byEntity.forEach((k, v) -> cleaned.put(k, Collections.unmodifiableList(new ArrayList<>(v))));
        return new RegistrySnapshot(Collections.unmodifiableMap(cleaned), def);
    }

    // ===== Builder — used by generated tests to assemble a registry without full Spring context =====

    public static class Builder {
        private final Map<String, List<Statement>> byEntity = new LinkedHashMap<>();
        private DefaultEffect defaultEffect = new DefaultEffect("deny", Decision.Effect.DENY, Statement.Scope.ALL);

        public Builder add(String entityName, Statement statement) {
            byEntity.computeIfAbsent(entityName, k -> new ArrayList<>()).add(statement);
            return this;
        }

        public Builder setDefault(String policy, Decision.Effect effect, Statement.Scope scope) {
            this.defaultEffect = new DefaultEffect(policy, effect, scope);
            return this;
        }

        public StaticPolicyRegistry build() {
            Map<String, List<Statement>> cleaned = new LinkedHashMap<>();
            byEntity.forEach((k, v) -> cleaned.put(k, Collections.unmodifiableList(new ArrayList<>(v))));
            return new StaticPolicyRegistry(new RegistrySnapshot(Collections.unmodifiableMap(cleaned), defaultEffect));
        }
    }

    /** Render-time factory — used by generated tests to assemble a custom registry. */
    public static Builder builder() {
        return new Builder();
    }

    /** Public read-only view used by the engine and advisor at runtime. */
    public RegistrySnapshot snapshot() {
        return ref.get();
    }

    public List<Statement> forEntity(String entityName) {
        RegistrySnapshot s = ref.get();
        if (entityName == null) return List.of();
        List<Statement> list = s.byEntity().get(entityName);
        return list == null ? List.of() : list;
    }

    public DefaultEffect defaultDecision(String entityName) {
        return ref.get().defaultEffect();
    }
}
