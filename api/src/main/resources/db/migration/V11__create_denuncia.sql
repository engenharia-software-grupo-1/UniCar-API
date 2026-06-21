CREATE TABLE denuncia (
  id BIGSERIAL PRIMARY KEY,

  denunciante_id BIGINT NOT NULL,
  denunciado_id BIGINT NOT NULL,

  motivo VARCHAR(100) NOT NULL,

  descricao TEXT,

  status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',

  data_denuncia TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_denuncia_denunciante
      FOREIGN KEY (denunciante_id)
          REFERENCES usuario(id),

  CONSTRAINT fk_denuncia_denunciado
      FOREIGN KEY (denunciado_id)
          REFERENCES usuario(id),

  CONSTRAINT chk_status_denuncia
      CHECK (
          status IN (
                     'PENDENTE',
                     'EM_ANALISE',
                     'PROCEDENTE',
                     'IMPROCEDENTE',
                     'ARQUIVADA'
              )
          )
);

COMMENT ON TABLE denuncia IS
'Registro de denúncias realizadas por usuários da plataforma.';

COMMENT ON COLUMN denuncia.denunciante_id IS
'Usuário responsável por registrar a denúncia.';

COMMENT ON COLUMN denuncia.denunciado_id IS
'Usuário denunciado.';

COMMENT ON COLUMN denuncia.motivo IS
'Motivo resumido da denúncia.';

COMMENT ON COLUMN denuncia.descricao IS
'Descrição detalhada fornecida pelo denunciante.';

COMMENT ON COLUMN denuncia.status IS
'Estados possíveis: PENDENTE, EM_ANALISE, PROCEDENTE, IMPROCEDENTE e ARQUIVADA.';

COMMENT ON COLUMN denuncia.data_denuncia IS
'Data e horário em que a denúncia foi registrada.';