package es.academia.campushub.service;

import es.academia.campushub.controller.dto.*;
import es.academia.campushub.document.SeguimientoDocument;
import es.academia.campushub.entity.Actividad;
import es.academia.campushub.entity.ContactoEstudiante;
import es.academia.campushub.entity.Departamento;
import es.academia.campushub.entity.Docente;
import es.academia.campushub.entity.Estudiante;
import es.academia.campushub.entity.Etiqueta;
import es.academia.campushub.entity.Grupo;
import es.academia.campushub.entity.Inscripcion;
import es.academia.campushub.entity.TipoActividad;
import es.academia.campushub.mapper.CampusMapper;
import es.academia.campushub.repository.ActividadRepository;
import es.academia.campushub.repository.ContactoEstudianteRepository;
import es.academia.campushub.repository.DepartamentoRepository;
import es.academia.campushub.repository.DocenteRepository;
import es.academia.campushub.repository.EstudianteRepository;
import es.academia.campushub.repository.EtiquetaRepository;
import es.academia.campushub.repository.GrupoRepository;
import es.academia.campushub.repository.InscripcionRepository;
import es.academia.campushub.repository.SeguimientoRepository;
import es.academia.campushub.util.exception.BusinessConflictException;
import es.academia.campushub.util.exception.InvalidInputException;
import es.academia.campushub.util.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de CampusHub.
 *
 * <p>Las validaciones de esta clase dan mensajes claros (400/404/409), pero NO son la única
 * protección: PostgreSQL también tiene NOT NULL, UNIQUE, CHECK y claves foráneas, y MongoDB tiene
 * un validador $jsonSchema. Si dos peticiones simultáneas pasan las comprobaciones, la base de
 * datos rechaza la segunda (DataIntegrityViolationException → 409 en ApiExceptionHandler).
 */
@Service
public class CampusHubServiceImpl implements CampusHubService {
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final DepartamentoRepository departamentos;
  private final DocenteRepository docentes;
  private final GrupoRepository grupos;
  private final EstudianteRepository estudiantes;
  private final ContactoEstudianteRepository contactos;
  private final ActividadRepository actividades;
  private final InscripcionRepository inscripciones;
  private final EtiquetaRepository etiquetas;
  private final SeguimientoRepository seguimientos;

  public CampusHubServiceImpl(
      DepartamentoRepository departamentos,
      DocenteRepository docentes,
      GrupoRepository grupos,
      EstudianteRepository estudiantes,
      ContactoEstudianteRepository contactos,
      ActividadRepository actividades,
      InscripcionRepository inscripciones,
      EtiquetaRepository etiquetas,
      SeguimientoRepository seguimientos) {
    this.departamentos = departamentos;
    this.docentes = docentes;
    this.grupos = grupos;
    this.estudiantes = estudiantes;
    this.contactos = contactos;
    this.actividades = actividades;
    this.inscripciones = inscripciones;
    this.etiquetas = etiquetas;
    this.seguimientos = seguimientos;
  }

  // ---------------------------------------------------------------- RF02 departamentos y docentes

  @Override
  @Transactional
  public DepartamentoResponse crearDepartamento(CreateDepartamentoRequest request) {
    requerido(request);
    String nombre = texto(request.nombre(), "nombre", 100);
    if (departamentos.existsByNombreIgnoreCase(nombre)) {
      throw new BusinessConflictException("Ya existe un departamento con ese nombre");
    }
    return CampusMapper.toResponse(departamentos.saveAndFlush(new Departamento(nombre)));
  }

  @Override
  @Transactional
  public DocenteResponse crearDocente(CreateDocenteRequest request) {
    requerido(request);
    Docente docente = new Docente();
    docente.setNombre(texto(request.nombre(), "nombre", 100));
    docente.setApellidos(texto(request.apellidos(), "apellidos", 150));
    docente.setEmail(email(request.email(), "email"));
    Long departamentoId = id(request.departamentoId(), "departamentoId");
    docente.setDepartamento(
        departamentos
            .findById(departamentoId)
            .orElseThrow(() -> noExiste("departamento", departamentoId)));
    if (docentes.existsByEmailIgnoreCase(docente.getEmail())) {
      throw new BusinessConflictException("Ya existe un docente con ese correo");
    }
    return CampusMapper.toResponse(docentes.saveAndFlush(docente));
  }

  // ---------------------------------------------------------------- RF01 grupos y estudiantes

