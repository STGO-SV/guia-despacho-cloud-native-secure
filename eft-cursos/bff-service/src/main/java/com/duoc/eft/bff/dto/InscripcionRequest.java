package com.duoc.eft.bff.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InscripcionRequest(@NotNull @Positive Long cursoId, boolean simularError) { }
