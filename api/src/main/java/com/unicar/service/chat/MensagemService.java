package com.unicar.service.chat;

import com.unicar.domain.Usuario;
import com.unicar.domain.chat.Chat;
import com.unicar.domain.chat.Mensagem;
import com.unicar.dto.chat.EnviarMensagemRequestDTO;
import com.unicar.dto.chat.MensagemDTO;
import com.unicar.repository.UsuarioRepository;
import com.unicar.repository.chat.MensagemRepository;
import com.unicar.enums.StatusCarona;

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


        StatusCarona statusCarona = chat.getReserva().getCarona().getStatus();

        if (statusCarona == StatusCarona.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível enviar mensagens em caronas canceladas");
        }
        if (statusCarona == StatusCarona.FINALIZADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível enviar mensagens após o encerramento da carona");
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