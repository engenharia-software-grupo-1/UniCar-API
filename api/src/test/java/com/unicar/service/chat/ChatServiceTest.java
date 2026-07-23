package com.unicar.service.chat;

import com.unicar.domain.Carona;
import com.unicar.domain.ReservaCarona;
import com.unicar.domain.Usuario;
import com.unicar.domain.chat.Chat;
import com.unicar.dto.chat.ChatDTO;
import com.unicar.repository.chat.ChatRepository;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @InjectMocks
    private ChatService chatService;

    private final Long chatId = 1L;
    private final Long reservaId = 10L;
    private final Long motoristaId = 100L;
    private final Long passageiroId = 200L;
    private final Long outroUsuarioId = 300L;

    private Usuario motorista;
    private Usuario passageiro;
    private Carona carona;
    private ReservaCarona reserva;
    private Chat chat;

    @BeforeEach
    void setUp() {
        motorista = new Usuario();
        motorista.setId(motoristaId);
        motorista.setNome("João Motorista");

        passageiro = new Usuario();
        passageiro.setId(passageiroId);
        passageiro.setNome("Maria Passageira");

        carona = new Carona();
        carona.setMotorista(motorista);

        reserva = new ReservaCarona();
        reserva.setId(reservaId);
        reserva.setUsuario(passageiro);
        reserva.setCarona(carona);

        chat = Chat.builder()
                .id(chatId)
                .reserva(reserva)
                .dataCriacao(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Listar chats do usuário")
    class ListarChatsDoUsuario {

        @Test
        @DisplayName("Deve mostrar o nome do motorista quando o usuário autenticado for o passageiro")
        void deveMostrarNomeDoMotoristaParaOPassageiro() {
            when(chatRepository.findChatsByUsuarioId(passageiroId)).thenReturn(List.of(chat));

            List<ChatDTO> resultado = chatService.listarChatsDoUsuario(passageiroId);

            assertThat(resultado).hasSize(1);
            ChatDTO dto = resultado.getFirst();
            assertThat(dto.id()).isEqualTo(chatId);
            assertThat(dto.reservaId()).isEqualTo(reservaId);
            assertThat(dto.nomeParticipante()).isEqualTo("João Motorista");
        }

        @Test
        @DisplayName("Deve mostrar o nome do passageiro quando o usuário autenticado for o motorista")
        void deveMostrarNomeDoPassageiroParaOMotorista() {
            when(chatRepository.findChatsByUsuarioId(motoristaId)).thenReturn(List.of(chat));

            List<ChatDTO> resultado = chatService.listarChatsDoUsuario(motoristaId);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.getFirst().nomeParticipante()).isEqualTo("Maria Passageira");
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando o usuário não participa de nenhum chat")
        void deveRetornarListaVaziaSemChats() {
            when(chatRepository.findChatsByUsuarioId(outroUsuarioId)).thenReturn(List.of());

            List<ChatDTO> resultado = chatService.listarChatsDoUsuario(outroUsuarioId);

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("Criar chat para reserva")
    class CriarChatParaReserva {

        @Test
        @DisplayName("Deve criar o chat quando a reserva ainda não tiver um")
        void deveCriarChat() {
            when(chatRepository.findByReservaId(reservaId)).thenReturn(Optional.empty());
            when(chatRepository.save(org.mockito.ArgumentMatchers.any(Chat.class)))
                    .thenAnswer(inv -> {
                        Chat salvo = inv.getArgument(0);
                        salvo.setId(chatId);
                        salvo.setDataCriacao(LocalDateTime.now());
                        return salvo;
                    });

            ChatDTO resultado = chatService.criarChatParaReserva(reserva);

            assertThat(resultado.id()).isEqualTo(chatId);
            assertThat(resultado.reservaId()).isEqualTo(reservaId);
            assertThat(resultado.nomeParticipante()).isEqualTo("João Motorista");
            assertThat(resultado.mensagensNaoLidas()).isZero();
        }

        @Test
        @DisplayName("Não deve criar quando já existir um chat para a reserva")
        void naoDeveCriarChatDuplicado() {
            when(chatRepository.findByReservaId(reservaId)).thenReturn(Optional.of(chat));

            assertThatThrownBy(() -> chatService.criarChatParaReserva(reserva))
                    .hasFieldOrPropertyWithValue("statusCode", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Buscar e validar acesso ao chat")
    class BuscarEValidarAcessoAoChat {

        @Test
        @DisplayName("Deve permitir acesso do passageiro")
        void devePermitirAcessoDoPassageiro() {
            when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

            Chat resultado = chatService.buscarEValidarAcessoAoChat(chatId, passageiroId);

            assertThat(resultado).isEqualTo(chat);
        }

        @Test
        @DisplayName("Deve permitir acesso do motorista")
        void devePermitirAcessoDoMotorista() {
            when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

            Chat resultado = chatService.buscarEValidarAcessoAoChat(chatId, motoristaId);

            assertThat(resultado).isEqualTo(chat);
        }

        @Test
        @DisplayName("Deve lançar 404 quando o chat não existir")
        void deveLancarNotFoundQuandoChatNaoExistir() {
            when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.buscarEValidarAcessoAoChat(chatId, passageiroId))
                    .hasFieldOrPropertyWithValue("statusCode", org.springframework.http.HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Deve lançar 403 quando o usuário não participar do chat")
        void deveLancarForbiddenQuandoUsuarioNaoParticipar() {
            when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

            assertThatThrownBy(() -> chatService.buscarEValidarAcessoAoChat(chatId, outroUsuarioId))
                    .hasFieldOrPropertyWithValue("statusCode", org.springframework.http.HttpStatus.FORBIDDEN);
        }
    }
}
