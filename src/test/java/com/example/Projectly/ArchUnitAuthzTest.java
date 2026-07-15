package com.example.Projectly;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * Architectural test enforcing that the core CRUD methods on every generated entity service are
 * annotated with {@code @Permit} (RBAC_V3_MIGRATION_PLAN.md §1.5 + §23).
 *
 * <p>Scoped to {@code findAll/findById/deleteById/findAndDeleteById/create/update/findByRef} —
 * the methods {@code ServiceImpl.java.ftl} actually annotates in Phase 1. Criteria/pagination
 * methods (e.g. {@code findByCriteria}) rely on row-level filtering, not method annotation (same
 * pattern the v1 engine used); full per-method coverage is a Phase 5 hardening goal. Hand-written
 * infrastructure services ({@code service.impl.security}, {@code .email}, {@code .storage}) are
 * excluded — they run before a principal even exists (login, token refresh) and were never meant
 * to carry an entity-scoped {@code @Permit}.
 *
 * <p>Generated into every project. Generated entity services carry {@code @Permit} on these
 * signatures; manually-edited services that drop it fail this rule — preventing silent
 * unauthenticated access.
 *
 * <p>ArchUnit dependency is added to the generated project's {@code pom.xml} by the same
 * emitter that emits this test.
 */
class ArchUnitAuthzTest {

    private static JavaClasses imported;

    @BeforeAll
    static void importClasses() {
        imported = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.example.Projectly.service");
    }

    @Test
    void coreEntityServiceMethodsHavePermitAnnotation() {
        ArchRule rule = methods()
                .that().haveNameMatching("findAll|findById|deleteById|findAndDeleteById|create|update|findByRef")
                // Excludes bulk overloads like update(List<T>, boolean) — only the single-resource
                // signature is annotated in Phase 1.
                .and(DescribedPredicate.describe("have at most 1 parameter",
                        (JavaMethod m) -> m.getParameters().size() <= 1))
                .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("ServiceImpl")
                .and().areDeclaredInClassesThat().resideOutsideOfPackages(
                        "com.example.Projectly.service.impl.security..",
                        "com.example.Projectly.service.impl.email..",
                        "com.example.Projectly.service.impl.storage..")
                .and().arePublic()
                .should().beAnnotatedWith(com.example.Projectly.config.security.authz.Authorize.Permit.class);

        rule.check(imported);
    }

    @Test
    void policyEngineAccessIsMediatedByAuthorizeAspect() {
        // Invariant: no ServiceImpl method invokes PolicyEngine.decide() directly. The aspect
        // owns the decision-point — bypassing it would break the deny-overrides audit trail.
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("ServiceImpl")
                .and().arePublic()
                .should(new ArchCondition<JavaMethod>("not directly call PolicyEngine.decide()") {
                    @Override
                    public void check(JavaMethod method, ConditionEvents events) {
                        for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
                            if (call.getTarget().getFullName().contains("PolicyEngine.decide")) {
                                events.add(SimpleConditionEvent.violated(method,
                                        method.getFullName() + " calls PolicyEngine.decide() directly — "
                                                + "route through @Permit / the Authorize aspect instead"));
                            }
                        }
                    }
                });

        rule.check(imported);
    }
}
