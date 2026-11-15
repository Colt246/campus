# Decisiones técnicas — CampusHub

## 1. Qué se guarda en cada motor

| PostgreSQL | MongoDB |
|---|---|
| Departamentos, docentes, grupos, estudiantes, fichas de contacto, actividades, inscripciones, etiquetas y la relación actividad–etiqueta | Registros de seguimiento de las actividades (colección `seguimientos`) |

## 2. Por qué

**PostgreSQL para el núcleo del sistema.** Estos datos tienen una estructura estable y conocida, y
los problemas del enunciado (estudiantes inscritos dos veces, aforo superado, inscripciones de
estudiantes que no existen) son problemas de **integridad entre registros relacionados**. Eso es
justo lo que resuelve una base de datos relacional: claves foráneas, `UNIQUE`, `CHECK`, `NOT NULL`
y transacciones ACID. Por ejemplo, la regla "no inscribirse dos veces" la garantiza el índice
único `(actividad_id, estudiante_id)` aunque lleguen dos peticiones a la vez; y el aforo se protege
bloqueando la fila de la actividad (`SELECT ... FOR UPDATE`) durante la inscripción. Las consultas
más habituales (participantes, historial, actividades por etiqueta) son *joins* entre tablas.

**MongoDB para los seguimientos.** Cada tipo de actividad produce información diferente (un
taller guarda lenguaje y ejercicios; una visita, empresa e incidencias; una charla, ponente y
preguntas), pueden aparecer tipos nuevos y hay que conservar **exactamente** la estructura
recibida y recuperar registros antiguos aunque el formato evolucione. Un modelo documental encaja:
cada registro es un documento independiente con su propia forma, se añade sin tocar el esquema
y se lee con una sola consulta indexada por `actividadId`. Además, estos registros no participan
en reglas de integridad complejas: solo necesitan apuntar a una actividad existente.

## 3. Alternativas consideradas

- **Todo en PostgreSQL usando `jsonb`** para `datos`. Es una opción totalmente válida: mantendría
  la clave foránea a `actividad` y una sola transacción. La descartamos porque el enunciado pide
  usar MongoDB de forma real y porque para datos cuyo formato cambia con frecuencia MongoDB ofrece
  validación flexible por colección, índices sobre cualquier campo anidado y un modelo de lectura
  natural (el documento se devuelve tal cual). Si el proyecto solo pudiera tener un motor,
  elegiríamos PostgreSQL + `jsonb`.
- **Una tabla por tipo de seguimiento** (taller, visita, charla). Obligaría a crear una tabla y
  migrar el esquema cada vez que aparezca un tipo nuevo, y a hacer `UNION` para listar el
  detalle. Va en contra del requisito de "pueden aparecer nuevos tipos".
- **Inscripciones en MongoDB** (como array dentro de la actividad). Descartado: el control de
  plazas y de duplicados con peticiones simultáneas es mucho más sencillo y seguro con
  `UNIQUE` + bloqueo de fila que con documentos.
- **Inscripción como `@ManyToMany` simple.** Descartado porque hay que guardar la fecha de
  inscripción: se modela como entidad `Inscripcion`.
- **Ficha de contacto incrustada en la tabla estudiante** (columnas opcionales). Se prefirió una
  tabla 1:1 con `@MapsId` (PK = FK): la ficha es opcional, contiene datos sensibles de un tercero
  y la PK compartida garantiza "como máximo una ficha" y "no existe sin estudiante".

## 4. Riesgos de usar dos motores

- **No hay transacción común.** Al registrar un seguimiento se comprueba en PostgreSQL que la
  actividad existe y después se escribe en MongoDB. Si alguien borrase la actividad justo entre
  ambos pasos, quedaría un seguimiento huérfano. Del mismo modo, si se borra una actividad en
  PostgreSQL, sus seguimientos de MongoDB no se borran solos (no hay `ON DELETE CASCADE` entre
  motores).
- **Cómo reduciríamos el riesgo**: (1) la API no permite borrar actividades; (2) si se añadiera,
  se borrarían primero los seguimientos y luego la actividad, o se marcaría la actividad como
  "cancelada" en lugar de borrarla (borrado lógico); (3) un proceso periódico podría detectar
  documentos con `actividadId` inexistente; (4) para garantías fuertes se podría usar el patrón
  *outbox* (guardar el evento en PostgreSQL en la misma transacción y publicarlo después en
  MongoDB) o una saga con compensación.
- **Consultas combinadas.** El detalle (RF06) hace varias consultas: PostgreSQL para la
  actividad, inscritos y etiquetas, y MongoDB para los seguimientos. Si un motor no está
  disponible la operación falla entera. Es más trabajo de la capa de servicio que un `JOIN`.
- **Más infraestructura**: dos motores que arrancar, respaldar y vigilar.

## 5. Limitaciones de la solución

- `ddl-auto: update` crea el esquema a partir de las entidades. Es cómodo para la práctica, pero
  no añade restricciones nuevas a tablas ya existentes y no versiona cambios. En producción se
  usaría Flyway o Liquibase.
- La comprobación `fecha_nacimiento < CURRENT_DATE` se evalúa al insertar/actualizar (es lo que
  se necesita), no se recalcula sobre filas antiguas.
- MongoDB no admite claves que contengan `.` o empiecen por `$`; esos seguimientos se rechazan
  con 400 en lugar de modificar las claves, para no alterar la estructura recibida.
- El bloqueo pesimista serializa las inscripciones **de una misma actividad**. Con muchísimo
  tráfico sobre una sola actividad podría ser un cuello de botella; para este caso es adecuado.
- Los nombres de etiqueta se guardan en minúsculas para evitar duplicados como "Seguridad" y
  "seguridad".
- No se ha implementado la ampliación de Redis.

## 6. Cambios respecto al diseño inicial

- La primera idea era modelar las inscripciones como `@ManyToMany` entre actividad y estudiante;
  se cambió a una entidad propia al necesitar la fecha de inscripción y una restricción `UNIQUE`
  con nombre.
- Inicialmente el aforo se comprobaba solo contando inscripciones; se añadió el bloqueo
  `PESSIMISTIC_WRITE` sobre la actividad porque dos peticiones simultáneas podían leer el mismo
  número de inscritos y superar el aforo.
- La validación de MongoDB empezó validando también los campos de `datos` para cada tipo; se
  redujo a los campos comunes para no perder la flexibilidad que justifica usar MongoDB.
- Se añadió `versionEsquema` a los documentos para poder evolucionar el formato sin perder la
  capacidad de leer los registros históricos.
