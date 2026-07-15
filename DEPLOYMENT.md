# Projectly — Deployment Guide

## Quick Start (Docker Compose)

```bash
# 1. Copy environment file and fill in secrets
cp .env.example .env

# 2. Start all services
docker compose -f docker-compose.dev.yml up -d

# 3. Verify health
curl http://localhost:8080/actuator/health
```

## Environment Profiles

| Profile | Purpose | Database | Logging |
|---------|---------|----------|---------|
| `dev` | Local development | PostgreSQL/MariaDB (dockerized) | DEBUG |
| `staging` | Pre-production testing | Managed DB | INFO |
| `prod` | Production | Managed DB (HA) | WARN |
| `test` | CI/CD tests | H2 in-memory | WARN |

## CI/CD Pipeline

GitHub Actions workflow located at `.github/workflows/ci-cd.yml`:

1. **On push to `develop`** → Build → Test → Push image → Auto-deploy dev
2. **On push to `staging`** → Build → Test → Push image → Deploy staging
3. **On push to `main`** → Build → Test → Push image → Deploy production
4. **On pull request** → Build → Test → Build image (no push)

Images are pushed to `ghcr.io` (GitHub Container Registry).

## GitHub Secrets Required

Set these in **Settings → Secrets and variables → Actions**:

| Secret | Description |
|--------|-------------|
| `GITHUB_TOKEN` | Auto-provided — GHCR authentication |
| `SMTP_HOST` | SMTP server hostname |
| `SMTP_PORT` | SMTP server port |
| `SMTP_USER` | SMTP username |
| `SMTP_PASS` | SMTP password / API key |
| `DB_PASSWORD` | Database password |
| `ADMIN_EMAIL` | Default admin email |
| `ADMIN_PASSWORD` | Default admin password |

## Manual Deployment

```bash
# Build
mvn clean package

# Run with specific profile
java -jar target/*.jar --spring.profiles.active=prod

# Or via Docker
docker compose -f docker-compose.prod.yml up -d
```

## Rollback

```bash
# Revert to previous image tag
docker compose -f docker-compose.prod.yml stop
docker compose -f docker-compose.prod.yml rm -f
docker pull ghcr.io/your-org/your-repo:v1.0.0
# Update tag in docker-compose.prod.yml, then:
docker compose -f docker-compose.prod.yml up -d
```

## Health Checks

- **Liveness:** `GET /actuator/health/liveness`
- **Readiness:** `GET /actuator/health/readiness`
- **Metrics:** `GET /actuator/prometheus`

## Database Migrations

Flyway runs automatically on startup (`validate` mode in staging/prod). To run manually:

```bash
mvn flyway:migrate -Dflyway.configFiles=src/main/resources/application-prod.yml
```
