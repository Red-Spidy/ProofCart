-- Fix: audit_events.metadata inserts were failing on Postgres.
--
-- Hibernate was writing the default '{}' as a plain varchar without an explicit JSON cast,
-- which Postgres rejects for a json-typed column ("column is of type json but expression is of
-- type character varying"). H2 (used in tests) is looser about this and let it slip through
-- undetected. No column type change is needed here — the fix is entity-side
-- (@JdbcTypeCode(SqlTypes.JSON) on AuditEventEntity.metadata) so Hibernate emits the cast.
-- This migration exists only as a record of the incident for anyone reading migration history.
SELECT 1;
