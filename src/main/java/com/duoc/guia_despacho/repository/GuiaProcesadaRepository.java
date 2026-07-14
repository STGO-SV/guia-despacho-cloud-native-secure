package com.duoc.guia_despacho.repository;

import com.duoc.guia_despacho.model.GuiaProcesada;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuiaProcesadaRepository extends JpaRepository<GuiaProcesada, Long> {

    boolean existsByEventoId(String eventoId);

    Optional<GuiaProcesada> findByEventoId(String eventoId);
}
