# Guion de video (duración objetivo: 7 minutos)

Regla de seguridad: no mostrar `.env`, tokens completos, credenciales, correos completos ni identificadores sensibles. Usar tokens frescos obtenidos previamente mediante PKCE y mantener la simulación de error deshabilitada salvo durante la demostración controlada de la DLQ.

## 0:00–0:40 — Presentación y arquitectura

- Mostrar la portada del Word o `docs/arquitectura.md`.
- Explicar: Java 17, Spring Boot, seguridad Azure AD B2C, RabbitMQ, S3 y Docker.
- Recorrer el flujo: API → exchange → cola principal → consumidor → tabla local; fallo → tres intentos → DLQ.
- Declarar desde el comienzo: Oracle Cloud, API Gateway y EC2 no fueron verificados en esta entrega.

## 0:40–1:30 — Docker y salud

- Ejecutar `docker-compose ps`.
- Mostrar `app` y `rabbitmq` en estado `healthy`.
- Abrir `GET http://localhost:8080/actuator/health` y mostrar HTTP 200 / `UP`.
- Abrir RabbitMQ Management y señalar el exchange y las dos colas durables.

## 1:30–2:30 — Azure AD B2C y roles

- Mostrar, con datos personales ocultos, el user flow y el claim `extension_RolGuia`.
- Mostrar claims redactados de `GESTION_GUIAS` y `DESCARGA_GUIAS`.
- Explicar issuer, audience, JWKS y Authorization Code con PKCE.
- Aclarar que `DESCARGA_GUIAS` solo descarga y `GESTION_GUIAS` usa las operaciones restantes.

## 2:30–3:40 — Creación de guía

- En Postman, usar `GESTION_GUIAS` y ejecutar `POST /api/guias`.
- Mostrar HTTP 201, ID de guía y referencia al PDF.
- Mostrar que la creación publica el evento sin esperar el procesamiento pesado.
- Si la demostración cloud no está disponible, usar la evidencia ya documentada; no simular una nueva subida S3.

## 3:40–4:40 — RabbitMQ y persistencia

- Mostrar `guia.procesamiento.queue`, el consumidor y los logs del evento.
- Mostrar el registro en `GUIA_PROCESADA_RABBIT` local.
- Explicar la restricción única de `eventoId` y la prueba de duplicado con conteo igual a uno.
- Mostrar la evidencia de tres intentos, backoff de 1 s/2 s y mensaje en `guia.error.queue`.

## 4:40–5:40 — AWS S3 y descarga

- Mostrar el bucket privado y el objeto `guia-2.pdf`.
- Mostrar `Content-Type=application/pdf`, AES256, cabecera `%PDF-` y el SHA-256 documentado.
- Con `DESCARGA_GUIAS`, ejecutar la descarga y mostrar HTTP 200 y el PDF.
- No borrar el objeto ni modificar ACL, IAM o Block Public Access.

## 5:40–6:30 — Pruebas 401 y 403

- Sin token, ejecutar una ruta protegida: HTTP 401.
- Con `DESCARGA_GUIAS`, intentar crear, procesar y consultar colas: HTTP 403 en los tres casos.
- Confirmar que son 403, no 401, porque el usuario está autenticado.
- Mostrar que los rechazos no publicaron mensajes ni crearon registros.

## 6:30–7:00 — Maven y cierre

- Mostrar `Tests run: 37, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS`.
- Abrir el checklist de rúbrica.
- Resumir lo verificado: Spring Boot, RabbitMQ, seguridad, B2C, S3 y Docker.
- Cerrar declarando: Oracle Cloud real no verificado, API Gateway no desplegado, EC2 no auditado y capturas finales pendientes.

## Preparación inmediata

1. Cerrar pestañas con callbacks, tokens o datos personales.
2. Aumentar el tamaño de fuente de terminal y navegador.
3. Ordenar Postman según el guion.
4. Tener abiertas las vistas de Docker, RabbitMQ, S3 y Maven.
5. Grabar una sola toma de prueba y comprobar que dure entre 6 y 8 minutos.
6. Al subir el video, reemplazar `PENDIENTE DE GRABACIÓN Y CARGA` en `enlace-video.txt` por la URL compartible.
