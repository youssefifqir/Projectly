# Security Integration Guide

## Overview

This project includes a comprehensive security implementation with JWT-based authentication using RSA asymmetric encryption. The security layer provides:

- User authentication and registration
- JWT token-based authorization (Access & Refresh tokens)
- Role-based access control (RBAC)
- Secure password encryption with BCrypt
- Full audit trail (created by, last modified by, timestamps)
- UUID-based entity IDs
- Email validation with disposable email blocking
- Spring Security integration

## Key Components

### 1. Authentication Flow

#### Registration
- **Endpoint**: `POST /api/v1/auth/register`
- **Body**: `RegistrationRequest` (firstName, lastName, email, phoneNumber, password, confirmPassword)
- **Validations**:
  - Strong password requirements (min 8 chars, uppercase, lowercase, digit, special char)
  - Non-disposable email addresses
  - Valid phone number format
  - Unique email and phone number

#### Login
- **Endpoint**: `POST /api/v1/auth/login`
- **Body**: `AuthenticationRequest` (email, password)
- **Response**: `AuthenticationResponse` (accessToken, refreshToken, tokenType)

#### Token Refresh
- **Endpoint**: `POST /api/v1/auth/refresh`
- **Body**: `RefreshRequest` (refreshToken)
- **Response**: New `AuthenticationResponse` with fresh access token

### 2. User Management

Protected endpoints (require Bearer token):

- **Update Profile**: `PATCH /api/v1/users/profile`
- **Change Password**: `PATCH /api/v1/users/password`
- **Deactivate Account**: `POST /api/v1/users/deactivate`
- **Reactivate Account**: `POST /api/v1/users/reactivate`
- **Delete Account**: `DELETE /api/v1/users`

### 3. Security Configuration

#### JWT Tokens
- **Access Token**: Valid for 24 hours (configurable via `app.security.jwt.access-token-expiration`)
- **Refresh Token**: Valid for 7 days (configurable via `app.security.jwt.refresh-token-expiration`)
- **Algorithm**: RSA-2048 asymmetric encryption
- **Keys**: Auto-generated on first run in `src/main/resources/keys/`

#### Public Endpoints
The following endpoints are accessible without authentication:
- `/api/v1/auth/**` - Authentication endpoints
- `/swagger-ui/**` - API documentation
- `/v3/api-docs/**` - OpenAPI specification
- `/actuator/**` - Health checks and metrics

### 4. Database Schema

#### Users Table
- `id` (VARCHAR UUID) - Primary key
- `ref` (VARCHAR) - Optional reference
- `first_name`, `last_name`, `email`, `phone_number`, `password`
- `date_of_birth`, `profile_picture_url`
- Boolean flags: `is_enabled`, `is_account_locked`, `is_credential_expired`, `is_email_verified`, `is_phone_verified`
- Audit fields: `created_date`, `last_modified_date`, `created_by`, `last_modified_by`

> **Default Role Assignment**: When a new user registers, they are automatically assigned the `ROLE_USER` role. If this role doesn't exist in your configuration, the system assigns the first available role from your role list.

#### Roles Table
- `id` (VARCHAR UUID) - Primary key
- `name` (VARCHAR) - Role name (e.g., ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR)
- Audit trail fields

**Configured Roles:**
- `ROLE_ADMIN`
- `ROLE_MANAGER`
- `ROLE_MEMBER`

> **Note**: Roles are defined in the project configuration YAML and automatically created during database initialization. New users are assigned the `ROLE_USER` role by default, or the first available role if `ROLE_USER` doesn't exist.

#### Users_Roles (Join Table)
- Many-to-many relationship between users and roles

### 5. RSA Key Generation

Keys are automatically generated on application startup if they don't exist:
- **Location**: `src/main/resources/keys/`
- **Files**: `private_key.pem`, `public_key.pem`
- **Algorithm**: RSA-2048

> **Note**: For production, consider using a secure key management service or vault.

### 6. Configuration Properties

Add to your `application.yml`:

```yaml
app:
  security:
    jwt:
      access-token-expiration: 86400000  # 24 hours
      refresh-token-expiration: 604800000  # 7 days
    disposable-email: 10minutemail,20minutemail,33mail,guerrillamail,mailinator
```

## Usage Examples

### 1. Register a New User

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \\
  -H "Content-Type: application/json" \\
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+1234567890",
    "password": "SecurePass123!",
    "confirmPassword": "SecurePass123!"
  }'
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \\
  -H "Content-Type: application/json" \\
  -d '{
    "email": "john.doe@example.com",
    "password": "SecurePass123!"
  }'
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiJ9...",
  "refresh_token": "eyJhbGciOiJSUzI1NiJ9...",
  "token_type": "Bearer"
}
```

### 3. Access Protected Endpoint

```bash
curl -X PATCH http://localhost:8080/api/v1/users/profile \\
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \\
  -H "Content-Type: application/json" \\
  -d '{
    "firstName": "John",
    "lastName": "Smith",
    "dateOfBirth": "1990-01-01"
  }'
