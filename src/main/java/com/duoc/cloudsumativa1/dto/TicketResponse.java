package com.duoc.cloudsumativa1.dto;

import com.duoc.cloudsumativa1.model.Ticket;
import java.time.LocalDateTime;

public record TicketResponse(Long id, String codigo, LocalDateTime fechaReserva,
                             Long eventoId, Long usuarioId) {
    public static TicketResponse desde(Ticket ticket) {
        return new TicketResponse(ticket.getId(), ticket.getCodigo(), ticket.getFechaReserva(),
                ticket.getEvento().getId(), ticket.getUsuario().getId());
    }
}
