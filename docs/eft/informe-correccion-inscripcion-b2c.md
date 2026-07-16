# Informe de corrección de inscripción B2C

Fecha de validación: 2026-07-16  
Repositorio: `guia-despacho-cloud-native-secure`  
Rama inspeccionada: `eft-cursos`

## 1. Causa raíz exacta del HTTP 400

El payload del navegador era correcto: `{"cursoId":1}`.

El mensaje `JSON de inscripcion invalido` no se genera en el BFF. Su única fuente en el
proyecto es `GlobalExceptionHandler` de `inscripciones-service`, que lo devuelve cuando
falla la validación de `InscripcionRequest`. El BFF conserva el status y el body del error
downstream mediante `BffExceptionHandler`.

Los contenedores que presentaron el fallo habían sido creados a las 01:26, antes de que
los cambios locales que eliminaron `estudianteId` fueran compilados e incorporados a las
imágenes. En consecuencia, la imagen ejecutaba el contrato anterior, cuyo DTO exigía
`estudianteId`, y rechazaba `{"cursoId":1}`. La ausencia de una línea de request en logs
no demostraba que la petición no hubiera llegado: el servicio no registra cada request.

## 2. DTO responsable y corrección

Se corrigieron ambos DTO homónimos:

- `bff-service/.../dto/InscripcionRequest.java`;
- `inscripciones-service/.../dto/InscripcionRequest.java`.

Contrato implementado:

```java
public record InscripcionRequest(
        @NotNull @Positive Long cursoId,
        boolean simularError
) { }
```

El tipo primitivo hace que Jackson interprete la ausencia de `simularError` como `false`.
`cursoId` continúa siendo obligatorio y positivo.

## 3. Contrato anterior y contrato corregido

Anterior:

```json
{"cursoId":1,"estudianteId":"identidad-controlada-por-cliente"}
```

Petición pública corregida:

```json
{"cursoId":1}
```

`simularError` permanece opcional únicamente para la evidencia académica de reintentos y
DLQ. El frontend normal no lo envía.

## 4. JSON recibido y reenviado

JSON recibido por el BFF:

```json
{"cursoId":1}
```

JSON normalizado y reenviado a `inscripciones-service`:

```json
{"cursoId":1,"simularError":false}
```

Una prueba con `MockRestServiceServer` confirma además que el header `Authorization` se
propaga. El valor del bearer usado en la prueba es ficticio y nunca se registra.

## 5. Identidad técnica

`inscripciones-service` sigue usando exclusivamente:

```java
jwt.getSubject()
```

El mismo `sub` llena `Inscripcion.estudianteId` y
`InscripcionCreadaEvento.estudianteId`. El navegador no puede controlar esta identidad
mediante body, query parameters o headers alternativos.

## 6. Correcciones visuales

- El bloque **Evidencia de autorización** queda completamente oculto sin sesión.
- El bloque reaparece después de autenticar y conserva la prueba deliberada de HTTP 403.
- Los valores `unknown`, `undefined`, `—`, vacíos o compuestos solo por espacios se
  descartan al elegir el nombre visible.
- Se conserva el orden de fallback solicitado para claims y datos de `account`.
- Los paneles por rol, selector de curso y alternancia login/logout permanecen activos.
- El payload del selector se construye explícitamente como `{cursoId: Number(valor)}`.

## 7. Archivos modificados en esta corrección

- `eft-cursos/bff-service/src/main/java/com/duoc/eft/bff/dto/InscripcionRequest.java`
- `eft-cursos/bff-service/src/test/java/com/duoc/eft/bff/client/InscripcionesClientTests.java`
- `eft-cursos/bff-service/src/test/java/com/duoc/eft/bff/controller/BffControllerTests.java`
- `eft-cursos/frontend/app.js`
- `eft-cursos/frontend/auth-utils.js`
- `eft-cursos/frontend/auth-utils.test.js`
- `eft-cursos/frontend/index.html`
- `eft-cursos/inscripciones-service/src/main/java/com/duoc/eft/inscripciones/dto/InscripcionRequest.java`
- `eft-cursos/inscripciones-service/src/main/java/com/duoc/eft/inscripciones/service/InscripcionService.java`
- `eft-cursos/inscripciones-service/src/test/java/com/duoc/eft/inscripciones/controller/InscripcionControllerTests.java`
- este informe.

También existen cambios acumulados de la corrección anterior en controladores, clientes,
frontend, pruebas y documentación. No se descartó ninguno.

## 8. Pruebas de frontend

Comando: `npm test`

- 1 archivo de pruebas aprobado.
- 7 pruebas aprobadas.
- 0 fallos.

Cobertura relevante: aliases de rol, 401/403, fallback de nombre, descarte de `unknown`,
paneles y evidencia según sesión, login/logout y payload exacto `{"cursoId":7}`.

