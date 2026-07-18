package com.unicar.service.carona;

import com.unicar.domain.Carona;
import com.unicar.domain.Usuario;
import com.unicar.dto.carona.CaronaRequestDTO;
import com.unicar.dto.carona.CaronaResponseDTO;
import com.unicar.dto.trajeto.TrajetoRecorrenteDTO;
import com.unicar.dto.trajeto.TrajetoRecorrenteDetalhesDTO;
import com.unicar.dto.trajeto.TrajetoRecorrenteRecriarRequestDTO;
import com.unicar.dto.trajeto.TrajetoRecorrenteRecriarResponseDTO;
import com.unicar.enums.StatusCarona;
import com.unicar.exception.TrajetoRecorrenteNaoEncontradoException;
import com.unicar.repository.CaronaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrajetoRecorrenteServiceTest {

    @Mock
    private CaronaRepository caronaRepository;

    @Mock
    private CaronaService caronaService;

    @InjectMocks
    private TrajetoRecorrenteService trajetoRecorrenteService;

    private final Long motoristaId = 10L;
    private final Long outroMotoristaId = 20L;

    private static final BigDecimal ORIGEM_LAT = new BigDecimal("-7.22000000");
    private static final BigDecimal ORIGEM_LON = new BigDecimal("-35.91000000");
    private static final BigDecimal DESTINO_LAT = new BigDecimal("-7.23000000");
    private static final BigDecimal DESTINO_LON = new BigDecimal("-35.87000000");

    private static final BigDecimal OUTRA_ORIGEM_LAT = new BigDecimal("-7.10000000");
    private static final BigDecimal OUTRA_ORIGEM_LON = new BigDecimal("-35.80000000");
    private static final BigDecimal OUTRO_DESTINO_LAT = new BigDecimal("-7.11000000");
    private static final BigDecimal OUTRO_DESTINO_LON = new BigDecimal("-35.81000000");

    private Usuario motorista;

    @BeforeEach
    void setUp() {
        motorista = new Usuario();
        motorista.setId(motoristaId);
    }

    private Carona criarCarona(BigDecimal origemLat, BigDecimal origemLon, BigDecimal destinoLat, BigDecimal destinoLon,
                                LocalDateTime dataHoraPartida) {
        Carona carona = new Carona();
        carona.setMotorista(motorista);
        carona.setOrigemDescricao("Bodocongó");
        carona.setOrigemLatitude(origemLat);
        carona.setOrigemLongitude(origemLon);
        carona.setDestinoDescricao("UFCG");
        carona.setDestinoLatitude(destinoLat);
        carona.setDestinoLongitude(destinoLon);
        carona.setDataHoraPartida(dataHoraPartida);
        return carona;
    }

    @Nested
    @DisplayName("Listar trajetos recorrentes")
    class Listar {

        @Test
        @DisplayName("Deve retornar lista vazia quando o motorista não tem caronas")
        void deveRetornarListaVaziaSemCaronas() {
            when(caronaRepository.findByMotorista_Id(motoristaId)).thenReturn(List.of());

            List<TrajetoRecorrenteDTO> resultado = trajetoRecorrenteService.listar(motoristaId);

            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("Não deve considerar trajeto com apenas uma viagem")
        void naoDeveConsiderarTrajetoComUmaViagem() {
            List<Carona> caronas = List.of(
                    criarCarona(ORIGEM_LAT, ORIGEM_LON, DESTINO_LAT, DESTINO_LON, LocalDateTime.now().minusDays(10))
            );
            when(caronaRepository.findByMotorista_Id(motoristaId)).thenReturn(caronas);

            List<TrajetoRecorrenteDTO> resultado = trajetoRecorrenteService.listar(motoristaId);

            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("Deve agrupar viagens com mesma origem/destino e calcular quantidade e última utilização")
        void deveAgruparEcalcularQuantidadeEUltimaUtilizacao() {
            LocalDateTime maisAntiga = LocalDateTime.now().minusDays(30);
            LocalDateTime maisRecente = LocalDateTime.now().minusDays(1);

            List<Carona> caronas = List.of(
                    criarCarona(ORIGEM_LAT, ORIGEM_LON, DESTINO_LAT, DESTINO_LON, maisAntiga),
                    criarCarona(ORIGEM_LAT, ORIGEM_LON, DESTINO_LAT, DESTINO_LON, maisRecente)
            );
            when(caronaRepository.findByMotorista_Id(motoristaId)).thenReturn(caronas);

            List<TrajetoRecorrenteDTO> resultado = trajetoRecorrenteService.listar(motoristaId);

            assertEquals(1, resultado.size());
            TrajetoRecorrenteDTO trajeto = resultado.getFirst();
            assertEquals(2, trajeto.quantidadeViagens());
            assertEquals(maisRecente, trajeto.ultimaUtilizacao());
            assertEquals("Bodocongó", trajeto.origem().descricao());
            assertEquals("UFCG", trajeto.destino().descricao());
            assertNotNull(trajeto.id());
        }

        @Test
        @DisplayName("Deve ordenar trajetos pela quantidade de viagens em ordem decrescente")
        void deveOrdenarPorQuantidadeDecrescente() {
            List<Carona> caronas = new ArrayList<>();
            // Trajeto A: 2 viagens
            caronas.add(criarCarona(ORIGEM_LAT, ORIGEM_LON, DESTINO_LAT, DESTINO_LON, LocalDateTime.now().minusDays(5)));
            caronas.add(criarCarona(ORIGEM_LAT, ORIGEM_LON, DESTINO_LAT, DESTINO_LON, LocalDateTime.now().minusDays(4)));
            // Trajeto B: 3 viagens
            caronas.add(criarCarona(OUTRA_ORIGEM_LAT, OUTRA_ORIGEM_LON, OUTRO_DESTINO_LAT, OUTRO_DESTINO_LON, LocalDateTime.now().minusDays(3)));
            caronas.add(criarCarona(OUTRA_ORIGEM_LAT, OUTRA_ORIGEM_LON, OUTRO_DESTINO_LAT, OUTRO_DESTINO_LON, LocalDateTime.now().minusDays(2)));
            caronas.add(criarCarona(OUTRA_ORIGEM_LAT, OUTRA_ORIGEM_LON, OUTRO_DESTINO_LAT, OUTRO_DESTINO_LON, LocalDateTime.now().minusDays(1)));

            when(caronaRepository.findByMotorista_Id(motoristaId)).thenReturn(caronas);

            List<TrajetoRecorrenteDTO> resultado = trajetoRecorrenteService.listar(motoristaId);

            assertEquals(2, resultado.size());
            assertEquals(3, resultado.get(0).quantidadeViagens());
            assertEquals(2, resultado.get(1).quantidadeViagens());
        }

        @Test
        @DisplayName("Deve gerar sempre o mesmo id para o mesmo trajeto")
        void deveGerarIdDeterministico() {
            List<Carona> caronas = List.of(
                    criarCarona(ORIGEM_LAT, ORIGEM_LON, DESTINO_LAT, DESTINO_LON, LocalDateTime.now().minusDays(5)),
                    criarCarona(ORIGEM_LAT, ORIGEM_LON, DESTINO_LAT, DESTINO_LON, LocalDateTime.now().minusDays(4))
            );
            when(caronaRepository.findByMotorista_Id(motoristaId)).thenReturn(caronas);

            String id1 = trajetoRecorrenteService.listar(motoristaId).getFirst().id();
            String id2 = trajetoRecorrenteService.listar(motoristaId).getFirst().id();

            assertEquals(id1, id2);
        }
    }

    @Nested
    @DisplayName("Buscar detalhes de um trajeto recorrente")
    class Buscar {

        @Test
        @DisplayName("Deve retornar detalhes com primeira e última utilização corretas")
        void deveRetornarDetalhes() {
            LocalDateTime primeira = LocalDateTime.now().minusDays(30);
            LocalDateTime ultima = LocalDateTime.now().minusDays(1);

            List<Carona> caronas = List.of(
                    criarCarona(ORIGEM_LAT, ORIGEM_LON, DESTINO_LAT, DESTINO_LON, ultima),
                    criarCarona(ORIGEM_LAT, ORIGEM_LON, DESTINO_LAT, DESTINO_LON, primeira)
            );
            when(caronaRepository.findByMotorista_Id(motoristaId)).thenReturn(caronas);

            String id = trajetoRecorrenteService.listar(motoristaId).getFirst().id();
            TrajetoRecorrenteDetalhesDTO detalhes = trajetoRecorrenteService.buscar(id, motoristaId);

            assertEquals(id, detalhes.id());
            assertEquals(2, detalhes.quantidadeViagens());
            assertEquals(primeira, detalhes.primeiraUtilizacao());
            assertEquals(ultima, detalhes.ultimaUtilizacao());
        }

        @Test
        @DisplayName("Deve lançar erro quando o id não corresponder a nenhum trajeto")
        void deveLancarErroQuandoIdNaoExistir() {
            when(caronaRepository.findByMotorista_Id(motoristaId)).thenReturn(List.of());

            assertThrows(TrajetoRecorrenteNaoEncontradoException.class, () ->
                    trajetoRecorrenteService.buscar("id-inexistente", motoristaId));
        }

        @Test
        @DisplayName("Deve lançar erro quando o trajeto tiver apenas uma viagem")
        void deveLancarErroQuandoTrajetoComUmaViagem() {
            List<Carona> caronas = List.of(
                    criarCarona(ORIGEM_LAT, ORIGEM_LON, DESTINO_LAT, DESTINO_LON, LocalDateTime.now())
            );
            when(caronaRepository.findByMotorista_Id(motoristaId)).thenReturn(caronas);

            assertThrows(TrajetoRecorrenteNaoEncontradoException.class, () ->
                    trajetoRecorrenteService.buscar("qualquer-id", motoristaId));
        }

        @Test
        @DisplayName("Não deve encontrar trajeto de outro motorista")
        void naoDeveEncontrarTrajetoDeOutroMotorista() {
            List<Carona> caronasDoMotorista = List.of(
                    criarCarona(ORIGEM_LAT, ORIGEM_LON, DESTINO_LAT, DESTINO_LON, LocalDateTime.now().minusDays(5)),
                    criarCarona(ORIGEM_LAT, ORIGEM_LON, DESTINO_LAT, DESTINO_LON, LocalDateTime.now().minusDays(4))
            );
            when(caronaRepository.findByMotorista_Id(motoristaId)).thenReturn(caronasDoMotorista);
            String idDoMotorista = trajetoRecorrenteService.listar(motoristaId).getFirst().id();

            when(caronaRepository.findByMotorista_Id(outroMotoristaId)).thenReturn(List.of());

            assertThrows(TrajetoRecorrenteNaoEncontradoException.class, () ->
                    trajetoRecorrenteService.buscar(idDoMotorista, outroMotoristaId));
        }
    }

    @Nested
    @DisplayName("Recriar carona a partir de um trajeto recorrente")
    class Recriar {

        @Test
        @DisplayName("Deve montar a carona reaproveitando origem/destino do trajeto e delegar para CaronaService")
        void deveRecriarReaproveitandoOrigemEDestino() {
            List<Carona> caronas = List.of(
                    criarCarona(ORIGEM_LAT, ORIGEM_LON, DESTINO_LAT, DESTINO_LON, LocalDateTime.now().minusDays(5)),
                    criarCarona(ORIGEM_LAT, ORIGEM_LON, DESTINO_LAT, DESTINO_LON, LocalDateTime.now().minusDays(4))
            );
            when(caronaRepository.findByMotorista_Id(motoristaId)).thenReturn(caronas);
            String id = trajetoRecorrenteService.listar(motoristaId).getFirst().id();

            LocalDateTime novaData = LocalDateTime.now().plusDays(7);
            TrajetoRecorrenteRecriarRequestDTO request = new TrajetoRecorrenteRecriarRequestDTO(
                    99L, novaData, 3, new BigDecimal("7.50"), "Portão principal");

            CaronaResponseDTO caronaResponse = new CaronaResponseDTO(500L, StatusCarona.CRIADA);
            when(caronaService.criar(any(CaronaRequestDTO.class), eq(motoristaId)))
                    .thenReturn(List.of(caronaResponse));

            TrajetoRecorrenteRecriarResponseDTO resultado =
                    trajetoRecorrenteService.recriar(id, request, motoristaId);

            assertEquals(500L, resultado.caronaId());
            assertEquals(StatusCarona.CRIADA, resultado.status());

            ArgumentCaptor<CaronaRequestDTO> captor = ArgumentCaptor.forClass(CaronaRequestDTO.class);
            verify(caronaService).criar(captor.capture(), eq(motoristaId));
            CaronaRequestDTO enviado = captor.getValue();

            assertEquals(99L, enviado.veiculoId());
            assertEquals("Bodocongó", enviado.origem().descricao());
            assertEquals(ORIGEM_LAT, enviado.origem().latitude());
            assertEquals("UFCG", enviado.destino().descricao());
            assertEquals(DESTINO_LAT, enviado.destino().latitude());
            assertEquals("Portão principal", enviado.pontoEncontro());
            assertEquals(List.of(novaData), enviado.datasHorasSaida());
            assertEquals(3, enviado.quantidadeVagas());
            assertEquals(new BigDecimal("7.50"), enviado.valorContribuicao());
            assertNull(enviado.observacao());
        }

        @Test
        @DisplayName("Deve lançar erro quando o trajeto não existir")
        void deveLancarErroQuandoTrajetoNaoExistir() {
            when(caronaRepository.findByMotorista_Id(motoristaId)).thenReturn(List.of());

            TrajetoRecorrenteRecriarRequestDTO request = new TrajetoRecorrenteRecriarRequestDTO(
                    99L, LocalDateTime.now().plusDays(7), 3, new BigDecimal("7.50"), "Portão principal");

            assertThrows(TrajetoRecorrenteNaoEncontradoException.class, () ->
                    trajetoRecorrenteService.recriar("id-inexistente", request, motoristaId));
        }
    }
}
