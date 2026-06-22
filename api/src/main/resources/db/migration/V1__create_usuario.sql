CREATE TABLE usuario (
 id BIGSERIAL PRIMARY KEY,

 matricula VARCHAR(30) NOT NULL UNIQUE,
 nome VARCHAR(150) NOT NULL,
 cpf VARCHAR(11) NOT NULL UNIQUE,
 email VARCHAR(150) NOT NULL UNIQUE,

 curso VARCHAR(150),
 genero VARCHAR(30),

 receber_email BOOLEAN NOT NULL DEFAULT TRUE,

 data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 data_atualizacao TIMESTAMP
);

COMMENT ON TABLE usuario IS
'Tabela responsável por armazenar os dados dos usuários cadastrados no sistema.';

COMMENT ON COLUMN usuario.id IS
'Identificador único do usuário gerado automaticamente pelo banco de dados.';

COMMENT ON COLUMN usuario.matricula IS
'Matrícula acadêmica do usuário. Deve ser única no sistema.';

COMMENT ON COLUMN usuario.cpf IS
'CPF do usuário. Deve ser único no sistema.';

COMMENT ON COLUMN usuario.nome IS
'Nome completo do usuário.';

COMMENT ON COLUMN usuario.email IS
'Endereço de e-mail do usuário. Deve ser único no sistema.';

COMMENT ON COLUMN usuario.curso IS
'Curso ao qual o usuário pertence.';

COMMENT ON COLUMN usuario.genero IS
'Gênero informado pelo usuário.';

COMMENT ON COLUMN usuario.receber_email IS
'Define se o usuário deseja receber notificações por e-mail. Possui valor padrão TRUE.';

COMMENT ON COLUMN usuario.data_criacao IS
'Data e horário em que o cadastro do usuário foi criado.';

COMMENT ON COLUMN usuario.data_atualizacao IS
'Data e horário da última atualização dos dados do usuário.';