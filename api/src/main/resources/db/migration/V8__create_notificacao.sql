CREATE TABLE notificacao (
 id BIGSERIAL PRIMARY KEY,

 usuario_id BIGINT NOT NULL,

 titulo VARCHAR(150) NOT NULL,

 mensagem TEXT NOT NULL,

 tipo VARCHAR(50) NOT NULL,

 visualizada BOOLEAN NOT NULL DEFAULT FALSE,

 data_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

 CONSTRAINT fk_notificacao_usuario
     FOREIGN KEY (usuario_id)
         REFERENCES usuario(id)
);

COMMENT ON TABLE notificacao IS 
'Tabela responsável por armazenar as notificações enviadas aos usuários do sistema.';


COMMENT ON COLUMN notificacao.id IS 
'Identificador único da notificação gerado automaticamente pelo banco de dados.';


COMMENT ON COLUMN notificacao.usuario_id IS 
'Identificador do usuário que receberá a notificação. Deve existir previamente na tabela usuario.';


COMMENT ON COLUMN notificacao.titulo IS 
'Título resumido da notificação exibido ao usuário.';


COMMENT ON COLUMN notificacao.mensagem IS 
'Conteúdo completo da mensagem da notificação enviada ao usuário.';


COMMENT ON COLUMN notificacao.tipo IS 
'Categoria ou tipo da notificação, utilizada para identificar sua finalidade dentro do sistema.';


COMMENT ON COLUMN notificacao.visualizada IS 
'Indica se a notificação já foi visualizada pelo usuário. Possui valor padrão FALSE.';


COMMENT ON COLUMN notificacao.data_envio IS 
'Data e horário em que a notificação foi criada e enviada ao usuário.';

COMMENT ON CONSTRAINT fk_notificacao_usuario ON notificacao IS
'Garante que a notificação esteja associada a um usuário existente na tabela usuario.';