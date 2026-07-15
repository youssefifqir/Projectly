package com.example.Projectly.config.security.authz.grant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GrantRepository extends JpaRepository<Grant, String> {

    /** The one active-grant predicate (RBAC_V3 §9) — every runtime lookup goes through this method. */
    @Query("SELECT g FROM Grant g WHERE g.granteeId = :granteeId AND g.targetType = :targetType "
         + "AND g.targetId = :targetId AND g.revokedAt IS NULL "
         + "AND (g.expiresAt IS NULL OR g.expiresAt > :now)")
    List<Grant> findActiveGrants(@Param("granteeId") String granteeId,
                                  @Param("targetType") String targetType,
                                  @Param("targetId") String targetId,
                                  @Param("now") LocalDateTime now);

    List<Grant> findByGranteeIdAndRevokedAtIsNull(String granteeId);

    List<Grant> findByTargetTypeAndTargetIdAndRevokedAtIsNull(String targetType, String targetId);

    /** Wildcard grants ({@code target_id = '*'}) — whole-entity-type access, e.g. break-glass elevation (RBAC_V3 §14.1). */
    @Query("SELECT g FROM Grant g WHERE g.granteeId = :granteeId AND g.targetType = :targetType "
         + "AND g.targetId = '*' AND g.revokedAt IS NULL AND (g.expiresAt IS NULL OR g.expiresAt > :now)")
    List<Grant> findActiveWildcardGrants(@Param("granteeId") String granteeId,
                                          @Param("targetType") String targetType,
                                          @Param("now") LocalDateTime now);

    /**
     * Any active grant for this grantee + entity type, regardless of which specific resource it
     * targets — used ONLY for the coarse point-check widening when no resource instance exists yet
     * (id-only methods: {@code findById}, {@code findByRef}, {@code deleteById}). The real,
     * per-resource enforcement stays with {@code AuthorizationSpecificationAdvisor}'s row-filtered
     * fetch; this only lets a legitimate grantee reach that fetch instead of being denied earlier.
     */
    @Query("SELECT g FROM Grant g WHERE g.granteeId = :granteeId AND g.targetType = :targetType "
         + "AND g.revokedAt IS NULL AND (g.expiresAt IS NULL OR g.expiresAt > :now)")
    List<Grant> findActiveGrantsForType(@Param("granteeId") String granteeId,
                                         @Param("targetType") String targetType,
                                         @Param("now") LocalDateTime now);

    /** Hygiene only (RBAC_V3 §10) — enforcement is the read-time predicate above, never this. */
    @Query("SELECT g FROM Grant g WHERE g.revokedAt IS NULL AND g.expiresAt IS NOT NULL AND g.expiresAt <= :now")
    List<Grant> findNewlyExpired(@Param("now") LocalDateTime now);
}
