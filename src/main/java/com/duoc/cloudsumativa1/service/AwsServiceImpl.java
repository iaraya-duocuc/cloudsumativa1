package com.duoc.cloudsumativa1.service;

import com.duoc.cloudsumativa1.model.Asset;
import com.duoc.cloudsumativa1.model.Ticket;
import com.duoc.cloudsumativa1.repository.S3Repository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.function.Supplier;

@Service
public class AwsServiceImpl implements AwsService {
    private final ObjectProvider<S3Repository> repositorios;
    private final boolean habilitado;

    public AwsServiceImpl(ObjectProvider<S3Repository> repositorios,
                          @Value("${app.s3.enabled:false}") boolean habilitado) {
        this.repositorios = repositorios;
        this.habilitado = habilitado;
    }

    @Override
    public boolean habilitado() {
        return habilitado;
    }

    @Override
    public Asset subir(Ticket ticket, byte[] contenido) {
        return ejecutar(() -> repositorio().subir(clave(ticket), contenido, "text/plain; charset=UTF-8"));
    }

    @Override
    public byte[] descargar(Ticket ticket) {
        return ejecutar(() -> {
            S3Repository repository = repositorio();
            if (!repository.existe(clave(ticket))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado en S3");
            }
            return repository.descargar(clave(ticket));
        });
    }

    @Override
    public byte[] respaldo(Ticket ticket) {
        return ejecutar(() -> repositorio().existe(clave(ticket))
                ? repositorio().descargar(clave(ticket)) : null);
    }

    @Override
    public void eliminar(Ticket ticket) {
        ejecutar(() -> { repositorio().eliminar(clave(ticket)); return null; });
    }

    @Override
    public void restaurar(Ticket ticket, byte[] contenido) {
        if (contenido == null) {
            eliminar(ticket);
        } else {
            subir(ticket, contenido);
        }
    }

    private String clave(Ticket ticket) {
        return "evento" + ticket.getEvento().getId() + "/usuario" + ticket.getUsuario().getId()
                + "/ticket" + ticket.getCodigo() + ".txt";
    }

    private S3Repository repositorio() {
        if (!habilitado) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 está deshabilitado");
        }
        return repositorios.getObject();
    }

    private <T> T ejecutar(Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo completar la operación en S3", ex);
        }
    }
}
