package com.example.Projectly.config.security.authz.grant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RBAC_V3 §10 rule 3 — "revoking or expiry of the delegator's underlying access cascades:
 * generated service revokes dependent delegations in the same transaction; a nightly
 * consistency job re-verifies (belt and suspenders)."
 *
 * <p>{@code GrantService.revoke()} already cascades synchronously in the same transaction —
 * this job is the backstop for what that path cannot see: a delegator's access lost through
 * some OTHER route (direct SQL, a future admin tool, a race with the synchronous cascade) or
 * a delegator who simply no longer holds a qualifying grant for reasons the cascade never
 * observed. A delegated grant is considered orphaned when its delegator ({@code delegatedBy})
 * no longer holds ANY active grant on the same target covering AT LEAST the delegated actions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrantConsistencyJob {

    private final GrantRepository grantRepository;

    /** Once a day is enough for a belt-and-suspenders sweep — real-time correctness is the
     *  synchronous cascade in {@code GrantService.revoke()}, not this job. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void sweepOrphanedDelegations() {
        LocalDateTime now = LocalDateTime.now();
        List<Grant> allActive = grantRepository.findAll().stream()
                .filter(Grant::isActive)
                .toList();
        int revokedCount = 0;
        for (Grant delegated : allActive) {
            if (delegated.getDelegatedBy() == null) continue; // not a delegation
            boolean delegatorStillHolds = allActive.stream()
                    .anyMatch(g -> g != delegated
                            && g.getGranteeId().equals(delegated.getDelegatedBy())
                            && g.getTargetType().equals(delegated.getTargetType())
                            && g.getTargetId().equals(delegated.getTargetId())
                            && g.getActions().containsAll(delegated.getActions()));
            if (!delegatorStillHolds) {
                delegated.setRevokedAt(now);
                grantRepository.save(delegated);
                revokedCount++;
                log.warn("Consistency sweep: revoked orphaned delegated grant {} (delegator {} no longer "
                                + "holds a qualifying grant on {}:{})",
                        delegated.getId(), delegated.getDelegatedBy(), delegated.getTargetType(), delegated.getTargetId());
            }
        }
        if (revokedCount > 0) {
            log.info("Grant consistency sweep: revoked {} orphaned delegated grant(s)", revokedCount);
        }
    }
}
