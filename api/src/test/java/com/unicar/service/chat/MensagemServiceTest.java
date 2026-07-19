package com.unicar.service.chat;

import com.unicar.domain.Carona;
import com.unicar.domain.ReservaCarona;
import com.unicar.domain.Usuario;
import com.unicar.domain.chat.Chat;
import com.unicar.domain.chat.Mensagem;
import com.unicar.dto.chat.EnviarMensagemRequestDTO;
import com.unicar.dto.chat.MensagemDTO;
import com.unicar.enums.StatusReserva;
import com.unicar.repository.BloqueioUsuarioRepository;
import com.unicar.repository.UsuarioRepository;
import com.unicar.repository.chat.MensagemRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MensagemServiceTest {

    @Mock
    private MensagemRepository mensagemRepository;

    @Mock
    private ChatService chatService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private BloqueioUsuarioRepository bloqueioUsuarioRepository;

    @InjectMocks
    private MensagemService mensagemService;

    private final Long chatId = 1L;
    private final Long motoristaId = 100L;
    private final Long passageiroId = 200L;
    private final Long outroUsuarioId = 300L;

    private Usuario motorista;
    private Usuario passageiro;
    private Chat chat;

    @BeforeEach
    void setUp() {
        motorista = new Usuario();
        motorista.setId(motoristaId);
        motorista.setNome("João Motorista");

        passageiro = new Usuario();
        passageiro.setId(passageiroId);
        passageiro.setNome("Maria Passageira");

        Carona carona = new Carona();
        carona.setMotorista(motorista);

        ReservaCarona reserva = new ReservaCarona();
        reserva.setUsuario(passageiro);
        reserva.setCarona(carona);
        reserva.setStatus(StatusReserva.ACEITA);

        chat = Chat.builder()
                .id(chatId)
                .reserva(reserva)
                .build();
    }

    @Nested
    @DisplayName("Listar mensagens do chat")
    class ListarMensagensDoChat {

        @Test
        @DisplayName("Deve validar acesso e listar as mensagens em ordem de envio")
        void deveListarMensagens() {
            when(chatService.buscarEValidarAcessoAoChat(chatId, passageiroId)).thenReturn(chat);

            Mensagem mensagem = Mensagem.builder()
                    .id(1L)
                    .chat(chat)
                    .remetente(passageiro)
                    .conteudo("Oi, tudo bem?")
                    .lida(false)
                    .dataEnvio(LocalDateTime.now())
                    .build();

            when(mensagemRepository.findByChatIdOrderByDataEnvioAsc(chatId)).thenReturn(List.of(mensagem));

            List<MensagemDTO> resultado = mensagemService.listarMensagensDoChat(chatId, passageiroId);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.getFirst().conteudo()).isEqualTo("Oi, tudo bem?");
            assertThat(resultado.getFirst().remetenteId()).isEqualTo(passageiroId);
        }

        @Test
        @DisplayName("Deve propagar o erro de acesso negado do ChatService")
        void devePropagarErroDeAcesso() {
            when(chatService.buscarEValidarAcessoAoChat(chatId, outroUsuarioId))
                    .thenThrow(new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.FORBIDDEN, "Acesso negado"));

            assertThatThrownBy(() -> mensagemService.listarMensagensDoChat(chatId, outroUsuarioId))
                    .hasFieldOrPropertyWithValue("statusCode", org.springframework.http.HttpStatus.FORBIDDEN);

            verify(mensagemRepository, never()).findByChatIdOrderByDataEnvioAsc(any());
        }
    }

    @Nested
    @DisplayName("Enviar mensagem")
    class EnviarMensagem {

        private final EnviarMensagemRequestDTO request = new EnviarMensagemRequestDTO("Olá!");

        @Test
        @DisplayName("Deve permitir que o passageiro envie mensagem para o motorista")
        void devePermitirPassageiroEnviarParaMotorista() {
            when(chatService.buscarEValidarAcessoAoChat(chatId, passageiroId)).thenReturn(chat);
            when(bloqueioUsuarioRepository.existsByUsuarioIdAndUsuarioBloqueadoId(passageiroId, motoristaId)).thenReturn(false);
            when(bloqueioUsuarioRepository.existsByUsuarioIdAndUsuarioBloqueadoId(motoristaId, passageiroId)).thenReturn(false);
            when(usuarioRepository.findById(passageiroId)).thenReturn(Optional.of(passageiro));
            when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(inv -> {
                Mensagem m = inv.getArgument(0);
                m.setId(1L);
                m.setLida(false);
                m.setDataEnvio(LocalDateTime.now());
                return m;
            });

            MensagemDTO resultado = mensagemService.enviarMensagem(chatId, passageiroId, request);

            assertThat(resultado.conteudo()).isEqualTo("Olá!");
            assertThat(resultado.remetenteId()).isEqualTo(passageiroId);
        }

        @Test
        @DisplayName("Deve permitir que o motorista envie mensagem para o passageiro")
        void devePermitirMotoristaEnviarParaPassageiro() {
            when(chatService.buscarEValidarAcessoAoChat(chatId, motoristaId)).thenReturn(chat);
            when(bloqueioUsuarioRepository.existsByUsuarioIdAndUsuarioBloqueadoId(motoristaId, passageiroId)).thenReturn(false);
            when(bloqueioUsuarioRepository.existsByUsuarioIdAndUsuarioBloqueadoId(passageiroId, motoristaId)).thenReturn(false);
            when(usuarioRepository.findById(motoristaId)).thenReturn(Optional.of(motorista));
            when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(inv -> {
                Mensagem m = inv.getArgument(0);
                m.setId(2L);
                m.setLida(false);
                m.setDataEnvio(LocalDateTime.now());
                return m;
            });

            MensagemDTO resultado = mensagemService.enviarMensagem(chatId, motoristaId, request);

            assertThat(resultado.remetenteId()).isEqualTo(motoristaId);
        }

        @Test
        @DisplayName("Não deve permitir envio quando o chat retornado não pertence ao usuário autenticado")
        void naoDevePermitirQuandoUsuarioNaoParticipaDoChatRetornado() {
            when(chatService.buscarEValidarAcessoAoChat(chatId, outroUsuarioId)).thenReturn(chat);

            assertThatThrownBy(() -> mensagemService.enviarMensagem(chatId, outroUsuarioId, request))
                    .hasFieldOrPropertyWithValue("statusCode", org.springframework.http.HttpStatus.FORBIDDEN);

            verify(mensagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve permitir envio quando a reserva não estiver ACEITA nem PENDENTE")
        void naoDevePermitirEnvioParaReservaInativa() {
            chat.getReserva().setStatus(StatusReserva.CANCELADA);
            when(chatService.buscarEValidarAcessoAoChat(chatId, passageiroId)).thenReturn(chat);

            assertThatThrownBy(() -> mensagemService.enviarMensagem(chatId, passageiroId, request))
                    .hasFieldOrPropertyWithValue("statusCode", org.springframework.http.HttpStatus.BAD_REQUEST);

            verify(mensagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve permitir envio quando o remetente bloqueou o destinatário")
        void naoDevePermitirQuandoRemetenteBloqueouDestinatario() {
            when(chatService.buscarEValidarAcessoAoChat(chatId, passageiroId)).thenReturn(chat);
            when(bloqueioUsuarioRepository.existsByUsuarioIdAndUsuarioBloqueadoId(passageiroId, motoristaId)).thenReturn(true);

            assertThatThrownBy(() -> mensagemService.enviarMensagem(chatId, passageiroId, request))
                    .hasFieldOrPropertyWithValue("statusCode", org.springframework.http.HttpStatus.FORBIDDEN);

            verify(mensagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve permitir envio quando o destinatário bloqueou o remetente")
        void naoDevePermitirQuandoDestinatarioBloqueouRemetente() {
            when(chatService.buscarEValidarAcessoAoChat(chatId, passageiroId)).thenReturn(chat);
            when(bloqueioUsuarioRepository.existsByUsuarioIdAndUsuarioBloqueadoId(passageiroId, motoristaId)).thenReturn(false);
            when(bloqueioUsuarioRepository.existsByUsuarioIdAndUsuarioBloqueadoId(motoristaId, passageiroId)).thenReturn(true);

            assertThatThrownBy(() -> mensagemService.enviarMensagem(chatId, passageiroId, request))
                    .hasFieldOrPropertyWithValue("statusCode", org.springframework.http.HttpStatus.FORBIDDEN);

            verify(mensagemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar 404 quando o remetente não for encontrado")
        void deveLancarNotFoundQuandoRemetenteNaoExistir() {
            when(chatService.buscarEValidarAcessoAoChat(chatId, passageiroId)).thenReturn(chat);
            when(bloqueioUsuarioRepository.existsByUsuarioIdAndUsuarioBloqueadoId(passageiroId, motoristaId)).thenReturn(false);
            when(bloqueioUsuarioRepository.existsByUsuarioIdAndUsuarioBloqueadoId(motoristaId, passageiroId)).thenReturn(false);
            when(usuarioRepository.findById(passageiroId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mensagemService.enviarMensagem(chatId, passageiroId, request))
                    .hasFieldOrPropertyWithValue("statusCode", org.springframework.http.HttpStatus.NOT_FOUND);

            verify(mensagemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Marcar mensagens como lidas")
    class MarcarComoLidas {

        @Test
        @DisplayName("Deve validar acesso e marcar as mensagens do outro participante como lidas")
        void deveMarcarComoLidas() {
            when(chatService.buscarEValidarAcessoAoChat(chatId, passageiroId)).thenReturn(chat);

            mensagemService.marcarComoLidas(chatId, passageiroId);

            verify(chatService).buscarEValidarAcessoAoChat(chatId, passageiroId);
            verify(mensagemRepository).marcarMensagensComoLidas(chatId, passageiroId);
        }
    }
}
