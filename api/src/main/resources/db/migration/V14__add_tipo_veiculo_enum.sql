DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_veiculo') THEN
        CREATE TYPE tipo_veiculo AS ENUM ('CARRO', 'MOTO');
    END IF;
END
$$;

ALTER TABLE veiculo
    ADD COLUMN IF NOT EXISTS tipo_veiculo tipo_veiculo NOT NULL DEFAULT 'CARRO';

COMMENT ON COLUMN veiculo.tipo_veiculo IS
'Tipo do veículo: CARRO ou MOTO.';

COMMENT ON TYPE tipo_veiculo IS
'Tipo de veículo cadastrado na plataforma.';
