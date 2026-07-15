# Plan de evidencias EFT

## Evidencias locales completadas

1. `docker compose config` válido: cuatro servicios, puertos 8080/8081/8082/5672/15672 y valores ficticios, sin secretos reales.
2. Contenedores `bff-service`, `cursos-service`, `inscripciones-service` y RabbitMQ saludables.
3. Health público HTTP 200 / `UP` en los tres servicios.
4. Endpoints de negocio sin token: 401 con JSON; control de rol: estudiante no puede crear cursos (403).
5. CRUD de cursos: creación 201 por BFF, ID generado, consulta/listado, actualización y validación 400.
6. Material: clave `cursos/2/materiales/readme.md`, `subidoAS3=false`.
7. Inscripción automática 201, evento publicado, consumido y persistido; cola principal vacía.
8. Republicación del mismo `eventoId`: una sola fila persistida.
9. Listener desactivado en recreación exclusiva de inscripciones: mensaje pendiente con cero consumidores.
10. Endpoint consumidor BFF: evento devuelto, persistido y retirado de la cola.
11. Fallo controlado: tres intentos, backoff finito, `x-death`, cola principal vacía y un mensaje preservado en DLQ.
12. Maven Wrapper: 19 pruebas, 0 fallos, 0 errores, 0 omitidas y `BUILD SUCCESS`.

## Comandos principales

```powershell
.\mvnw.cmd -f eft-cursos\pom.xml clean verify
docker compose -f eft-cursos\docker-compose.yml config
docker compose -f eft-cursos\docker-compose.yml up -d --build
$env:RABBITMQ_LISTENER_AUTO_STARTUP='false'
docker compose -f eft-cursos\docker-compose.yml up -d --no-deps --force-recreate inscripciones-service
```

## Evidencias futuras o humanas

- Revisar el informe y capturar imágenes si la entrega exige anexos visuales.
- Crear el Word oficial y video Kaltura cuando corresponda.
- Commit y push manuales mediante GitHub Desktop.
- Integraciones reales Azure, AWS y API Manager quedan fuera de esta validación académica local.

No registrar tokens, correos, tenant IDs, account IDs ni credenciales en evidencias.
