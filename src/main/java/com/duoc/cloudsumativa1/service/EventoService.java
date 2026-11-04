package com.duoc.cloudsumativa1.service;

import com.duoc.cloudsumativa1.model.Evento;
import com.duoc.cloudsumativa1.repository.EventoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EventoService {

    private final EventoRepository repository;

    public EventoService(EventoRepository repository) {
        this.repository = repository;
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
}
