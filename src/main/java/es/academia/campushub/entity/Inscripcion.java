package es.academia.campushub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Inscripción de un estudiante en una actividad (RF04). Es una entidad propia (y no un simple
 * ManyToMany) porque guarda la fecha de inscripción. La restricción UNIQUE impide inscribir dos
 * veces al mismo estudiante en la misma actividad, incluso con peticiones simultáneas.
 */
@Entity
@Table(
    name = "inscripcion",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_inscripcion_actividad_estudiante",
            columnNames = {"actividad_id", "estudiante_id"}),
    indexes = @Index(name = "ix_inscripcion_estudiante", columnList = "estudiante_id"))
@Getter
@Setter
@NoArgsConstructor
public class Inscripcion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "actividad_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_inscripcion_actividad"))
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Actividad actividad;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "estudiante_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_inscripcion_estudiante"))
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Estudiante estudiante;

  @Column(name = "fecha_inscripcion", nullable = false)
  private Instant fechaInscripcion;
}
