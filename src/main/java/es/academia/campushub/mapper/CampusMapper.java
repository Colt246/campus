package es.academia.campushub.mapper;

import es.academia.campushub.controller.dto.ActividadHistorialResponse;
import es.academia.campushub.controller.dto.ActividadResponse;
import es.academia.campushub.controller.dto.ContactoResponse;
import es.academia.campushub.controller.dto.DepartamentoResponse;
import es.academia.campushub.controller.dto.DocenteResponse;
import es.academia.campushub.controller.dto.EstudianteResponse;
import es.academia.campushub.controller.dto.EtiquetaResponse;
import es.academia.campushub.controller.dto.GrupoResponse;
import es.academia.campushub.controller.dto.InscripcionResponse;
import es.academia.campushub.controller.dto.SeguimientoResponse;
import es.academia.campushub.document.SeguimientoDocument;
import es.academia.campushub.entity.Actividad;
import es.academia.campushub.entity.ContactoEstudiante;
import es.academia.campushub.entity.Departamento;
import es.academia.campushub.entity.Docente;
import es.academia.campushub.entity.Estudiante;
import es.academia.campushub.entity.Etiqueta;
import es.academia.campushub.entity.Grupo;
import es.academia.campushub.entity.Inscripcion;
import java.time.temporal.Temporal;
import java.util.LinkedHashMap;

/** Conversión manual entre entidades/documentos y DTO. Las fechas se devuelven en ISO-8601. */
public final class CampusMapper {
  private CampusMapper() {}

  public static DepartamentoResponse toResponse(Departamento d) {
    return new DepartamentoResponse(d.getId(), d.getNombre());
  }

  public static DocenteResponse toResponse(Docente d) {
    return new DocenteResponse(
        d.getId(), d.getNombre(), d.getApellidos(), d.getEmail(), d.getDepartamento().getId());
  }

  public static GrupoResponse toResponse(Grupo g) {
    return new GrupoResponse(g.getId(), g.getNombre(), g.getNivel(), g.getTutor().getId());
  }

  public static EstudianteResponse toResponse(Estudiante e) {
    return new EstudianteResponse(
        e.getId(),
        e.getNombre(),
        e.getApellidos(),
        e.getEmail(),
        iso(e.getFechaNacimiento()),
        e.getGrupo().getId());
  }

  public static ActividadResponse toResponse(Actividad a) {
    return new ActividadResponse(
        a.getId(),
        a.getTitulo(),
        a.getDescripcion(),
        a.getTipo().name(),
        iso(a.getFechaPrevista()),
        a.getMaxParticipantes(),
        a.getResponsable().getId());
  }

  public static InscripcionResponse toResponse(Inscripcion i) {
    return new InscripcionResponse(
        i.getId(),
        i.getActividad().getId(),
        i.getEstudiante().getId(),
        iso(i.getFechaInscripcion()));
  }

  public static ActividadHistorialResponse toHistorial(Inscripcion i) {
    Actividad a = i.getActividad();
    return new ActividadHistorialResponse(
        a.getId(),
        a.getTitulo(),
        a.getTipo().name(),
        iso(a.getFechaPrevista()),
        iso(i.getFechaInscripcion()),
        toResponse(a.getResponsable()));
  }

  public static ContactoResponse toResponse(ContactoEstudiante c) {
    return new ContactoResponse(
        c.getEstudianteId(),
        c.getTutorLegal(),
        c.getTelefono(),
        c.getEmail(),
        c.getAutorizado(),
        iso(c.getFechaAutorizacion()));
  }

  public static EtiquetaResponse toResponse(Etiqueta e) {
    return new EtiquetaResponse(e.getId(), e.getNombre());
  }

  public static SeguimientoResponse toResponse(SeguimientoDocument s) {
    return new SeguimientoResponse(
        s.getId(),
        s.getActividadId(),
        s.getTipo(),
        s.getDatos() == null ? new LinkedHashMap<>() : s.getDatos(),
        iso(s.getCreadoEn()));
  }

  private static String iso(Temporal valor) {
    return valor == null ? null : valor.toString();
  }
}