  @Override
  @Transactional
  public GrupoResponse crearGrupo(CreateGrupoRequest request) {
    requerido(request);
    Grupo grupo = new Grupo();
    grupo.setNombre(texto(request.nombre(), "nombre", 50));
    grupo.setNivel(texto(request.nivel(), "nivel", 50));
    Long tutorId = id(request.tutorDocenteId(), "tutorDocenteId");
    grupo.setTutor(docentes.findById(tutorId).orElseThrow(() -> noExiste("docente", tutorId)));
    return CampusMapper.toResponse(grupos.saveAndFlush(grupo));
  }

  @Override
  @Transactional
  public EstudianteResponse crearEstudiante(CreateEstudianteRequest request) {
    requerido(request);
    Estudiante estudiante = new Estudiante();
    estudiante.setNombre(texto(request.nombre(), "nombre", 100));
    estudiante.setApellidos(texto(request.apellidos(), "apellidos", 150));
    estudiante.setEmail(email(request.email(), "email"));
    LocalDate nacimiento = fecha(request.fechaNacimiento(), "fechaNacimiento");
    if (!nacimiento.isBefore(LocalDate.now())) {
      throw new InvalidInputException("fechaNacimiento debe ser anterior a la fecha actual");
    }
    estudiante.setFechaNacimiento(nacimiento);
    Long grupoId = id(request.grupoId(), "grupoId");
    estudiante.setGrupo(grupos.findById(grupoId).orElseThrow(() -> noExiste("grupo", grupoId)));
    if (estudiantes.existsByEmailIgnoreCase(estudiante.getEmail())) {
      throw new BusinessConflictException("Ya existe un estudiante con ese correo");
    }
    return CampusMapper.toResponse(estudiantes.saveAndFlush(estudiante));
  }

  @Override
  @Transactional(readOnly = true)
  public EstudianteResponse obtenerEstudiante(Long id) {
    return CampusMapper.toResponse(buscarEstudiante(id));
  }

  // ---------------------------------------------------------------- RF03 actividades

  @Override
  @Transactional
  public ActividadResponse crearActividad(CreateActividadRequest request) {
    requerido(request);
    Actividad actividad = new Actividad();
    actividad.setTitulo(texto(request.titulo(), "titulo", 200));
    actividad.setDescripcion(texto(request.descripcion(), "descripcion", 10_000));
    TipoActividad tipo = TipoActividad.desdeTexto(request.tipo());
    if (tipo == null) {
      throw new InvalidInputException("tipo debe ser TALLER, CHARLA, VISITA o JORNADA");
    }
    actividad.setTipo(tipo);
    actividad.setFechaPrevista(fecha(request.fechaPrevista(), "fechaPrevista"));
    if (request.maxParticipantes() == null || request.maxParticipantes() <= 0) {
      throw new InvalidInputException("maxParticipantes debe ser mayor que cero");
    }
    actividad.setMaxParticipantes(request.maxParticipantes());
    Long docenteId = id(request.docenteId(), "docenteId");
    actividad.setResponsable(
        docentes.findById(docenteId).orElseThrow(() -> noExiste("docente", docenteId)));
    return CampusMapper.toResponse(actividades.saveAndFlush(actividad));
  }

  @Override
  @Transactional(readOnly = true)
  public ActividadResponse obtenerActividad(Long id) {
    return CampusMapper.toResponse(buscarActividad(id));
  }

  // ---------------------------------------------------------------- RF04 inscripciones

  @Override
  @Transactional
  public InscripcionResponse inscribir(Long actividadId, CreateInscripcionRequest request) {
    requerido(request);
    Long estudianteId = id(request.estudianteId(), "estudianteId");
    // Bloqueo pesimista: las inscripciones de una misma actividad se serializan.
    Actividad actividad =
        actividades
            .findByIdForUpdate(actividadId)
            .orElseThrow(() -> noExiste("actividad", actividadId));
    Estudiante estudiante = buscarEstudiante(estudianteId);

    if (inscripciones.countByActividadAndEstudiante(actividadId, estudianteId) > 0) {
      throw new BusinessConflictException("El estudiante ya está inscrito en esta actividad");
    }
    if (inscripciones.countByActividad(actividadId) >= actividad.getMaxParticipantes()) {
      throw new BusinessConflictException("La actividad no tiene plazas disponibles");
    }

    Inscripcion inscripcion = new Inscripcion();
    inscripcion.setActividad(actividad);
    inscripcion.setEstudiante(estudiante);
    inscripcion.setFechaInscripcion(Instant.now().truncatedTo(ChronoUnit.MICROS));
    // saveAndFlush: si la restricción UNIQUE salta, el error se produce aquí y no al confirmar.
    return CampusMapper.toResponse(inscripciones.saveAndFlush(inscripcion));
  }

