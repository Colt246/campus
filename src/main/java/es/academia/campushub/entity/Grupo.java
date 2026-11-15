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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "grupo",
    indexes = @Index(name = "ix_grupo_tutor", columnList = "tutor_docente_id"),
    check = {
      @CheckConstraint(name = "ck_grupo_nombre", constraint = "length(trim(nombre)) > 0"),
      @CheckConstraint(name = "ck_grupo_nivel", constraint = "length(trim(nivel)) > 0")
    })
@Getter
@Setter
@NoArgsConstructor
public class Grupo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50)
  private String nombre;

  @Column(nullable = false, length = 50)
  private String nivel;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "tutor_docente_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_grupo_tutor_docente"))
  private Docente tutor;
}
