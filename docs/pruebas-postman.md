# Pruebas Postman

## Entorno

Coleccion exportable preparada: `postman/guia-despacho-b2c.postman_collection.json`. No contiene tokens, secretos ni identificadores Azure reales. Sus variables locales son `baseUrl`, `gestionAccessToken`, `descargaAccessToken`, `guiaId`, `eventoId`, `transportista`, `fecha`, `tenant`, `tenantId`, `policy`, `clientId`, `apiClientId`, `scope` y `redirectUri`.

Al `2026-07-13`, tenant, IDs de cliente, redirect URI y scope completo siguen sin estar disponibles en el repositorio o el entorno local. Deben confirmarse en Azure antes de construir las URLs reales de Authorization Code con PKCE.

> Actualización: esos identificadores ya están configurados localmente en `.env` (ignorado por Git). Se verificaron flujos PKCE S256 reales para `GESTION_GUIAS` y `DESCARGA_GUIAS`, sin exportar ni guardar tokens.

Evidencia real del rol limitado: `GET /api/guias/1/descargar?transportista=Transportes%20Evaluacion` respondió `200`, `application/pdf`, `guia-1.pdf`, 974 bytes y magic bytes `%PDF-`. Con el mismo token, procesamiento, creación y consulta de colas respondieron `403`, nunca `401`; colas y contadores de publicación/persistencia no cambiaron.

Evidencia S3 real adicional del 2026-07-13: `POST /api/guias` con `GESTION_GUIAS` creó la guía `id=2` con HTTP `201` y `AWS_S3_AUTO_UPLOAD=true`. La key devuelta por el backend apareció como único objeto nuevo en `duoc-guia-despacho-d624cd925633`; `head-object` confirmó `application/pdf`, 979 bytes y `AES256`. El archivo descargado con AWS CLI comenzó con `%PDF-` y su SHA-256 fue `F392F3E4C1645BA90C7ADF11C4DE761322D22CCD3C9E8BB90463A1ECB8553853E`. Con un PKCE nuevo, `DESCARGA_GUIAS` descargó `id=2` por el endpoint con HTTP `200` y el mismo hash. Sus intentos de procesamiento, creación y consulta de colas devolvieron `403`; RabbitMQ, la consulta de guías denegadas y el único objeto S3 permanecieron sin cambios.

Importar `openapi.yaml` y crear variables:

| Variable | Ejemplo no secreto |
| --- | --- |
| `baseUrl` | `http://localhost:8080` o URL del stage API Gateway |
| `tokenGestion` | access token B2C del rol gestión |
| `tokenDescarga` | access token B2C del rol descarga |
| `guiaId` | se asigna desde la creación |
| `eventoId` | se asigna desde el `202` |
| `transportista` | `Transportes Norte` |

No guardar tokens en el workspace sincronizado ni exportarlos. En cada request usar Bearer Token `{{tokenGestion}}` o `{{tokenDescarga}}`.

## Obtener access token

Configurar OAuth 2.0 Authorization Code con PKCE usando el authorization/token endpoint del user flow `B2C_1_guias_signupsignin`, client ID del cliente Postman, redirect URI autorizada y scope completo de `access_as_user`. Azure debe emitir `aud` igual al client ID de la API y `extension_RolGuia` con el rol. Usar access token, nunca ID token.

## 1. Seguridad negativa

1. `GET {{baseUrl}}/api/guias?transportista={{transportista}}&fecha=2026-07-13` sin token: esperar `401` JSON.
2. Mismo GET con `tokenDescarga`: esperar `403`.
3. `GET {{baseUrl}}/api/guias/1/descargar?transportista={{transportista}}` con `tokenGestion`: esperar `403`.
4. `POST {{baseUrl}}/api/procesamiento/guias/1` con `tokenDescarga`: esperar `403`.

Esto demuestra diferencia entre autenticación (401) y autorización (403).

## 2. Crear y publicar automáticamente

`POST {{baseUrl}}/api/guias` con `tokenGestion`:

