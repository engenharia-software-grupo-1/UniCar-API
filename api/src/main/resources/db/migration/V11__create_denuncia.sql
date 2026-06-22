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
'Tabela responsável por armazenar denúncias realizadas entre usuários do sistema.';


COMMENT ON COLUMN denuncia.id IS
'Identificador único da denúncia gerado automaticamente pelo banco de dados.';


COMMENT ON COLUMN denuncia.denunciante_id IS
'Identificador do usuário responsável por realizar a denúncia.';


COMMENT ON COLUMN denuncia.denunciado_id IS
'Identificador do usuário que foi denunciado.';


COMMENT ON COLUMN denuncia.motivo IS
'Motivo informado para justificar a realização da denúncia.';


COMMENT ON COLUMN denuncia.descricao IS
'Descrição detalhada da situação relatada pelo denunciante.';


COMMENT ON COLUMN denuncia.status IS
'Estado atual da denúncia. Pode assumir os valores: PENDENTE, EM_ANALISE, PROCEDENTE, IMPROCEDENTE ou ARQUIVADA.';


COMMENT ON COLUMN denuncia.data_denuncia IS
'Data e horário em que a denúncia foi registrada no sistema.';