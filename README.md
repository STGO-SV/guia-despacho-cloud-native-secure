# Sistema de guías de despacho Cloud Native

API Java 17 / Spring Boot 3.5.14 que crea guías, genera PDF, integra AWS S3, valida JWT de Azure AD B2C y procesa eventos de forma asíncrona con RabbitMQ. El consumidor persiste cada evento en la tabla nueva `GUIA_PROCESADA_RABBIT`, compatible con Oracle Cloud; H2 en modo Oracle se usa solo para desarrollo.

## Arquitectura

`Cliente -> API Gateway -> Spring Security/JWT -> API de guías -> S3` y `API -> guia.exchange -> guia.procesamiento.queue -> consumidor -> Oracle`. Después de 3 intentos fallidos, RabbitMQ rechaza el mensaje y el DLX lo enruta a `guia.error.queue`. El `eventoId` es único en base de datos y evita duplicados durante reintentos.

Detalles y diagrama: [docs/arquitectura.md](docs/arquitectura.md).

## Requisitos

- JDK 17 (no se requiere Maven global; se incluye Maven Wrapper).
- RabbitMQ 4.x o Docker con Compose v2.
- Para nube: tenant Azure AD B2C, bucket S3, rol IAM de EC2 y Oracle Cloud accesible.

## Variables de entorno

Copiar `.env.example` como `.env` y reemplazar solo los marcadores ficticios. Nunca versionar `.env`.

