package com.example.Projectly.ws.controller.admin;

import com.example.Projectly.bean.core.user.User;
import com.example.Projectly.bean.core.role.Role;
import com.example.Projectly.exception.BusinessException;
import com.example.Projectly.exception.ErrorCode;
import com.example.Projectly.dao.facade.security.UserDao;
import com.example.Projectly.dao.facade.security.RoleDao;
import com.example.Projectly.ws.dto.PageResponse;
import com.example.Projectly.ws.dto.user.AdminUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Users", description = "Administrator user management")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
        "id", "ref", "createdDate", "lastModifiedDate", "firstName", "lastName", "email", "password", "enabled", "locked", "credentialsExpired", "emailVerified"
    );

    private final UserDao userDao;
    private final RoleDao roleDao;

    @GetMapping
    @Operation(summary = "List all users (paginated)")
    public ResponseEntity<PageResponse<AdminUserResponse>> listUsers(
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") @Max(200) final int size,
            @RequestParam(defaultValue = "createdDate") final String sortBy,
            @RequestParam(defaultValue = "desc") final String sortDir) {
        if (!ALLOWED_SORT_COLUMNS.contains(sortBy)) {
            return ResponseEntity.badRequest().body(null);
        }
        if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir)) {
            return ResponseEntity.badRequest().body(null);
        }
        final int effectiveSize = Math.min(size, 200);
        final var pageable = PageRequest.of(page, effectiveSize,
                "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        final var result = this.userDao.findAll(pageable).map(this::toAdminResponse);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<AdminUserResponse> getUser(@PathVariable final String id) {
        final User user = this.userDao.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return ResponseEntity.ok(toAdminResponse(user));
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate a user account")
    public ResponseEntity<Void> activate(@PathVariable final String id) {
        final User user = this.userDao.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setEnabled(true);
        this.userDao.save(user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a user account")
    public ResponseEntity<Void> deactivate(@PathVariable final String id) {
        final User user = this.userDao.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setEnabled(false);
        this.userDao.save(user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Permanently delete a user account")
    public ResponseEntity<Void> delete(@PathVariable final String id) {
        if (!this.userDao.existsById(id)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        this.userDao.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/roles")
    @Transactional
    @Operation(summary = "Update user roles (assign specific roles)")
    public ResponseEntity<AdminUserResponse> setRoles(@PathVariable final String id,
                                                       @RequestBody final List<String> roles) {
        final User user = this.userDao.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.getRoles().clear();
        for (final String roleName : roles) {
            final String prefixed = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
            final Role role = this.roleDao.findByName(prefixed)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
            user.getRoles().add(role);
        }
        this.userDao.save(user);
        return ResponseEntity.ok(toAdminResponse(user));
    }

    @PatchMapping("/{id}")
    @Transactional
    @Operation(summary = "Update user fields (branchId, departmentCode, etc.)")
    public ResponseEntity<AdminUserResponse> patchUser(@PathVariable final String id,
                                                        @RequestBody final Map<String, Object> fields) {
        final User user = this.userDao.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (fields.containsKey("firstName")) {
            user.setFirstName((String) fields.get("firstName"));
        }
        if (fields.containsKey("lastName")) {
            user.setLastName((String) fields.get("lastName"));
        }

        this.userDao.save(user);
        return ResponseEntity.ok(toAdminResponse(user));
    }

    private AdminUserResponse toAdminResponse(final User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getRef(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toList()),
                user.isEnabled(),
                user.isLocked(),
                user.isEmailVerified(),
                user.getCreatedDate(),
                user.getLastModifiedDate()
        );
    }
}
