package com.duoc.eft.inscripciones.repository;

import com.duoc.eft.inscripciones.model.Inscripcion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    List<Inscripcion> findByCursoId(Long cursoId);
    boolean existsByEstudianteIdAndCursoId(String estudianteId, Long cursoId);
}
