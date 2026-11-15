package es.academia.campushub.repository;

import es.academia.campushub.entity.Estudiante;
import es.academia.campushub.entity.Inscripcion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

  @Query("select count(i) from Inscripcion i where i.actividad.id = :actividadId")
  long countByActividad(@Param("actividadId") Long actividadId);

  @Query(
      "select count(i) from Inscripcion i"
          + " where i.actividad.id = :actividadId and i.estudiante.id = :estudianteId")
  long countByActividadAndEstudiante(
      @Param("actividadId") Long actividadId, @Param("estudianteId") Long estudianteId);

  @Query(
      "select i.estudiante from Inscripcion i where i.actividad.id = :actividadId"
          + " order by i.fechaInscripcion, i.id")
  List<Estudiante> findEstudiantesInscritos(@Param("actividadId") Long actividadId);

  @Query(
      "select i from Inscripcion i join fetch i.actividad a join fetch a.responsable"
          + " where i.estudiante.id = :estudianteId order by a.fechaPrevista, i.id")
  List<Inscripcion> findHistorial(@Param("estudianteId") Long estudianteId);
}
