package com.duoc.cloudsumativa1.service;

import com.duoc.cloudsumativa1.model.Evento;
import com.duoc.cloudsumativa1.dto.EstadisticasEventoResponse;
import com.duoc.cloudsumativa1.repository.TicketRepository;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import com.duoc.cloudsumativa1.repository.EventoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EventoService {

    private final EventoRepository repository;

    private final TicketRepository tickets;

    public EventoService(EventoRepository repository, TicketRepository tickets) {
        this.repository = repository;
        this.tickets = tickets;
    }

    public Evento crear(Evento evento) {
        Evento nuevo = new Evento();
        nuevo.setNombre(evento.getNombre());
        nuevo.setFecha(evento.getFecha());
        nuevo.setPrecio(evento.getPrecio());
        nuevo.setCuposDisponibles(evento.getCuposDisponibles());
        return repository.save(nuevo);
    }

    public List<Evento> listar() {
        return repository.findAll();
    }

    public Evento buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento no encontrado"));
    }
    @Transactional(readOnly = true)
    public EstadisticasEventoResponse estadisticas(Long id) {
        Evento evento = buscarPorId(id);
        long totalReservas = tickets.countByEventoId(id);
        return new EstadisticasEventoResponse(evento.getId(), evento.getNombre(),
                totalReservas, evento.getCuposDisponibles(),
                evento.getPrecio().multiply(BigDecimal.valueOf(totalReservas)));
    }
}
