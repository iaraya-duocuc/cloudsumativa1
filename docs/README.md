# Documentación general — cloudsumativa1

API REST para gestionar usuarios, eventos y reservas. Cada reserva genera un ticket TXT; los datos se guardan en H2 o MySQL y los archivos en disco local o EFS, con sincronización opcional en S3.

## Tecnologías y estructura

Java 21, Spring Boot 4.1.2, Spring Data JPA, Bean Validation, Spring Cloud AWS, Maven y Docker.

| Ruta | Responsabilidad |
| --- | --- |
| `src/main/java/com/duoc/cloudsumativa1/controller` | Endpoints HTTP. |
| `src/main/java/com/duoc/cloudsumativa1/service` | Reservas, cupos, estadísticas y archivos. |
| `src/main/java/com/duoc/cloudsumativa1/repository` | Acceso a base de datos y S3. |
| `src/main/java/com/duoc/cloudsumativa1/model` y `dto` | Entidades y contratos de datos. |
| `src/main/resources` | Configuración general y perfiles H2/MySQL. |
| `src/test` | Pruebas de API, persistencia e integración con S3 mediante dobles de prueba. |
| `postman` | Colección para probar la API. |
| `.github/workflows/main.yml` | Pruebas, publicación de imagen y despliegue. |

## Ejecución local

Ejecutar los comandos desde la raíz del repositorio. Se requiere Java 21; Maven se incluye mediante el wrapper.

```sh
# Iniciar con H2 (perfil predeterminado)
./mvnw spring-boot:run

# Ejecutar pruebas con H2 en memoria
./mvnw test
```

La API queda disponible en `http://localhost:8080`. H2 persiste en `./data/cloudsumativa1` y los tickets en `./storage/tickets`. Hibernate crea o actualiza las tablas automáticamente.

Para usar MySQL local con Docker:

```sh
docker build -t db-sumativa1 ./db-sumativa1
docker run -d --name db-sumativa1 -p 3306:3306 db-sumativa1
# Esperar a que MySQL esté listo
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

El perfil MySQL apunta a la base `cloudsumativa1` en `localhost:3306`. Si el contenedor ya existe y está detenido, iniciarlo con `docker start db-sumativa1`.

## API principal

Las solicitudes con cuerpo usan JSON. Los identificadores se indican entre llaves.

| Método | Ruta | Operación |
| --- | --- | --- |
| POST / GET | `/usuarios` | Crear o listar usuarios. |
| GET | `/usuarios/{id}` | Consultar un usuario. |
| POST / GET | `/eventos` | Crear o listar eventos. |
| GET | `/eventos/{id}` | Consultar un evento. |
| GET | `/eventos/{id}/estadisticas` | Consultar reservas, cupos e importe calculado. |
| POST | `/eventos/{eventoId}/tickets` | Reservar con cuerpo `{"usuarioId": 1}`. |
| GET | `/tickets/{id}` | Consultar un ticket. |
| GET | `/tickets/{id}/archivo` | Descargar el TXT local o almacenado en EFS. |
| GET | `/usuarios/{id}/tickets` | Listar tickets de un usuario. |
| GET | `/eventos/{id}/tickets` | Listar tickets de un evento. |
| PATCH | `/tickets/{id}` | Actualizar con `{"observacion": "Texto"}` (máximo 500 caracteres). |
| DELETE | `/tickets/{id}` | Eliminar la reserva y devolver el cupo. |
| POST / GET | `/tickets/{id}/s3` | Subir o descargar el TXT en S3. |

Flujo habitual: crear un usuario, crear un evento, reservar un ticket y descargar su archivo. La colección [Postman](../postman/cloudsumativa1.postman_collection.json) contiene solicitudes de ejemplo.

## Reservas y almacenamiento

- La reserva descuenta un cupo y genera un código UUID y un TXT. Si no quedan cupos, responde `409`.
- Al modificar una observación se actualiza el TXT; al eliminar el ticket se borra el archivo y se recupera el cupo.
- Con S3 habilitado, crear, modificar y eliminar tickets también actualiza el objeto remoto. Su clave sigue el formato `evento{eventoId}/usuario{usuarioId}/ticket{codigo}.txt`.
- Las operaciones usan transacciones y mecanismos de limpieza o restauración de archivos ante una reversión. Las fallas de restauración se registran en los logs.
- Los recursos inexistentes responden `404`; las operaciones S3 responden `503` si está deshabilitado y `502` ante fallas de integración.

## Configuración

| Variable de entorno | Uso |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Seleccionar `h2` o `mysql`. |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Sobrescribir la conexión a la base de datos. |
| `TICKETS_STORAGE_PATH` | Directorio de tickets; por defecto `./storage/tickets`. |
| `S3_ENABLED` | Activar S3 con `true`; por defecto `false`. |
| `S3_BUCKET`, `AWS_REGION` | Bucket y región; región predeterminada `us-east-1`. |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | Credenciales AWS; el token se requiere para credenciales temporales de AWS Academy. |

Mantener los secretos fuera del código y la documentación. Las contraseñas incluidas en la configuración MySQL local deben sustituirse para otros entornos mediante variables de entorno. En CI/CD se utilizan GitHub Secrets.

## Despliegue

GitHub Actions ejecuta las pruebas en pushes y pull requests a `main`. Después de un push exitoso a `main`, construye la imagen Docker, la publica en Docker Hub y despliega en EC2 mediante SSH.

El despliegue utiliza MySQL, habilita S3 y exige EFS montado en `/mnt/efs`; dentro del contenedor los tickets quedan en `/app/efs/tickets`. Finalmente verifica `GET /eventos` en el puerto 8080.

Configurar estos GitHub Secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `USER_SERVER`, `EC2_SSH_KEY`, `MYSQL_JDBC_URL`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`, `S3_BUCKET`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN`.

Antes del primer despliegue, crear la base `cloudsumativa1` en MySQL/RDS y asegurar la conectividad desde EC2. Al reiniciar AWS Academy, renovar las credenciales temporales en GitHub Secrets y comprobar el montaje EFS antes de desplegar.
