# cloudsumativa1

Sistema de reservas de eventos desarrollado con Spring Boot que permite gestionar usuarios, reservar cupos y generar tickets en formato TXT. Incluye consulta, modificación y eliminación de tickets, además de estadísticas por evento. Utiliza H2 o MySQL para los datos, EFS para los archivos y S3 para su almacenamiento y descarga, con despliegue automatizado en EC2 mediante GitHub Actions y Docker.

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

S3 (opcional, con H2 o MySQL):

```sh
S3_ENABLED=true \
AWS_REGION=us-east-1 \
S3_BUCKET='nombre-del-bucket' \
AWS_ACCESS_KEY_ID='tu-access-key' \
AWS_SECRET_ACCESS_KEY='tu-secret-key' \
AWS_SESSION_TOKEN='tu-session-token' \
./mvnw spring-boot:run
```

Para MySQL, agregar `-Dspring-boot.run.profiles=mysql` al comando.

Pruebas (H2 en memoria):

```sh
./mvnw test
```

RDS: antes del primer despliegue, conectarse a MySQL y crear la base:

```sql
CREATE DATABASE IF NOT EXISTS cloudsumativa1;
```

`MYSQL_JDBC_URL` debe apuntar a esa base. Las tablas se crean automáticamente.

Al reiniciar el lab de AWS Academy, actualizar `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN` en GitHub Secrets y volver a ejecutar el workflow de despliegue.

Al reiniciar EC2 o el lab, verificar el montaje de EFS y, si falta, volver a montarlo antes de desplegar :

```sh
sudo mount -t nfs4 -o nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport fs-0a18d88fdf8fc799a.efs.us-east-1.amazonaws.com:/ /mnt/efs
```
