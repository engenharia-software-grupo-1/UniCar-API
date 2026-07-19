package com.unicar.service;

import com.unicar.domain.Usuario;
import com.unicar.domain.Veiculo;
import com.unicar.dto.veiculo.VeiculoRequestDTO;
import com.unicar.dto.veiculo.VeiculoResponseDTO;
import com.unicar.enums.TipoVeiculo;
import com.unicar.exception.RegraDeNegocioException;
import com.unicar.exception.VeiculoNaoEncontradoException;
import com.unicar.repository.CaronaRepository;
import com.unicar.repository.VeiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class VeiculoServiceTest {

    private VeiculoRepository veiculoRepository;
    private CaronaRepository caronaRepository;
    private VeiculoService veiculoService;

    private Usuario usuario;
    private Veiculo veiculo;
    private VeiculoRequestDTO request;

    @BeforeEach
    void setUp() {
        veiculoRepository = mock(VeiculoRepository.class);
        caronaRepository = mock(CaronaRepository.class);

        veiculoService = new VeiculoService(veiculoRepository, caronaRepository);

        usuario = Usuario.builder()
            .id(1L)
            .nome("João")
            .build();

        veiculo = Veiculo.builder()
            .id(10L)
            .usuario(usuario)
            .modelo("Onix")
            .placa("ABC1234")
            .cor("Prata")
            .tipoVeiculo(TipoVeiculo.CARRO)
            .build();

        request = new VeiculoRequestDTO(
            "HB20",
            "XYZ9876",
            "Branco",
            TipoVeiculo.CARRO
        );
    }

    @Nested
    @DisplayName("listarPorUsuario")
    class Listar {

        @Test
        @DisplayName("Deve listar os veículos do usuário")
        void deveListar() {
            when(veiculoRepository.findAllByUsuarioId(usuario.getId()))
                .thenReturn(List.of(veiculo));

            List<VeiculoResponseDTO> response =
                veiculoService.listarPorUsuario(usuario.getId());

            assertThat(response)
                .hasSize(1)
                .first()
                .satisfies(v -> {
                    assertThat(v.id()).isEqualTo(10L);
                    assertThat(v.modelo()).isEqualTo("Onix");
                    assertThat(v.placa()).isEqualTo("ABC1234");
                    assertThat(v.cor()).isEqualTo("Prata");
                    assertThat(v.tipoVeiculo()).isEqualTo(TipoVeiculo.CARRO);
                });

            verify(veiculoRepository).findAllByUsuarioId(usuario.getId());
        }
    }

    @Nested
    @DisplayName("buscarPorId")
    class Buscar {

        @Test
        @DisplayName("Deve buscar veículo por id")
        void deveBuscar() {
            when(veiculoRepository.findByIdAndUsuarioId(10L, usuario.getId()))
                    .thenReturn(Optional.of(veiculo));

            VeiculoResponseDTO response =
                    veiculoService.buscarPorId(usuario.getId(), 10L);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.modelo()).isEqualTo("Onix");
            assertThat(response.placa()).isEqualTo("ABC1234");

            verify(veiculoRepository).findByIdAndUsuarioId(10L, usuario.getId());
        }

        @Test
        @DisplayName("Deve lançar exceção quando veículo não existir")
        void deveLancarExcecao() {
            when(veiculoRepository.findByIdAndUsuarioId(10L, usuario.getId()))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                veiculoService.buscarPorId(usuario.getId(), 10L))
                .isInstanceOf(VeiculoNaoEncontradoException.class)
                .hasMessage("Veículo não encontrado");

            verify(veiculoRepository).findByIdAndUsuarioId(10L, usuario.getId());
        }
    }

    @Nested
    @DisplayName("criar")
    class Criar {

        @Test
        @DisplayName("Deve criar veículo")
        void deveCriar() {

            when(veiculoRepository.save(any(Veiculo.class)))
                .thenAnswer(invocation -> {
                    Veiculo salvo = invocation.getArgument(0);
                    salvo.setId(20L);
                    return salvo;
                });

            VeiculoResponseDTO response =
                veiculoService.criar(usuario.getId(), request);

            assertThat(response.id()).isEqualTo(20L);
            assertThat(response.modelo()).isEqualTo(request.modelo());
            assertThat(response.placa()).isEqualTo(request.placa());
            assertThat(response.cor()).isEqualTo(request.cor());
            assertThat(response.tipoVeiculo()).isEqualTo(request.tipoVeiculo());

            verify(veiculoRepository).save(any(Veiculo.class));
        }
    }

    @Nested
    @DisplayName("atualizar")
    class Atualizar {

        @Test
        @DisplayName("Deve atualizar veículo")
        void deveAtualizar() {

            when(veiculoRepository.findByIdAndUsuarioId(10L, usuario.getId()))
                .thenReturn(Optional.of(veiculo));

            when(veiculoRepository.save(any(Veiculo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            VeiculoResponseDTO response =
                veiculoService.atualizar(usuario.getId(), 10L, request);

            assertThat(response.modelo()).isEqualTo(request.modelo());
            assertThat(response.placa()).isEqualTo(request.placa());
            assertThat(response.cor()).isEqualTo(request.cor());

            verify(veiculoRepository).findByIdAndUsuarioId(10L, usuario.getId());
            verify(veiculoRepository).save(veiculo);
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar veículo inexistente")
        void deveLancarExcecao() {

            when(veiculoRepository.findByIdAndUsuarioId(10L, usuario.getId()))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                veiculoService.atualizar(usuario.getId(), 10L, request))
                .isInstanceOf(VeiculoNaoEncontradoException.class)
                .hasMessage("Veículo não encontrado");

            verify(veiculoRepository).findByIdAndUsuarioId(10L, usuario.getId());
            verify(veiculoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("excluir")
    class Excluir {

        @Test
        @DisplayName("Deve excluir veículo")
        void deveExcluir() {

            when(veiculoRepository.findByIdAndUsuarioId(10L, usuario.getId()))
                .thenReturn(Optional.of(veiculo));

            when(caronaRepository.existsByVeiculoId(10L))
                    .thenReturn(false);

            veiculoService.excluir(usuario.getId(), 10L);

            verify(veiculoRepository).findByIdAndUsuarioId(10L, usuario.getId());
            verify(caronaRepository).existsByVeiculoId(10L);
            verify(veiculoRepository).delete(veiculo);
        }

        @Test
        @DisplayName("Não deve excluir veículo vinculado a uma carona")
        void naoDeveExcluirVeiculoVinculadoACarona() {

            when(veiculoRepository.findByIdAndUsuarioId(10L, usuario.getId()))
                    .thenReturn(Optional.of(veiculo));

            when(caronaRepository.existsByVeiculoId(10L))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    veiculoService.excluir(usuario.getId(), 10L))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Não é possível excluir este veículo, pois ele já está vinculado a uma ou mais caronas.");

            verify(veiculoRepository).findByIdAndUsuarioId(10L, usuario.getId());
            verify(caronaRepository).existsByVeiculoId(10L);
            verify(veiculoRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Deve lançar exceção ao excluir veículo inexistente")
        void deveLancarExcecao() {

            when(veiculoRepository.findByIdAndUsuarioId(10L, usuario.getId()))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                veiculoService.excluir(usuario.getId(), 10L))
                .isInstanceOf(VeiculoNaoEncontradoException.class)
                .hasMessage("Veículo não encontrado");

            verify(veiculoRepository).findByIdAndUsuarioId(10L, usuario.getId());
            verify(caronaRepository, never()).existsByVeiculoId(anyLong());
            verify(veiculoRepository, never()).delete(any());
        }
    }
}