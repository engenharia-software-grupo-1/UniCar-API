package com.unicar.service.carona;

import com.unicar.domain.Carona;
import com.unicar.domain.Usuario;
import com.unicar.dto.avaliacao.ReputacaoDTO;
import com.unicar.dto.carona.BuscaCaronaFiltroDTO;
import com.unicar.dto.carona.CaronaBuscaResponseDTO;
import com.unicar.exception.RegraDeNegocioException;
import com.unicar.repository.CaronaRepository;
import com.unicar.service.AvaliacaoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuscaCaronaServiceTest {

    @Mock
    private CaronaRepository caronaRepository;
    @Mock
    private AvaliacaoService avaliacaoService;
    @InjectMocks
    private BuscaCaronaService buscaCaronaService;

    private final Long caronaId = 1L;
    private final Long motoristaId = 10L;
    private final Long usuarioAutenticadoId = 99L;

    private Carona carona;

    @BeforeEach
    void setUp() {
        Usuario motorista = new Usuario();
        motorista.setId(motoristaId);

        carona = new Carona();
        carona.setId(caronaId);
        carona.setMotorista(motorista);
        carona.setDataHoraPartida(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Buscar caronas disponíveis")
    class BuscarCaronasDisponiveis {

        @Test
        @DisplayName("Deve lançar exceção quando raio ultrapassa o máximo permitido")
        void deveLancarErroQuandoRaioUltrapassaMaximo() {
            BuscaCaronaFiltroDTO filtro = new BuscaCaronaFiltroDTO(
                    null, null, null, null, null, null, 150.0, null);

            assertThrows(RegraDeNegocioException.class, () ->
                    buscaCaronaService.buscarCaronasDisponiveis(filtro, usuarioAutenticadoId)
            );
            verifyNoInteractions(caronaRepository);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há caronas candidatas")
        void deveRetornarListaVaziaQuandoNaoHaCandidatas() {
            BuscaCaronaFiltroDTO filtro = new BuscaCaronaFiltroDTO(
                    null, null, null, null, null, null, null, null);

            when(caronaRepository.findAll(any(Specification.class))).thenReturn(List.of());

            List<CaronaBuscaResponseDTO> resultado =
                    buscaCaronaService.buscarCaronasDisponiveis(filtro, usuarioAutenticadoId);

            assertNotNull(resultado);
            assertTrue(resultado.isEmpty());
            verifyNoInteractions(avaliacaoService);
        }

        @Test
        @DisplayName("Deve mapear carona candidata incluindo reputação do motorista")
        void deveMapearCaronaComReputacao() {
            BuscaCaronaFiltroDTO filtro = new BuscaCaronaFiltroDTO(
                    null, null, null, null, null, null, null, null);

            carona.setOrigemDescricao("Partage Shopping");
            carona.setOrigemLatitude(new BigDecimal("-7.23490000"));
            carona.setOrigemLongitude(new BigDecimal("-35.86920000"));
            carona.setDestinoDescricao("UFCG");
            carona.setDestinoLatitude(new BigDecimal("-7.21456000"));
            carona.setDestinoLongitude(new BigDecimal("-35.90872000"));
            carona.setVagasTotais(4);
            carona.setValorContribuicao(new BigDecimal("2.00"));
            carona.getMotorista().setNome("Jennifer");
            carona.getMotorista().setCurso("Ciência da Computação");

            when(caronaRepository.findAll(any(Specification.class))).thenReturn(List.of(carona));
            when(avaliacaoService.buscarReputacoes(List.of(motoristaId)))
                    .thenReturn(List.of(new ReputacaoDTO(motoristaId, 4.0, 10L)));

            List<CaronaBuscaResponseDTO> resultado =
                    buscaCaronaService.buscarCaronasDisponiveis(filtro, usuarioAutenticadoId);

            assertEquals(1, resultado.size());
            CaronaBuscaResponseDTO dto = resultado.getFirst();
            assertEquals(caronaId, dto.id());
            assertEquals("Partage Shopping", dto.origem().descricao());
            assertEquals("Jennifer", dto.motorista().nome());
            assertEquals(4.0, dto.motorista().reputacao());
            verify(avaliacaoService).buscarReputacoes(List.of(motoristaId));
        }

        @Test
        @DisplayName("Deve excluir caronas fora do raio exato mesmo dentro do bounding box")
        void deveFiltrarPorDistanciaExata() {
            BigDecimal origemLat = new BigDecimal("-7.22850");
            BigDecimal origemLon = new BigDecimal("-35.87120");

            BuscaCaronaFiltroDTO filtro = new BuscaCaronaFiltroDTO(
                    origemLat, origemLon, null, null, null, null, 1.0, null);

            carona.setOrigemLatitude(new BigDecimal("-7.50000"));
            carona.setOrigemLongitude(new BigDecimal("-36.50000"));
            carona.setDestinoLatitude(new BigDecimal("-7.21456000"));
            carona.setDestinoLongitude(new BigDecimal("-35.90872000"));
            carona.setVagasTotais(4);

            when(caronaRepository.findAll(any(Specification.class))).thenReturn(List.of(carona));

            List<CaronaBuscaResponseDTO> resultado =
                    buscaCaronaService.buscarCaronasDisponiveis(filtro, usuarioAutenticadoId);

            assertTrue(resultado.isEmpty());
            verifyNoInteractions(avaliacaoService);
        }
    }

    @Nested
    @DisplayName("Buscar caronas próximas")
    class BuscarProximas {

        @Test
        @DisplayName("Deve lançar exceção quando latitude ou longitude não são informadas")
        void deveLancarErroSemCoordenadas() {
            assertThrows(RegraDeNegocioException.class, () ->
                    buscaCaronaService.buscarProximas(null, new BigDecimal("-35.87"), 5.0)
            );
            verifyNoInteractions(caronaRepository);
        }

        @Test
        @DisplayName("Deve lançar exceção quando raio ultrapassa o máximo permitido")
        void deveLancarErroQuandoRaioUltrapassaMaximo() {
            assertThrows(RegraDeNegocioException.class, () ->
                    buscaCaronaService.buscarProximas(new BigDecimal("-7.22"), new BigDecimal("-35.87"), 150.0)
            );
            verifyNoInteractions(caronaRepository);
        }
    }
}