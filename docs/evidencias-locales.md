# Evidencias locales verificadas

## Identificación

- Fecha: `2026-07-13 11:58 -04:00` (`America/Santiago`).
- Rama: `main`.
- Commit base: `543ef79a562298ca8c2a9d7cf6896a526d647a70`.
- No se ejecutaron push, pull, merge, rebase, commit, reset ni `git clean`.
- Configuración local: `.env` ignorado por Git, con valores ficticios para B2C/S3 y credenciales exclusivas del broker local. No contiene credenciales cloud reales.

## Docker y Compose

- Contexto: `desktop-linux`.
- Docker Client: `29.4.3`, API `1.54`, `windows/amd64`.
- Docker Server: Docker Desktop `4.74.0`, Engine `29.4.3`, API `1.54`, `linux/amd64`, WSL2.
- Compose standalone: `v5.1.3`.
- `docker-compose config --quiet`: exit code `0`.
- `docker-compose up -d --build`: imagen de aplicación construida con `BUILD SUCCESS` y servicios iniciados.

| Servicio | Imagen | Puerto host | Estado comprobado |
| --- | --- | --- | --- |
| `app` | `guia-despacho-app` | `8080` | `healthy` |
| `rabbitmq` | `rabbitmq:4.1-management` (RabbitMQ `4.1.8`) | `5672`, `15672` | `healthy` |

Volúmenes usados: `rabbit-data`, `app-data`, `app-efs`. Red: `guia-network`. No se borró ningún volumen.

## Aplicación, base local y seguridad

- Perfil: `default`.
- Base local: H2 `2.3.232`, archivo `/app/data/guiadespacho`, modo de compatibilidad Oracle.
- La aplicación creó `GUIA_DESPACHO` y la tabla distinta `GUIA_PROCESADA_RABBIT`, más índice `IX_GUIA_PROC_GUIA_ID` y unique `UK_GUIA_PROC_EVENTO`.
- Conexión AMQP real: creada como usuario local contra `rabbitmq:5672`.
- `GET /actuator/health`: HTTP `200`, `status=UP`.
- `GET /api/guias/1` sin token: HTTP `401`.
- `POST /api/procesamiento/guias/900001?simularError=true` sin token y con simulación deshabilitada: HTTP `401`.
- Estado final comprobado: `RABBITMQ_ERROR_SIMULATION_ENABLED=false`.

No existe issuer/JWT local documentado y el entorno usa URLs B2C ficticias. Por ello no se probó un token `GESTION_GUIAS`, no se ejecutó el endpoint autenticado y no se afirma HTTP `202` real. Las pruebas MockMvc sí cubren `202`, `401` y `403`, pero no sustituyen una integración HTTP con B2C.

## Topología RabbitMQ real

`rabbitmqctl` confirmó:

- Exchange `guia.exchange`: tipo `direct`, durable `true`, auto-delete `false`.
- Cola `guia.procesamiento.queue`: durable, un consumidor, DLX `guia.exchange`, routing key de dead letter `guia.error`.
- Cola `guia.error.queue`: durable, sin consumidor automático.
- Binding `guia.exchange` -> `guia.procesamiento.queue` con `guia.creada`.
- Binding `guia.exchange` -> `guia.error.queue` con `guia.error`.
- Puertos activos: AMQP `5672`, Management HTTP `15672`.

## Flujo exitoso con broker real

Debido al bloqueo de JWT, se publicó un mensaje sintético controlado mediante la API local de RabbitMQ; esto verifica broker, consumidor y persistencia, pero no el endpoint HTTP productor.

- `eventoId`: `11111111-1111-4111-8111-111111111111`.
- `guiaId`: `900001`.
- `message_id`: igual al `eventoId`.
- `correlation_id`: `900001`.
- Resultado de publicación: `routed=true`.
- Log del consumidor: `Consumiendo eventoId=... guiaId=900001 intento=0`.
- Log JPA: `Evento persistido eventoId=... guiaId=900001` y un `INSERT` real.
- Estado posterior: principal `ready=0/unacked=0`, errores `ready=0`.

Una instantánea de H2 se consultó con SQL:

```text
EVENTO_ID                            | EVENT_COUNT
11111111-1111-4111-8111-111111111111 | 1
```

## Idempotencia con broker real

El mismo payload se publicó nuevamente con el mismo `eventoId`:

- Segunda publicación: `routed=true`.
- Segundo consumo observado.
- Log: `Evento duplicado ignorado eventoId=11111111-... guiaId=900001`.
- SQL final: `EVENT_COUNT = 1`.
- Cola principal vacía y DLQ sin mensaje nuevo.

## Error, reintentos y DLQ reales

La simulación se habilitó temporalmente solo en `.env`, se recreó únicamente `app` y se publicó el evento sintético `22222222-2222-4222-8222-222222222222` con `simularError=true`.

Intentos observados en UTC:

1. `15:54:58.407`.
2. `15:54:59.419` (aprox. 1 s).
3. `15:55:01.421` (aprox. 2 s).

Luego `RejectAndDontRequeueRecoverer` registró `Retries exhausted`; no hubo cuarto intento ni ciclo infinito. Resultado de colas:

