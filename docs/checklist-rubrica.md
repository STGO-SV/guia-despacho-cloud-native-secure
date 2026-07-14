# Checklist de rúbrica

Estados: `VERIFICADO-REAL`, `VERIFICADO-PRUEBAS`, `PREPARADO` y `PENDIENTE-CLOUD`.

| Criterio | Estado | Evidencia concreta | Pendiente |
| --- | --- | --- | --- |
| Spring Boot completo (20) | VERIFICADO-REAL | Aplicación Docker healthy, H2 y Rabbit conectados; 37 pruebas pasan | Probar operaciones autenticadas con token real |
| Exchange y dos colas durables | VERIFICADO-REAL | `rabbitmqctl list_exchanges/list_queues/list_bindings` | Captura para entrega |
| Productor JSON persistente | VERIFICADO-PRUEBAS | `GuiaEventoPublisherTests`; mensajes sintéticos persistentes en broker | Endpoint productor con JWT real |
| Consumidor Java | VERIFICADO-REAL | Un consumidor activo y logs de `GuiaEventoConsumer` | — |
| Tres intentos, backoff y DLQ | VERIFICADO-REAL | Tiempos 1 s/2 s, `Retries exhausted`, DLQ `ready=1`, `x-death` | — |
| Sin ciclo infinito | VERIFICADO-REAL | Solo tres invocaciones; principal vacía | — |
| Idempotencia | VERIFICADO-REAL | Mismo evento publicado dos veces; log duplicado; SQL `COUNT=1` | — |
| Tabla nueva | VERIFICADO-REAL-LOCAL | `GUIA_PROCESADA_RABBIT`, unique e índice creados en H2 | PENDIENTE-CLOUD: Oracle real |
| Endpoint proceso `202` | VERIFICADO-PRUEBAS | MockMvc cubre rol/202 | JWT `GESTION_GUIAS` para prueba HTTP real |
| CRUD, consulta, S3 y descarga | VERIFICADO-PRUEBAS | Controlador/servicio y pruebas | S3 real requiere bucket/rol |
| Security de endpoints (15) | VERIFICADO-REAL | Reglas explícitas, `denyAll`, HTTP 401 y HTTP 403 reales | Capturas para entrega |
| Rol descarga limitado | VERIFICADO-REAL | Token B2C `DESCARGA_GUIAS`: PDF 200 y tres operaciones de gestión 403 | Capturas para entrega |
| JWT issuer/audience/claim | VERIFICADO-PRUEBAS | Validadores y claim `extension_RolGuia` | PENDIENTE-CLOUD: issuer/JWKS reales |
| Azure AD B2C (15) | PENDIENTE-CLOUD | Guía de configuración | Configurar/verificar tenant y dos usuarios |
| S3 organizado (10) | VERIFICADO-REAL | Bucket privado, auto-upload HTTP 201, key organizada, `application/pdf`, `%PDF-` y SHA-256 | Captura de consola para entrega |
| API Gateway (15) | PREPARADO | OpenAPI, CORS e inventario | PENDIENTE-CLOUD: importar y configurar authorizer |
| Docker Rabbit/app | VERIFICADO-REAL | Build, servicios healthy, puertos, health checks y volúmenes | — |
| Durabilidad RabbitMQ | VERIFICADO-REAL | DLQ persistió tras `restart rabbitmq` | — |
| Reconexión automática | VERIFICADO-REAL | Cierre, reintentos, conexión nueva y publicación posterior | — |
| CI/CD EC2 | PREPARADO | Workflow y configuración externa | PENDIENTE-CLOUD: EC2/EFS |
| Sin secretos versionados | REVISADO | `.env` ignorado; valores cloud ficticios | Mantener secretos fuera del repositorio |
| Documentación | HECHO | README, arquitectura, cloud, Postman, OpenAPI, evidencias y video | Añadir capturas |
| Video (10) | PENDIENTE-GRABAR | `docs/guion-video.md` | Grabar demostración |

## Criterios de aceptación

- [x] El proyecto compila.
- [x] 37 pruebas pasan; 0 fallos, 0 errores, 0 omitidas.
- [x] RabbitMQ inicia mediante Docker.
- [x] Existen dos colas y un exchange durables.
- [x] La cola principal tiene un consumidor Java real.
- [x] Un evento real del broker se persiste en la tabla nueva local.
- [x] El mismo `eventoId` conserva un único registro.
- [x] Tres fallos y backoff terminan en la DLQ.
- [x] `x-death` y propiedades del mensaje fueron inspeccionados sin retirarlo.
- [x] La DLQ persiste tras reiniciar RabbitMQ.
- [x] La aplicación se reconecta y procesa otro mensaje.
- [x] Health real HTTP 200 y ruta protegida HTTP 401.
- [x] Simulación de error queda deshabilitada al finalizar.
- [x] HTTP 202 real con JWT `GESTION_GUIAS` (evento consumido y persistido localmente).
- [x] HTTP 403 real con el perfil limitado B2C y gestión real validada por separado.
- [x] AWS S3 real: bucket privado, subida automática y objeto PDF verificado.
- [ ] Oracle Cloud, API Gateway y EC2 reales.
- [ ] Video final.

## Estimación conservadora

La parte local Spring Boot/RabbitMQ/Docker está demostrada con broker real, persistencia H2, DLQ, idempotencia y reconexión. Azure AD B2C, los dos roles y AWS S3 también fueron verificados contra servicios reales. Oracle Cloud, API Gateway y EC2 permanecen preparados pero no verificados contra infraestructura remota.

## Auditoría B2C pendiente de datos reales

- [x] Claim configurado: `extension_RolGuia`.
- [x] Roles confirmados: `GESTION_GUIAS`, `DESCARGA_GUIAS`.
- [x] HTTP 401 real sin token.
- [x] HTTP 401 real con token malformado.
- [x] Colección Postman sin secretos creada y validada.
- [x] Tenant, policy, client IDs, redirect URI y scope completo verificados contra Azure.
- [x] Usuarios reales con ambos atributos de rol verificados.
- [x] Token real `GESTION_GUIAS` validado: firma/JWKS, issuer, audience, policy, nonce, scope y claim de rol.
- [x] HTTP 202 con token real `GESTION_GUIAS`.
- [x] HTTP 403 y descarga PDF con token real `DESCARGA_GUIAS`.

## Actualización de estado B2C real

- `GESTION_GUIAS`: VERIFICADO-REAL mediante Graph, token PKCE y llamadas HTTP `200`, `201` y `202`.
- Productor HTTP autenticado: VERIFICADO-REAL; RabbitMQ consumió y JPA persistió el evento en `GUIA_PROCESADA_RABBIT`.
- `DESCARGA_GUIAS`: VERIFICADO-REAL mediante Graph, PKCE, descarga PDF HTTP `200` y tres rechazos HTTP `403`.
- Azure AD B2C y la separación de ambos perfiles quedan VERIFICADO-REAL localmente. Persisten solo capturas y despliegues cloud externos.
