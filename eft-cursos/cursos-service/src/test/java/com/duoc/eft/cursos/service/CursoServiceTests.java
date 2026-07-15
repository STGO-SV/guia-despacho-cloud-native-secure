package com.duoc.eft.cursos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.duoc.eft.cursos.dto.CursoRequest;
import com.duoc.eft.cursos.model.Curso;
import com.duoc.eft.cursos.repository.CursoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;

@ExtendWith(MockitoExtension.class)
class CursoServiceTests {
    @Mock CursoRepository repository;
    @Mock S3Client s3;
    @Test void creaYLista() {
        Curso guardado = new Curso(); guardado.setId(1L); guardado.setTitulo("Cloud");
        guardado.setDescripcion("EFT"); guardado.setInstructor("Docente"); guardado.setEstado("ACTIVO");
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenReturn(guardado);
        when(repository.findAll()).thenReturn(List.of(guardado));
        CursoService service = new CursoService(repository, s3, "demo", false);
        assertEquals(1L, service.crear(new CursoRequest("Cloud", "EFT", "Docente", "ACTIVO")).id());
        assertEquals(1, service.listar().size());
    }
}