- `guia.procesamiento.queue`: `ready=0`, `unacked=0`, consumidores `1`.
- `guia.error.queue`: `ready=1`, `unacked=0`.

Propiedades inspeccionadas con `ack_requeue_true`, por lo que el mensaje quedó en DLQ:

```text
message_id: 22222222-2222-4222-8222-222222222222
correlation_id: 900002
content_type: application/json
delivery_mode: 2
routing_key: guia.error
x-first-death-queue: guia.procesamiento.queue
x-first-death-exchange: guia.exchange
x-first-death-reason: rejected
x-death.count: 1
x-death.routing-keys: [guia.creada]
```

Al terminar se restauró `RABBITMQ_ERROR_SIMULATION_ENABLED=false` y se recreó solo `app`.

## Durabilidad y reconexión

Con el mensaje todavía en DLQ se ejecutó `docker-compose restart rabbitmq`, sin borrar volúmenes.

- Tras el reinicio, las dos colas durables reaparecieron.
- La DLQ conservó `ready=1`.
- La aplicación registró `CONNECTION_FORCED`, intentos temporales con `Connection refused` y reinicio automático del consumidor.
- A las `15:56:13.516Z` se creó una conexión AMQP nueva.
- La cola principal recuperó un consumidor.
- Se publicó el evento posterior al reinicio `33333333-3333-4333-8333-333333333333`: `routed=true`, consumido y persistido.
- Estado posterior: principal vacía; DLQ todavía `ready=1`.

## Maven final

Comando:

```powershell
.\mvnw.cmd clean verify
```

Resultado:

```text
Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 39.482 s
Finished at: 2026-07-13T11:58:17-04:00
```

Advertencias no bloqueantes: `@MockBean` deprecado y aviso de Commons Logging.

## Validación Git final

- `git status --short` confirma cambios locales previos y los archivos nuevos de la solución; no se alteró el índice ni se creó commit.
- `.env` aparece ignorado por la regla `.gitignore:37:.env`.
- `git diff --check` global devolvió exit code `2` porque este repositorio tiene archivos de `target/` versionados y Surefire regeneró XML con espacios finales.
- `git diff --check -- . ':(exclude)target/**'` devolvió exit code `0`; no se detectaron errores de whitespace fuera de artefactos Maven generados.
- Persisten advertencias informativas LF/CRLF de Git en Windows.

## Limitaciones declaradas

No fueron verificados contra servicios reales: Azure AD B2C, Oracle Cloud, AWS S3, API Gateway ni EC2. Tampoco se verificó el HTTP `202` autenticado por ausencia de JWT válido. La integración real comprobada usa Docker Desktop, RabbitMQ, H2 y publicación controlada directa al broker.

## Auditoría B2C del 2026-07-13

- Aplicación y RabbitMQ continúan `healthy`; health HTTP `200`.
- El contenedor usa perfil `default`, claim `extension_RolGuia` y simulación de errores `false`.
- Issuer, JWKS y audience cargados son marcadores ficticios locales.
- Policy documentada: `B2C_1_guias_signupsignin`; scope corto documentado: `access_as_user`. No fueron verificados contra Azure.
- Roles del backend: `GESTION_GUIAS` y `DESCARGA_GUIAS`.
- Faltan tenant, tenant ID, dominio real, API client ID, Postman client ID, scope completo y redirect URI confirmada.
- No existen tokens B2C en variables del proceso ni una colección Postman previa.
- Azure CLI, Postman CLI y `jq` no están disponibles; `curl.exe` sí.
- La inspección visual de Azure Portal no pudo iniciarse por una restricción local del navegador integrado, no por un error de Azure.
- Sin token: HTTP `401`. Token malformado: HTTP `401` con `invalid_token`.
- Se creó y validó una colección Postman sin secretos con nueve requests: `docs/postman/guia-despacho-b2c.postman_collection.json`.
- HTTP `202`, HTTP `403`, descarga autorizada y usuarios/claims reales siguen pendientes.

## Actualización B2C real y endpoint autenticado (2026-07-13)

Esta sección reemplaza las limitaciones B2C anteriores de este documento:

- Se consultaron el discovery OIDC y JWKS reales de la policy `B2C_1_guias_signupsignin`; issuer, audience, policy, nonce y scope del access token coinciden con la configuración activa de Spring Boot.
- JWKS respondió HTTP `200` y contiene claves con `kid`.
- Microsoft Graph confirmó la propiedad `extension_d01da54653ee4bd18e924fc1f8efbab8_RolGuia` y se asignó exclusivamente `GESTION_GUIAS` al usuario objetivo enmascarado `7ba…1c4`; el PATCH respondió `204` y el GET posterior devolvió el valor exacto.
- Un flujo Authorization Code con PKCE S256 completamente nuevo obtuvo un access token real. El token no se imprimió ni se guardó en archivos y contiene `extension_RolGuia=GESTION_GUIAS`.
- El token B2C real obtuvo HTTP `200` en `GET /api/procesamiento/colas`, HTTP `201` en `POST /api/guias` y HTTP `202` en `POST /api/procesamiento/guias/1`.
- El evento del endpoint `202` fue consumido y persistido en `GUIA_PROCESADA_RABBIT`; los logs muestran el `INSERT` y `Evento persistido` para el mismo evento enmascarado `ccd…17f`.
- El access token B2C no incluye claim `tid`; la pertenencia al tenant se valida mediante el issuer exacto configurado, que sí coincide. Spring valida firma por JWKS, issuer y audience y aceptó el token.
- Validación final: app y RabbitMQ `healthy`, actuator HTTP `200`, endpoint protegido sin token HTTP `401`, 37 pruebas, 0 fallos, 0 errores y 0 omitidas.

