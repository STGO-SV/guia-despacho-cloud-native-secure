package com.duoc.guia_despacho.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.duoc.guia_despacho.messaging.GuiaEvento;
import com.duoc.guia_despacho.model.GuiaProcesada;
import com.duoc.guia_despacho.repository.GuiaProcesadaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuiaProcesamientoServiceTests {

    @Mock
    private GuiaProcesadaRepository repository;

    private GuiaProcesamientoService service;

    @BeforeEach
    void setUp() {
        service = new GuiaProcesamientoService(repository);
    }

    @Test
    void persisteEventoNuevoEnTablaSeparada() {
        GuiaEvento evento = evento(false);
        when(repository.existsByEventoId(evento.eventoId().toString())).thenReturn(false);

        service.procesar(evento);

        verify(repository).save(any(GuiaProcesada.class));
    }

    @Test
    void ignoraEventoDuplicadoParaMantenerIdempotencia() {
        GuiaEvento evento = evento(false);
        when(repository.existsByEventoId(evento.eventoId().toString())).thenReturn(true);

        service.procesar(evento);

        verify(repository, never()).save(any());
    }

    @Test
    void errorControladoSePropagaParaQueElListenerReintenteYRechace() {
        assertThatThrownBy(() -> service.procesar(evento(true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fallo controlado");

        verify(repository, never()).save(any());
    }

    private GuiaEvento evento(boolean simularError) {
        return new GuiaEvento(
                UUID.randomUUID(), 1L, Instant.parse("2026-07-13T12:00:00Z"), "GUIA_CREADA",
                "GD-001", "Transportes Norte", LocalDate.parse("2026-07-13"), "Cliente",
                "Direccion 123", "Carga", "CREADA", null, 0, simularError
        );
    }
}
