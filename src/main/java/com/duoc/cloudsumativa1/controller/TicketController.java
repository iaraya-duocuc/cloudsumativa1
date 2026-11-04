package com.duoc.cloudsumativa1.controller;

import com.duoc.cloudsumativa1.dto.CrearTicketRequest;
import com.duoc.cloudsumativa1.dto.TicketResponse;
import com.duoc.cloudsumativa1.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @PostMapping("/eventos/{eventoId}/tickets")
    public ResponseEntity<TicketResponse> crear(@PathVariable Long eventoId,
                                               @Valid @RequestBody CrearTicketRequest request) {
        TicketResponse ticket = service.crear(eventoId, request.usuarioId());
        return ResponseEntity.created(URI.create("/tickets/" + ticket.id())).body(ticket);
    }

    @GetMapping("/tickets/{id}")
    public TicketResponse consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @GetMapping("/tickets/{id}/archivo")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("ticket" + id + ".txt").build().toString())
                .body(service.descargar(id));
    }
}
