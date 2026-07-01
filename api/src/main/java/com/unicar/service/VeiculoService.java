package com.unicar.service;

import com.unicar.domain.Usuario;
import com.unicar.domain.Veiculo;
import com.unicar.dto.VeiculoRequest;
import com.unicar.dto.VeiculoResponse;
import com.unicar.repository.UsuarioRepository;
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
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public VeiculoResponse criar(VeiculoRequest request) {
        Usuario usuario = getUsuarioAtual();
        validarPlacaDisponivel(request.placa());

        Veiculo veiculo = new Veiculo();
        veiculo.setUsuario(usuario);
        veiculo.setModelo(request.modelo().trim());
        veiculo.setPlaca(request.placa().trim().toUpperCase());
        veiculo.setCor(request.cor() != null ? request.cor().trim() : null);

        return toResponse(veiculoRepository.save(veiculo));
    }

    @Transactional(readOnly = true)
    public List<VeiculoResponse> listar() {
        Usuario usuario = getUsuarioAtual();
        return veiculoRepository.findByUsuarioOrderByIdAsc(usuario)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VeiculoResponse buscarPorId(Long id) {
        Usuario usuario = getUsuarioAtual();
        Veiculo veiculo = veiculoRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veículo não encontrado"));
        return toResponse(veiculo);
    }

    @Transactional
    public VeiculoResponse atualizar(Long id, VeiculoRequest request) {
        Usuario usuario = getUsuarioAtual();
        Veiculo veiculo = veiculoRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veículo não encontrado"));

        if (veiculoRepository.existsByPlacaIgnoreCaseAndIdNot(request.placa().trim().toUpperCase(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Placa já cadastrada");
        }

        veiculo.setModelo(request.modelo().trim());
        veiculo.setPlaca(request.placa().trim().toUpperCase());
        veiculo.setCor(request.cor() != null ? request.cor().trim() : null);

        return toResponse(veiculoRepository.save(veiculo));
    }

    @Transactional
    public void excluir(Long id) {
        Usuario usuario = getUsuarioAtual();
        Veiculo veiculo = veiculoRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veículo não encontrado"));
        veiculoRepository.delete(veiculo);
    }

    private Usuario getUsuarioAtual() {
        return usuarioRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nenhum usuário autenticado"));
    }

    private void validarPlacaDisponivel(String placa) {
        String placaNormalizada = placa.trim().toUpperCase();
        if (veiculoRepository.existsByPlacaIgnoreCase(placaNormalizada)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Placa já cadastrada");
        }
    }

    private VeiculoResponse toResponse(Veiculo veiculo) {
        return new VeiculoResponse(
                veiculo.getId(),
                veiculo.getModelo(),
                veiculo.getPlaca(),
                veiculo.getCor()
        );
    }
}
