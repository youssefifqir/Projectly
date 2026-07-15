-- V4__Authz_Grants.sql (RBAC_V3 §9 — grants module)

CREATE TABLE IF NOT EXISTS authz_grant (
    id            VARCHAR(255) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    grantee_id    VARCHAR(255) NOT NULL,
    target_type   VARCHAR(64)  NOT NULL,
    target_id     VARCHAR(255) NOT NULL,
    actions       TEXT         NOT NULL,
    exclusive     BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at    TIMESTAMP(6),
    granted_by    VARCHAR(255) NOT NULL,
    delegated_by  VARCHAR(255),
    delegable     BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at    TIMESTAMP(6),
    created_date  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_authz_grant_delegation CHECK (delegated_by IS NULL OR delegable = FALSE)
);

CREATE INDEX ix_authz_grant_lookup ON authz_grant (grantee_id, target_type, target_id) WHERE revoked_at IS NULL;
CREATE INDEX ix_authz_grant_expiry ON authz_grant (expires_at) WHERE revoked_at IS NULL;
