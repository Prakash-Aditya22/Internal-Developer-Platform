-- Initial data for development
-- Roles
INSERT INTO roles (id, name, description) VALUES (1, 'DEVELOPER', 'Standard developer role');
INSERT INTO roles (id, name, description) VALUES (2, 'PLATFORM_ADMIN', 'Platform administrator role');

-- Admin user (password: admin123)
INSERT INTO users (id, username, email, password, first_name, last_name, enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, updated_at, version)
VALUES (1, 'admin', 'admin@idp.local', '$2a$10$rDkPvvAFV6GgJjXpYWYq8OQKxLxMZYxLRGXqzgqLq0xVZDPEQlKWK', 'Admin', 'User', true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Developer user (password: dev123)
INSERT INTO users (id, username, email, password, first_name, last_name, enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, updated_at, version)
VALUES (2, 'developer', 'developer@idp.local', '$2a$10$8K1p/a0dL1LXMIgoEDFrwOe.WxL6M0kx/OPB0l7lzXhWvGm.TWtZu', 'Dev', 'User', true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- User-Role mappings
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);
INSERT INTO user_roles (user_id, role_id) VALUES (1, 2);
INSERT INTO user_roles (user_id, role_id) VALUES (2, 1);
