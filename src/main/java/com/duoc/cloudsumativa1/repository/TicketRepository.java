package com.duoc.cloudsumativa1.repository;

import com.duoc.cloudsumativa1.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByUsuarioId(Long usuarioId);

    List<Ticket> findByEventoId(Long eventoId);
}
