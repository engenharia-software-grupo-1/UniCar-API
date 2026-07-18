CREATE TABLE reserva_carona (
    id BIGSERIAL PRIMARY KEY,

    carona_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    quantidade_passageiros INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL,
    data_reserva TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_expiracao TIMESTAMP NOT NULL,
    data_resposta TIMESTAMP,

    CONSTRAINT fk_reserva_carona
        FOREIGN KEY (carona_id)
            REFERENCES carona(id),

    CONSTRAINT fk_reserva_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuario(id),

    CONSTRAINT uk_reserva
        UNIQUE (carona_id, usuario_id),

    CONSTRAINT chk_quantidade_passageiros
        CHECK (quantidade_passageiros > 0),

    CONSTRAINT chk_status_reserva
        CHECK (
            status IN (
                       'PENDENTE',
                       'ACEITA',
                       'RECUSADA',
                       'CANCELADA',
                       'REMOVIDA',
                       'CONCLUIDA'
                )
            )
);

COMMENT ON TABLE reserva_carona IS
'Representa a reserva de vagas realizada por um usuário em uma carona. A reserva também controla o ciclo de vida da participação do passageiro na viagem.';

COMMENT ON COLUMN reserva_carona.id IS
'Identificador único da reserva.';

COMMENT ON COLUMN reserva_carona.carona_id IS
'Carona para a qual a reserva foi realizada.';

COMMENT ON COLUMN reserva_carona.usuario_id IS
'Usuário responsável pela reserva e pela comunicação com o motorista.';

COMMENT ON COLUMN reserva_carona.quantidade_passageiros IS
'Quantidade total de passageiros representados pela reserva, incluindo o usuário solicitante.';

COMMENT ON COLUMN reserva_carona.status IS
'Estado atual da reserva. Valores permitidos: PENDENTE, ACEITA, RECUSADA, CANCELADA, REMOVIDA e CONCLUIDA.';

COMMENT ON COLUMN reserva_carona.data_reserva IS
'Data e hora em que a reserva foi criada.';

COMMENT ON COLUMN reserva_carona.data_expiracao IS
'Data e hora limite para resposta do motorista. Após esse momento a reserva deve ser marcada automaticamente como REMOVIDA.';

COMMENT ON COLUMN reserva_carona.data_resposta IS
'Data e hora em que a reserva foi aceita ou recusada pelo motorista.';

-- Versão com adição de campos para cálculo de contribuição

ALTER TABLE reserva_carona
    ADD COLUMN origem_embarque_descricao VARCHAR(255) NOT NULL;

ALTER TABLE reserva_carona
    ADD COLUMN origem_embarque_latitude DECIMAL(10,8) NOT NULL;

ALTER TABLE reserva_carona
    ADD COLUMN origem_embarque_longitude DECIMAL(11,8) NOT NULL;

ALTER TABLE reserva_carona
    ADD COLUMN valor_contribuicao NUMERIC(10,2) NOT NULL;

COMMENT ON COLUMN reserva_carona.origem_embarque_descricao IS
'Descrição textual do local onde o passageiro embarcará na carona.';

COMMENT ON COLUMN reserva_carona.origem_embarque_latitude IS
'Latitude do ponto de embarque informado pelo passageiro.';

COMMENT ON COLUMN reserva_carona.origem_embarque_longitude IS
'Longitude do ponto de embarque informado pelo passageiro.';

COMMENT ON COLUMN reserva_carona.valor_contribuicao IS
'Valor da contribuição calculado para a reserva com base no trecho percorrido e na quantidade de passageiros.';
