# RabbitMQ para inscripciones

| Elemento | Valor |
| --- | --- |
| Exchange | `cursos.exchange` |
| Cola principal | `inscripciones.procesamiento.queue` |
| Routing key | `inscripcion.creada` |
| Cola de errores | `inscripciones.error.queue` |
| Routing de error | `inscripcion.error` |

`InscripcionEventoPublisher` publica `InscripcionCreadaEvento` persistente. `InscripcionEventoConsumer` recibe el evento con `@RabbitListener`. El procesamiento persiste `eventoId` con restricción única y omite duplicados.

La cola principal configura DLX. El listener realiza tres intentos con backoff finito y rechaza el mensaje hacia la cola de errores al agotar los reintentos.

El `POST /api/bff/inscripciones` inicia el flujo productor. Para cumplir literalmente la pauta, `POST /api/bff/inscripciones/consumir` delega en un consumo explícito con `receiveAndConvert`. En una demostración de ese endpoint se debe iniciar `inscripciones-service` con `RABBITMQ_LISTENER_AUTO_STARTUP=false`; en el flujo normal el listener automático permanece habilitado.
