package com.example.Projectly.config.security.authz.grant;

import com.example.Projectly.config.security.authz.Decision;
import com.example.Projectly.config.security.authz.PolicyEngine;
import com.example.Projectly.config.security.authz.PrincipalContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Grant lifecycle (RBAC_V3 §10): sharing, temporary access, delegation, revoke.
 *
 * <p><b>Simplification vs. the design doc:</b> the "caller must hold every action being granted"
 * check is evaluated via {@code PolicyEngine.decide(principal, action, null, targetType)} — a
 * resource-blind check ("do you have this action on this entity type at all"), not a per-resource
 * OWN-scope check. This keeps the endpoint entity-agnostic (one controller for every entity)
 * rather than requiring a generated per-entity share endpoint. The grant itself is still enforced
 * with full per-resource precision at read/write time — only the *pre-share eligibility check* is
 * coarser. Note the explicit {@code targetType} entityKey argument — the 3-arg overload derives
 * the entity key from the resource instance, which is null here (no resource to check against);
 * that used to make this check silently deny every caller, always (see archeon-knowledge.md §14).
 *
 * <p><b>{@code exclusive} grants</b> (RBAC_V3 §6.3 step 3, "resource overrides inherited container
 * permissions") are accepted and stored correctly, but have no observable effect yet — nothing
 * above resource-level (level 0) exists to override until container-level inheritance is wired
 * into {@code PolicyEngine.decide()} (a documented Phase 3 gap). Deny-overrides still applies to
 * exclusive grants exactly like any other grant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrantService {

    private final GrantRepository grantRepository;
    private final PolicyEngine engine;

    /**
     * @param shareAction the entity's declared share action, e.g. {@code "Article:SHARE"}
     * @param grantedActions the actions being granted, e.g. {@code ["READ", "UPDATE"]}
     */
    @Transactional
    public Grant share(String targetType, String targetId, String granteeId, List<String> grantedActions,
                        LocalDateTime expiresAt, String shareAction, boolean exclusive, boolean delegable) {
        PrincipalContext.Snapshot principal = PrincipalContext.currentOrAnonymous();
        if (principal.isAnonymous()) {
            throw new AccessDeniedException("Anonymous principals cannot share access");
        }
        String granterId = principal.user().getId();

        // shareAction arrives as "<targetType>:SHARE" (see GrantController) — split it back apart
        // rather than passing the compound string as the action: decide()'s 3-arg overload derives
        // the entity key from `resource` alone, and a null resource resolves to the registry
        // wildcard "*" (no compiled statements), so the compound form here always fell through to
        // defaultPolicy regardless of who called it. The 4-arg overload takes entityKey explicitly.
        String shareActionName = shareAction.contains(":")
                ? shareAction.substring(shareAction.indexOf(':') + 1) : shareAction;
        Decision shareDecision = engine.decide(principal, shareActionName, null, targetType);
        if (shareDecision.isDeny()) {
            throw new AccessDeniedException("Caller does not hold '" + shareAction + "' on " + targetType);
        }
        for (String action : grantedActions) {
            Decision d = engine.decide(principal, action, null, targetType);
            if (d.isDeny()) {
                throw new AccessDeniedException(
                        "Cannot grant '" + action + "' on " + targetType + " — you do not hold it yourself");
            }
        }

        Grant grant = Grant.builder()
                .granteeId(granteeId)
                .targetType(targetType)
                .targetId(targetId)
                .actions(grantedActions)
                .expiresAt(expiresAt)
                .grantedBy(granterId)
                .exclusive(exclusive)
                .delegable(delegable)
                .build();
        Grant saved = grantRepository.save(grant);
        log.info("Grant created: {} granted {} on {}:{} to {} (expires {}, exclusive={}, delegable={})",
                granterId, grantedActions, targetType, targetId, granteeId, expiresAt, exclusive, delegable);
        return saved;
    }

    /**
     * RBAC_V3 §10 — delegation rules: the caller must hold the source grant, and must be its
     * grantee; delegated actions must be a subset of the source grant's actions; depth-1 cap (a
     * delegated grant is never itself delegable — enforced by never setting {@code delegable} on
     * the output); the source must itself be {@code delegable}.
     */
    @Transactional
    public Grant delegate(String sourceGrantId, String newGranteeId, List<String> delegatedActions, LocalDateTime expiresAt) {
        Grant source = grantRepository.findById(sourceGrantId)
                .orElseThrow(() -> new AccessDeniedException("Grant not found: " + sourceGrantId));
        if (!source.isActive()) {
            throw new AccessDeniedException("Cannot delegate an expired or revoked grant");
        }
        if (!source.isDelegable()) {
            throw new AccessDeniedException("Grant " + sourceGrantId + " is not delegable");
        }
        PrincipalContext.Snapshot principal = PrincipalContext.currentOrAnonymous();
        String callerId = principal.isAnonymous() ? null : principal.user().getId();
        if (callerId == null || !callerId.equals(source.getGranteeId())) {
            throw new AccessDeniedException("Only the grant's holder may delegate it");
        }
        if (!source.getActions().containsAll(delegatedActions)) {
            throw new AccessDeniedException("Cannot delegate actions the source grant does not itself carry");
        }

        Grant delegated = Grant.builder()
                .granteeId(newGranteeId)
                .targetType(source.getTargetType())
                .targetId(source.getTargetId())
                .actions(delegatedActions)
                .expiresAt(expiresAt == null || (source.getExpiresAt() != null && expiresAt.isAfter(source.getExpiresAt()))
                        ? source.getExpiresAt() : expiresAt)
                .grantedBy(callerId)
                .delegatedBy(callerId)
                .exclusive(false)
                .delegable(false)
                .build();
        Grant saved = grantRepository.save(delegated);
        log.info("Grant delegated: {} delegated {} from {} to {}", callerId, delegatedActions, sourceGrantId, newGranteeId);
        return saved;
    }

    /**
     * System-issued grant — bypasses the "you must already hold what you grant" eligibility check
     * (unlike {@link #share}), because eligibility here comes from holding a break-glass role, not
     * from prior possession of the action (RBAC_V3 §14.1: "everything the session touches is just
     * grants, which is exactly why it is safe" — the safety is deny-overrides + expiry + audit,
     * not a possession check). {@code targetId = "*"} grants the whole entity type.
     */
    @Transactional
    public Grant createSystemGrant(String granteeId, String targetType, String targetId,
                                    List<String> actions, LocalDateTime expiresAt, String grantedByLabel) {
        Grant grant = Grant.builder()
                .granteeId(granteeId)
                .targetType(targetType)
                .targetId(targetId)
                .actions(actions)
                .expiresAt(expiresAt)
                .grantedBy(grantedByLabel)
                .exclusive(false)
                .delegable(false)
                .build();
        Grant saved = grantRepository.save(grant);
        log.info("System grant created: {} granted {} on {}:{} to {} (expires {})",
                grantedByLabel, actions, targetType, targetId, granteeId, expiresAt);
        return saved;
    }

    @Transactional
    public void revoke(String grantId) {
        Grant grant = grantRepository.findById(grantId)
                .orElseThrow(() -> new AccessDeniedException("Grant not found: " + grantId));
        PrincipalContext.Snapshot principal = PrincipalContext.currentOrAnonymous();
        String callerId = principal.isAnonymous() ? null : principal.user().getId();
        if (callerId == null || !callerId.equals(grant.getGrantedBy())) {
            throw new AccessDeniedException("Only the grantor may revoke this grant");
        }
        LocalDateTime now = LocalDateTime.now();
        grant.setRevokedAt(now);
        grantRepository.save(grant);
        cascadeRevokeDelegated(grant, now);
        log.info("Grant revoked: {} by {}", grantId, callerId);
    }

    /** Best-effort cascade (RBAC_V3 §10): revoke grants delegated from this holder for the same target. */
    private void cascadeRevokeDelegated(Grant revoked, LocalDateTime now) {
        List<Grant> dependents = grantRepository.findByTargetTypeAndTargetIdAndRevokedAtIsNull(
                revoked.getTargetType(), revoked.getTargetId());
        for (Grant dependent : dependents) {
            if (revoked.getGranteeId().equals(dependent.getDelegatedBy())) {
                dependent.setRevokedAt(now);
                grantRepository.save(dependent);
                log.info("Cascade-revoked delegated grant {} (delegator {} lost access)", dependent.getId(), revoked.getGranteeId());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Grant> listForTarget(String targetType, String targetId) {
        return grantRepository.findByTargetTypeAndTargetIdAndRevokedAtIsNull(targetType, targetId);
    }

    /** Active grants held by a principal against a specific resource — read by {@code PolicyEngine.decide()}. */
    @Transactional(readOnly = true)
    public List<Grant> activeGrantsFor(String granteeId, String targetType, String targetId) {
        return grantRepository.findActiveGrants(granteeId, targetType, targetId, LocalDateTime.now());
    }
}
