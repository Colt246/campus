package es.academia.campushub.repository;

import es.academia.campushub.entity.Actividad;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {

  /**
   * Bloquea la fila de la actividad (SELECT ... FOR UPDATE) hasta el final de la transacción. Así
   * dos inscripciones simultáneas en la misma actividad se ejecutan una detrás de otra y no se
   * puede superar el aforo.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Actividad a where a.id = :id")
  Optional<Actividad> findByIdForUpdate(@Param("id") Long id);

  @Query(
      "select a from Actividad a join a.etiquetas e where e.id = :etiquetaId"
          + " order by a.fechaPrevista, a.id")
  List<Actividad> findByEtiqueta(@Param("etiquetaId") Long etiquetaId);
}
