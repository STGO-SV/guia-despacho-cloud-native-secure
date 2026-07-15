package com.duoc.eft.cursos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CursoRequest(
        @NotBlank @Size(max = 160) String titulo,
        @NotBlank @Size(max = 1000) String descripcion,
        @NotBlank @Size(max = 160) String instructor,
        @NotBlank @Size(max = 40) String estado
) { }

