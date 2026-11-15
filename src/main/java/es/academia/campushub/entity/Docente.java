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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "docente",
    uniqueConstraints = @UniqueConstraint(name = "uk_docente_email", columnNames = "email"),
    indexes = @Index(name = "ix_docente_departamento", columnList = "departamento_id"),
    check = {
      @CheckConstraint(name = "ck_docente_nombre", constraint = "length(trim(nombre)) > 0"),
      @CheckConstraint(name = "ck_docente_apellidos", constraint = "length(trim(apellidos)) > 0"),
      @CheckConstraint(name = "ck_docente_email", constraint = "email LIKE '%_@_%'")
    })
@Getter
@Setter
@NoArgsConstructor
public class Docente {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(nullable = false, length = 150)
  private String apellidos;

  @Column(nullable = false, length = 254)
  private String email;

  // RESTRICT (comportamiento por defecto): no se puede borrar un departamento con docentes.
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "departamento_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_docente_departamento"))
  private Departamento departamento;
}