```json
{
  "numeroGuia": "GD-001",
  "transportista": "Transportes Norte",
  "fecha": "2026-07-13",
  "destinatario": "Cliente Demo",
  "direccionDestino": "Av. Siempre Viva 123",
  "descripcionCarga": "Cajas con insumos"
}
```

Esperar `201`, header `Location` y body con `id`. Script Tests:

```javascript
pm.test("201", () => pm.response.to.have.status(201));
pm.environment.set("guiaId", pm.response.json().id);
```

La creación genera PDF, ejecuta S3 si `AWS_S3_AUTO_UPLOAD=true` y publica un evento normal.

## 3. Consultar, actualizar y S3

- `GET {{baseUrl}}/api/guias/{{guiaId}}` con gestión: `200`.
- `GET {{baseUrl}}/api/guias?transportista={{transportista}}&fecha=2026-07-13`: lista con la guía.
- `PUT {{baseUrl}}/api/guias/{{guiaId}}` con el body anterior más `"estado":"EN_TRANSITO"`: `200`.
- `POST {{baseUrl}}/api/guias/{{guiaId}}/subir-s3`: `200`, `s3Key` debe comenzar con `guias/transportes-norte/`.
- En S3 comprobar objeto privado, key, tamaño y `Content-Type=application/pdf`.

## 4. Descargar con rol limitado

`GET {{baseUrl}}/api/guias/{{guiaId}}/descargar?transportista={{transportista}}` con `tokenDescarga`: esperar `200`, `application/pdf` y `Content-Disposition: attachment`. Usar “Send and Download”. Repetir con transportista diferente: `403` aunque el rol sea correcto.

## 5. Camino RabbitMQ exitoso

`POST {{baseUrl}}/api/procesamiento/guias/{{guiaId}}` con gestión: esperar `202`.

```javascript
pm.test("202", () => pm.response.to.have.status(202));
pm.environment.set("eventoId", pm.response.json().eventoId);
```

Verificar:

1. `GET {{baseUrl}}/api/procesamiento/colas` devuelve nombres de ambas colas.
2. Rabbit Management muestra consumidor en `guia.procesamiento.queue`.
3. Oracle:

```sql
SELECT * FROM GUIA_PROCESADA_RABBIT
WHERE EVENTO_ID = '<eventoId>';
```

Debe existir exactamente una fila. La cola principal puede verse siempre en cero porque el listener consume rápidamente; usar la tasa de mensajes y logs como evidencia.

## 6. Camino de error y DLQ

Antes de esta prueba, definir `RABBITMQ_ERROR_SIMULATION_ENABLED=true` solo en el ambiente local de demostración y reiniciar la aplicación. Con el valor seguro por defecto `false`, la llamada responde `403` y no publica ningún mensaje.

`POST {{baseUrl}}/api/procesamiento/guias/{{guiaId}}?simularError=true` con gestión: esperar `202`. Guardar el nuevo `eventoId`, esperar al menos 4 segundos y verificar:

- logs con tres intentos;
- `guia.error.queue` tiene un mensaje;
- en headers del mensaje existen `x-death`, `message_id` y correlation ID/guía;
- Oracle no contiene fila para ese `eventoId`.

No usar “Get message” con requeue desactivado antes de capturar evidencia, porque eliminaría el mensaje de la cola de error.

Al terminar la demostración, volver a `RABBITMQ_ERROR_SIMULATION_ENABLED=false` y reiniciar.

## 7. Eliminar

`DELETE {{baseUrl}}/api/guias/{{guiaId}}` con gestión: `204`. Confirmar que metadata, PDF local y objeto S3 fueron eliminados. Ejecutarlo al final para no perder evidencia.

## API Gateway

Repetir al menos 401, 403, creación, descarga y `202` cambiando solo `baseUrl` a la URL del stage. Si PDF se corrompe, habilitar `application/pdf` como binario o usar HTTP API proxy y confirmar que no se transforma el body.
