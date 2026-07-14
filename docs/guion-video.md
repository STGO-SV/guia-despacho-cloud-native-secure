# Guion de video (7–9 minutos)

Preflight obligatorio: sustituir los marcadores B2C por issuer, JWKS y audience reales; obtener tokens frescos mediante Authorization Code con PKCE; verificar `iss`, `aud`, `scp` y `extension_RolGuia` antes de grabar. Si faltan tenant, client IDs, scope completo o redirect URI, no simular esta sección con JWT falsos.

## 0:00–0:40 — Apertura

Mostrar README y decir: Java 17, Spring Boot 3.5, JWT B2C, S3, RabbitMQ con dos colas, consumidor idempotente y tabla Oracle nueva. No mostrar `.env` ni tokens.

## 0:40–1:30 — Arquitectura

Abrir `docs/arquitectura.md`, recorrer el diagrama y explicar `guia.exchange`, routing `guia.creada`, cola principal, tres intentos, routing `guia.error`, DLQ y `EVENTO_ID` único.

## 1:30–2:10 — Infraestructura viva

Mostrar `docker-compose ps` con app y Rabbit healthy; luego Rabbit Management: exchange, bindings y ambas colas durables. Mostrar brevemente Oracle `GUIA_PROCESADA_RABBIT` y constraint única, todavía sin revelar credenciales.

## 2:10–3:10 — Seguridad y roles

En Postman:

1. GET de consulta sin token -> 401.
2. Misma consulta con `DESCARGA_GUIAS` -> 403.
3. Crear con `DESCARGA_GUIAS` -> 403.
4. Señalar payload JWT redactado con `aud`, `iss` y `extension_RolGuia`.

Explicar que el rol descarga solo descarga y gestión usa el resto; rutas no inventariadas quedan denegadas.

## 3:10–4:20 — Crear guía y S3

Con `GESTION_GUIAS`, ejecutar `POST /api/guias`; mostrar `201`, `id`, PDF y `s3Key` (activar `AWS_S3_AUTO_UPLOAD=true`). Abrir S3 y mostrar key `guias/transportes-norte/yyyy/MM/guia-id.pdf`, content type y bucket privado. Ejecutar consulta por transportista/fecha.

## 4:20–4:50 — Descarga

Con token de gestión intentar descargar -> 403. Cambiar a token `DESCARGA_GUIAS`, ejecutar descarga -> 200/PDF y abrir el archivo. Si hay tiempo, transportista incorrecto -> 403.

Evidencia ya verificada para repetir en cámara: `guia-1.pdf`, HTTP `200`, `application/pdf` y cabecera `%PDF-`. Con el token `DESCARGA_GUIAS`, mostrar también HTTP `403` —no `401`— en procesamiento, creación y consulta de colas; luego comprobar que los contadores RabbitMQ y los logs de persistencia no aumentaron.

## 4:50–5:50 — Camino asíncrono exitoso

Ejecutar con gestión `POST /api/procesamiento/guias/{id}` -> 202. Copiar `eventoId`. Mostrar logs del consumidor y consultar Oracle por ese UUID: exactamente una fila. Mostrar `GET /api/procesamiento/colas` y la tasa/consumer en Management; explicar que la cola puede estar vacía porque el consumo es rápido.

## 5:50–7:00 — Camino de error

Mostrar que la demo usa temporalmente `RABBITMQ_ERROR_SIMULATION_ENABLED=true` sin revelar el archivo de entorno. Ejecutar `POST /api/procesamiento/guias/{id}?simularError=true` -> 202. Mostrar tres intentos en logs, esperar el backoff y refrescar `guia.error.queue`: un mensaje. Abrir detalles sin consumirlo y mostrar `x-death`, message ID y guía. Consultar Oracle por el evento fallido: cero filas. Explicar ausencia de retry infinito y que el valor por defecto es `false`.

## 7:00–7:40 — API Gateway y EC2

Mostrar rutas importadas, integración, authorizer JWT y stage. Repetir una llamada corta cambiando `baseUrl` a Gateway. Mostrar EC2 `docker ps` y health; señalar rol IAM y EFS sin abrir secretos.

## 7:40–8:20 — Calidad y cierre

Mostrar terminal con `mvnw clean test`: total y cero fallos. Abrir `docs/checklist-rubrica.md`, señalar honestamente bloqueos o pendientes. Cerrar mostrando árbol del repositorio y recordando: seguridad completa, S3, dos colas, DLQ, Oracle nueva, Docker y documentación.

## Preparación antes de grabar

- Limpiar consola y cerrar pestañas con secretos.
- Tener tokens frescos y Postman con requests ordenados.
- Crear una guía de respaldo por si falla una llamada cloud.
- Vaciar solo las colas de demostración de forma consciente antes de grabar; no usar `down -v`.
- Preparar queries Oracle con placeholders y la consola S3 en la key correcta.
- Habilitar la simulación solo durante la grabación y volverla a `false` al terminar.
- Aumentar fuente; ocultar bookmarks/cuentas personales.
- Cronometrar una vez: si supera 10 minutos, omitir actualización/eliminación y mostrar su inventario en OpenAPI.
