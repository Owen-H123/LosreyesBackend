-- ============================================================
-- CONTRASEÑA INICIAL DEL ADMINISTRADOR
-- ============================================================
-- El hash que insertó V1 no corresponde a "Admin2024!", la contraseña
-- documentada en el README: en una instalación nueva no se podía entrar.
-- V1 no se puede editar (Flyway valida su checksum), por eso se corrige aquí.
--
-- Solo cambia la fila si el admin TODAVÍA tiene el hash original de V1: si
-- alguien ya cambió su contraseña, esta migración no la toca.
UPDATE users
SET password = '$2a$10$0wYRvrd/b3uxSMqOSGccTudgWCwGQ2OCqKRjoJtVwb3gek3yEif26',
    updated_at = NOW()
WHERE email = 'admin@delosreyes.com'
  AND password = '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewFkbyZ4p1DsKVi6';
