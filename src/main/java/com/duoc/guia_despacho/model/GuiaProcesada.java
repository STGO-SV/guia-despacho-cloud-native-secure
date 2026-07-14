package com.duoc.guia_despacho.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "guia_procesada_rabbit",
        uniqueConstraints = @UniqueConstraint(name = "uk_guia_proc_evento", columnNames = "evento_id"),
        indexes = @Index(name = "ix_guia_proc_guia_id", columnList = "guia_id")
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuiaProcesada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, length = 36)
    private String eventoId;

    @Column(name = "guia_id", nullable = false)
    private Long guiaId;

    @Column(name = "tipo_evento", nullable = false, length = 40)
    private String tipoEvento;

    @Column(name = "numero_guia", nullable = false, length = 80)
    private String numeroGuia;

    @Column(nullable = false, length = 160)
    private String transportista;

    @Column(name = "fecha_guia", nullable = false)
    private LocalDate fechaGuia;

    @Column(nullable = false, length = 160)
    private String destinatario;

    @Column(name = "direccion_destino", nullable = false, length = 300)
    private String direccionDestino;

    @Column(name = "descripcion_carga", nullable = false, length = 1000)
    private String descripcionCarga;

    @Column(nullable = false, length = 40)
    private String estado;

    @Column(name = "s3_key", length = 500)
    private String s3Key;

    @Column(name = "fecha_evento", nullable = false)
    private Instant fechaEvento;

    @Column(name = "fecha_procesamiento", nullable = false)
    private Instant fechaProcesamiento;
}