  @Override
  @Transactional(readOnly = true)
  public List<EstudianteResponse> participantes(Long actividadId) {
    buscarActividad(actividadId);
    return inscripciones.findEstudiantesInscritos(actividadId).stream()
        .map(CampusMapper::toResponse)
        .toList();
  }

  // ---------------------------------------------------------------- RF05 seguimientos (MongoDB)

  @Override
  @Transactional(readOnly = true)
  public SeguimientoResponse registrarSeguimiento(
      Long actividadId, CreateSeguimientoRequest request) {
    requerido(request);
    // Integridad entre motores: MongoDB no puede comprobar que la actividad existe en PostgreSQL.
    buscarActividad(actividadId);
    if (request.datos() == null) {
      throw new InvalidInputException("datos es obligatorio y debe ser un objeto JSON");
    }
    validarClavesMongo(request.datos(), "datos");

    SeguimientoDocument doc = new SeguimientoDocument();
    doc.setActividadId(actividadId);
    doc.setTipo(texto(request.tipo(), "tipo", 50));
    doc.setDatos(new LinkedHashMap<>(request.datos()));
    // MongoDB guarda las fechas con precisión de milisegundos.
    doc.setCreadoEn(Instant.now().truncatedTo(ChronoUnit.MILLIS));
    doc.setVersionEsquema(SeguimientoDocument.VERSION_ESQUEMA_ACTUAL);
    return CampusMapper.toResponse(seguimientos.save(doc));
  }

  // ---------------------------------------------------------------- RF06 detalle combinado

  @Override
  @Transactional(readOnly = true)
  public ActividadDetalleResponse detalle(Long id) {
    Actividad actividad = buscarActividad(id);
    int ocupadas = (int) inscripciones.countByActividad(id);
    List<EstudianteResponse> inscritos =
        inscripciones.findEstudiantesInscritos(id).stream().map(CampusMapper::toResponse).toList();
    List<EtiquetaResponse> etiquetasActividad =
        etiquetas.findByActividad(id).stream().map(CampusMapper::toResponse).toList();
    List<SeguimientoResponse> registros =
        seguimientos.findByActividadIdOrderByCreadoEnAsc(id).stream()
            .map(CampusMapper::toResponse)
            .toList();
    return new ActividadDetalleResponse(
        CampusMapper.toResponse(actividad),
        CampusMapper.toResponse(actividad.getResponsable()),
        ocupadas,
        Math.max(0, actividad.getMaxParticipantes() - ocupadas),
        inscritos,
        etiquetasActividad,
        registros);
  }

  // ---------------------------------------------------------------- RF08 historial

  @Override
  @Transactional(readOnly = true)
  public List<ActividadHistorialResponse> historial(Long estudianteId) {
    buscarEstudiante(estudianteId);
    return inscripciones.findHistorial(estudianteId).stream()
        .map(CampusMapper::toHistorial)
        .toList();
  }

  // ---------------------------------------------------------------- RF09 ficha de contacto

  @Override
  @Transactional
  public ContactoResponse guardarContacto(Long estudianteId, UpsertContactoRequest request) {
    requerido(request);
    Estudiante estudiante = buscarEstudiante(estudianteId);
    ContactoEstudiante ficha =
        contactos
            .findById(estudianteId)
            .orElseGet(
                () -> {
                  ContactoEstudiante nueva = new ContactoEstudiante();
                  nueva.setEstudiante(estudiante);
                  return nueva;
                });
    ficha.setTutorLegal(texto(request.tutorLegal(), "tutorLegal", 150));
    ficha.setTelefono(texto(request.telefono(), "telefono", 20));
    ficha.setEmail(
        request.email() == null || request.email().isBlank()
            ? null
            : email(request.email(), "email"));
    if (request.autorizado() == null) {
      throw new InvalidInputException("autorizado es obligatorio");
    }
    ficha.setAutorizado(request.autorizado());
    LocalDate fechaAutorizacion =
        request.fechaAutorizacion() == null || request.fechaAutorizacion().isBlank()
            ? null
            : fecha(request.fechaAutorizacion(), "fechaAutorizacion");
    if (request.autorizado() && fechaAutorizacion == null) {
      throw new InvalidInputException("fechaAutorizacion es obligatoria si autorizado es true");
    }
    ficha.setFechaAutorizacion(fechaAutorizacion);
    return CampusMapper.toResponse(contactos.saveAndFlush(ficha));
  }

