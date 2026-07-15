# RabbitMQ para inscripciones

| Elemento | Valor verificado |
| --- | --- |
| Exchange | `cursos.exchange`, direct, durable |
| Cola principal | `inscripciones.procesamiento.queue`, durable |
| Routing principal | `inscripcion.creada` |
| DLQ | `inscripciones.error.queue`, durable |
| Routing de error | `inscripcion.error` |
| DLX de la cola principal | `cursos.exchange` |

`InscripcionEventoPublisher` publica `InscripcionCreadaEvento` persistente. `InscripcionEventoConsumer` usa `@RabbitListener`. El procesamiento guarda un `eventoId` único y omite duplicados.

El listener tiene tres intentos totales con backoff de 500 ms, multiplicador 2 y máximo 2 s. Al agotarlos, `RejectAndDontRequeueRecoverer` rechaza el mensaje y el DLX lo enruta a la DLQ; no hay ciclo infinito.

## Evidencia ejecutada

- Listener automático: evento `4db3bc17-97b2-4b9e-96c6-8b0bdb573636`, persistencia `cantidad=1`, cola principal vacía.
- Idempotencia: se republicó ese mismo evento mediante `POST /api/inscripciones/republicar-evento`; `cantidad` siguió en 1.
- Consumo explícito: se recreó solo `inscripciones-service` con `RABBITMQ_LISTENER_AUTO_STARTUP=false`; se observaron cero consumidores y un mensaje ready. `POST /api/bff/inscripciones/consumir` retiró el evento, lo persistió y dejó la cola vacía.
- Fallo controlado: evento `58a8dba7-930b-4de8-8f99-580c1102109d`; los logs registraron tres intentos. La cola principal quedó en 0 y la DLQ en 1.
- Management confirmó binding principal, routing de error y encabezado `x-death` con razón `rejected`. La lectura de comprobación usó requeue y preservó el mensaje.

No se eliminó ni recreó el volumen RabbitMQ durante las pruebas.
