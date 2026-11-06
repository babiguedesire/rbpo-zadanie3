-- ============================================================
-- Ограниченный пользователь БД для приложения.
-- Права: только DML (SELECT/INSERT/UPDATE/DELETE) на таблицы и
-- USAGE на последовательности. Никаких CREATE/DROP/GRANT.
-- ============================================================

CREATE ROLE app_user LOGIN PASSWORD 'app_password';

GRANT CONNECT ON DATABASE repair_db TO app_user;
GRANT USAGE ON SCHEMA public TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;
