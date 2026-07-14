package com.duoc.guia_despacho.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.duoc.guia_despacho.dto.CrearGuiaRequest;
import com.duoc.guia_despacho.messaging.GuiaEventoPublisher;
import com.duoc.guia_despacho.exception.SimulacionErrorDeshabilitadaException;
import com.duoc.guia_despacho.model.GuiaDespacho;
import com.duoc.guia_despacho.repository.GuiaDespachoRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;

@ExtendWith(MockitoExtension.class)
class GuiaDespachoServiceTests {

    @Mock private GuiaDespachoRepository repository;
    @Mock private S3Client s3Client;
    @Mock private GuiaEventoPublisher publisher;
    @TempDir Path tempDir;

    private GuiaDespachoService service;

    @BeforeEach
    void setUp() {
        service = new GuiaDespachoService(repository, s3Client, publisher);
        ReflectionTestUtils.setField(service, "efsPath", tempDir.toString());
        ReflectionTestUtils.setField(service, "bucketName", "bucket-test");
        ReflectionTestUtils.setField(service, "autoUploadS3", false);
        ReflectionTestUtils.setField(service, "errorSimulationEnabled", false);
    }

    @Test
    void crearGeneraPdfGuardaMetadataYPublicaEvento() {
        when(repository.save(any(GuiaDespacho.class))).thenAnswer(invocation -> {
            GuiaDespacho guia = invocation.getArgument(0);
            if (guia.getId() == null) {
                guia.setId(1L);
                guia.prePersist();
            }
            return guia;
        });
        var response = service.crearGuia(new CrearGuiaRequest(
                "GD-001", "Transportes Norte", LocalDate.parse("2026-07-13"),
                "Cliente", "Direccion", "Carga"
        ));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.estado()).isEqualTo("CREADA");
        assertThat(Files.exists(Path.of(response.rutaEfs()))).isTrue();
        verify(publisher).publicar(any(GuiaDespacho.class), eq(false));
    }

    @Test
    void simulacionDeErrorEstaDeshabilitadaPorDefecto() {
        assertThatThrownBy(() -> service.publicarProcesamiento(1L, true))
                .isInstanceOf(SimulacionErrorDeshabilitadaException.class);
    }

    @Test
    void simulacionHabilitadaPublicaEventoMarcadoParaError() {
        ReflectionTestUtils.setField(service, "errorSimulationEnabled", true);
        GuiaDespacho guia = GuiaDespacho.builder().id(1L).build();
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(guia));

        service.publicarProcesamiento(1L, true);

        verify(publisher).publicar(guia, true);
    }
}
