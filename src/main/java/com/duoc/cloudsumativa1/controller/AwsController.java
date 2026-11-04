package com.duoc.cloudsumativa1.controller;

import com.duoc.cloudsumativa1.model.Asset;
import com.duoc.cloudsumativa1.service.TicketService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/tickets/{id}/s3")
public class AwsController {
    private final TicketService tickets;

    public AwsController(TicketService tickets) {
        this.tickets = tickets;
    }

    @PostMapping
    public Asset subir(@PathVariable Long id) {
        return tickets.subirS3(id);
    }

    @GetMapping
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("ticket" + id + ".txt").build().toString())
                .body(tickets.descargarS3(id));
    }
}
