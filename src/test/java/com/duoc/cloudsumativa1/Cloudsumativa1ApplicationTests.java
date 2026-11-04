package com.duoc.cloudsumativa1;

import com.duoc.cloudsumativa1.model.Evento;
import com.duoc.cloudsumativa1.model.Ticket;
import com.duoc.cloudsumativa1.model.Usuario;
import com.duoc.cloudsumativa1.repository.EventoRepository;
import com.duoc.cloudsumativa1.repository.TicketRepository;
import com.duoc.cloudsumativa1.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Cloudsumativa1ApplicationTests {

    @Autowired
    private EventoRepository eventos;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private TicketRepository tickets;

    @Test
    void guardaTicketYPermiteBuscarPorUsuarioYEvento() {
        Evento evento = new Evento();
        evento.setNombre("Concierto");
        evento.setFecha(LocalDateTime.of(2027, 1, 10, 20, 0));
        evento.setPrecio(new BigDecimal("15000"));
        evento.setCuposDisponibles(10);
        eventos.save(evento);

        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setCorreo("ana@example.com");
        usuarios.save(usuario);

        Ticket ticket = new Ticket();
        ticket.setCodigo("TICKET-001");
        ticket.setFechaReserva(LocalDateTime.now());
        ticket.setEvento(evento);
        ticket.setUsuario(usuario);
        tickets.saveAndFlush(ticket);

        assertThat(tickets.findByUsuarioId(usuario.getId()))
                .extracting(Ticket::getCodigo).containsExactly("TICKET-001");
        assertThat(tickets.findByEventoId(evento.getId()))
                .extracting(Ticket::getCodigo).containsExactly("TICKET-001");
        assertThat(tickets.findByUsuarioId(-1L)).isEmpty();
    }
}
