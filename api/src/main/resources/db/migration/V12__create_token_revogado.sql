CREATE TABLE token_revogado (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
        CONSTRAINT uq_token_revogado_token
            UNIQUE (token)
);

COMMENT ON TABLE token_revogado IS
'Tabela responsável por armazenar os tokens JWT revogados pelo sistema, utilizada para invalidar sessões após logout.';

COMMENT ON COLUMN token_revogado.id IS
'Identificador único do registro gerado automaticamente pelo banco de dados.';

COMMENT ON COLUMN token_revogado.token IS
'Token JWT revogado. Deve ser único e é utilizado para verificar se um token foi invalidado.';

COMMENT ON COLUMN token_revogado.expires_at IS
'Data e horário de expiração natural do token. Utilizado para limpeza automática de registros obsoletos via job agendado.';

COMMENT ON CONSTRAINT uq_token_revogado_token ON token_revogado IS
'Garante que o mesmo token não seja inserido mais de uma vez na blacklist.';