package com.unicar.service;

import com.unicar.domain.Notificacao;
import com.unicar.domain.Usuario;
import com.unicar.dto.notificacao.ContadorNotificacoesDTO;
import com.unicar.dto.notificacao.NotificacaoDTO;
import com.unicar.enums.TipoNotificacao;
import com.unicar.repository.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;

    @Transactional(readOnly = true)
    public List<NotificacaoDTO> listarNotificacoesDoUsuario(Long usuarioId) {
        return notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioId)
                .stream()
                .map(NotificacaoDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContadorNotificacoesDTO obterContadorNaoLidas(Long usuarioId) {
        long contagem = notificacaoRepository.countByUsuarioIdAndLidaFalse(usuarioId);
        return new ContadorNotificacoesDTO(contagem);
    }

    @Transactional
    public void marcarComoVisualizada(Long notificacaoId, Long usuarioId) {
        Notificacao notificacao = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada"));

        if (!notificacao.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: Esta notificação não pertence a você");
        }

        notificacao.setLida(true);
        notificacaoRepository.save(notificacao);
    }

    /**
     * RN: Método que será chamado por outros Services (ReservaService, CaronaService, etc)
     * para disparar notificações automaticamente com base nos Eventos Geradores da US.
     */
    @Transactional
    public void dispararNotificacaoSistemica(Usuario destino, String titulo, String mensagem, TipoNotificacao tipo) {
        Notificacao notificacao = Notificacao.builder()
                .usuario(destino)
                .titulo(titulo)
                .mensagem(mensagem)
                .lida(false)
                .tipo(tipo)
                .build();
        notificacaoRepository.save(notificacao);
    }
}