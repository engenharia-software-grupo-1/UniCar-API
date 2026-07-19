package com.unicar.repository.chat;

import com.unicar.domain.chat.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    Optional<Chat> findByReservaId(Long reservaId);

    /**
     * RN: Permitir acesso apenas aos participantes da carona.
     * Traz os chats onde o usuário logado é o Passageiro (da Reserva) OU o Motorista (da Carona vinculada).
     */
    @Query("SELECT c FROM Chat c " +
            "JOIN c.reserva r " +
            "JOIN r.carona car " +
            "WHERE r.usuario.id = :usuarioId OR car.motorista.id = :usuarioId")
    List<Chat> findChatsByUsuarioId(@Param("usuarioId") Long usuarioId);
}