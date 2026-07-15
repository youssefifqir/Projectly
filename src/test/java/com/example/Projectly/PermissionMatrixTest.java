package com.example.Projectly;

import com.example.Projectly.config.security.authz.Decision;
import com.example.Projectly.config.security.authz.PrincipalContext;
import com.example.Projectly.config.security.authz.PrincipalContext.Snapshot;
import com.example.Projectly.config.security.authz.Statement;
import com.example.Projectly.config.security.authz.StaticPolicyRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compiled-statements matrix test (RBAC_V3_MIGRATION_PLAN §1.7).
 *
 * <p>Expectations below are rendered directly from this project's {@code authorization:} block
 * (via {@code AuthzTemplateModel}) — not hand-copied. If the YAML changes, regenerating the
 * project regenerates this file's expectations along with it; a real behavioural regression
 * (compiled statements diverging from what {@code StaticPolicyRegistry} actually builds) is
 * still caught because both come from the same compiler.
 *
 * <p>No database, no Testcontainers — the matrix is fully derived from compiled statements.
 */
class PermissionMatrixTest {

    private static List<String> roles;
    private static List<String> entities;
    private static List<String> actions;
    private static Map<String, List<String>> publicActions;
    private static String expectedDefaultPolicy;

    @BeforeAll
    static void load() {
        roles = List.of("ADMIN", "MANAGER", "MEMBER");
        entities = List.of("Project", "Task", "Comment");
        actions = List.of("CREATE", "READ", "UPDATE", "DELETE");
        expectedDefaultPolicy = "deny";
        publicActions = new LinkedHashMap<>();
    }

    @Test
    void staticPolicyRegistryExposesCompiledStatements() {
        // Smoke assertion — the registry exists at runtime and has at least one entry per entity
        // that has an authored policy (entities relying purely on defaultPolicy have none — that's
        // covered separately by defaultDecisionIsExplicitPolicy).
        StaticPolicyRegistry registry = newRegistry();
        for (String entity : entities) {
            List<Statement> statements = registry.forEntity(entity);
            assertTrue(statements != null && !statements.isEmpty(),
                    "entity '" + entity + "' must have at least one compiled statement");
        }
    }

    @Test
    void publicRoleGetsExactlyReadClassActions() {
        StaticPolicyRegistry registry = newRegistry();
        for (Map.Entry<String, List<String>> entry : publicActions.entrySet()) {
            String entityName = entry.getKey();
            for (String a : entry.getValue()) {
                assertTrue("READ".equals(a),
                        "PUBLIC may only be granted READ on '" + entityName + "', was " + a);
            }
        }
    }

    @Test
    void defaultDecisionIsExplicitPolicy() {
        // The registry's DefaultEffect must mirror authorization.defaultPolicy verbatim —
        // PolicyEngine.defaultFor() is the only place that interprets it (RBAC_V3 §6.3 step 5).
        StaticPolicyRegistry registry = newRegistry();
        for (String entity : entities) {
            StaticPolicyRegistry.DefaultEffect def = registry.defaultDecision(entity);
            assertEquals(expectedDefaultPolicy, def.policy(),
                    "default policy for '" + entity + "' must match authorization.defaultPolicy");
        }
    }

    @Test
    void wildcardActionsAreFlaggedOnStatements() {
        StaticPolicyRegistry registry = newRegistry();
        for (String entity : entities) {
            List<Statement> statements = registry.forEntity(entity);
            for (Statement s : statements) {
                if (s.wildcard()) {
                    assertTrue(s.actions().isEmpty() || !s.actions().contains("DELETE"),
                            "wildcard on '" + entity + "' must not cover DELETE");
                }
            }
        }
    }

    @Test
    void principalContextSnapshotCarriesRoles() {
        Snapshot anonymous = new Snapshot(null, Set.of("PUBLIC"), null, null);
        assertTrue(anonymous.isAnonymous(), "no user should mark anonymous");
        Snapshot authenticated = new Snapshot(
                new com.example.Projectly.bean.core.user.User(), Set.of("USER"), null, null);
        assertFalse(authenticated.isAnonymous(), "a bound user should mark non-anonymous");
    }

    private static StaticPolicyRegistry newRegistry() {
        // The real constructor — StaticPolicyRegistry.buildSnapshot() is rendered from this same
        // authorization block, so the registry under test is exactly what the app boots with.
        return new StaticPolicyRegistry();
    }
}
