package com.duoc.eft.cursos.service;

import com.duoc.eft.cursos.dto.CursoRequest;
import com.duoc.eft.cursos.dto.CursoResponse;
import com.duoc.eft.cursos.dto.MaterialResponse;
import com.duoc.eft.cursos.exception.RecursoNoEncontradoException;
import com.duoc.eft.cursos.model.Curso;
import com.duoc.eft.cursos.repository.CursoRepository;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class CursoService {
    private final CursoRepository repository;
    private final S3Client s3Client;
    private final String bucket;
    private final boolean uploadEnabled;

    public CursoService(CursoRepository repository, S3Client s3Client,
                        @Value("${app.aws.s3.bucket}") String bucket,
                        @Value("${app.aws.s3.upload-enabled:false}") boolean uploadEnabled) {
        this.repository = repository;
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.uploadEnabled = uploadEnabled;
    }

    @Transactional
    public CursoResponse crear(CursoRequest request) {
        Curso curso = new Curso();
        aplicar(curso, request);
        return CursoResponse.from(repository.save(curso));
    }

    @Transactional(readOnly = true)
    public List<CursoResponse> listar() {
        return repository.findAll().stream().map(CursoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CursoResponse obtener(Long id) { return CursoResponse.from(buscar(id)); }

    @Transactional
    public CursoResponse actualizar(Long id, CursoRequest request) {
        Curso curso = buscar(id);
        aplicar(curso, request);
        return CursoResponse.from(repository.save(curso));
    }

    @Transactional
    public void eliminar(Long id) { repository.delete(buscar(id)); }

    @Transactional
    public MaterialResponse guardarMaterial(Long id, MultipartFile archivo) {
        Curso curso = buscar(id);
        String nombre = sanitizar(archivo.getOriginalFilename());
        String key = "cursos/" + id + "/materiales/" + nombre;
        if (uploadEnabled) {
            try {
                s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                                .contentType(archivo.getContentType()).build(),
                        RequestBody.fromBytes(archivo.getBytes()));
            } catch (Exception ex) {
                throw new IllegalStateException("No fue posible subir el material a S3", ex);
            }
        }
        curso.setMaterialS3Key(key);
        repository.save(curso);
        return new MaterialResponse(id, key, uploadEnabled);
    }

    private Curso buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(id));
    }

    private void aplicar(Curso curso, CursoRequest request) {
        curso.setTitulo(request.titulo());
        curso.setDescripcion(request.descripcion());
        curso.setInstructor(request.instructor());
        curso.setEstado(request.estado());
    }

    private String sanitizar(String original) {
        String valor = original == null || original.isBlank() ? "material.bin" : original;
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");
    }
}

