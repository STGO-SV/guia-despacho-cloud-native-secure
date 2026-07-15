package com.duoc.eft.inscripciones.messaging;

import java.time.Instant;
import java.util.UUID;

public record InscripcionCreadaEvento(UUID eventoId, Long inscripcionId, Long cursoId,
                                      String estudianteId, Instant fecha, boolean simularError) { }
