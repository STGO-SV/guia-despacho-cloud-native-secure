# Endpoints de la adaptación EFT

## BFF (`8080`)

| Método | Ruta | Rol académico |
| --- | --- | --- |
| GET | `/api/bff/cursos` | Estudiante o instructor |
| POST | `/api/bff/cursos` | Instructor |
| POST | `/api/bff/inscripciones` | Estudiante |
| GET | `/api/bff/inscripciones/{id}` | Estudiante o instructor |
| POST | `/api/bff/inscripciones/consumir` | Instructor; consumo académico explícito |

El BFF propaga `Authorization` si está presente y usa `CURSOS_SERVICE_URL` e `INSCRIPCIONES_SERVICE_URL`.

## Cursos (`8081`)

| Método | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/cursos` | Crear curso |
| GET | `/api/cursos` | Listar cursos |
| GET | `/api/cursos/{id}` | Obtener curso |
| PUT | `/api/cursos/{id}` | Actualizar curso |
| DELETE | `/api/cursos/{id}` | Eliminar curso |
| POST | `/api/cursos/{id}/material` | Preparar/subir material |

JSON mínimo:

```json
{"titulo":"Cloud Native","descripcion":"Curso EFT","instructor":"Docente","estado":"ACTIVO"}
```

## Inscripciones (`8082`)

| Método | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/inscripciones` | Crear y publicar evento |
| GET | `/api/inscripciones` | Listar |
| GET | `/api/inscripciones/{id}` | Consultar |
| GET | `/api/inscripciones/curso/{cursoId}` | Consultar por curso |
| POST | `/api/inscripciones/consumir-siguiente` | Consumir explícitamente un mensaje |

```json
{"cursoId":1,"estudianteId":"estudiante-001"}
```
