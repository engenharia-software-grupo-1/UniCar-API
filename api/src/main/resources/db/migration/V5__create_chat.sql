CREATE TABLE chat (
  id BIGSERIAL PRIMARY KEY,

  reserva_id BIGINT NOT NULL UNIQUE,

  data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_chat_reserva
      FOREIGN KEY (reserva_id)
          REFERENCES reserva_carona(id)
);

COMMENT ON TABLE chat IS
'Canal privado de comunicação entre o motorista da carona e o usuário responsável pela reserva. Cada reserva possui exatamente um chat associado.';

COMMENT ON COLUMN chat.id IS
'Identificador único do chat.';

COMMENT ON COLUMN chat.reserva_id IS
'Reserva associada ao chat. Existe uma relação 1:1 entre reserva e chat.';

COMMENT ON COLUMN chat.data_criacao IS
'Data e hora em que o chat foi criado automaticamente pelo sistema.';