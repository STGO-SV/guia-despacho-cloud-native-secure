package com.duoc.eft.inscripciones.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.duoc.eft.inscripciones.model.Inscripcion;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ComprobantePdfGeneratorTests {
    @Test
    void generaPdfConDatosObligatorios() throws Exception {
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(15L);
        inscripcion.setCursoId(4L);
        inscripcion.setEstudianteId("sub-estudiante-15");

        byte[] pdf = new ComprobantePdfGenerator().generar(
                inscripcion, Instant.parse("2026-07-18T12:30:00Z"));

        assertThat(pdf).startsWith("%PDF-".getBytes());
        PdfReader reader = new PdfReader(pdf);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();
        assertThat(text).contains(
                "Comprobante de inscripción",
                "ID de inscripción: 15",
                "ID de curso: 4",
                "sub-estudiante-15",
                "2026-07-18T12:30:00Z",
                "Inscripción realizada exitosamente");
    }
}
