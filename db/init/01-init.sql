-- Database initialization script
-- This will be run by docker-compose on first startup
-- Note: Database is already created by Docker Compose

-- Create extensions if needed
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant permissions to application user
GRANT ALL PRIVILEGES ON DATABASE Projectly_prod TO Projectly_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO Projectly_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO Projectly_user;

