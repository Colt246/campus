package es.academia.campushub.repository;

import es.academia.campushub.entity.Etiqueta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EtiquetaRepository extends JpaRepository<Etiqueta, Long> {
  boolean existsByNombreIgnoreCase(String nombre);

  @Query("select e from Actividad a join a.etiquetas e where a.id = :actividadId order by e.nombre")
  List<Etiqueta> findByActividad(@Param("actividadId") Long actividadId);
}