```

## Security Best Practices

1. **Never commit RSA keys** - Add `src/main/resources/keys/` to `.gitignore`
2. **Use HTTPS in production** - All JWT tokens should be transmitted over HTTPS
3. **Rotate keys regularly** - Implement key rotation strategy for production
4. **Monitor failed login attempts** - Implement rate limiting and account lockout
5. **Use strong password policies** - Already enforced by validation
6. **Keep tokens short-lived** - Configure appropriate expiration times
7. **Secure refresh token storage** - Store refresh tokens securely on client side

## Authorization

This project runs ARCHEON's v3 authorization engine (RBAC_V3) — one decision point, `PolicyEngine`, enforced identically at every layer: point checks (`@Authorize.Permit`), list/criteria queries (`AuthorizationSpecificationAdvisor`).

### Combining algorithm: deny-overrides (NOT most-permissive-wins)

For every `(principal, action, resource)` decision, the engine:

1. Collects every statement that matches the action: compiled YAML policies for the principal's roles, plus `PUBLIC` statements for anonymous callers, plus statements synthesized from active `Grant`s targeting the resource.
2. **Any matching statement with effect `DENY` wins immediately.** This is absolute — a later or broader `PERMIT` never overrides it, and holding an extra role never silences it.
3. If the resource carries an active **exclusive** grant for this principal, the decision is narrowed to level-0 statements only (the resource's own compiled policies + its own grants) — inherited container/membership access is set aside for that resource. Deny statements still apply on top of this.
4. Otherwise, any matching `PERMIT` wins (its scope decides which rows/fields it actually covers).
5. If nothing matched: `defaultPolicy: deny` — `read` only ever permits the `READ` action; anything else (including `deny`) denies.

> **This is the opposite of "most roles wins."** Assigning an extra role to a user can only ever narrow access (via a `DENY` statement) or grant what that role's own statements say — never silently unlock more than any single statement grants.

### Scopes

| Scope | Behavior |
|---|---|
| `all` | Unconditional — matches every resource of the entity |
| `own` | The resource's ownership field (below) equals the current principal's id |
| `expression` | Arbitrary `when:` condition over the resource (dual-compiled: the SAME condition becomes both a Java boolean for point checks and a JPA Criteria predicate for list queries — enforcement can't drift between the two) |
| `grant` | Implicit — synthesized from an active `Grant` row targeting the resource (or the whole entity via a wildcard grant) |

**Ownership bindings** (`own` scope, default path is `createdBy`):

| Entity | Path | Resolved column |
|---|---|---|
| Project | `owner` | owner_id |
| Task | `assignee` | assignee_id |
| Comment | `author` | author_id |


### Grants (ad-hoc sharing)

A `Grant` is a runtime `(grantee, target, actions, expiresAt?, exclusive?, delegable?)` fact — covers sharing, temporary access, and per-resource overrides. `exclusive: true` means "this resource's own access list replaces inherited container/role access" (deny statements still pierce it). A `delegable: true` grant can be re-shared once (depth-1 only). Manage via `/api/v1/grants`.







### Generated engine files

| File | Purpose |
|---|---|
| `StaticPolicyRegistry.java` | Compiled YAML statements, built once at boot — zero per-decision allocation |
| `PolicyEngine.java` | The single decision point (`decide()`) — deny-overrides combining, described above |
| `Authorize.java` (`@Authorize.Permit("Entity:ACTION")`) | Point-check annotation + aspect, on every public entity-service method |
| `AuthorizationSpecificationAdvisor.java` | Injects the SAME rule as a JPA `Specification` into every list/page/criteria query |

### Known limitations (documented, not silently missing)

- `authorization.ownership` paths are single-hop only — a dotted path like `author.department` resolves using only the first segment (`author`); multi-hop ownership needs a denormalized field instead.


## API Documentation

Access Swagger UI at: `http://localhost:8080/swagger-ui.html`

All endpoints are documented with:
- Request/Response schemas
- Validation rules
- Security requirements
- Example payloads

## Troubleshooting

### JWT Token Issues
- Ensure keys are properly generated in `src/main/resources/keys/`
- Check token expiration times in configuration
- Verify Bearer token format: `Authorization: Bearer <token>`

### Database Issues
- Run Flyway migrations: Automatically applied on startup
- Check database connection in application-{profile}.yml
- Ensure PostgreSQL is running for prod/test profiles

### Email Validation Failing
- Update disposable email list in configuration
- Check email format matches standard pattern
