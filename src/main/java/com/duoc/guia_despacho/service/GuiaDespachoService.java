package com.duoc.guia_despacho.service;

import com.duoc.guia_despacho.dto.ActualizarGuiaRequest;
import com.duoc.guia_despacho.dto.CrearGuiaRequest;
import com.duoc.guia_despacho.dto.GuiaDespachoResponse;
import com.duoc.guia_despacho.exception.GuiaNoEncontradaException;
import com.duoc.guia_despacho.exception.OperacionGuiaException;
import com.duoc.guia_despacho.exception.PermisoDenegadoException;
import com.duoc.guia_despacho.exception.SimulacionErrorDeshabilitadaException;
import com.duoc.guia_despacho.messaging.GuiaEventoPublisher;
import com.duoc.guia_despacho.model.GuiaDespacho;
import com.duoc.guia_despacho.repository.GuiaDespachoRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
public class GuiaDespachoService {

    private static final String ESTADO_CREADA = "CREADA";
    private static final String CONTENT_TYPE_PDF = "application/pdf";

    private final GuiaDespachoRepository guiaRepository;
    private final S3Client s3Client;
    private final GuiaEventoPublisher eventoPublisher;

    @Value("${app.storage.efs-path}")
    private String efsPath;

    @Value("${app.aws.s3.bucket}")
    private String bucketName;

    @Value("${app.aws.s3.auto-upload}")
    private boolean autoUploadS3;

    @Value("${app.rabbit.error-simulation-enabled}")
    private boolean errorSimulationEnabled;

    @Transactional
    public GuiaDespachoResponse crearGuia(CrearGuiaRequest request) {
        GuiaDespacho guia = GuiaDespacho.builder()
                .numeroGuia(request.numeroGuia())
                .transportista(request.transportista())
                .fecha(request.fecha())
                .destinatario(request.destinatario())
                .direccionDestino(request.direccionDestino())
                .descripcionCarga(request.descripcionCarga())
                .estado(ESTADO_CREADA)
                .build();

        guia = guiaRepository.save(guia);
        generarPdf(guia);
        if (autoUploadS3) {
            subirArchivoAS3(guia);
        }
        guia = guiaRepository.save(guia);
        eventoPublisher.publicar(guia, false);
        return GuiaDespachoResponse.fromEntity(guia);
    }

    @Transactional
    public GuiaDespachoResponse subirGuiaAS3(Long id) {
        GuiaDespacho guia = buscarGuia(id);
        subirArchivoAS3(guia);
        return GuiaDespachoResponse.fromEntity(guiaRepository.save(guia));
    }

    @Transactional(readOnly = true)
    public byte[] descargarGuia(Long id, String transportista) {
        GuiaDespacho guia = buscarGuia(id);
        validarTransportista(guia, transportista);

        Path archivoLocal = guia.getRutaEfs() == null || guia.getRutaEfs().isBlank()
                ? null
                : Path.of(guia.getRutaEfs());
        if (archivoLocal != null && Files.exists(archivoLocal)) {
            try {
                return Files.readAllBytes(archivoLocal);
            } catch (IOException ex) {
                throw new OperacionGuiaException("No fue posible leer el PDF temporal", ex);
            }
        }

        if (guia.getS3Key() != null && !guia.getS3Key().isBlank()) {
            try {
                GetObjectRequest request = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(guia.getS3Key())
                        .build();
                ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
                return response.asByteArray();
            } catch (S3Exception ex) {
                throw new OperacionGuiaException("No fue posible descargar el PDF desde S3", ex);
            }
        }

        throw new OperacionGuiaException("No existe archivo local ni objeto S3 para la guia", null);
    }

    @Transactional
    public GuiaDespachoResponse actualizarGuia(Long id, ActualizarGuiaRequest request) {
        GuiaDespacho guia = buscarGuia(id);
        guia.setNumeroGuia(request.numeroGuia());
        guia.setTransportista(request.transportista());
        guia.setFecha(request.fecha());
        guia.setDestinatario(request.destinatario());
        guia.setDireccionDestino(request.direccionDestino());
        guia.setDescripcionCarga(request.descripcionCarga());
        guia.setEstado(request.estado());

        generarPdf(guia);
        if (guia.getS3Key() != null && !guia.getS3Key().isBlank()) {
            subirArchivoAS3(guia);
        }

        return GuiaDespachoResponse.fromEntity(guiaRepository.save(guia));
    }