Sigue pendiente crear por formulario y asignar `DESCARGA_GUIAS` a la segunda cuenta B2C. Por ello aún no se afirma una prueba HTTP real de `403` entre ambos perfiles ni descarga con el rol limitado. Oracle Cloud, AWS S3, API Gateway y EC2 tampoco se probaron contra infraestructura remota.

## Segundo perfil B2C verificado (2026-07-13)

Esta sección reemplaza el pendiente del párrafo anterior:

- Microsoft Graph verificó la cuenta personal local B2C `sa***@gm***`, Object ID `f5b…04e`, habilitada y distinta de la cuenta administrativa.
- Con autorización explícita se ejecutó un único PATCH, con una sola propiedad, para cambiar `extension_d01da54653ee4bd18e924fc1f8efbab8_RolGuia` de `GESTION_GUIAS` a `DESCARGA_GUIAS`; respuesta HTTP `204`.
- Los GET posteriores confirmaron `f5b…04e=DESCARGA_GUIAS` y `7ba…1c4=GESTION_GUIAS`. Los demás atributos seleccionados y los roles de otros usuarios permanecieron iguales; no se ejecutó otro cambio Azure.
- Un PKCE S256 completamente nuevo produjo un access token con issuer, audience, policy, nonce y scope `access_as_user` válidos, `extension_RolGuia=DESCARGA_GUIAS` y ausencia de `GESTION_GUIAS`.
- Descarga real: HTTP `200`, `Content-Type: application/pdf`, archivo `guia-1.pdf`, 974 bytes y cabecera `%PDF-` válida.
- Con el mismo token: `POST /api/procesamiento/guias/1`, `POST /api/guias` y `GET /api/procesamiento/colas` devolvieron HTTP `403`; ninguna respuesta fue `401`.
- Antes y después de los tres rechazos: cola principal `ready=0/unacked=0`, DLQ `ready=1/unacked=0`, logs de publicación `2` y persistencia `2`. No se publicó ni persistió ningún evento nuevo.
- Logs JWT posteriores: cero errores de firma o validación.

## Integración AWS S3 real (2026-07-13)

Esta sección reemplaza el pendiente de AWS S3 indicado arriba:

- Bucket creado y verificado: `duoc-guia-despacho-d624cd925633`, región `us-east-1`, privado, Block Public Access completo, sin bucket policy y sin grants públicos en la ACL.
- Las credenciales temporales de AWS Academy se mantuvieron en memoria de proceso y se inyectaron al contenedor mediante variables de entorno; no se creó perfil ni se escribieron access keys en archivos versionados.
- Aplicación y RabbitMQ permanecieron `healthy`; `/actuator/health` respondió `200`.
- Un PKCE S256 nuevo validó issuer, audience, policy, scope `access_as_user` y `extension_RolGuia=GESTION_GUIAS`.
- `POST /api/guias` respondió HTTP `201` para la guía `id=2`; la respuesta incluyó `s3Key` y la aplicación ejecutó la subida automática con `AWS_S3_AUTO_UPLOAD=true`.
- El bucket pasó de vacío a contener exactamente la key devuelta por la aplicación. `head-object` confirmó `Content-Type=application/pdf`, 979 bytes, cifrado `AES256` y fecha `2026-07-14T00:43:31Z`.
- La descarga temporal desde S3 comenzó con `%PDF-` y produjo SHA-256 `F392F3E4C1645BA90C7ADF11C4DE761322D22CCD3C9E8BB90463A1ECB8553853E`.
- Otro PKCE S256 nuevo validó `extension_RolGuia=DESCARGA_GUIAS`. El endpoint de descarga de `id=2` respondió HTTP `200`, 979 bytes, `%PDF-` y el mismo SHA-256.
- Con ese token limitado, procesamiento, creación y consulta de colas respondieron HTTP `403`, no `401`. Antes y después: principal `0`, DLQ `1`, sin mensajes ready/unacked nuevos; la consulta de persistencia no cambió y S3 conservó exclusivamente el objeto esperado.
- No se borró el objeto de evidencia ni se movió/eliminó el PDF local para forzar el fallback. Por tanto, queda demostrado el auto-upload real y la descarga autorizada del mismo contenido, pero no se afirma que el endpoint haya usado el fallback S3.

Persisten como bloqueos externos Oracle Cloud, API Gateway y EC2 reales, además de las capturas y el video final.

Validación Maven posterior a la integración S3:

```text
Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 43.421 s
Finished at: 2026-07-13T21:00:42-04:00
```
