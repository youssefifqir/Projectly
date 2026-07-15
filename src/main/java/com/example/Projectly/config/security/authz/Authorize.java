package com.example.Projectly.config.security.authz;

import com.example.Projectly.config.security.authz.PrincipalContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * {@code @Authorize.Permit("Article:PUBLISH")} annotation + its aspect.
 *
 * <p>Replaces {@code @PreAuthorize("hasPermission(...)")} for newly-generated services.
 * Both annotations live on the same method for the duration of the v1 → v3 cutover
 * (Phase 0+1) so Spring Security can satisfy either path. Once Phase 1.8 cutover
 * completes, the {@code @PreAuthorize} line is removed from {@code ServiceImpl.ftl}.
 *
 * <p>Action string parsing: an annotation value of the form {@code "<Entity>:<ACTION>"}
 * is parsed. Spring Expression Language (SpEL) references like {@code #id} are
 * extracted into actual method arguments via the join point.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class Authorize {

    private final PolicyEngine engine;

    @Around("@annotation(permit)")
    public Object check(ProceedingJoinPoint pjp, Permit permit) throws Throwable {
        PrincipalContext.Snapshot principal = PrincipalContext.currentOrAnonymous();
        String value = permit.value();
        Parsed parsed = Parsed.parse(value);
        ActionAndTarget at = extractActionAndTarget(pjp, parsed);
        // Pass the annotation's declared entity explicitly — it's always accurate (code-generated),
        // unlike deriving the entity key from `at.resource()`, which is null whenever the annotated
        // method only received an id (e.g. findById(Long)); a null resource would otherwise resolve
        // to the registry wildcard "*" and silently fall through to defaultPolicy.
        Decision decision = engine.decide(principal, at.action(), at.resource(), parsed.entity());
        if (decision.isDeny()) {
            log.warn("Authorize denied: principal={}, action={}, resource={}, origin={}",
                    principal.user(), at.action(), at.resource(), decision.origin());
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: " + at.action()
                            + (decision.hasDenyOrigin()
                                    ? " (denied by statement at "
                                            + decision.origin().entityName() + ")"
                                    : "")
            );
        }
        return pjp.proceed();
    }


    /**
     * The annotation value {@code "<EntityName>:<action>"} is parsed into an entity name and an
     * action. The entity name is informational; the engine resolves the resource's class name at
     * runtime. ACTIONS are upper-cased to match the convention ({@code READ}, not {@code read}).
     */
    private record Parsed(String entity, String action) {
        static Parsed parse(String value) {
            String[] parts = value.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("@Permit value '" + value + "' must be '<Entity>:<action>'");
            }
            return new Parsed(parts[0].trim(), parts[1].trim().toUpperCase());
        }
    }

    private record ActionAndTarget(String action, Object resource) {}

    /**
     * Resolves what resource the engine sees for this invocation:
     * <ul>
     *   <li>The first non-primitive, non-String parameter that is NOT a {@code Long}/{@code Integer}
     *       is used as the resource (entity instance).</li>
     *   <li>Otherwise {@code null} is passed (engine returns default).</li>
     * </ul>
     */
    private ActionAndTarget extractActionAndTarget(ProceedingJoinPoint pjp, Parsed parsed) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Object[] args = pjp.getArgs();
        for (Object arg : args) {
            if (arg != null && !arg.getClass().isPrimitive()
                    && !(arg instanceof String)
                    && !(arg instanceof Long)
                    && !(arg instanceof Integer)
                    && !(arg.getClass().isEnum())) {
                return new ActionAndTarget(parsed.action(), arg);
            }
        }
        return new ActionAndTarget(parsed.action(), null);
    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Permit {
        /** {@code "<EntityName>:<ACTION>"}, e.g. {@code "Article:PUBLISH"}. */
        String value();
    }
}
