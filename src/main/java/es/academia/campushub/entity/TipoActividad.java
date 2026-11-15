package es.academia.campushub.entity;

import java.util.Locale;

/** Tipos de actividad que organiza el centro (RF03). */
public enum TipoActividad {
  TALLER,
  CHARLA,
  VISITA,
  JORNADA;

  /** Convierte el texto recibido en la API; devuelve null si no es un tipo conocido. */
  public static TipoActividad desdeTexto(String texto) {
    if (texto == null) {
      return null;
    }
    try {
      return TipoActividad.valueOf(texto.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
