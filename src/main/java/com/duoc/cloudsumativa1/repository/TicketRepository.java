package com.duoc.cloudsumativa1.repository;

import com.duoc.cloudsumativa1.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> buscarParaModificar(@Param("id") Long id);

    List<Ticket> findByUsuarioId(Long usuarioId);

    List<Ticket> findByEventoId(Long eventoId);
}
