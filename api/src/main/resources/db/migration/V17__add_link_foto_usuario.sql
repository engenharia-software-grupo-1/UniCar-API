ALTER TABLE usuario
    ADD COLUMN link_foto VARCHAR(1000);

COMMENT ON COLUMN usuario.link_foto IS
'URL da foto de perfil do usuário.';