package com.example.Projectly.config.security.authz;

import com.example.Projectly.bean.core.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * PUBLIC pseudo-role wiring (RBAC_V3 §6.1 step 4) .
 *
 * <p>Engages when no authenticated principal is in the security context. Binds an
 * anonymous {@code PrincipalContext.Snapshot} for the duration of the request so the
 * new {@code PolicyEngine} sees the same input shape whether authenticated or not —
 * and the {@code PUBLIC} role-keyed policies declared in {@code authorization.policies}
 * match via {@code roles == "PUBLIC"}.
 *
 * <p>Mounted as a filter ordered after the JWT filter (which can still authenticate
 * first if a token was supplied). Anonymous binding only happens if the JWT filter left
 * the context empty.
 */
@Slf4j
@Component
public class AnonymousPrincipalSupport extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = auth == null
                || !auth.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(auth.getPrincipal()));
        if (anonymous) {
            try (PrincipalContext.Scope ignored = PrincipalContext.bind(anonymousSnapshot())) {
                log.debug("Bound anonymous PrincipalContext for {}.", request.getRequestURI());
                chain.doFilter(request, response);
            }
            return;
        }

        String tenantId = null;
        Set<String> containerRoleNames = Set.of();
        try (PrincipalContext.Scope ignored =
                PrincipalContext.bind(authenticatedSnapshot(auth, tenantId, containerRoleNames))) {
            chain.doFilter(request, response);
        }
    }

    private PrincipalContext.Snapshot anonymousSnapshot() {
        Set<String> roles = new LinkedHashSet<>();
        roles.add("PUBLIC");
        return new PrincipalContext.Snapshot(null, roles, null, currentRequestIp());
    }

    private PrincipalContext.Snapshot authenticatedSnapshot(Authentication auth, String tenantId,
            Set<String> containerRoleNames) {
        Object principal = auth.getPrincipal();
        User user = (principal instanceof User u) ? u : null;
        Set<String> roles = new LinkedHashSet<>();
        if (auth.getAuthorities() != null) {
            auth.getAuthorities().forEach(a -> roles.add(a.getAuthority().replace("ROLE_", "")));
        }
        if (roles.isEmpty()) {
            roles.add("PUBLIC");
        }
        return new PrincipalContext.Snapshot(user, roles, tenantId, currentRequestIp(), null, containerRoleNames);
    }

    private String currentRequestIp() {
        return null;
    }
}
