# Arquitectura académica EFT de cursos

## Alcance

La adaptación está aislada en `eft-cursos/` y no modifica la aplicación heredada de guías. Es un reactor Maven con tres aplicaciones Spring Boot y RabbitMQ local.

| Componente | Puerto | Responsabilidad |
| --- | ---: | --- |
| `bff-service` | 8080 | Entrada HTTP y propagación de `Authorization`. |
| `cursos-service` | 8081 | CRUD de cursos y preparación académica de claves S3. |
| `inscripciones-service` | 8082 | Inscripciones, comprobantes PDF privados en S3, productor, listener, consumo explícito e idempotencia. |
| RabbitMQ | 5672 / 15672 | Mensajería y Management local. |

Flujo de inscripción: `Cliente -> BFF -> inscripciones-service -> H2 -> PDF -> S3 -> RabbitMQ`.

Flujo asíncrono: `POST inscripción -> publisher Java -> cursos.exchange -> listener Java -> INSCRIPCION_PROCESADA`.

## Ejecución verificada

Desde la raíz del repositorio:

```powershell
.\mvnw.cmd -f eft-cursos\pom.xml clean verify
docker compose -f eft-cursos\docker-compose.yml config
docker compose -f eft-cursos\docker-compose.yml up -d --build
docker compose -f eft-cursos\docker-compose.yml ps
```

El 15-07-2026 se verificaron los cuatro contenedores saludables y los healthchecks públicos con HTTP 200 / `UP` en 8080, 8081 y 8082.

## Seguridad local controlada

Spring Security permanece activo. Sin token, los endpoints de negocio devuelven 401 JSON; Actuator health es público. Compose habilita solo para esta demostración el decodificador académico mediante `APP_SECURITY_DEMO_ENABLED=true`, con tokens locales deterministas de estudiante e instructor. La configuración normal mantiene issuer, audiencia, JWK y claim de roles Azure parametrizados; no se generaron tokens reales.

## Diferencias entre local y EC2

- H2 es en memoria: recrear `inscripciones-service`, paso obligatorio de la evidencia manual, reinicia sus tablas locales.
- Localmente `AWS_S3_UPLOAD_ENABLED=false`: el servicio genera el PDF y la key, pero no llama AWS.
- En EC2 `AWS_S3_UPLOAD_ENABLED=true`: el PDF se almacena en el bucket privado `guia-despacho-ssaezv-dcn` mediante el perfil IAM de la instancia y `DefaultCredentialsProvider`.
- Los datos durables de RabbitMQ usan el volumen nombrado `rabbit-data`, que no se eliminó.
- La aplicación heredada permanece intacta.
