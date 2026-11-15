package es.academia.campushub.document;

import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Registro de seguimiento de una actividad (RF05). Cada registro es un documento independiente de
 * la colección "seguimientos". El campo datos guarda el objeto JSON recibido tal cual: su
 * estructura cambia según el tipo (taller, visita, charla...) y puede evolucionar sin migraciones.
 *
 * <p>actividadId es una referencia lógica al id de la actividad en PostgreSQL (no hay clave foránea
 * entre motores): la capa de servicio comprueba que la actividad existe antes de guardar.
 */
@Document(collection = SeguimientoDocument.COLECCION)
@Getter
@Setter
@NoArgsConstructor
public class SeguimientoDocument {
  public static final String COLECCION = "seguimientos";
  public static final int VERSION_ESQUEMA_ACTUAL = 1;

  @Id private String id;

  private Long actividadId;

  private String tipo;

  private Map<String, Object> datos;

  private Instant creadoEn;

  /** Versión del formato del documento, para poder leer registros antiguos si el formato cambia. */
  private Integer versionEsquema;
}
