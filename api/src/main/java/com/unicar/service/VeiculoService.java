package com.unicar.service;

import com.unicar.domain.Usuario;
import com.unicar.domain.Veiculo;
import com.unicar.dto.veiculo.VeiculoRequestDTO;
import com.unicar.dto.veiculo.VeiculoResponseDTO;
import com.unicar.exception.RegraDeNegocioException;
import com.unicar.exception.VeiculoNaoEncontradoException;
import com.unicar.repository.CaronaRepository;
import com.unicar.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final CaronaRepository caronaRepository;

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
        validarPlacar(veiculo.getPlaca());

        return VeiculoResponseDTO.from(veiculoRepository.save(veiculo));
    }

    @Transactional
    public VeiculoResponseDTO atualizar(Long usuarioId, Long veiculoId, VeiculoRequestDTO request) {
        Veiculo veiculo = buscarVeiculo(usuarioId, veiculoId);

        validarPlacaParaAtualizacao(request.placa(), veiculoId);

        veiculo.setModelo(request.modelo());
        veiculo.setPlaca(request.placa());
        veiculo.setCor(request.cor());
        veiculo.setTipoVeiculo(request.tipoVeiculo());

        return VeiculoResponseDTO.from(veiculoRepository.save(veiculo));
    }

    @Transactional
    public void excluir(Long usuarioId, Long veiculoId) {
        Veiculo veiculo = buscarVeiculo(usuarioId, veiculoId);
        if(!veiculo.getUsuario().getId().equals(usuarioId)){
            throw new RegraDeNegocioException(
                    "O usuário só pode excluir veículos dele mesmo.");
        }

        if (caronaRepository.existsByVeiculoId(veiculoId)) {
            throw new RegraDeNegocioException(
                    "Não é possível excluir este veículo, pois ele já está vinculado a uma ou mais caronas.");
        }

        veiculoRepository.delete(veiculo);
    }
    private Veiculo buscarVeiculo(Long usuarioId, Long veiculoId) {
        return veiculoRepository.findByIdAndUsuarioId(veiculoId, usuarioId)
            .orElseThrow(() -> new VeiculoNaoEncontradoException("Veículo não encontrado"));
    }

    private void validarPlacar(String placa){
        if(veiculoRepository.existsByPlaca(placa)){
            throw new RegraDeNegocioException(
                    "Já existe um veículo com a placa informada.");
        }
    }

    private void validarPlacaParaAtualizacao(String placa, Long veiculoId) {
        if (veiculoRepository.existsByPlacaAndIdNot(placa, veiculoId)) {
            throw new RegraDeNegocioException(
                    "Já existe um veículo com a placa informada.");
        }
    }
}