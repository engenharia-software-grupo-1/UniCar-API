package com.unicar.repository.chat;

import com.unicar.domain.chat.Mensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    // Critério de Aceitação: Mensagens são exibidas em ordem cronológica (data_envio ASC)
    List<Mensagem> findByChatIdOrderByDataEnvioAsc(Long chatId);

    /**
     * Critério de Aceitação: Usuários conseguem marcar mensagens como lidas.
     * Atualiza o status apenas das mensagens enviadas pelo OUTRO participante.
     */
    @Modifying
    @Query("UPDATE Mensagem m SET m.lida = true " +
            "WHERE m.chat.id = :chatId AND m.remetente.id != :usuarioAutenticadoId AND m.lida = false")
    void marcarMensagensComoLidas(@Param("chatId") Long chatId, @Param("usuarioAutenticadoId") Long usuarioAutenticadoId);
}