CREATE TABLE bloqueio_usuario (
  id BIGSERIAL PRIMARY KEY,

  usuario_id BIGINT NOT NULL,
  usuario_bloqueado_id BIGINT NOT NULL,

  data_bloqueio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_bloqueio_usuario
      FOREIGN KEY (usuario_id)
          REFERENCES usuario(id),

  CONSTRAINT fk_bloqueio_usuario_bloqueado
      FOREIGN KEY (usuario_bloqueado_id)
          REFERENCES usuario(id),

  CONSTRAINT uk_bloqueio
      UNIQUE (
              usuario_id,
              usuario_bloqueado_id
          )
);

COMMENT ON TABLE bloqueio_usuario IS
'Relacionamento de bloqueio entre usuários.';

COMMENT ON COLUMN bloqueio_usuario.usuario_id IS
'Usuário que realizou o bloqueio.';

COMMENT ON COLUMN bloqueio_usuario.usuario_bloqueado_id IS
'Usuário bloqueado.';

COMMENT ON COLUMN bloqueio_usuario.data_bloqueio IS
'Data e horário em que o bloqueio foi realizado.';