package com.duoc.cloudsumativa1.service;

import com.duoc.cloudsumativa1.model.Asset;
import com.duoc.cloudsumativa1.model.Ticket;

public interface AwsService {
    boolean habilitado();
    Asset subir(Ticket ticket, byte[] contenido);
    byte[] descargar(Ticket ticket);
    byte[] respaldo(Ticket ticket);
    void eliminar(Ticket ticket);
    void restaurar(Ticket ticket, byte[] contenido);
}
