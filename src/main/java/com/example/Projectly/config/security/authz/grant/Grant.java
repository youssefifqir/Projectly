package com.example.Projectly.config.security.authz.grant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A runtime permission fact (RBAC_V3 §9-10): "someone gave someone specific access to something
 * specific, possibly until some date." Covers sharing, temporary access, and (Phase 4) delegation.
 *
 * <p>Not a {@code BaseEntity} subclass — grants are append-mostly audit records (revoke sets
 * {@link #revokedAt}, never deletes), not the versioned/soft-deletable business entities the rest
 * of the app models.
 */
@Entity
@Table(name = "authz_grant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "grantee_id", nullable = false)
    private String granteeId;

    /** Entity simple name (e.g. "Article") or container type. */
    @Column(name = "target_type", nullable = false, length = 64)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @Convert(converter = GrantActionsConverter.class)
    @Column(name = "actions", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private List<String> actions = new ArrayList<>();

    @Column(name = "exclusive", nullable = false)
    private boolean exclusive;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "granted_by", nullable = false)
    private String grantedBy;

    /** Non-null means this grant was created via delegation (RBAC_V3 §10 — Phase 4). */
    @Column(name = "delegated_by")
    private String delegatedBy;

    /** A delegated grant is never itself delegable (depth-1 cap, RBAC_V3 §10). */
    @Column(name = "delegable", nullable = false)
    private boolean delegable;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @PrePersist
    void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }

    /** {@code revoked_at IS NULL AND (expires_at IS NULL OR expires_at > now)} — the one active-grant predicate (RBAC_V3 §9). */
    public boolean isActive() {
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }
}
