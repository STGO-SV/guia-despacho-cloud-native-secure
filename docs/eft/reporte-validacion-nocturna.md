# Reporte de validación nocturna EFT

Fecha: 15-07-2026. Alcance: ejercicio formativo académico local, evaluado contra la pauta; no se evaluó viabilidad comercial.

1. **Rama.** `eft-cursos`, verificada antes de trabajar. No se ejecutaron operaciones Git de escritura.
2. **Estado inicial.** Working tree limpio; `eft-cursos` estaba un commit por delante de `origin/eft-cursos` (`[ahead 1]`).
3. **Estado Docker.** Docker Desktop estaba detenido. Se inició la instalación existente; engine Docker 29.4.3 operativo. No se instaló ni actualizó software.
4. **Contenedores.** Compose válido con cuatro servicios. El stack EFT quedó en ejecución: BFF 8080, cursos 8081, inscripciones 8082 y RabbitMQ 5672/15672. Los dos contenedores restaurados automáticamente de la copia padre se detuvieron, sin eliminarlos, porque ocupaban los puertos requeridos.
5. **Health.** Los tres endpoints `/actuator/health` respondieron HTTP 200 y `UP`; los cuatro contenedores quedaron `healthy`.
6. **Seguridad.** Sin token, BFF, cursos e inscripciones respondieron 401 JSON. Actuator permaneció público. Un estudiante recibió 403 al intentar crear curso. Se usó decodificación académica local controlada; no se generaron tokens Azure ni se desactivó Spring Security.
7. **Curso creado.** Se crearon cursos con ID 1 y 2; el curso 2 se creó por BFF, se listó y actualizó. Una entrada vacía devolvió 400. El material produjo `cursos/2/materiales/readme.md` con `subidoAS3=false`.
8. **Inscripción creada.** Listener automático: inscripción ID 1, evento `4db3bc17-97b2-4b9e-96c6-8b0bdb573636`, HTTP 201. Consumo manual: evento `b62db80c-534c-490b-9c25-9ed374e582df`. Evidencia DLQ final: evento `58a8dba7-930b-4de8-8f99-580c1102109d`.
9. **Evento RabbitMQ.** Management confirmó exchange direct durable `cursos.exchange`, cola principal durable, binding `inscripcion.creada`, DLX y routing de error `inscripcion.error`.
10. **Listener automático.** Consumió el evento normal, persistió una fila y dejó `messages=0`, `ready=0`, `unacked=0`.
11. **Consumo explícito.** Se recreó únicamente `inscripciones-service` con listener apagado. Se observaron cero consumidores y un mensaje ready. `POST /api/bff/inscripciones/consumir` devolvió el evento correcto, lo retiró, lo persistió y dejó la cola vacía.
12. **Idempotencia.** Se republicó el mismo `eventoId` del flujo automático. Antes y después, `/procesadas/{eventoId}` informó `cantidad=1` e `idempotente=true`.
13. **Reintentos.** El fallo controlado registró exactamente tres intentos. Configuración: 500 ms inicial, multiplicador 2, máximo 2 s; después se rechazó, sin ciclo infinito.
14. **DLQ.** Cola principal en 0 y `inscripciones.error.queue` en 1. Se observó `x-death` con razón `rejected`, cola origen y routing original. La inspección usó requeue; el mensaje quedó conservado.
15. **Maven.** El wrapper de la raíz ejecutó `clean verify` sobre `eft-cursos/pom.xml`. Resultado final: 19 pruebas, 0 fallos, 0 errores, 0 omitidas y `BUILD SUCCESS`.
16. **Archivos creados o modificados.** Código/configuración solo bajo `eft-cursos/`; documentación solo en `docs/eft/arquitectura-cursos.md`, `rabbitmq-cursos.md`, `plan-evidencias.md`, `endpoints-cursos.md` y este informe. El detalle exacto queda visible en `git status --short`.
17. **Correcciones aplicadas.** Perfil JWT académico controlado; variable de arranque del listener en Compose; flag mínimo `simularError`; logging del fallo; endpoint de republicación; consulta de persistencia idempotente; respuesta de inscripción con `eventoId`; prueba del fallo controlado; documentación actualizada.
18. **Bloqueos.** Se resolvieron Docker detenido, sandbox de Docker y colisión de puertos causada por contenedores de la copia padre. No quedan bloqueos técnicos para la validación local.
19. **Tareas con intervención humana.** Revisar los cambios, tomar capturas si se exigen, preparar Word/video y realizar commit/push manual desde GitHub Desktop. No se efectuaron estas acciones.
20. **Próximo paso recomendado.** Revisar este informe junto con la pauta, capturar la vista final de Compose/RabbitMQ y luego crear el commit manual en `eft-cursos`.

## Limitaciones reales

- H2 es en memoria. La recreación obligatoria de `inscripciones-service` reinició sus datos entre escenarios; el estado final conserva la inscripción de fallo y RabbitMQ conserva la DLQ.
- No se llamó Azure, AWS, API Gateway, EC2, Docker Hub, GitHub Actions ni ningún servicio cloud.
- Las credenciales locales de RabbitMQ no se incluyen en este informe.
- No se creó el Word final.
