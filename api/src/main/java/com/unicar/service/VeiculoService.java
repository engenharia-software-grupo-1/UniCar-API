package com.unicar.service;

import com.unicar.domain.Usuario;
import com.unicar.domain.Veiculo;
import com.unicar.dto.veiculo.VeiculoRequestDTO;
import com.unicar.dto.veiculo.VeiculoResponseDTO;
import com.unicar.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;

    public List<VeiculoResponseDTO> listarPorUsuario(Long usuarioId) {
        return veiculoRepository.findAllByUsuarioId(usuarioId).stream()
            .map(VeiculoResponseDTO::from)
            .toList();
    }

    public VeiculoResponseDTO buscarPorId(Long usuarioId, Long veiculoId) {
        return VeiculoResponseDTO.from(buscarVeiculo(usuarioId, veiculoId));
    }

    @Transactional
    public VeiculoResponseDTO criar(Long usuarioId, VeiculoRequestDTO request) {
        Veiculo veiculo = Veiculo.builder()
            .usuario(Usuario.builder().id(usuarioId).build())
            .modelo(request.modelo())
            .placa(request.placa())
            .cor(request.cor())
            .tipoVeiculo(request.tipoVeiculo())
            .build();

        return VeiculoResponseDTO.from(veiculoRepository.save(veiculo));
    }

    @Transactional
    public VeiculoResponseDTO atualizar(Long usuarioId, Long veiculoId, VeiculoRequestDTO request) {
        Veiculo veiculo = buscarVeiculo(usuarioId, veiculoId);

        veiculo.setModelo(request.modelo());
        veiculo.setPlaca(request.placa());
        veiculo.setCor(request.cor());
        veiculo.setTipoVeiculo(request.tipoVeiculo());

        return VeiculoResponseDTO.from(veiculoRepository.save(veiculo));
    }

    @Transactional
    public void excluir(Long usuarioId, Long veiculoId) {
        Veiculo veiculo = buscarVeiculo(usuarioId, veiculoId);
        veiculoRepository.delete(veiculo);
    }

    private Veiculo buscarVeiculo(Long usuarioId, Long veiculoId) {
        return veiculoRepository.findByIdAndUsuarioId(veiculoId, usuarioId)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Veículo não encontrado"));
    }
}