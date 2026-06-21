CREATE TABLE veiculo (
 id BIGSERIAL PRIMARY KEY,

 usuario_id BIGINT NOT NULL,

 modelo VARCHAR(100) NOT NULL,
 placa VARCHAR(20) NOT NULL UNIQUE,
 cor VARCHAR(50),

 CONSTRAINT fk_veiculo_usuario
     FOREIGN KEY (usuario_id)
         REFERENCES usuario(id)
);

COMMENT ON TABLE veiculo IS
'Veículos cadastrados pelos usuários da plataforma.';

COMMENT ON COLUMN veiculo.usuario_id IS
'Usuário proprietário do veículo.';

COMMENT ON COLUMN veiculo.modelo IS
'Modelo do veículo.';

COMMENT ON COLUMN veiculo.placa IS
'Placa do veículo. Deve ser única no sistema.';

COMMENT ON COLUMN veiculo.cor IS
'Cor do veículo.';