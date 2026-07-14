# Configuración cloud y evidencias

## Azure AD B2C

La configuración detallada está en `azure-ad-b2c-configuracion.md`.

Preflight local B2C: obtener, sin secretos, nombre y GUID del tenant, dominio B2C, policy real, client ID de la API, client ID del cliente público Postman, scope completo y redirect URI registrada. Al `2026-07-13` estos datos no están disponibles en el repositorio ni en variables locales; los valores activos del `.env` son ficticios. No construir URLs ni modificar Azure hasta confirmar los siete datos.

1. Registrar API y exponer scope `access_as_user`.
2. Registrar cliente Postman con redirect URI autorizada y permisos al scope.
3. Crear/usar atributo personalizado `RolGuia` y emitirlo como `extension_RolGuia` en el access token.
4. Preparar usuarios `GESTION_GUIAS` y `DESCARGA_GUIAS`.
5. Configurar issuer exacto, JWKS y client ID de la API mediante variables.
6. Verificar 401, 403 y ambos caminos autorizados. No usar ID token para la API.

Capturas: registros de aplicaciones sin secretos, user flow/claim, payload redactado de cada rol, respuesta 401 y respuesta 403.

### Estado verificado el 2026-07-13

- Discovery OIDC, issuer y JWKS reales verificados; JWKS responde `200` con claves `kid`.
- Propiedad Graph confirmada: `extension_d01da54653ee4bd18e924fc1f8efbab8_RolGuia` (`String`, target `User`).
- Usuario objetivo enmascarado `7ba…1c4` actualizado con autorización explícita: PATCH `204`, GET posterior `GESTION_GUIAS`.
- PKCE S256 real verificado; el access token contiene `extension_RolGuia=GESTION_GUIAS` y fue aceptado por Spring en endpoints de gestión.
- Segunda cuenta local B2C verificada: `DESCARGA_GUIAS` asignado mediante un único PATCH Graph HTTP `204`; cuenta institucional conservó `GESTION_GUIAS`.
- Token PKCE real `DESCARGA_GUIAS` validado. Descarga PDF HTTP `200`; procesamiento, creación y consulta de colas HTTP `403`, sin `401` inesperados ni efectos Rabbit/JPA.
- Pendiente solamente capturar estas pantallas para la entrega.

No se guardaron tokens ni contraseñas en el repositorio. La sesión Azure CLI se mantiene abierta y no se realizaron otros cambios en Azure.

## AWS S3

Estado real del 2026-07-13: se creó exclusivamente el bucket privado `duoc-guia-despacho-d624cd925633` en `us-east-1`. `head-bucket` fue exitoso, Block Public Access conserva las cuatro opciones activas, no existe bucket policy y la ACL no concede acceso público. Con credenciales temporales de Learner Lab mantenidas fuera de archivos versionados, la aplicación realizó una subida automática real y se verificaron metadata, `%PDF-` y SHA-256. No se modificaron IAM, ACL, policy, versionado, logging, hosting ni replication.

1. Crear bucket privado en la misma región.
2. Bloquear acceso público y habilitar cifrado administrado.
3. Asociar a EC2 un rol IAM limitado a `s3:GetObject`, `s3:PutObject` y `s3:DeleteObject` sobre `arn:aws:s3:::BUCKET/guias/*`.
4. Definir `AWS_REGION`, `AWS_S3_BUCKET` y `AWS_S3_AUTO_UPLOAD=true` en `/opt/guia-despacho/app.env`.
5. Probar creación y comprobar key `guias/{transportista}/{yyyy}/{MM}/guia-{id}.pdf` y `application/pdf`.

Capturas: política/rol sin credenciales, objeto con key completa y metadata, respuesta de subida y descarga.

## EC2

1. Instalar Docker, montar EFS en `/mnt/efs/guias` y limitar security groups.
2. Crear `/opt/guia-despacho/app.env` con permisos `600`; incluir Azure, DB, RabbitMQ, S3 y CORS.
3. Dar al usuario de despliegue acceso controlado a Docker y al directorio.
4. Mantener RabbitMQ/Oracle en red privada; publicar 8080 solo hacia API Gateway/ALB cuando sea viable.
5. Asociar rol IAM; no colocar access keys en el env file.
6. Ejecutar imagen con `--restart unless-stopped`, health check y montaje EFS. El workflow realiza el pull/reinicio.

