package com.duoc.cloudsumativa1.controller;

import com.duoc.cloudsumativa1.model.Evento;
import com.duoc.cloudsumativa1.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/eventos")
public class EventoController {

    private final EventoService service;

    public EventoController(EventoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Evento> crear(@Valid @RequestBody Evento evento) {
        Evento creado = service.crear(evento);
        return ResponseEntity.created(URI.create("/eventos/" + creado.getId())).body(creado);
    }

    @GetMapping
    public List<Evento> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Evento buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }
}