    @Transactional
    public void eliminarGuia(Long id) {
        GuiaDespacho guia = buscarGuia(id);
        eliminarArchivoLocal(guia);
        eliminarArchivoS3(guia);
        guiaRepository.delete(guia);
    }

    @Transactional(readOnly = true)
    public List<GuiaDespachoResponse> buscarPorTransportistaYFecha(String transportista, LocalDate fecha) {
        return guiaRepository.findByTransportistaIgnoreCaseAndFecha(transportista, fecha)
                .stream()
                .map(GuiaDespachoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public GuiaDespachoResponse obtenerGuia(Long id) {
        return GuiaDespachoResponse.fromEntity(buscarGuia(id));
    }

    @Transactional(readOnly = true)
    public UUID publicarProcesamiento(Long id, boolean simularError) {
        if (simularError && !errorSimulationEnabled) {
            throw new SimulacionErrorDeshabilitadaException();
        }
        return eventoPublisher.publicar(buscarGuia(id), simularError);
    }

    private GuiaDespacho buscarGuia(Long id) {
        return guiaRepository.findById(id)
                .orElseThrow(() -> new GuiaNoEncontradaException(id));
    }

    private void generarPdf(GuiaDespacho guia) {
        try {
            Files.createDirectories(Path.of(efsPath));
            String nombreArchivo = "guia-" + guia.getId() + ".pdf";
            Path rutaArchivo = Path.of(efsPath, nombreArchivo);

            try (OutputStream outputStream = Files.newOutputStream(rutaArchivo)) {
                Document document = new Document();
                PdfWriter.getInstance(document, outputStream);
                document.open();
                document.add(new Paragraph("Guia de Despacho"));
                document.add(new Paragraph("Numero de guia: " + guia.getNumeroGuia()));
                document.add(new Paragraph("Transportista: " + guia.getTransportista()));
                document.add(new Paragraph("Fecha: " + guia.getFecha()));
                document.add(new Paragraph("Destinatario: " + guia.getDestinatario()));
                document.add(new Paragraph("Direccion destino: " + guia.getDireccionDestino()));
                document.add(new Paragraph("Descripcion carga: " + guia.getDescripcionCarga()));
                document.add(new Paragraph("Estado: " + guia.getEstado()));
                document.close();
            }

            guia.setNombreArchivo(nombreArchivo);
            guia.setRutaEfs(rutaArchivo.toString());
        } catch (IOException | DocumentException ex) {
            throw new OperacionGuiaException("No fue posible generar el PDF de la guia", ex);
        }
    }

    private void subirArchivoAS3(GuiaDespacho guia) {
        try {
            Path archivo = Path.of(guia.getRutaEfs());
            if (!Files.exists(archivo)) {
                generarPdf(guia);
            }

            String s3Key = construirS3Key(guia);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(CONTENT_TYPE_PDF)
                    .build();

            s3Client.putObject(request, RequestBody.fromFile(archivo));
            guia.setS3Key(s3Key);
        } catch (S3Exception ex) {
            throw new OperacionGuiaException("No fue posible subir el PDF a S3", ex);
        }
    }

    private void eliminarArchivoLocal(GuiaDespacho guia) {
        if (guia.getRutaEfs() == null || guia.getRutaEfs().isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(Path.of(guia.getRutaEfs()));
        } catch (IOException ex) {
            throw new OperacionGuiaException("No fue posible eliminar el PDF temporal", ex);
        }
    }

    private void eliminarArchivoS3(GuiaDespacho guia) {
        if (guia.getS3Key() == null || guia.getS3Key().isBlank()) {
            return;
        }

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(guia.getS3Key())
                    .build();
            s3Client.deleteObject(request);
        } catch (S3Exception ex) {
            throw new OperacionGuiaException("No fue posible eliminar el PDF desde S3", ex);
        }
    }

    private void validarTransportista(GuiaDespacho guia, String transportistaSolicitante) {
        if (transportistaSolicitante == null
                || !guia.getTransportista().equalsIgnoreCase(transportistaSolicitante.trim())) {
            throw new PermisoDenegadoException();
        }
    }

    private String construirS3Key(GuiaDespacho guia) {
        String fecha = guia.getFecha().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String transportista = guia.getTransportista()
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-|-$", "");
        return "guias/" + transportista + "/" + fecha + "/guia-" + guia.getId() + ".pdf";
    }
}
