package com.unicar.controller;

import com.unicar.domain.Usuario;
import com.unicar.domain.Veiculo;
import com.unicar.dto.VeiculoRequest;
import com.unicar.dto.VeiculoResponse;
import com.unicar.repository.UsuarioRepository;
import com.unicar.repository.VeiculoRepository;
import com.unicar.service.VeiculoService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VeiculoControllerTest {

    @Test
    void deveCriarVeiculoComSucesso() {
        UsuarioRepository usuarioRepository = usuarioRepository();
        VeiculoRepository veiculoRepository = veiculoRepository();
        VeiculoService service = new VeiculoService(veiculoRepository, usuarioRepository);

        VeiculoResponse response = service.criar(new VeiculoRequest("Onix", "ABC1D23", "Prata"));

        assertEquals(1L, response.id());
        assertEquals("Onix", response.modelo());
        assertEquals("ABC1D23", response.placa());
        assertEquals("Prata", response.cor());
        assertEquals(1, veiculoRepository.findAll().size());
    }

    @Test
    void deveListarVeiculosDoUsuarioAtual() {
        UsuarioRepository usuarioRepository = usuarioRepository();
        VeiculoRepository veiculoRepository = veiculoRepository();
        VeiculoService service = new VeiculoService(veiculoRepository, usuarioRepository);

        service.criar(new VeiculoRequest("HB20", "XYZ9A87", "Branco"));
        service.criar(new VeiculoRequest("Civic", "LMN4P56", "Preto"));

        List<VeiculoResponse> veiculos = service.listar();

        assertEquals(2, veiculos.size());
        assertEquals("HB20", veiculos.get(0).modelo());
        assertEquals("XYZ9A87", veiculos.get(0).placa());
    }

    @Test
    void deveRecusarPlacaDuplicada() {
        UsuarioRepository usuarioRepository = usuarioRepository();
        VeiculoRepository veiculoRepository = veiculoRepository();
        VeiculoService service = new VeiculoService(veiculoRepository, usuarioRepository);

        service.criar(new VeiculoRequest("Onix", "ABC1D23", "Prata"));

        assertThrows(RuntimeException.class, () -> service.criar(new VeiculoRequest("HB20", "ABC1D23", "Branco")));
    }

    private UsuarioRepository usuarioRepository() {
        Map<Long, Usuario> usuarios = new LinkedHashMap<>();
        AtomicLong sequence = new AtomicLong();

        return (UsuarioRepository) Proxy.newProxyInstance(
                UsuarioRepository.class.getClassLoader(),
                new Class[]{UsuarioRepository.class},
                (proxy, method, args) -> {
                    if ("findAll".equals(method.getName())) {
                        return new ArrayList<>(usuarios.values());
                    }
                    if ("save".equals(method.getName())) {
                        Usuario entity = (Usuario) args[0];
                        if (entity.getId() == null) {
                            entity.setId(sequence.incrementAndGet());
                        }
                        usuarios.put(entity.getId(), entity);
                        return entity;
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    if (method.getReturnType().equals(void.class)) {
                        return null;
                    }
                    if (method.getReturnType().equals(Optional.class)) {
                        return Optional.empty();
                    }
                    if (method.getReturnType().equals(List.class)) {
                        return List.of();
                    }
                    return null;
                }
        );
    }

    private VeiculoRepository veiculoRepository() {
        Map<Long, Veiculo> veiculos = new LinkedHashMap<>();
        AtomicLong sequence = new AtomicLong();

        return (VeiculoRepository) Proxy.newProxyInstance(
                VeiculoRepository.class.getClassLoader(),
                new Class[]{VeiculoRepository.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "findByUsuarioOrderByIdAsc" -> {
                            Usuario usuario = (Usuario) args[0];
                            return veiculos.values().stream()
                                    .filter(veiculo -> veiculo.getUsuario() != null && veiculo.getUsuario().getId().equals(usuario.getId()))
                                    .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
                                    .toList();
                        }
                        case "findByIdAndUsuario" -> {
                            Long id = (Long) args[0];
                            Usuario usuario = (Usuario) args[1];
                            return veiculos.values().stream()
                                    .filter(veiculo -> veiculo.getId().equals(id) && veiculo.getUsuario() != null && veiculo.getUsuario().getId().equals(usuario.getId()))
                                    .findFirst();
                        }
                        case "existsByPlacaIgnoreCase" -> {
                            String placa = (String) args[0];
                            return veiculos.values().stream().anyMatch(veiculo -> veiculo.getPlaca().equalsIgnoreCase(placa));
                        }
                        case "existsByPlacaIgnoreCaseAndIdNot" -> {
                            String placa = (String) args[0];
                            Long id = (Long) args[1];
                            return veiculos.values().stream()
                                    .anyMatch(veiculo -> !veiculo.getId().equals(id) && veiculo.getPlaca().equalsIgnoreCase(placa));
                        }
                        case "findAll" -> {
                            return new ArrayList<>(veiculos.values());
                        }
                        case "save" -> {
                            Veiculo entity = (Veiculo) args[0];
                            if (entity.getId() == null) {
                                entity.setId(sequence.incrementAndGet());
                            }
                            veiculos.put(entity.getId(), entity);
                            return entity;
                        }
                        case "delete" -> {
                            Veiculo entity = (Veiculo) args[0];
                            veiculos.remove(entity.getId());
                            return null;
                        }
                        default -> {
                            if (method.getReturnType().equals(boolean.class)) {
                                return false;
                            }
                            if (method.getReturnType().equals(void.class)) {
                                return null;
                            }
                            if (method.getReturnType().equals(Optional.class)) {
                                return Optional.empty();
                            }
                            if (method.getReturnType().equals(List.class)) {
                                return List.of();
                            }
                            return null;
                        }
                    }
                }
        );
    }
}
