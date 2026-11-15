package es.academia.campushub.entity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "actividad",
    indexes = {
      @Index(name = "ix_actividad_docente", columnList = "docente_id"),
      @Index(name = "ix_actividad_fecha_prevista", columnList = "fecha_prevista")
    },
    check = {
      @CheckConstraint(name = "ck_actividad_titulo", constraint = "length(trim(titulo)) > 0"),
      @CheckConstraint(
          name = "ck_actividad_descripcion",
          constraint = "length(trim(descripcion)) > 0"),
      @CheckConstraint(
          name = "ck_actividad_max_participantes",
          constraint = "max_participantes > 0")
    })
@Getter
@Setter
@NoArgsConstructor
public class Actividad {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String titulo;

  @Column(nullable = false, columnDefinition = "text")
  private String descripcion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TipoActividad tipo;

  @Column(name = "fecha_prevista", nullable = false)
  private LocalDate fechaPrevista;

  @Column(name = "max_participantes", nullable = false)
  private Integer maxParticipantes;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "docente_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_actividad_docente"))
  private Docente responsable;

  /**
   * Relación N:M con etiquetas. Al ser un Set, Hibernate crea la tabla actividad_etiqueta con clave
   * primaria (actividad_id, etiqueta_id): la misma etiqueta no puede asociarse dos veces.
   */
  @ManyToMany
  @JoinTable(
      name = "actividad_etiqueta",
      joinColumns =
          @JoinColumn(
              name = "actividad_id",
              foreignKey = @ForeignKey(name = "fk_actividad_etiqueta_actividad")),
      inverseJoinColumns =
          @JoinColumn(
              name = "etiqueta_id",
              foreignKey = @ForeignKey(name = "fk_actividad_etiqueta_etiqueta")),
      indexes = @Index(name = "ix_actividad_etiqueta_etiqueta", columnList = "etiqueta_id"))
  private Set<Etiqueta> etiquetas = new HashSet<>();
}
