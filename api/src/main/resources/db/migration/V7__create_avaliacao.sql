CREATE TABLE avaliacao (
   id BIGSERIAL PRIMARY KEY,

   carona_id BIGINT NOT NULL,

   avaliador_id BIGINT NOT NULL,
   avaliado_id BIGINT NOT NULL,

   nota INTEGER NOT NULL,

   comentario TEXT,

   data_avaliacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

   CONSTRAINT fk_avaliacao_carona
       FOREIGN KEY (carona_id)
           REFERENCES carona(id),

   CONSTRAINT fk_avaliacao_avaliador
       FOREIGN KEY (avaliador_id)
           REFERENCES usuario(id),

   CONSTRAINT fk_avaliacao_avaliado
       FOREIGN KEY (avaliado_id)
           REFERENCES usuario(id),

   CONSTRAINT chk_nota
       CHECK (nota BETWEEN 1 AND 5)
);

COMMENT ON TABLE avaliacao IS
'Avaliação realizada após a conclusão de uma carona.';

COMMENT ON COLUMN avaliacao.carona_id IS
'Carona à qual a avaliação está vinculada.';

COMMENT ON COLUMN avaliacao.avaliador_id IS
'Usuário responsável por realizar a avaliação.';

COMMENT ON COLUMN avaliacao.avaliado_id IS
'Usuário que recebeu a avaliação.';

COMMENT ON COLUMN avaliacao.nota IS
'Nota atribuída ao usuário avaliado, variando de 1 a 5.';

COMMENT ON COLUMN avaliacao.comentario IS
'Comentário opcional sobre a experiência da carona.';

COMMENT ON COLUMN avaliacao.data_avaliacao IS
'Data e horário em que a avaliação foi registrada.';