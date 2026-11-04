package com.duoc.cloudsumativa1.repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.Assert;

import com.duoc.cloudsumativa1.model.Asset;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;

/**
 * Implementación con S3Template (Spring Cloud AWS), que envuelve al cliente del SDK de AWS.
 */
@Repository
@ConditionalOnProperty(name = "app.s3.enabled", havingValue = "true")
public class S3RepositoryImpl implements S3Repository {

    private final S3Template s3Template;
    private final String bucket;

    public S3RepositoryImpl(S3Template s3Template, @Value("${app.s3.bucket}") String bucket) {
        Assert.hasText(bucket, "Configura app.s3.bucket para habilitar S3");
        this.s3Template = s3Template;
        this.bucket = bucket;
    }

    @Override
    public Asset subir(String key, byte[] contenido, String contentType) {
        ObjectMetadata metadata = ObjectMetadata.builder()
                .contentType(contentType)
                .build();
        // En S3, subir con una clave que ya existe REEMPLAZA el archivo.
        // Si el bucket tiene Versioning activado, la versión anterior queda guardada.
        s3Template.upload(bucket, key, new ByteArrayInputStream(contenido), metadata);

        return new Asset(key, nombreDe(key), contentType, contenido.length);
    }

    @Override
    public byte[] descargar(String key) {
        try (InputStream in = s3Template.download(bucket, key).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo '" + key + "' desde S3", e);
        }
    }

    @Override
    public boolean existe(String key) {
        return s3Template.objectExists(bucket, key);
    }

    @Override
    public void eliminar(String key) {
        // OJO: S3 responde "éxito" aunque la clave no exista; por eso el servicio
        // pregunta existe() antes, para poder responder 404.
        s3Template.deleteObject(bucket, key);
    }

    private String nombreDe(String key) {
        return key.substring(key.lastIndexOf('/') + 1);
    }
}
