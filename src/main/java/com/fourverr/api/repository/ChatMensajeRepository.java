package com.fourverr.api.repository;

import com.fourverr.api.model.ChatMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMensajeRepository extends JpaRepository<ChatMensaje, Long> {

    // Conversación entre dos usuarios (bidireccional)
    @Query("SELECT m FROM ChatMensaje m WHERE " +
           "(m.remitente.id = :a AND m.destinatario.id = :b) OR " +
           "(m.remitente.id = :b AND m.destinatario.id = :a) " +
           "ORDER BY m.fechaEnvio ASC")
    List<ChatMensaje> findConversacion(@Param("a") Long a, @Param("b") Long b);

    // Usuarios que tienen conversación con el admin (para listar chats en el panel)
    @Query("SELECT DISTINCT CASE WHEN m.remitente.id = :adminId THEN m.destinatario " +
           "ELSE m.remitente END FROM ChatMensaje m " +
           "WHERE m.remitente.id = :adminId OR m.destinatario.id = :adminId")
    List<Object> findUsuariosConChat(@Param("adminId") Long adminId);

    // Mensajes no leídos dirigidos a alguien
    long countByDestinatario_IdAndLeidoFalse(Long destinatarioId);

    // Marcar como leídos los mensajes de una conversación dirigidos a alguien
    @Query("SELECT m FROM ChatMensaje m WHERE " +
           "((m.remitente.id = :otro AND m.destinatario.id = :yo) " +
           "OR (m.remitente.id = :yo AND m.destinatario.id = :otro)) " +
           "AND m.leido = false AND m.destinatario.id = :yo")
    List<ChatMensaje> findNoLeidosParaMi(@Param("yo") Long yo, @Param("otro") Long otro);
}
