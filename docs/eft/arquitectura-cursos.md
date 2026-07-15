# Arquitectura académica EFT de cursos

## Estrategia

La adaptación vive en `eft-cursos/` y no modifica la aplicación existente de guías. Es un proyecto Maven multimódulo con tres aplicaciones Spring Boot independientes.

| Servicio | Puerto | Responsabilidad |
| --- | ---: | --- |
| `bff-service` | 8080 | Entrada única y orquestación HTTP hacia los servicios internos. |
| `cursos-service` | 8081 | CRUD de cursos y preparación de materiales S3. |
| `inscripciones-service` | 8082 | Inscripciones, publicación y consumo RabbitMQ. |
| RabbitMQ | 5672 / 15672 | Mensajería y consola académica. |

Flujo principal:

`Cliente -> BFF -> cursos-service / inscripciones-service -> H2`

Flujo asíncrono:

`BFF -> POST inscripciones -> publisher Java -> RabbitMQ -> listener Java -> INSCRIPCION_PROCESADA`

## Ejecución local

Desde `eft-cursos/`:

```powershell
..\mvnw.cmd clean verify
docker compose config
docker compose up -d --build
```

Health checks:

- `http://localhost:8080/actuator/health`
- `http://localhost:8081/actuator/health`
- `http://localhost:8082/actuator/health`

## Limitaciones de esta fase

- No existe frontend.
- No existe API Manager.
- No hay despliegue cloud.
- Se usa H2 local, no Oracle real.
- No se crean usuarios ni roles nuevos en Azure.
- La aplicación original de guías permanece intacta.

