package es.academia.campushub.repository;

import es.academia.campushub.entity.ContactoEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;

/** La clave de la ficha es el id del estudiante. */
public interface ContactoEstudianteRepository extends JpaRepository<ContactoEstudiante, Long> {}
