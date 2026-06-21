CREATE TABLE mensagem (
  id BIGSERIAL PRIMARY KEY,

  chat_id BIGINT NOT NULL,
  remetente_id BIGINT NOT NULL,

  conteudo TEXT NOT NULL,

  lida BOOLEAN NOT NULL DEFAULT FALSE,

  data_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_mensagem_chat
      FOREIGN KEY (chat_id)
          REFERENCES chat(id),

  CONSTRAINT fk_mensagem_remetente
      FOREIGN KEY (remetente_id)
          REFERENCES usuario(id)
);

COMMENT ON TABLE mensagem IS
'Mensagens trocadas entre o motorista e o responsável pela reserva dentro de um chat.';

COMMENT ON COLUMN mensagem.id IS
'Identificador único da mensagem.';

COMMENT ON COLUMN mensagem.chat_id IS
'Chat ao qual a mensagem pertence.';

COMMENT ON COLUMN mensagem.remetente_id IS
'Usuário que enviou a mensagem. Pode ser o motorista da carona ou o responsável pela reserva.';

COMMENT ON COLUMN mensagem.conteudo IS
'Conteúdo textual da mensagem enviada.';

COMMENT ON COLUMN mensagem.lida IS
'Indica se a mensagem já foi visualizada pelo destinatário.';

COMMENT ON COLUMN mensagem.data_envio IS
'Data e hora em que a mensagem foi enviada.';