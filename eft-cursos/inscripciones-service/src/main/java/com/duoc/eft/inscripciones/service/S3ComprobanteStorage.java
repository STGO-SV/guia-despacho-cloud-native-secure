package com.duoc.eft.inscripciones.service;

import com.duoc.eft.inscripciones.exception.ComprobanteStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3ComprobanteStorage {
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private final S3Client s3Client;
    private final boolean uploadEnabled;
    private final String bucket;

    public S3ComprobanteStorage(S3Client s3Client,
            @Value("${app.aws.s3.upload-enabled:false}") boolean uploadEnabled,
            @Value("${app.aws.s3.bucket:}") String bucket) {
        this.s3Client = s3Client;
        this.uploadEnabled = uploadEnabled;
        this.bucket = bucket;
    }

    public boolean guardar(String key, byte[] pdf) {
        if (!uploadEnabled) {
            return false;
        }
        if (bucket == null || bucket.isBlank()) {
            throw new ComprobanteStorageException(
                    "AWS_S3_BUCKET es obligatorio cuando AWS_S3_UPLOAD_ENABLED=true");
        }
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(PDF_CONTENT_TYPE)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(pdf));
            return true;
        } catch (SdkException ex) {
            throw new ComprobanteStorageException(
                    "No fue posible almacenar el comprobante de inscripción en S3", ex);
        }
    }
}
