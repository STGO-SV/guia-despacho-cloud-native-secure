package com.duoc.eft.inscripciones.service;

import com.duoc.eft.inscripciones.exception.ComprobanteGeneracionException;
import com.duoc.eft.inscripciones.model.Inscripcion;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class ComprobantePdfGenerator {
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    public byte[] generar(Inscripcion inscripcion, Instant fechaHora) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            document.add(new Paragraph("Comprobante de inscripción", title));
            document.add(new Paragraph("ID de inscripción: " + inscripcion.getId()));
            document.add(new Paragraph("ID de curso: " + inscripcion.getCursoId()));
            document.add(new Paragraph("Estudiante: " + inscripcion.getEstudianteId()));
            document.add(new Paragraph("Fecha y hora (UTC): " + FECHA_HORA.format(fechaHora)));
            document.add(new Paragraph("Inscripción realizada exitosamente."));
            document.close();
            return output.toByteArray();
        } catch (DocumentException | java.io.IOException ex) {
            throw new ComprobanteGeneracionException(ex);
        }
    }
}
