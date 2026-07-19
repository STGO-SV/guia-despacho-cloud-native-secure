package com.duoc.eft.inscripciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.duoc.eft.inscripciones.exception.ComprobanteStorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class S3ComprobanteStorageTests {
    @Mock S3Client s3Client;

    @Test
    void subePdfUnaVezConBucketKeyYContentType() {
        S3ComprobanteStorage storage = new S3ComprobanteStorage(
                s3Client, true, "guia-despacho-ssaezv-dcn");

        assertThat(storage.guardar(
                "2026/inscripciones/7/comprobante-inscripcion.pdf", "pdf".getBytes())).isTrue();

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("guia-despacho-ssaezv-dcn");
        assertThat(request.getValue().key())
                .isEqualTo("2026/inscripciones/7/comprobante-inscripcion.pdf");
        assertThat(request.getValue().contentType()).isEqualTo("application/pdf");
    }

    @Test
    void modoDeshabilitadoNoInvocaAws() {
        S3ComprobanteStorage storage = new S3ComprobanteStorage(s3Client, false, "");

        assertThat(storage.guardar("key", "pdf".getBytes())).isFalse();

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void falloSdkSeConvierteEnExcepcionControlada() {
        S3ComprobanteStorage storage = new S3ComprobanteStorage(
                s3Client, true, "guia-despacho-ssaezv-dcn");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("fallo AWS").build());

        assertThatThrownBy(() -> storage.guardar("key", "pdf".getBytes()))
                .isInstanceOf(ComprobanteStorageException.class)
                .hasMessageContaining("S3");
    }
}
