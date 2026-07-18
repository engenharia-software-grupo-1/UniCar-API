package com.unicar.service.carona;

import com.unicar.domain.Carona;
import com.unicar.dto.carona.CaronaRequestDTO;
import com.unicar.dto.carona.CaronaResponseDTO;
import com.unicar.dto.carona.EnderecoDTO;
import com.unicar.dto.trajeto.TrajetoRecorrenteDTO;
import com.unicar.dto.trajeto.TrajetoRecorrenteDetalhesDTO;
import com.unicar.dto.trajeto.TrajetoRecorrenteRecriarRequestDTO;
import com.unicar.dto.trajeto.TrajetoRecorrenteRecriarResponseDTO;
import com.unicar.exception.TrajetoRecorrenteNaoEncontradoException;
import com.unicar.repository.CaronaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Trajetos recorrentes não têm entidade própria: são calculados em tempo de
 * consulta a partir do histórico de Carona do motorista autenticado, agrupando
 * por origem/destino. Nada aqui é persistido.
 */
@Service
@RequiredArgsConstructor
public class TrajetoRecorrenteService {

    private static final int MINIMO_VIAGENS_RECORRENTE = 2;

    private final CaronaRepository caronaRepository;
    private final CaronaService caronaService;

    public List<TrajetoRecorrenteDTO> listar(Long motoristaId) {
        return agruparTrajetosRecorrentes(motoristaId).stream()
                .map(grupo -> {
                    Carona modelo = grupo.caronas().getFirst();
                    return new TrajetoRecorrenteDTO(
                            grupo.id(),
                            enderecoOrigem(modelo),
                            enderecoDestino(modelo),
                            grupo.caronas().size(),
                            ultimaUtilizacao(grupo.caronas()));
                })
                .toList();
    }

    public TrajetoRecorrenteDetalhesDTO buscar(String id, Long motoristaId) {
        Grupo grupo = localizarGrupo(id, motoristaId);
        Carona modelo = grupo.caronas().getFirst();

        return new TrajetoRecorrenteDetalhesDTO(
                grupo.id(),
                enderecoOrigem(modelo),
                enderecoDestino(modelo),
                grupo.caronas().size(),
                primeiraUtilizacao(grupo.caronas()),
                ultimaUtilizacao(grupo.caronas()));
    }

    @Transactional
    public TrajetoRecorrenteRecriarResponseDTO recriar(String id, TrajetoRecorrenteRecriarRequestDTO request, Long motoristaId) {
        Grupo grupo = localizarGrupo(id, motoristaId);
        Carona modelo = grupo.caronas().getFirst();

        CaronaRequestDTO caronaRequest = new CaronaRequestDTO(
                request.veiculoId(),
                enderecoOrigem(modelo),
                enderecoDestino(modelo),
                request.pontoEncontro(),
                List.of(request.dataHoraSaida()),
                request.quantidadeVagas(),
                request.valorContribuicao(),
                null
        );

        List<CaronaResponseDTO> caronasCriadas = caronaService.criar(caronaRequest, motoristaId);
        CaronaResponseDTO caronaCriada = caronasCriadas.getFirst();

        return new TrajetoRecorrenteRecriarResponseDTO(caronaCriada.id(), caronaCriada.status());
    }

    private Grupo localizarGrupo(String id, Long motoristaId) {
        return agruparTrajetosRecorrentes(motoristaId).stream()
                .filter(grupo -> grupo.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new TrajetoRecorrenteNaoEncontradoException("Trajeto recorrente não encontrado"));
    }

    private List<Grupo> agruparTrajetosRecorrentes(Long motoristaId) {
        return caronaRepository.findByMotorista_Id(motoristaId).stream()
                .collect(Collectors.groupingBy(TrajetoKey::from))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() >= MINIMO_VIAGENS_RECORRENTE)
                .map(entry -> new Grupo(gerarId(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparingInt((Grupo grupo) -> grupo.caronas().size()).reversed())
                .toList();
    }

    private static String gerarId(TrajetoKey chave) {
        String chaveTexto = chave.origemLatitude() + "|" + chave.origemLongitude()
                + "|" + chave.destinoLatitude() + "|" + chave.destinoLongitude();
        return UUID.nameUUIDFromBytes(chaveTexto.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static EnderecoDTO enderecoOrigem(Carona carona) {
        return new EnderecoDTO(carona.getOrigemDescricao(), carona.getOrigemLatitude(), carona.getOrigemLongitude());
    }

    private static EnderecoDTO enderecoDestino(Carona carona) {
        return new EnderecoDTO(carona.getDestinoDescricao(), carona.getDestinoLatitude(), carona.getDestinoLongitude());
    }

    private static LocalDateTime primeiraUtilizacao(List<Carona> caronas) {
        return caronas.stream().map(Carona::getDataHoraPartida).min(LocalDateTime::compareTo).orElseThrow();
    }

    private static LocalDateTime ultimaUtilizacao(List<Carona> caronas) {
        return caronas.stream().map(Carona::getDataHoraPartida).max(LocalDateTime::compareTo).orElseThrow();
    }

    private record TrajetoKey(BigDecimal origemLatitude, BigDecimal origemLongitude,
                               BigDecimal destinoLatitude, BigDecimal destinoLongitude) {
        static TrajetoKey from(Carona carona) {
            return new TrajetoKey(
                    carona.getOrigemLatitude(), carona.getOrigemLongitude(),
                    carona.getDestinoLatitude(), carona.getDestinoLongitude());
        }
    }

    private record Grupo(String id, List<Carona> caronas) {}
}
