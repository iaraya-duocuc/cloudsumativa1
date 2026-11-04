package com.duoc.cloudsumativa1.service;

import com.duoc.cloudsumativa1.dto.TicketResponse;
import com.duoc.cloudsumativa1.model.Evento;
import com.duoc.cloudsumativa1.model.Ticket;
import com.duoc.cloudsumativa1.model.Usuario;
import com.duoc.cloudsumativa1.repository.EventoRepository;
import com.duoc.cloudsumativa1.repository.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TicketService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TicketService.class);
    private final TicketRepository tickets;
    private final EventoRepository eventos;
    private final UsuarioService usuarios;
    private final TicketStorageService storage;

    public TicketService(TicketRepository tickets, EventoRepository eventos,
                         UsuarioService usuarios, TicketStorageService storage) {
        this.tickets = tickets;
        this.eventos = eventos;
        this.usuarios = usuarios;
        this.storage = storage;
    }

    @Transactional
    public TicketResponse crear(Long eventoId, Long usuarioId) {
        Usuario usuario = usuarios.buscarPorId(usuarioId);
        Evento evento = eventos.buscarParaReserva(eventoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
        if (evento.getCuposDisponibles() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No quedan cupos disponibles");
        }

        Ticket ticket = new Ticket();
        ticket.setCodigo(UUID.randomUUID().toString());
        ticket.setFechaReserva(LocalDateTime.now());
        ticket.setEvento(evento);
        ticket.setUsuario(usuario);
        evento.setCuposDisponibles(evento.getCuposDisponibles() - 1);

        try {
            Path archivo = storage.guardar(ticket);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        try {
                            Files.deleteIfExists(archivo);
                        } catch (IOException ex) {
                            log.error("No se pudo eliminar el TXT de una reserva revertida: {}", archivo, ex);
                        }
                    }
                }
            });
            ticket.setRutaArchivo(archivo.toString());
            tickets.saveAndFlush(ticket);
            return TicketResponse.desde(ticket);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el ticket", ex);
        }
    }

    @Transactional(readOnly = true)
    public TicketResponse consultar(Long id) {
        return TicketResponse.desde(buscar(id));
    }

    @Transactional(readOnly = true)
    public byte[] descargar(Long id) {
        return leerArchivo(buscar(id));
    }

    private byte[] leerArchivo(Ticket ticket) {
        if (ticket.getRutaArchivo() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo de ticket no encontrado");
        }
        try {
            return storage.leer(ticket.getRutaArchivo());
        } catch (NoSuchFileException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo de ticket no encontrado", ex);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer el ticket", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listarPorUsuario(Long usuarioId) {
        return buscarPorUsuario(usuarioId).stream().map(TicketResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listarPorEvento(Long eventoId) {
        return buscarPorEvento(eventoId).stream().map(TicketResponse::desde).toList();
    }

    private List<Ticket> buscarPorUsuario(Long usuarioId) {
        usuarios.buscarPorId(usuarioId);
        return tickets.findByUsuarioId(usuarioId);
    }

    private List<Ticket> buscarPorEvento(Long eventoId) {
        if (!eventos.existsById(eventoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado");
        }
        return tickets.findByEventoId(eventoId);
    }

    @Transactional
    public TicketResponse modificar(Long id, String observacion) {
        Ticket ticket = buscarParaModificar(id);
        ticket.setObservacion(observacion);
        prepararRestauracion(ticket);
        try {
            storage.actualizar(ticket);
            tickets.saveAndFlush(ticket);
            return TicketResponse.desde(ticket);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo actualizar el TXT", ex);
        }
    }

    @Transactional
    public void eliminar(Long id) {
        Ticket ticket = buscarParaModificar(id);
        Evento evento = eventos.buscarParaReserva(ticket.getEvento().getId()).orElseThrow();
        prepararRestauracion(ticket);
        try {
            storage.eliminar(ticket.getRutaArchivo());
            evento.setCuposDisponibles(evento.getCuposDisponibles() + 1);
            tickets.delete(ticket);
            tickets.flush();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo eliminar el TXT", ex);
        }
    }

    private Ticket buscarParaModificar(Long id) {
        return tickets.buscarParaModificar(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
    }

    private void prepararRestauracion(Ticket ticket) {
        String ruta = ticket.getRutaArchivo();
        if (ruta == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo de ticket no encontrado");
        }
        byte[] anterior;
        try {
            anterior = storage.leer(ruta);
        } catch (NoSuchFileException ex) {
            anterior = null;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer el TXT", ex);
        }
        byte[] copia = anterior;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        storage.restaurar(ruta, copia);
                    } catch (IOException ex) {
                        log.error("No se pudo restaurar el TXT de una operación revertida: {}", ruta, ex);
                    }
                }
            }
        });
    }

    private Ticket buscar(Long id) {
        return tickets.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
    }
}
