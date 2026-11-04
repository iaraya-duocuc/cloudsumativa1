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
        try {
            Files.writeString(archivo, contenido(ticket), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) {
            Files.deleteIfExists(archivo);
            throw ex;
        }
        return archivo;
    }

    public void actualizar(Ticket ticket) throws IOException {
        Files.writeString(Path.of(ticket.getRutaArchivo()), contenido(ticket), StandardCharsets.UTF_8);
    }

    public void eliminar(String ruta) throws IOException {
        Files.deleteIfExists(Path.of(ruta));
    }

    public void restaurar(String ruta, byte[] contenido) throws IOException {
        if (contenido == null) {
            eliminar(ruta);
        } else {
            Files.write(Path.of(ruta), contenido);
        }
    }

    private String contenido(Ticket ticket) {
        return """
                TICKET DE RESERVA
                Código: %s
                Evento: %s
                Fecha del evento: %s
                Usuario: %s
                Correo: %s
                Precio: %s
                Fecha de reserva: %s
                Observación: %s
                """.formatted(ticket.getCodigo(), ticket.getEvento().getNombre(),
                ticket.getEvento().getFecha(), ticket.getUsuario().getNombre(),
                ticket.getUsuario().getCorreo(), ticket.getEvento().getPrecio(), ticket.getFechaReserva(),
                ticket.getObservacion() == null ? "" : ticket.getObservacion());
    }

    public byte[] leer(String ruta) throws IOException {
        return Files.readAllBytes(Path.of(ruta));
    }
}
