CREATE TABLE interesse_trajeto (
   id BIGSERIAL PRIMARY KEY,

   usuario_id BIGINT NOT NULL,

   origem_latitude DECIMAL(11,8) NOT NULL,
   origem_longitude DECIMAL(11,8) NOT NULL,

   destino_latitude DECIMAL(11,8) NOT NULL,
   destino_longitude DECIMAL(11,8) NOT NULL,

   data_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

   CONSTRAINT fk_interesse_usuario
       FOREIGN KEY (usuario_id)
           REFERENCES usuario(id)
);

COMMENT ON TABLE interesse_trajeto IS
'Tabela responsável por armazenar os trajetos de interesse cadastrados pelos usuários para auxiliar na busca por caronas compatíveis.';


COMMENT ON COLUMN interesse_trajeto.id IS
'Identificador único do interesse de trajeto gerado automaticamente pelo banco de dados.';


COMMENT ON COLUMN interesse_trajeto.usuario_id IS
'Identificador do usuário que cadastrou o interesse no trajeto. Deve existir previamente na tabela usuario.';


COMMENT ON COLUMN interesse_trajeto.origem_latitude IS
'Latitude da localização de origem desejada pelo usuário para o trajeto.';


COMMENT ON COLUMN interesse_trajeto.origem_longitude IS
'Longitude da localização de origem desejada pelo usuário para o trajeto.';


COMMENT ON COLUMN interesse_trajeto.destino_latitude IS
'Latitude da localização de destino desejada pelo usuário para o trajeto.';


COMMENT ON COLUMN interesse_trajeto.destino_longitude IS
'Longitude da localização de destino desejada pelo usuário para o trajeto.';


COMMENT ON COLUMN interesse_trajeto.data_registro IS
'Data e horário em que o interesse pelo trajeto foi cadastrado no sistema.';

COMMENT ON CONSTRAINT fk_interesse_usuario ON interesse_trajeto IS
'Garante que o interesse de trajeto esteja associado a um usuário existente na tabela usuario.';