| Variable | Uso |
| --- | --- |
| `AZURE_ISSUER_URI`, `AZURE_JWK_SET_URI`, `AZURE_CLIENT_ID` | Issuer, JWKS y audience JWT obligatorios. |
| `AZURE_ROLES_CLAIM` | Claim de roles; por defecto `extension_RolGuia`. |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER` | Conexión H2 u Oracle. |
| `SPRING_PROFILES_ACTIVE` | `default` local o `oracle` en Oracle Cloud. |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | Broker. |
| `RABBITMQ_RETRY_MAX_ATTEMPTS` | Intentos totales; por defecto `3`. |
| `RABBITMQ_ERROR_SIMULATION_ENABLED` | Habilita `simularError=true`; `false` por defecto y solo para una demo controlada. |
| `AWS_REGION`, `AWS_S3_BUCKET` | Región y bucket existente. |
| `AWS_S3_AUTO_UPLOAD` | `true` en nube para subir al crear; `false` local evita depender de AWS. |
| `APP_EFS_PATH` | Directorio temporal o montaje EFS. |
| `CORS_ALLOWED_ORIGINS` | Orígenes separados por coma, sin comodín con credenciales. |

El SDK AWS usa la cadena estándar de credenciales. En EC2 se recomienda un rol IAM, no access keys en archivos o GitHub Actions.

## Ejecución local

Con RabbitMQ disponible y las variables Azure definidas:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Health check público y mínimo:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Todos los endpoints `/api/**` requieren `Authorization: Bearer <access_token>`.

## Docker

```bash
cp .env.example .env
# editar .env: Azure, RabbitMQ y bucket
docker-compose up -d --build
docker-compose ps
docker-compose logs -f app rabbitmq
docker-compose down
```

- API: `http://localhost:8080`
- RabbitMQ Management: `http://localhost:15672`
- Datos RabbitMQ, H2 local y PDF: volúmenes Docker separados.
- La aplicación espera el health check del broker y el listener reintenta conexiones posteriores.

`docker-compose down -v` elimina datos locales y no debe usarse si se quieren conservar evidencias.

## Endpoints y roles

| Método | Ruta | Rol | Respuesta principal |
| --- | --- | --- | --- |
| `POST` | `/api/guias` | `GESTION_GUIAS` | `201`; crea PDF, opcionalmente S3 y publica evento. |
| `GET` | `/api/guias/{id}` | `GESTION_GUIAS` | `200`; metadata. |
| `POST` | `/api/guias/{id}/subir-s3` | `GESTION_GUIAS` | `200`; subida manual/repetible. |
| `GET` | `/api/guias/{id}/descargar?transportista=...` | `DESCARGA_GUIAS` | `200 application/pdf`. |
| `PUT` | `/api/guias/{id}` | `GESTION_GUIAS` | `200`; actualiza y regenera PDF. |
| `DELETE` | `/api/guias/{id}` | `GESTION_GUIAS` | `204`. |
| `GET` | `/api/guias?transportista=...&fecha=YYYY-MM-DD` | `GESTION_GUIAS` | `200`; lista filtrada. |
| `POST` | `/api/procesamiento/guias/{id}?simularError=false` | `GESTION_GUIAS` | `202`; publica sin bloquear el consumo. |
| `GET` | `/api/procesamiento/colas` | `GESTION_GUIAS` | `200`; mensajes/consumidores de ambas colas. |
| `GET` | `/actuator/health` | Público | Estado técnico sin datos de negocio. |

El rol de descarga no puede crear, consultar, modificar, eliminar, subir ni publicar. El rol de gestión no descarga; un usuario que necesite ambos comportamientos debe recibir ambos roles.

Especificación para importar en API Gateway/Postman: [docs/openapi.yaml](docs/openapi.yaml).

## Flujo RabbitMQ demostrable

1. Crear una guía o invocar `POST /api/procesamiento/guias/{id}`.
2. Observar `guia.procesamiento.queue` en Management; el listener la consume automáticamente.
3. Verificar `SELECT * FROM GUIA_PROCESADA_RABBIT ORDER BY FECHA_PROCESAMIENTO DESC`.
4. Solo en la demo, definir `RABBITMQ_ERROR_SIMULATION_ENABLED=true`, reiniciar e invocar el mismo endpoint con `?simularError=true`.
5. Esperar 3 intentos (backoff 1 s, 2 s) y comprobar un mensaje en `guia.error.queue`.

Con la variable deshabilitada, `simularError=true` responde `403` y no publica ni modifica datos. Los mensajes son JSON persistentes con `eventoId`, `guiaId`, timestamp, tipo, datos de guía y metadata. La cola principal declara `x-dead-letter-exchange=guia.exchange` y `x-dead-letter-routing-key=guia.error`; no hay ciclos de reintento.

## Oracle Cloud

1. Ejecutar [docs/sql/oracle-guia-procesada-rabbit.sql](docs/sql/oracle-guia-procesada-rabbit.sql) en el esquema nuevo/existente.
2. Definir `SPRING_PROFILES_ACTIVE=oracle`, `DB_URL=jdbc:oracle:thin:@...`, `DB_DRIVER=oracle.jdbc.OracleDriver` y credenciales fuera del repositorio.
3. El perfil Oracle usa `ddl-auto=validate`: falla temprano si la tabla no coincide.

Oracle Cloud no puede probarse sin red y credenciales externas. La ejecución local usa H2 en modo Oracle.

## S3

Los objetos quedan en `guias/{transportista-normalizado}/{yyyy}/{MM}/guia-{id}.pdf` con `Content-Type: application/pdf`. `AWS_S3_AUTO_UPLOAD=true` habilita subida automática al crear; el endpoint manual se conserva. La descarga usa primero el montaje local y luego S3. El bucket se crea fuera de la aplicación.

Validación real del 2026-07-13: con credenciales temporales de AWS Academy inyectadas solo al proceso y al contenedor, `POST /api/guias` respondió `201` y la aplicación subió automáticamente `guia-2.pdf` al bucket privado `duoc-guia-despacho-d624cd925633` en `us-east-1`. `head-object` confirmó `application/pdf`, 979 bytes y cifrado `AES256`; la descarga directa desde S3 comenzó con `%PDF-` y tuvo SHA-256 `F392F3E4C1645BA90C7ADF11C4DE761322D22CCD3C9E8BB90463A1ECB8553853E`. El objeto se conserva como evidencia. El endpoint protegido de descarga devolvió el mismo PDF con rol `DESCARGA_GUIAS`; no se eliminó el archivo local para forzar el fallback S3.

## Pruebas y documentación

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
```

- Arquitectura: [docs/arquitectura.md](docs/arquitectura.md)
- Configuración cloud/API Gateway/EC2: [docs/configuracion-cloud.md](docs/configuracion-cloud.md)
- Azure AD B2C: [docs/azure-ad-b2c-configuracion.md](docs/azure-ad-b2c-configuracion.md)
- Postman y casos 401/403/Rabbit/S3: [docs/pruebas-postman.md](docs/pruebas-postman.md)
- Checklist: [docs/checklist-rubrica.md](docs/checklist-rubrica.md)
- Video: [docs/guion-video.md](docs/guion-video.md)
- Evidencias locales verificadas: [docs/evidencias-locales.md](docs/evidencias-locales.md)

## CI/CD y despliegue

El workflow compila/prueba, publica la imagen y la reinicia en EC2. EC2 debe tener `/opt/guia-despacho/app.env`, EFS montado en `/mnt/efs/guias`, acceso al broker/Oracle y un rol IAM mínimo para S3. Las URLs se cambian mediante `baseUrl`, DNS/API Gateway y variables; no hay URL EC2 rígida en Java.
