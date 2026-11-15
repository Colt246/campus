# CampusHub

API REST con Spring Boot para gestionar las actividades complementarias de varios centros
educativos: grupos, estudiantes, departamentos, docentes, actividades, inscripciones, fichas de
contacto, etiquetas y registros de seguimiento.

- **PostgreSQL** guarda los datos estructurados y relacionados (JPA / Hibernate).
- **MongoDB** guarda los registros de seguimiento, cuya estructura varía (Spring Data MongoDB).

Documentación de la entrega:

- [docs/modelo-datos.md](docs/modelo-datos.md): diagrama relacional, documentos, índices y restricciones.
- [docs/decisiones-tecnicas.md](docs/decisiones-tecnicas.md): reparto entre motores, alternativas, riesgos y limitaciones.
- [docs/api-contract.md](docs/api-contract.md): contrato HTTP.

## Requisitos previos

- JDK 21.
- Maven 3.9 o superior (IntelliJ IDEA ya lo incluye).
- Docker Desktop (o Docker Engine) con Docker Compose v2, **arrancado**.

Comprobar:

```bash
java -version
mvn -version
docker compose version
```

## Arrancar la aplicación

Desde la raíz del proyecto:

```bash
mvn spring-boot:run
```

Spring Boot detecta `compose.yaml` y levanta PostgreSQL, MongoDB y Redis antes de iniciar la API.

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs

Gestionando los contenedores a mano:

```bash
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.docker.compose.enabled=false
```

## Ejecutar las pruebas

Las pruebas usan Testcontainers, así que Docker tiene que estar en marcha.

```bash
mvn spotless:apply   # aplica el formato del código (necesario antes de verify)
mvn test             # pruebas unitarias (*Test)
mvn verify           # pruebas de integración (*IT) + comprobación de formato
```

## Consultar directamente las bases de datos

### PostgreSQL

```bash
docker compose exec postgres psql -U campushub -d campushub
```

```sql
\dt                                   -- tablas
\d inscripcion                        -- columnas, índices y restricciones de una tabla
SELECT * FROM estudiante;
SELECT a.titulo, e.email, i.fecha_inscripcion
  FROM inscripcion i
  JOIN actividad a ON a.id = i.actividad_id
  JOIN estudiante e ON e.id = i.estudiante_id;

-- Comprobar que la base de datos rechaza datos inválidos:
INSERT INTO actividad (titulo, descripcion, tipo, fecha_prevista, max_participantes, docente_id)
VALUES ('X', 'Y', 'TALLER', '2027-01-01', 0, 1);   -- falla: ck_actividad_max_participantes
```

### MongoDB

```bash
docker compose exec mongo mongosh campushub
```

```js
db.seguimientos.find().pretty()
db.seguimientos.find({ actividadId: NumberLong(1) }).sort({ creadoEn: 1 })
db.seguimientos.getIndexes()
db.getCollectionInfos({ name: "seguimientos" })   // muestra el validador $jsonSchema

// Comprobar que la validación rechaza documentos inválidos:
db.seguimientos.insertOne({ tipo: "VISITA" })     // falla: faltan actividadId, datos y creadoEn
```

## Detener o reiniciar el entorno

- Parar la aplicación: `Ctrl + C` en la terminal donde se ejecuta `mvn spring-boot:run`
  (también detiene los contenedores que arrancó).
- Parar los contenedores a mano, **conservando los datos**: `docker compose down`
- Volver a arrancarlos: `docker compose up -d`
- Borrar también los datos (volúmenes) y empezar de cero: `docker compose down -v`

Los datos se guardan en los volúmenes `postgres_data` y `mongo_data`, por lo que se conservan al
reiniciar la aplicación o los contenedores.