## 9. Build de frontend

Comando: `npm run build`

- Vite 7.3.6.
- 146 módulos transformados.
- Build exitoso.
- `dist/index.html`: 3.64 kB.
- Bundle JavaScript: 286.03 kB, gzip 72.86 kB.

## 10. Resultado Maven

Reactor completo: **BUILD SUCCESS**.

- `cursos-service`: 6 pruebas aprobadas.
- `inscripciones-service`: 14 pruebas aprobadas.
- `bff-service`: 7 pruebas aprobadas.
- Total: 27 pruebas, 0 fallos, 0 errores, 0 omitidas.

La prueba BFF reproduce literalmente `POST /api/bff/inscripciones` con
`Content-Type: application/json`, JWT Estudiante y body `{"cursoId":1}`. No devuelve 400.

## 11. Reconstrucción y estado Docker

Se reconstruyeron correctamente:

- `eft-cursos-bff-service`;
- `eft-cursos-inscripciones-service`.

Se recrearon únicamente esos dos contenedores. Las variables de ambos se compararon antes
y después sin imprimir sus valores:

- variables BFF preservadas: `True`;
- variables inscripciones preservadas: `True`;
- seguridad real (`APP_SECURITY_DEMO_ENABLED=false`) preservada: `True`.

Health final:

- `8080`: `UP`;
- `8081`: `UP`;
- `8082`: `UP`;
- RabbitMQ: `healthy`.

Una petición real sin sesión a `POST /api/bff/inscripciones` devolvió 401, como corresponde.
No aparecieron errores recientes en los logs filtrados de BFF e inscripciones.

## 12. Resultado de inscripción B2C real

No se inventa un resultado. El navegador accesible a Codex no tenía pestañas ni sesión B2C
autenticada, y no se solicitaron ni manipularon credenciales o tokens. Por ello no fue
posible completar una inscripción autenticada real durante esta ejecución nocturna.

Quedaron validados el contrato exacto mediante MockMvc, la serialización HTTP mediante un
servidor simulado, el bearer, `sub`, la entidad, el evento, roles y las imágenes Docker.

## 13. Seguridad preservada

No se modificaron PKCE, MSAL, `sessionStorage`, CORS, aliases de rol, 401, 403, RabbitMQ,
reintentos, DLQ ni la idempotencia por `eventoId`. No se mostraron tokens, authorization
headers reales, secrets ni claims completos.

## 14. Configuración pública B2C

`eft-cursos/frontend/public/config.js` no fue modificado por Codex. Su última escritura
seguía siendo 2026-07-16 00:55:40, anterior a este trabajo. No se reemplazó ninguno de sus
seis valores públicos.

## 15. Riesgos pendientes

- Falta realizar una inscripción con una sesión B2C real después de esta reconstrucción.
- No existe todavía una restricción de unicidad para el par `sub` + `cursoId`.
- La idempotencia por `eventoId` protege el procesamiento RabbitMQ, pero no impide dos
  inscripciones distintas del mismo usuario al mismo curso.
- Corrección mínima futura: consulta `existsByEstudianteIdAndCursoId`, restricción única en
  base de datos y HTTP 409.

## 16. Repetición de la validación por Santiago

Desde la raíz del repositorio:

```powershell
docker compose -f eft-cursos\docker-compose.yml ps
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8081/actuator/health
Invoke-RestMethod http://localhost:8082/actuator/health

Set-Location eft-cursos\frontend
npm test
npm run build
npm run dev
```

Luego:

1. Abrir `http://localhost:5173`.
2. Sin sesión, comprobar que no aparecen paneles operativos ni evidencia de autorización.
3. Login Instructor, crear curso y comprobar que el listado se actualiza.
4. Logout.
5. Login Estudiante y comprobar que el nombre no sea `unknown` cuando exista otro fallback.
6. Seleccionar el curso ID 1 y crear inscripción.
7. En DevTools confirmar que el navegador envía únicamente `{"cursoId":1}`.
8. Esperar HTTP 201 y comprobar que la respuesta usa la identidad derivada de `sub`.
9. Ejecutar la operación prohibida y comprobar HTTP 403.
10. Logout y comprobar nuevamente la vista anónima.

Logs filtrados:

```powershell
docker compose -f eft-cursos\docker-compose.yml logs --since 10m --no-color bff-service inscripciones-service |
  Where-Object { $_ -notmatch '(?i)(authorization:|bearer\s|access_token|id_token)' }
```

## 17. Estado Git

No se ejecutaron commit, push, merge, checkout, restore, discard, reset ni otras operaciones
Git de escritura. Santiago conserva el control manual mediante GitHub Desktop.
