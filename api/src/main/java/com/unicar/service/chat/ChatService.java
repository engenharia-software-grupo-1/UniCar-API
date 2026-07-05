package com.unicar.service.chat;

import com.unicar.domain.chat.Chat;
import com.unicar.dto.chat.ChatDTO;
import com.unicar.repository.chat.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    @Transactional(readOnly = true)
    public List<ChatDTO> listarChatsDoUsuario(Long usuarioAutenticadoId) {
        List<Chat> chats = chatRepository.findChatsByUsuarioId(usuarioAutenticadoId);
        return chats.stream()
                .map(chat -> ChatDTO.from(chat, usuarioAutenticadoId))
                .toList();
    }

    @Transactional
    public ChatDTO criarChatParaReserva(ReservaCarona reserva) {
        chatRepository.findByReservaId(reserva.getId()).ifPresent(c -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Já existe um chat para esta reserva");
        });

        Chat chat = Chat.builder()
                .reserva(reserva)
                .build();

        return ChatDTO.from(chatRepository.save(chat), reserva.getUsuario().getId());
    }

    public Chat buscarEValidarAcessoAoChat(Long chatId, Long usuarioId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat não encontrado"));

        Long passageiroId = chat.getReserva().getUsuario().getId();
        Long motoristaId = chat.getReserva().getCarona().getMotorista().getId();

        if (!usuarioId.equals(passageiroId) && !usuarioId.equals(motoristaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: Você não participa deste chat");
        }

        return chat;
    }
}