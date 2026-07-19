package com.duoc.eft.inscripciones.exception;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<Map<String, Object>> notFound(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "timestamp", Instant.now(), "status", 404, "message", ex.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", Instant.now(), "status", 400, "message", "JSON de inscripcion invalido"));
    }
    @ExceptionHandler({InscripcionDuplicadaException.class, DataIntegrityViolationException.class})
    ResponseEntity<Map<String, Object>> conflict(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "timestamp", Instant.now(), "status", 409,
                "message", "El estudiante ya está inscrito en este curso"));
    }
    @ExceptionHandler(ComprobanteStorageException.class)
    ResponseEntity<Map<String, Object>> storage(ComprobanteStorageException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "timestamp", Instant.now(), "status", 502,
                "message", "No fue posible almacenar el comprobante de inscripción"));
    }
    @ExceptionHandler(ComprobanteGeneracionException.class)
    ResponseEntity<Map<String, Object>> pdf(ComprobanteGeneracionException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "timestamp", Instant.now(), "status", 500,
                "message", "No fue posible generar el comprobante de inscripción"));
    }
}
