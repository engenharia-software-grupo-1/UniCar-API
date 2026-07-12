CREATE TABLE carona (
    id BIGSERIAL PRIMARY KEY,

    motorista_id BIGINT NOT NULL,
    veiculo_id BIGINT NOT NULL,

    origem_descricao VARCHAR(255) NOT NULL,
    origem_latitude DECIMAL(10,8) NOT NULL,
    origem_longitude DECIMAL(11,8) NOT NULL,

    destino_descricao VARCHAR(255) NOT NULL,
    destino_latitude DECIMAL(10,8) NOT NULL,
    destino_longitude DECIMAL(11,8) NOT NULL,

    ponto_encontro_descricao VARCHAR(255) NOT NULL,

    data_hora_partida TIMESTAMP NOT NULL,

    observacao VARCHAR(255),

    vagas_totais INTEGER NOT NULL,

    valor_contribuicao DECIMAL(10,2),

    status VARCHAR(20) NOT NULL,

    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_carona_motorista
        FOREIGN KEY (motorista_id)
            REFERENCES usuario(id),

    CONSTRAINT fk_carona_veiculo
        FOREIGN KEY (veiculo_id)
            REFERENCES veiculo(id),

    CONSTRAINT chk_carona_vagas
        CHECK (vagas_totais > 0),

    CONSTRAINT chk_status_carona
        CHECK (
            status IN (
                       'CRIADA',
                       'EM_ANDAMENTO',
                       'FINALIZADA',
                       'CANCELADA'
                )
            )
);

COMMENT ON TABLE carona IS
'Oferta de viagem criada por um motorista.';

COMMENT ON COLUMN carona.motorista_id IS
'Usuário responsável pela oferta da carona.';

COMMENT ON COLUMN carona.veiculo_id IS
'Veículo utilizado para realizar a carona.';

COMMENT ON COLUMN carona.origem_descricao IS
'Descrição textual da origem informada pelo motorista.';

COMMENT ON COLUMN carona.origem_latitude IS
'Latitude utilizada para cálculos de proximidade geográfica.';

COMMENT ON COLUMN carona.origem_longitude IS
'Longitude utilizada para cálculos de proximidade geográfica.';

COMMENT ON COLUMN carona.destino_descricao IS
'Descrição textual do destino informado pelo motorista.';

COMMENT ON COLUMN carona.destino_latitude IS
'Latitude do destino da carona.';

COMMENT ON COLUMN carona.destino_longitude IS
'Longitude do destino da carona.';

COMMENT ON COLUMN carona.ponto_encontro_descricao IS
'Local combinado para embarque dos passageiros.';

COMMENT ON COLUMN carona.data_hora_partida IS
'Data e horário previstos para início da viagem.';

COMMENT ON COLUMN carona.vagas_totais IS
'Quantidade total de vagas disponíveis na carona.';

COMMENT ON COLUMN carona.valor_contribuicao IS
'Valor de contribuição referente ao percurso completo da carona, utilizado como base para cálculo das contribuições individuais das reservas.';

COMMENT ON COLUMN carona.status IS
'Estados possíveis: CRIADA, EM_ANDAMENTO, FINALIZADA e CANCELADA.';

COMMENT ON COLUMN carona.data_criacao IS
'Data de criação do registro da carona.';

COMMENT ON COLUMN carona.observacao IS
'Campo opcional para observações gerais do motorista sobre a carona, destinadas aos passageiros interessados.';