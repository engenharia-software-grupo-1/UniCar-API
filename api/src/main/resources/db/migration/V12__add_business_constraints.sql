-- ============================================================
-- V12__add_business_constraints.sql
--
-- Objetivo:
-- Adicionar restrições de integridade para garantir o
-- cumprimento das regras de negócio definidas pelo sistema.
--
-- Regras implementadas:
--
-- RN-BLOQ-01
-- Usuário não pode bloquear a si mesmo.
--
-- RN-BUS-13
-- Não permitir interesses duplicados para o mesmo trajeto.
--
-- RN-AVA-05
-- Não permitir múltiplas avaliações do mesmo avaliador
-- para o mesmo usuário na mesma carona.
--
-- RN-DEN-01
-- Usuário não pode denunciar a si mesmo.
--
-- RN-RES-09
-- Valor da contribuição da reserva deve ser armazenado.
--
-- RN-CAR-29
-- Valores de contribuição não podem ser negativos.
-- ============================================================


-- ============================================================
-- BLOQUEIO DE USUÁRIOS
-- ============================================================

ALTER TABLE bloqueio_usuario
    ADD CONSTRAINT chk_bloqueio_proprio
        CHECK (
            usuario_id <> usuario_bloqueado_id
            );

COMMENT ON CONSTRAINT chk_bloqueio_proprio ON bloqueio_usuario IS
'Impede que um usuário bloqueie a si próprio.';


-- ============================================================
-- INTERESSE EM TRAJETOS
-- ============================================================

ALTER TABLE interesse_trajeto
    ADD CONSTRAINT uk_interesse_trajeto
        UNIQUE (
                usuario_id,
                origem_latitude,
                origem_longitude,
                destino_latitude,
                destino_longitude
            );

COMMENT ON CONSTRAINT uk_interesse_trajeto ON interesse_trajeto IS
'Impede o cadastro duplicado do mesmo trajeto para um usuário.';


-- ============================================================
-- AVALIAÇÕES
-- ============================================================

ALTER TABLE avaliacao
    ADD CONSTRAINT uk_avaliacao
        UNIQUE (
                carona_id,
                avaliador_id,
                avaliado_id
            );

COMMENT ON CONSTRAINT uk_avaliacao ON avaliacao IS
'Impede múltiplas avaliações do mesmo avaliador para o mesmo usuário na mesma carona.';


-- ============================================================
-- DENÚNCIAS
-- ============================================================

ALTER TABLE denuncia
    ADD CONSTRAINT chk_denuncia_propria
        CHECK (
            denunciante_id <> denunciado_id
            );

COMMENT ON CONSTRAINT chk_denuncia_propria ON denuncia IS
'Impede que um usuário denuncie a si próprio.';


-- ============================================================
-- CARONAS
-- ============================================================

ALTER TABLE carona
    ADD CONSTRAINT chk_carona_valor_contribuicao
        CHECK (
            valor_contribuicao >= 0
            );

COMMENT ON CONSTRAINT chk_carona_valor_contribuicao ON carona IS
'Impede valores negativos para a contribuição da carona.';


-- ============================================================
-- RESERVAS
-- ============================================================

ALTER TABLE reserva_carona
    ADD CONSTRAINT chk_reserva_valor_contribuicao
        CHECK (
            valor_contribuicao >= 0
            );

COMMENT ON CONSTRAINT chk_reserva_valor_contribuicao ON reserva_carona IS
'Impede valores negativos para a contribuição calculada da reserva.';