Capturas: instancia y SG, rol IAM asociado, montaje EFS, `docker ps`, health `UP` y logs sin secretos.

## Oracle Cloud

1. Crear usuario/esquema o usar uno autorizado sin reutilizar tablas previas.
2. Ejecutar `docs/sql/oracle-guia-procesada-rabbit.sql`.
3. Permitir red desde EC2 y obtener JDBC URL TLS/Wallet si el servicio lo exige.
4. Configurar:

```env
SPRING_PROFILES_ACTIVE=oracle
DB_URL=jdbc:oracle:thin:@<servicio>
DB_DRIVER=oracle.jdbc.OracleDriver
DB_USERNAME=<usuario>
DB_PASSWORD=<secreto>
```

5. Reiniciar; `ddl-auto=validate` debe aprobar el esquema.
6. Ejecutar `SELECT EVENTO_ID, GUIA_ID, FECHA_PROCESAMIENTO FROM GUIA_PROCESADA_RABBIT ORDER BY FECHA_PROCESAMIENTO DESC;`.

Capturas: tabla/constraints, fila exitosa y ausencia de duplicado. No mostrar password o Wallet.

## API Gateway

Importar `docs/openapi.yaml` o crear estos métodos exactamente:

| Método | Ruta | Rol backend |
| --- | --- | --- |
| POST | `/api/guias` | Gestión |
| GET | `/api/guias/{id}` | Gestión |
| POST | `/api/guias/{id}/subir-s3` | Gestión |
| GET | `/api/guias/{id}/descargar` | Descarga |
| PUT | `/api/guias/{id}` | Gestión |
| DELETE | `/api/guias/{id}` | Gestión |
| GET | `/api/guias` | Gestión |
| POST | `/api/procesamiento/guias/{id}` | Gestión |
| GET | `/api/procesamiento/colas` | Gestión |

Configuración recomendada:

1. API HTTP con integración proxy `http://${EC2_HOST}:8080` o, preferiblemente, ALB privado/VPC Link.
2. Crear authorizer JWT con issuer y audience de la API. Aunque Gateway valide JWT, el backend vuelve a validarlo y aplica roles.
3. Reenviar header `Authorization`, query strings y `Content-Type`; permitir binario `application/pdf` si se usa REST API.
4. CORS: `Authorization,Content-Type,Accept,X-Correlation-ID`; métodos `GET,POST,PUT,DELETE,OPTIONS`; origen exacto.
5. Desplegar stage (`dev`/`prod`) y definir en Postman `baseUrl=https://{api-id}.execute-api.{region}.amazonaws.com/{stage}`.
6. Probar `401`, `403`, PDF y `202` a través de Gateway.

El backend no contiene una URL EC2 fija. La URL local es `http://localhost:8080`; la URL remota vive en API Gateway, DNS y variables de cliente.

Capturas: routes, integrations, authorizer, stage URL, respuesta protegida y logs de EC2.

## RabbitMQ en cloud

Para la evaluación puede ejecutarse con Docker en EC2 o instancia separada. No publicar 5672/15672 a Internet; limitar SG. Usar usuario no `guest`, password secreto y volumen persistente. Capturar overview, exchange, ambas colas, mensaje exitoso consumido y mensaje en DLQ.

Mantener `RABBITMQ_ERROR_SIMULATION_ENABLED=false` en producción. Habilitarla solo durante una demostración local controlada y deshabilitarla inmediatamente después.

## Evidencias mínimas ordenadas

1. `mvn clean test` exitoso.
2. `docker-compose ps` con app/Rabbit healthy.
3. Azure: tokens redactados de los dos roles.
4. Postman: 201, 202, 401, 403, descarga PDF.
5. Rabbit Management: exchange, dos colas, DLQ con mensaje.
6. Oracle: tabla nueva, unique constraint y fila.
7. S3: key organizada y metadata.
8. API Gateway: rutas/authorizer y llamada stage.
9. EC2: contenedor y health.