  @Override
  @Transactional(readOnly = true)
  public ContactoResponse contacto(Long estudianteId) {
    buscarEstudiante(estudianteId);
    return contactos
        .findById(estudianteId)
        .map(CampusMapper::toResponse)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "El estudiante " + estudianteId + " no tiene ficha de contacto"));
  }

  // ---------------------------------------------------------------- RF10 etiquetas

  @Override
  @Transactional
  public EtiquetaResponse crearEtiqueta(CreateEtiquetaRequest request) {
    requerido(request);
    // Las etiquetas se guardan en minúsculas para que "Seguridad" y "seguridad" sean la misma.
    String nombre = texto(request.nombre(), "nombre", 60).toLowerCase(Locale.ROOT);
    if (etiquetas.existsByNombreIgnoreCase(nombre)) {
      throw new BusinessConflictException("Ya existe una etiqueta con ese nombre");
    }
    return CampusMapper.toResponse(etiquetas.saveAndFlush(new Etiqueta(nombre)));
  }

  @Override
  @Transactional
  public void asociarEtiqueta(Long actividadId, Long etiquetaId) {
    Actividad actividad = buscarActividad(actividadId);
    Etiqueta etiqueta =
        etiquetas.findById(etiquetaId).orElseThrow(() -> noExiste("etiqueta", etiquetaId));
    boolean yaAsociada =
        actividad.getEtiquetas().stream().anyMatch(e -> e.getId().equals(etiqueta.getId()));
    // Operación idempotente: repetirla no duplica la relación (además, la PK de
    // actividad_etiqueta lo impediría en la base de datos).
    if (!yaAsociada) {
      actividad.getEtiquetas().add(etiqueta);
      actividades.saveAndFlush(actividad);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<ActividadResponse> actividadesPorEtiqueta(Long etiquetaId) {
    if (!etiquetas.existsById(etiquetaId)) {
      throw noExiste("etiqueta", etiquetaId);
    }
    return actividades.findByEtiqueta(etiquetaId).stream().map(CampusMapper::toResponse).toList();
  }

  // ---------------------------------------------------------------- utilidades

  private Estudiante buscarEstudiante(Long id) {
    return estudiantes.findById(id).orElseThrow(() -> noExiste("estudiante", id));
  }

  private Actividad buscarActividad(Long id) {
    return actividades.findById(id).orElseThrow(() -> noExiste("actividad", id));
  }

  private static ResourceNotFoundException noExiste(String recurso, Long id) {
    return new ResourceNotFoundException("No existe " + recurso + " con id " + id);
  }

  private static void requerido(Object request) {
    if (request == null) {
      throw new InvalidInputException("El cuerpo de la petición es obligatorio");
    }
  }

  private static Long id(Long valor, String campo) {
    if (valor == null) {
      throw new InvalidInputException(campo + " es obligatorio");
    }
    return valor;
  }

  private static String texto(String valor, String campo, int longitudMaxima) {
    if (valor == null || valor.isBlank()) {
      throw new InvalidInputException(campo + " es obligatorio");
    }
    String limpio = valor.trim();
    if (limpio.length() > longitudMaxima) {
      throw new InvalidInputException(
          campo + " no puede superar " + longitudMaxima + " caracteres");
    }
    return limpio;
  }

  private static String email(String valor, String campo) {
    String limpio = texto(valor, campo, 254).toLowerCase(Locale.ROOT);
    if (!EMAIL.matcher(limpio).matches()) {
      throw new InvalidInputException(campo + " no tiene un formato de correo válido");
    }
    return limpio;
  }

  private static LocalDate fecha(String valor, String campo) {
    if (valor == null || valor.isBlank()) {
      throw new InvalidInputException(campo + " es obligatorio");
    }
    try {
      return LocalDate.parse(valor.trim());
    } catch (DateTimeParseException e) {
      throw new InvalidInputException(campo + " debe tener el formato YYYY-MM-DD");
    }
  }

  /**
   * MongoDB y Spring Data no admiten claves con "." ni que empiecen por "$". Se rechazan con 400 en
   * vez de modificarlas, para conservar exactamente la estructura recibida.
   */
  private static void validarClavesMongo(Object valor, String ruta) {
    if (valor instanceof Map<?, ?> mapa) {
      for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
        String clave = String.valueOf(entrada.getKey());
        if (clave.isEmpty() || clave.contains(".") || clave.startsWith("$")) {
          throw new InvalidInputException(
              "Clave no permitida en " + ruta + ": '" + clave + "' (sin '.' ni '$' inicial)");
        }
        validarClavesMongo(entrada.getValue(), ruta + "." + clave);
      }
    } else if (valor instanceof List<?> lista) {
      for (Object elemento : lista) {
        validarClavesMongo(elemento, ruta + "[]");
      }
    }
  }
}
