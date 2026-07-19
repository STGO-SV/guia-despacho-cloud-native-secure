package com.duoc.eft.inscripciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.duoc.eft.inscripciones.dto.InscripcionRequest;
import com.duoc.eft.inscripciones.exception.ComprobanteStorageException;
import com.duoc.eft.inscripciones.messaging.InscripcionEventoPublisher;
import com.duoc.eft.inscripciones.repository.InscripcionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import(InscripcionService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InscripcionStorageTransactionTests {
    @Autowired InscripcionService service;
    @Autowired InscripcionRepository repository;
    @MockitoBean InscripcionEventoPublisher publisher;
    @MockitoBean ComprobantePdfGenerator pdfGenerator;
    @MockitoBean S3ComprobanteStorage comprobanteStorage;

    @BeforeEach
    void limpiar() {
        repository.deleteAll();
    }

    @Test
    void falloS3RevierteInscripcionYNoPublicaEvento() {
        when(pdfGenerator.generar(any(), any())).thenReturn("pdf".getBytes());
        when(comprobanteStorage.guardar(any(), any()))
                .thenThrow(new ComprobanteStorageException("S3 no disponible"));

        assertThatThrownBy(() -> service.crear(
                new InscripcionRequest(3L, false), "sub-transaccion"))
                .isInstanceOf(ComprobanteStorageException.class);

        assertThat(repository.count()).isZero();
        verifyNoInteractions(publisher);
    }

    @Test
    void modoDeshabilitadoPersisteKeySinMarcarUpload() {
        when(pdfGenerator.generar(any(), any())).thenReturn("pdf".getBytes());
        when(comprobanteStorage.guardar(any(), any())).thenReturn(false);

        var response = service.crear(new InscripcionRequest(4L, false), "sub-local");

        var persisted = repository.findById(response.id()).orElseThrow();
        assertThat(persisted.getComprobanteS3Key())
                .isEqualTo("2026/inscripciones/" + response.id()
                        + "/comprobante-inscripcion.pdf");
        assertThat(persisted.isComprobanteAlmacenado()).isFalse();
    }
}
