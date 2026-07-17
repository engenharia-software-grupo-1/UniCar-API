package com.unicar.service;

import com.unicar.domain.InteresseTrajeto;
import com.unicar.dto.interesseTrajeto.CoordenadaDTO;
import com.unicar.dto.interesseTrajeto.InteresseTrajetoCriadoDTO;
import com.unicar.dto.interesseTrajeto.InteresseTrajetoDTO;
import com.unicar.dto.interesseTrajeto.InteresseTrajetoRequest;
import com.unicar.exception.RegraDeNegocioException;
import com.unicar.repository.InteresseTrajetoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InteresseTrajetoService{

    private final InteresseTrajetoRepository repository;

    public InteresseTrajetoCriadoDTO cadastrar(Long usuarioId, InteresseTrajetoRequest request) {

        boolean existe = repository
                .existsByUsuarioIdAndOrigemLatitudeAndOrigemLongitudeAndDestinoLatitudeAndDestinoLongitude(
                        usuarioId,
                        request.origem().latitude(),
                        request.origem().longitude(),
                        request.destino().latitude(),
                        request.destino().longitude());

        if (existe) {
            throw new RegraDeNegocioException("Interesse já cadastrado.");
        }

        InteresseTrajeto interesse = InteresseTrajeto.builder()
                .usuarioId(usuarioId)
                .origemLatitude(request.origem().latitude())
                .origemLongitude(request.origem().longitude())
                .destinoLatitude(request.destino().latitude())
                .destinoLongitude(request.destino().longitude())
                .dataRegistro(LocalDateTime.now())
                .build();

        return new InteresseTrajetoCriadoDTO(repository.save(interesse).getId());
    }

    public List<InteresseTrajetoDTO> listar(Long usuarioId) {

        return repository.findByUsuarioId(usuarioId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void remover(Long usuarioId, Long interesseId) {

        InteresseTrajeto interesse = repository
                .findByIdAndUsuarioId(interesseId, usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Interesse não encontrado."));

        repository.delete(interesse);
    }

    private InteresseTrajetoDTO toResponse(InteresseTrajeto entity) {

        return new InteresseTrajetoDTO(
                entity.getId(),
                new CoordenadaDTO(
                        entity.getOrigemLatitude(),
                        entity.getOrigemLongitude()
                ),
                new CoordenadaDTO(
                        entity.getDestinoLatitude(),
                        entity.getDestinoLongitude()
                )
        );
    }
}