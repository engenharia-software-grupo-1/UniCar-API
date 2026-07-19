package com.unicar.service.chat;

import com.unicar.domain.Usuario;
import com.unicar.domain.chat.Chat;
import com.unicar.domain.chat.Mensagem;
import com.unicar.dto.chat.EnviarMensagemRequestDTO;
import com.unicar.dto.chat.MensagemDTO;
import com.unicar.enums.StatusReserva;
import com.unicar.repository.BloqueioUsuarioRepository;
import com.unicar.repository.UsuarioRepository;
import com.unicar.repository.chat.MensagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MensagemService {

    private final MensagemRepository mensagemRepository;
    private final ChatService chatService;
    private final UsuarioRepository usuarioRepository;
    private final BloqueioUsuarioRepository bloqueioUsuarioRepository;

    @Transactional(readOnly = true)
    public List<MensagemDTO> listarMensagensDoChat(Long chatId, Long usuarioAutenticadoId) {
        chatService.buscarEValidarAcessoAoChat(chatId, usuarioAutenticadoId);

        List<Mensagem> mensagens = mensagemRepository.findByChatIdOrderByDataEnvioAsc(chatId);
        return mensagens.stream()
                .map(MensagemDTO::from)
                .toList();
    }

    @Transactional
    public MensagemDTO enviarMensagem(Long chatId, Long usuarioAutenticadoId, EnviarMensagemRequestDTO request) {
        Chat chat = chatService.buscarEValidarAcessoAoChat(chatId, usuarioAutenticadoId);

        Long passageiroId = chat.getReserva().getUsuario().getId();
        Long motoristaId = chat.getReserva().getCarona().getMotorista().getId();

        if (!usuarioAutenticadoId.equals(passageiroId) && !usuarioAutenticadoId.equals(motoristaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: Você não faz parte deste chat.");
        }

        StatusReserva statusReserva = chat.getReserva().getStatus();
        if (statusReserva != StatusReserva.ACEITA && statusReserva != StatusReserva.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível enviar mensagens para uma reserva inativa.");
        }

        Long destinatarioId = usuarioAutenticadoId.equals(passageiroId) ? motoristaId : passageiroId;

        boolean remetenteBloqueou = bloqueioUsuarioRepository.existsByUsuarioIdAndUsuarioBloqueadoId(usuarioAutenticadoId, destinatarioId);
        boolean destinatarioBloqueou = bloqueioUsuarioRepository.existsByUsuarioIdAndUsuarioBloqueadoId(destinatarioId, usuarioAutenticadoId);

        if (remetenteBloqueou || destinatarioBloqueou) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuários bloqueados não podem trocar mensagens.");
        }

        Usuario remetente = usuarioRepository.findById(usuarioAutenticadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        Mensagem mensagem = Mensagem.builder()
                .chat(chat)
                .remetente(remetente)
                .conteudo(request.conteudo())
                .build();

        return MensagemDTO.from(mensagemRepository.save(mensagem));
    }

    @Transactional
    public void marcarComoLidas(Long chatId, Long usuarioAutenticadoId) {
        chatService.buscarEValidarAcessoAoChat(chatId, usuarioAutenticadoId);
        mensagemRepository.marcarMensagensComoLidas(chatId, usuarioAutenticadoId);
    }
}