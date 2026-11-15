package es.academia.campushub.entity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Ficha de contacto (RF09). La clave primaria es la misma que la del estudiante (@MapsId): así la
 * base de datos garantiza que un estudiante tiene como máximo una ficha y que la ficha no puede
 * existir sin él.
 */
@Entity
@Table(
    name = "contacto_estudiante",
    check = {
      @CheckConstraint(
          name = "ck_contacto_tutor_legal",
          constraint = "length(trim(tutor_legal)) > 0"),
      @CheckConstraint(name = "ck_contacto_telefono", constraint = "length(trim(telefono)) > 0"),
      @CheckConstraint(
          name = "ck_contacto_email",
          constraint = "email IS NULL OR email LIKE '%_@_%'"),
      @CheckConstraint(
          name = "ck_contacto_autorizacion",
          constraint = "autorizado = false OR fecha_autorizacion IS NOT NULL")
    })
@Getter
@Setter
@NoArgsConstructor
public class ContactoEstudiante {
  @Id
  @Column(name = "estudiante_id")
  private Long estudianteId;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "estudiante_id", foreignKey = @ForeignKey(name = "fk_contacto_estudiante"))
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Estudiante estudiante;

  @Column(name = "tutor_legal", nullable = false, length = 150)
  private String tutorLegal;

  @Column(nullable = false, length = 20)
  private String telefono;

  @Column(length = 254)
  private String email;

  @Column(nullable = false)
  private Boolean autorizado;

  @Column(name = "fecha_autorizacion")
  private LocalDate fechaAutorizacion;
}
