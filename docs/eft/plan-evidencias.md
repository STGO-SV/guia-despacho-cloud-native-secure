# Plan de evidencias EFT

## Evidencias de esta fase

1. Reactor Maven con tres módulos y pruebas exitosas.
2. Tres procesos Spring Boot en puertos 8080, 8081 y 8082.
3. Health HTTP 200 de los tres servicios.
4. Creación y listado de curso por BFF.
5. Creación de inscripción HTTP 201.
6. Exchange, cola principal, consumidor y DLQ en RabbitMQ Management.
7. Registro idempotente del evento procesado.
8. Endpoint productor y endpoint consumidor explícito, con listener desactivado durante esa evidencia.
9. Respuestas 401 y 403.
10. Key académica `cursos/{cursoId}/materiales/{nombreArchivo}` con S3 desactivado.

## Evidencias futuras

- Login desde frontend con PKCE.
- Material real en S3.
- Endpoints registrados en API Manager.
- Pipeline y URL cloud.
- Word oficial y video Kaltura.

## Seguridad de evidencia

Redactar tokens, correos, tenant IDs, account IDs y credenciales. No incluir `.env`, respaldos, archivos PKCE ni sesiones AWS en el ZIP.
