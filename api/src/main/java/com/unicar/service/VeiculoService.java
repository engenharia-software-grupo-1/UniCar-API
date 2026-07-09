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

    public List<VeiculoResponseDTO> listarPorUsuario(Usuario usuario) {
        return veiculoRepository.findAllByUsuario(usuario).stream()
            .map(VeiculoResponseDTO::from)
            .toList();
    }

    public VeiculoResponseDTO buscarPorId(Usuario usuario, Long veiculoId) {
        return VeiculoResponseDTO.from(buscarVeiculo(usuario, veiculoId));
    }

    @Transactional
    public VeiculoResponseDTO criar(Usuario usuario, VeiculoRequestDTO request) {
        Veiculo veiculo = Veiculo.builder()
            .usuario(usuario)
            .modelo(request.modelo())
            .placa(request.placa())
            .cor(request.cor())
            .tipoVeiculo(request.tipoVeiculo())
            .build();

        return VeiculoResponseDTO.from(veiculoRepository.save(veiculo));
    }

    @Transactional
    public VeiculoResponseDTO atualizar(Usuario usuario, Long veiculoId, VeiculoRequestDTO request) {
        Veiculo veiculo = buscarVeiculo(usuario, veiculoId);

        veiculo.setModelo(request.modelo());
        veiculo.setPlaca(request.placa());
        veiculo.setCor(request.cor());
        veiculo.setTipoVeiculo(request.tipoVeiculo());

        return VeiculoResponseDTO.from(veiculoRepository.save(veiculo));
    }

    @Transactional
    public void excluir(Usuario usuario, Long veiculoId) {
        Veiculo veiculo = buscarVeiculo(usuario, veiculoId);
        veiculoRepository.delete(veiculo);
    }

    private Veiculo buscarVeiculo(Usuario usuario, Long veiculoId) {
        return veiculoRepository.findByIdAndUsuario(veiculoId, usuario)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Veículo não encontrado"));
    }
}