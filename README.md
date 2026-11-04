# cloudsumativa1

Requiere Java 21. Ejecutar desde esta carpeta.

H2 (por defecto):

```sh
./mvnw spring-boot:run
```

MySQL (requiere Docker):

```sh
docker build -t db-sumativa1 ./db-sumativa1
docker run -d --name db-sumativa1 -p 3306:3306 db-sumativa1
```

Cuando MySQL esté listo:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

Credenciales locales: `admin` / `password`, base `cloudsumativa1`.
Para reutilizar un contenedor detenido: `docker start db-sumativa1`.

Las tablas se crean automáticamente. Cada motor conserva sus propios datos.

S3 (opcional, independiente de H2/MySQL): en `application.properties` configurar
`app.s3.enabled=true`, `app.s3.bucket` y las tres credenciales `app.aws.*` de AWS Academy.
Los tickets se suben automáticamente; `POST /tickets/{id}/s3` permite reintentarlo y
`GET /tickets/{id}/s3` descarga desde S3.

Pruebas (H2 en memoria):

```sh
./mvnw test
```
