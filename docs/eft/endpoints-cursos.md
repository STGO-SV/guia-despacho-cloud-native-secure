# Endpoints de la adaptación EFT

## BFF (`8080`)

| Método | Ruta | Rol académico |
| --- | --- | --- |
| GET | `/api/bff/cursos` | Estudiante o instructor |
| POST | `/api/bff/cursos` | Instructor |
| POST | `/api/bff/inscripciones` | Estudiante |
| GET | `/api/bff/inscripciones/{id}` | Estudiante o instructor |
| POST | `/api/bff/inscripciones/consumir` | Instructor; consumo explícito |

## Cursos (`8081`)

| Método | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/cursos` | Crear curso |
| GET | `/api/cursos` | Listar cursos |
| GET | `/api/cursos/{id}` | Obtener curso |
| PUT | `/api/cursos/{id}` | Actualizar curso |
| DELETE | `/api/cursos/{id}` | Eliminar curso |
| POST | `/api/cursos/{id}/material` | Preparar/subir material; localmente no sube a S3 |

```json
{"titulo":"Cloud Native","descripcion":"Curso EFT","instructor":"Docente","estado":"ACTIVO"}
```

## Inscripciones (`8082`)

| Método | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/inscripciones` | Crear y publicar evento; acepta `simularError` opcional |
| GET | `/api/inscripciones` | Listar |
| GET | `/api/inscripciones/{id}` | Consultar |
| GET | `/api/inscripciones/curso/{cursoId}` | Consultar por curso |
| POST | `/api/inscripciones/consumir-siguiente` | Consumir explícitamente un mensaje |
| POST | `/api/inscripciones/republicar-evento` | Republicar el mismo evento para evidencia de idempotencia |
| GET | `/api/inscripciones/procesadas/{eventoId}` | Consultar cantidad persistida e idempotencia |

Flujo normal:

```json
{"cursoId":2}
```

`estudianteId` no forma parte de la petición pública. El servicio lo obtiene exclusivamente
del claim inmutable `sub` del access token ya validado y lo conserva internamente en la
entidad y en el evento RabbitMQ.

La combinación interna `estudianteId` (`sub`) + `cursoId` es única. Un segundo intento del
mismo usuario sobre el mismo curso devuelve:

```json
{"status":409,"message":"El estudiante ya está inscrito en este curso"}
```

Fallo académico controlado:

```json
{"cursoId":2,"simularError":true}
```
