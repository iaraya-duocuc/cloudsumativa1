package com.duoc.cloudsumativa1.repository;

import com.duoc.cloudsumativa1.model.Asset;

/**
 * Operaciones sobre archivos en el bucket S3.
 *
 * No es un JpaRepository (S3 no es una base de datos): es el mismo patrón
 * "repositorio" aplicado al almacenamiento. Es la ÚNICA capa que conoce S3;
 * si mañana se cambia de proveedor, solo cambia la implementación.
 *
 * El bucket NO se recibe como parámetro: viene de la configuración (app.s3.bucket).
 */
public interface S3Repository {

    /** Sube (o reemplaza, si la clave ya existe) un archivo. */
    Asset subir(String key, byte[] contenido, String contentType);

    /** Descarga el contenido de un archivo. */
    byte[] descargar(String key);

    /** true si el archivo existe en el bucket. */
    boolean existe(String key);

    /** Borra el archivo. */
    void eliminar(String key);
}
