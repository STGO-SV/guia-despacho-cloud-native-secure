package com.duoc.eft.inscripciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class InscripcionProcesada {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 36)
    private String eventoId;
    private Long inscripcionId;
    private Instant fechaProcesamiento;

    public Long getId() { return id; }
    public String getEventoId() { return eventoId; }
    public void setEventoId(String eventoId) { this.eventoId = eventoId; }
    public Long getInscripcionId() { return inscripcionId; }
    public void setInscripcionId(Long inscripcionId) { this.inscripcionId = inscripcionId; }
    public Instant getFechaProcesamiento() { return fechaProcesamiento; }
    public void setFechaProcesamiento(Instant fechaProcesamiento) { this.fechaProcesamiento = fechaProcesamiento; }
}

