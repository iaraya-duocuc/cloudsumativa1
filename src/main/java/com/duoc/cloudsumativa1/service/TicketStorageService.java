package com.duoc.cloudsumativa1.service;

import com.duoc.cloudsumativa1.model.Ticket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
public class TicketStorageService {

    private final Path directorio;

    public TicketStorageService(@Value("${tickets.storage.path:./storage/tickets}") String ruta) {
        this.directorio = Path.of(ruta).toAbsolutePath().normalize();
    }

    public Path guardar(Ticket ticket) throws IOException {
        Path carpeta = directorio.resolve("evento" + ticket.getEvento().getId())
                .resolve("usuario" + ticket.getUsuario().getId());
        Files.createDirectories(carpeta);
        Path archivo = carpeta.resolve(ticket.getCodigo() + ".txt");
        String contenido = """
                TICKET DE RESERVA
                Código: %s
                Evento: %s
                Fecha del evento: %s
                Usuario: %s
                Correo: %s
                Precio: %s
                Fecha de reserva: %s
                """.formatted(ticket.getCodigo(), ticket.getEvento().getNombre(),
                ticket.getEvento().getFecha(), ticket.getUsuario().getNombre(),
                ticket.getUsuario().getCorreo(), ticket.getEvento().getPrecio(), ticket.getFechaReserva());
        try {
            Files.writeString(archivo, contenido, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) {
            Files.deleteIfExists(archivo);
            throw ex;
        }
        return archivo;
    }

    public byte[] leer(String ruta) throws IOException {
        return Files.readAllBytes(Path.of(ruta));
    }
}
