package com.duoc.eft.inscripciones.repository;

import com.duoc.eft.inscripciones.model.InscripcionProcesada;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InscripcionProcesadaRepository extends JpaRepository<InscripcionProcesada, Long> {
    boolean existsByEventoId(String eventoId);
}

