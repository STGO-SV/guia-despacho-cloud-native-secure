package com.duoc.eft.inscripciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InscripcionRequest(@NotNull @Positive Long cursoId, @NotBlank String estudianteId,
                                 Boolean simularError) { }
