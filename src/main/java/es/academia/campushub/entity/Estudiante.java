package es.academia.campushub.entity;

import jakarta.persistence.CheckConstraint;
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
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "estudiante",
    uniqueConstraints = @UniqueConstraint(name = "uk_estudiante_email", columnNames = "email"),
    indexes = @Index(name = "ix_estudiante_grupo", columnList = "grupo_id"),
    check = {
      @CheckConstraint(name = "ck_estudiante_nombre", constraint = "length(trim(nombre)) > 0"),
      @CheckConstraint(
          name = "ck_estudiante_apellidos",
          constraint = "length(trim(apellidos)) > 0"),
      @CheckConstraint(name = "ck_estudiante_email", constraint = "email LIKE '%_@_%'"),
      @CheckConstraint(
          name = "ck_estudiante_fecha_nacimiento",
          constraint = "fecha_nacimiento < CURRENT_DATE")
    })
@Getter
@Setter
@NoArgsConstructor
public class Estudiante {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(nullable = false, length = 150)
  private String apellidos;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(name = "fecha_nacimiento", nullable = false)
  private LocalDate fechaNacimiento;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "grupo_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_estudiante_grupo"))
  private Grupo grupo;
}
