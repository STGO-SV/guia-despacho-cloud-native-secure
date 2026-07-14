package com.duoc.guia_despacho.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.duoc.guia_despacho.messaging.GuiaEvento;
import com.duoc.guia_despacho.repository.GuiaProcesadaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class GuiaProcesamientoIdempotenciaJpaTests {

    @Autowired
    private GuiaProcesadaRepository repository;

    @Test
    void mismoEventoEntregadoDosVecesCreaUnaSolaFilaRealEnH2() {
        GuiaProcesamientoService service = new GuiaProcesamientoService(repository);
        UUID eventoId = UUID.randomUUID();
        GuiaEvento evento = new GuiaEvento(
                eventoId, 42L, Instant.parse("2026-07-13T14:00:00Z"), "GUIA_CREADA",
                "GD-042", "Transportes Norte", LocalDate.parse("2026-07-13"),
                "Cliente", "Direccion", "Carga", "CREADA", null, 0, false
        );

        service.procesar(evento);
        repository.flush();
        service.procesar(evento);
        repository.flush();

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findByEventoId(eventoId.toString())).isPresent();
    }
}
