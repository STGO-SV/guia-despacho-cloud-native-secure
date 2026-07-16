package com.duoc.eft.inscripciones.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.duoc.eft.inscripciones.model.Inscripcion;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class InscripcionRepositoryTests {
    @Autowired InscripcionRepository repository;

    @Test void restriccionFisicaImpideMismoEstudianteYMismoCurso() {
        repository.saveAndFlush(inscripcion("sub-a", 1L));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(inscripcion("sub-a", 1L)));
    }

    @Test void permiteEstudianteEnOtroCursoYOtroEstudianteEnMismoCurso() {
        repository.saveAndFlush(inscripcion("sub-a", 1L));

        assertDoesNotThrow(() -> repository.saveAndFlush(inscripcion("sub-a", 2L)));
        assertDoesNotThrow(() -> repository.saveAndFlush(inscripcion("sub-b", 1L)));
        assertTrue(repository.existsByEstudianteIdAndCursoId("sub-a", 1L));
    }

    private Inscripcion inscripcion(String estudianteId, Long cursoId) {
        Inscripcion value = new Inscripcion();
        value.setEstudianteId(estudianteId);
        value.setCursoId(cursoId);
        value.setFechaInscripcion(LocalDate.now());
        value.setEstado("CREADA");
        return value;
    }
}
