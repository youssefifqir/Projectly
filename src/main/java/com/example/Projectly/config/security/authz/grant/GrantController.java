package com.example.Projectly.config.security.authz.grant;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Grant lifecycle endpoints (RBAC_V3 §10). Entity-agnostic — {@code targetType} is the entity's
 * simple name (e.g. {@code "Article"}), matching {@code PolicyEngine}'s {@code entityKeyFor}.
 */
@RestController
@RequestMapping("/api/v1/grants")
@RequiredArgsConstructor
public class GrantController {

    private final GrantService grantService;

    public record ShareRequest(String targetType, String targetId, String granteeId, List<String> actions,
                                LocalDateTime expiresAt, boolean exclusive, boolean delegable) {
    }

    public record DelegateRequest(String sourceGrantId, String granteeId, List<String> actions, LocalDateTime expiresAt) {
    }

    public record GrantDto(String id, String granteeId, String targetType, String targetId,
                            List<String> actions, LocalDateTime expiresAt, String grantedBy, String delegatedBy,
                            boolean exclusive, boolean delegable, LocalDateTime revokedAt, LocalDateTime createdDate) {
        static GrantDto from(Grant g) {
            return new GrantDto(g.getId(), g.getGranteeId(), g.getTargetType(), g.getTargetId(),
                    g.getActions(), g.getExpiresAt(), g.getGrantedBy(), g.getDelegatedBy(),
                    g.isExclusive(), g.isDelegable(), g.getRevokedAt(), g.getCreatedDate());
        }
    }

    @PostMapping("/share")
    public ResponseEntity<GrantDto> share(@RequestBody ShareRequest request) {
        String shareAction = request.targetType() + ":SHARE";
        Grant grant = grantService.share(request.targetType(), request.targetId(), request.granteeId(),
                request.actions(), request.expiresAt(), shareAction, request.exclusive(), request.delegable());
        return ResponseEntity.ok(GrantDto.from(grant));
    }

    @PostMapping("/delegate")
    public ResponseEntity<GrantDto> delegate(@RequestBody DelegateRequest request) {
        Grant grant = grantService.delegate(request.sourceGrantId(), request.granteeId(), request.actions(), request.expiresAt());
        return ResponseEntity.ok(GrantDto.from(grant));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable String id) {
        grantService.revoke(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<GrantDto>> list(@RequestParam String targetType, @RequestParam String targetId) {
        List<GrantDto> grants = grantService.listForTarget(targetType, targetId).stream()
                .map(GrantDto::from)
                .toList();
        return ResponseEntity.ok(grants);
    }
}
