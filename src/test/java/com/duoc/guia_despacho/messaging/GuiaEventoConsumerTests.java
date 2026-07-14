package com.duoc.guia_despacho.messaging;

import static org.mockito.Mockito.verify;

import com.duoc.guia_despacho.service.GuiaProcesamientoService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuiaEventoConsumerTests {

    @Mock
    private GuiaProcesamientoService service;

    @InjectMocks
    private GuiaEventoConsumer consumer;

    @Test
    void delegaElMensajeAlServicioTransaccional() {
        GuiaEvento evento = new GuiaEvento(
                UUID.randomUUID(), 1L, Instant.now(), "GUIA_CREADA", "GD-001", "Transportista",
                LocalDate.now(), "Cliente", "Direccion", "Carga", "CREADA", null, 0, false
        );

        consumer.consumir(evento);

        verify(service).procesar(evento);
    }
}
