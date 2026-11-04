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

Pruebas (H2 en memoria):

```sh
./mvnw test
```
