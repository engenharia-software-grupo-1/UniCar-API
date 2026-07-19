package com.unicar.service.carona;

import com.unicar.domain.BloqueioUsuario;
import com.unicar.domain.Carona;
import com.unicar.domain.ReservaCarona;
import com.unicar.enums.StatusCarona;
import com.unicar.enums.StatusReserva;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa cada Specification isoladamente, mockando a Criteria API do JPA.
 * Sem isso, a lógica de montagem das queries nunca é exercitada, já que
 * BuscaCaronaServiceTest mocka o CaronaRepository inteiro.
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class CaronaSpecificationsTest {

    @Mock
    private Root<Carona> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Nested
    @DisplayName("comStatusCriada")
    class ComStatusCriada {

        @Test
        @DisplayName("Deve filtrar por status CRIADA")
        void deveFiltrarPorStatusCriada() {
            Path<Object> statusPath = mock(Path.class);
            Predicate predicate = mock(Predicate.class);
            when(root.get("status")).thenReturn(statusPath);
            when(cb.equal(statusPath, StatusCarona.CRIADA)).thenReturn(predicate);

            Predicate resultado = CaronaSpecifications.comStatusCriada().toPredicate(root, query, cb);

            assertSame(predicate, resultado);
        }
    }

    @Nested
    @DisplayName("comDataFutura")
    class ComDataFutura {

        @Test
        @DisplayName("Deve filtrar por dataHoraPartida maior que agora")
        void deveFiltrarPorDataMaiorQueAgora() {
            Path<LocalDateTime> dataPath = mock(Path.class);
            Predicate predicate = mock(Predicate.class);
            when(root.<LocalDateTime>get("dataHoraPartida")).thenReturn(dataPath);
            when(cb.greaterThan(eq(dataPath), any(LocalDateTime.class))).thenReturn(predicate);

            Predicate resultado = CaronaSpecifications.comDataFutura().toPredicate(root, query, cb);

            assertSame(predicate, resultado);
            verify(cb).greaterThan(eq(dataPath), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("comCursoMotorista")
    class ComCursoMotorista {

        @Test
        @DisplayName("Deve retornar conjunção quando curso não for informado")
        void deveRetornarConjuncaoQuandoCursoForNulo() {
            Predicate conjuncao = mock(Predicate.class);
            when(cb.conjunction()).thenReturn(conjuncao);

            Predicate resultado = CaronaSpecifications.comCursoMotorista(null).toPredicate(root, query, cb);

            assertSame(conjuncao, resultado);
            verify(root, never()).join("motorista");
        }

        @Test
        @DisplayName("Deve filtrar por curso do motorista (case-insensitive) quando informado")
        void deveFiltrarPorCursoQuandoInformado() {
            Join<Carona, Object> joinMotorista = mock(Join.class);
            Path<String> cursoPath = mock(Path.class);
            Expression<String> upperExpr = mock(Expression.class);
            Predicate predicate = mock(Predicate.class);

            when(root.<Carona, Object>join("motorista")).thenReturn(joinMotorista);
            when(joinMotorista.<String>get("curso")).thenReturn(cursoPath);
            when(cb.upper(cursoPath)).thenReturn(upperExpr);
            when(cb.like(upperExpr, "%ENGENHARIA%")).thenReturn(predicate);

            Predicate resultado = CaronaSpecifications.comCursoMotorista("engenharia").toPredicate(root, query, cb);

            assertSame(predicate, resultado);
        }
    }

    @Nested
    @DisplayName("comGeneroMotorista")
    class ComGeneroMotorista {

        @Test
        @DisplayName("Deve retornar conjunção quando gênero não for informado")
        void deveRetornarConjuncaoQuandoGeneroForNulo() {
            Predicate conjuncao = mock(Predicate.class);
            when(cb.conjunction()).thenReturn(conjuncao);

            Predicate resultado = CaronaSpecifications.comGeneroMotorista(null).toPredicate(root, query, cb);

            assertSame(conjuncao, resultado);
            verify(root, never()).join("motorista");
        }

        @Test
        @DisplayName("Deve filtrar por gênero do motorista quando informado")
        void deveFiltrarPorGeneroQuandoInformado() {
            Join<Carona, Object> joinMotorista = mock(Join.class);
            Path<Object> generoPath = mock(Path.class);
            Predicate predicate = mock(Predicate.class);

            when(root.<Carona, Object>join("motorista")).thenReturn(joinMotorista);
            when(joinMotorista.get("genero")).thenReturn(generoPath);
            when(cb.equal(generoPath, "FEMININO")).thenReturn(predicate);

            Predicate resultado = CaronaSpecifications.comGeneroMotorista("FEMININO").toPredicate(root, query, cb);

            assertSame(predicate, resultado);
        }
    }

    @Nested
    @DisplayName("comBoundingBox")
    class ComBoundingBox {

        @Test
        @DisplayName("Deve retornar conjunção quando latitude ou longitude não forem informadas")
        void deveRetornarConjuncaoQuandoCoordenadasAusentes() {
            Predicate conjuncao = mock(Predicate.class);
            when(cb.conjunction()).thenReturn(conjuncao);

            Predicate resultado = CaronaSpecifications.comBoundingBox(null, new BigDecimal("-35.91"), 5.0)
                    .toPredicate(root, query, cb);

            assertSame(conjuncao, resultado);
        }

        @Test
        @DisplayName("Deve retornar conjunção quando apenas a longitude não for informada")
        void deveRetornarConjuncaoQuandoApenasLongitudeAusente() {
            Predicate conjuncao = mock(Predicate.class);
            when(cb.conjunction()).thenReturn(conjuncao);

            Predicate resultado = CaronaSpecifications.comBoundingBox(new BigDecimal("-7.22"), null, 5.0)
                    .toPredicate(root, query, cb);

            assertSame(conjuncao, resultado);
        }

        @Test
        @DisplayName("Deve filtrar por área retangular quando coordenadas forem informadas")
        void deveFiltrarPorAreaQuandoCoordenadasInformadas() {
            Path<BigDecimal> latPath = mock(Path.class);
            Path<BigDecimal> lonPath = mock(Path.class);
            Predicate betweenLat = mock(Predicate.class);
            Predicate betweenLon = mock(Predicate.class);
            Predicate combinado = mock(Predicate.class);

            when(root.<BigDecimal>get("origemLatitude")).thenReturn(latPath);
            when(root.<BigDecimal>get("origemLongitude")).thenReturn(lonPath);
            when(cb.between(eq(latPath), any(BigDecimal.class), any(BigDecimal.class))).thenReturn(betweenLat);
            when(cb.between(eq(lonPath), any(BigDecimal.class), any(BigDecimal.class))).thenReturn(betweenLon);
            when(cb.and(betweenLat, betweenLon)).thenReturn(combinado);

            Predicate resultado = CaronaSpecifications
                    .comBoundingBox(new BigDecimal("-7.22"), new BigDecimal("-35.91"), 5.0)
                    .toPredicate(root, query, cb);

            assertSame(combinado, resultado);
        }
    }

    @Nested
    @DisplayName("comDataHoraSaida")
    class ComDataHoraSaida {

        @Test
        @DisplayName("Deve retornar conjunção quando data não for informada")
        void deveRetornarConjuncaoQuandoDataForNula() {
            Predicate conjuncao = mock(Predicate.class);
            when(cb.conjunction()).thenReturn(conjuncao);

            Predicate resultado = CaronaSpecifications.comDataHoraSaida(null).toPredicate(root, query, cb);

            assertSame(conjuncao, resultado);
        }

        @Test
        @DisplayName("Deve filtrar entre o início e o fim do dia informado")
        void deveFiltrarPeloIntervaloDoDia() {
            Path<LocalDateTime> dataPath = mock(Path.class);
            Predicate gePredicate = mock(Predicate.class);
            Predicate lePredicate = mock(Predicate.class);
            Predicate combinado = mock(Predicate.class);

            LocalDateTime dataHoraSaida = LocalDateTime.of(2026, 7, 20, 15, 0);
            LocalDateTime inicioDoDia = LocalDateTime.of(2026, 7, 20, 0, 0);
            LocalDateTime fimDoDia = LocalDateTime.of(2026, 7, 20, 23, 59, 59, 999_000_000);

            when(root.<LocalDateTime>get("dataHoraPartida")).thenReturn(dataPath);
            when(cb.greaterThanOrEqualTo(dataPath, inicioDoDia)).thenReturn(gePredicate);
            when(cb.lessThanOrEqualTo(dataPath, fimDoDia)).thenReturn(lePredicate);
            when(cb.and(gePredicate, lePredicate)).thenReturn(combinado);

            Predicate resultado = CaronaSpecifications.comDataHoraSaida(dataHoraSaida).toPredicate(root, query, cb);

            assertSame(combinado, resultado);
        }
    }

    @Nested
    @DisplayName("comVagasDisponiveis")
    class ComVagasDisponiveis {

        @Test
        @DisplayName("Deve comparar vagasTotais com a soma de passageiros das reservas aceitas")
        void deveCompararVagasTotaisComSomaDeReservasAceitas() {
            Subquery<Integer> subquery = mock(Subquery.class);
            Root<ReservaCarona> reservaRoot = mock(Root.class);
            Path<Integer> quantidadePath = mock(Path.class);
            Expression<Integer> sumExpr = mock(Expression.class);
            Path<Object> caronaPath = mock(Path.class);
            Path<Object> statusPath = mock(Path.class);
            Predicate predCarona = mock(Predicate.class);
            Predicate predStatus = mock(Predicate.class);
            Expression<Integer> coalesceExpr = mock(Expression.class);
            Path<Integer> vagasTotaisPath = mock(Path.class);
            Expression<Integer> diffExpr = mock(Expression.class);
            Predicate resultadoEsperado = mock(Predicate.class);

            when(query.subquery(Integer.class)).thenReturn(subquery);
            when(subquery.from(ReservaCarona.class)).thenReturn(reservaRoot);
            when(reservaRoot.<Integer>get("quantidadePassageiros")).thenReturn(quantidadePath);
            when(cb.sum(quantidadePath)).thenReturn(sumExpr);
            when(reservaRoot.get("carona")).thenReturn(caronaPath);
            when(cb.equal(caronaPath, root)).thenReturn(predCarona);
            when(reservaRoot.get("status")).thenReturn(statusPath);
            when(cb.equal(statusPath, StatusReserva.ACEITA)).thenReturn(predStatus);
            when(cb.coalesce(subquery, 0)).thenReturn(coalesceExpr);
            when(root.<Integer>get("vagasTotais")).thenReturn(vagasTotaisPath);
            when(cb.diff(vagasTotaisPath, coalesceExpr)).thenReturn(diffExpr);
            when(cb.greaterThan(diffExpr, 0)).thenReturn(resultadoEsperado);

            Predicate resultado = CaronaSpecifications.comVagasDisponiveis().toPredicate(root, query, cb);

            assertSame(resultadoEsperado, resultado);
        }
    }

    @Nested
    @DisplayName("semBloqueioBidirecional")
    class SemBloqueioBidirecional {

        @Test
        @DisplayName("Deve excluir caronas com bloqueio em qualquer direção entre os usuários")
        void deveExcluirCaronasComBloqueioBidirecional() {
            Long usuarioAutenticadoId = 42L;

            Subquery<Long> subquery = mock(Subquery.class);
            Root<BloqueioUsuario> bloqueioRoot = mock(Root.class);
            Path<Object> idPath = mock(Path.class);

            Path<Object> usuarioPath = mock(Path.class);
            Path<Object> usuarioIdPath = mock(Path.class);
            Path<Object> usuarioBloqueadoPath = mock(Path.class);
            Path<Object> usuarioBloqueadoIdPath = mock(Path.class);
            Path<Object> motoristaPath = mock(Path.class);
            Path<Object> motoristaIdPath = mock(Path.class);

            Predicate andA = mock(Predicate.class);
            Predicate andB = mock(Predicate.class);
            Predicate or = mock(Predicate.class);
            Predicate exists = mock(Predicate.class);
            Predicate not = mock(Predicate.class);

            when(query.subquery(Long.class)).thenReturn(subquery);
            when(subquery.from(BloqueioUsuario.class)).thenReturn(bloqueioRoot);
            when(bloqueioRoot.get("id")).thenReturn(idPath);

            when(bloqueioRoot.get("usuario")).thenReturn(usuarioPath);
            when(usuarioPath.get("id")).thenReturn(usuarioIdPath);
            when(bloqueioRoot.get("usuarioBloqueado")).thenReturn(usuarioBloqueadoPath);
            when(usuarioBloqueadoPath.get("id")).thenReturn(usuarioBloqueadoIdPath);
            when(root.get("motorista")).thenReturn(motoristaPath);
            when(motoristaPath.get("id")).thenReturn(motoristaIdPath);

            when(cb.equal(usuarioIdPath, usuarioAutenticadoId)).thenReturn(mock(Predicate.class));
            when(cb.equal(usuarioBloqueadoIdPath, motoristaIdPath)).thenReturn(mock(Predicate.class));
            when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(andA, andB);
            when(cb.equal(usuarioIdPath, motoristaIdPath)).thenReturn(mock(Predicate.class));
            when(cb.equal(usuarioBloqueadoIdPath, usuarioAutenticadoId)).thenReturn(mock(Predicate.class));
            when(cb.or(andA, andB)).thenReturn(or);
            when(cb.exists(subquery)).thenReturn(exists);
            when(cb.not(exists)).thenReturn(not);

            Predicate resultado = CaronaSpecifications.semBloqueioBidirecional(usuarioAutenticadoId)
                    .toPredicate(root, query, cb);

            assertSame(not, resultado);
        }
    }
}
