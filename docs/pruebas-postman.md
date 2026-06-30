# Pruebas Postman

Usar access tokens emitidos por Azure AD B2C para la aplicacion `guia-despacho-postman` contra la API `guia-despacho-api`, flujo `B2C_1_guias_signupsignin`, scope `access_as_user`.

No guardar tokens reales en el repositorio ni pegarlos en capturas versionadas.

## Configuracion Base

Crear una variable de entorno en Postman:

```text
baseUrl=http://localhost:8080
```

Enviar el token en cada solicitud protegida:

```http
Authorization: Bearer <access_token>
```

El token debe incluir:

```json
{
  "extension_RolGuia": "GESTION_GUIAS"
}
```

o:

```json
{
  "extension_RolGuia": "DESCARGA_GUIAS"
}
```

## Casos De Prueba

| Caso | Solicitud | Token | Resultado esperado |
| --- | --- | --- | --- |
| Sin token sobre consulta | `GET {{baseUrl}}/api/guias?transportista=Transportes%20Norte&fecha=2026-06-29` | Sin token | `401 Unauthorized` |
| Gestion consulta guias | `GET {{baseUrl}}/api/guias?transportista=Transportes%20Norte&fecha=2026-06-29` | `GESTION_GUIAS` | Acceso autorizado |
| Gestion intenta descargar | `GET {{baseUrl}}/api/guias/1/descargar?transportista=Transportes%20Norte` | `GESTION_GUIAS` | `403 Forbidden` |
| Descarga descarga guia | `GET {{baseUrl}}/api/guias/1/descargar?transportista=Transportes%20Norte` | `DESCARGA_GUIAS` | Acceso autorizado si existe la guia y coincide el transportista |
| Descarga intenta crear guia | `POST {{baseUrl}}/api/guias` | `DESCARGA_GUIAS` | `403 Forbidden` |

## Body Para Crear Guia

Usar este body en el caso `POST /api/guias`:

```json
{
  "numeroGuia": "GD-001",
  "transportista": "Transportes Norte",
  "fecha": "2026-06-29",
  "destinatario": "Cliente Demo",
  "direccionDestino": "Av. Siempre Viva 123",
  "descripcionCarga": "Cajas con insumos"
}
```

Para probar una descarga autorizada normalmente primero se debe crear una guia con un token `GESTION_GUIAS`, anotar el `id` devuelto y luego llamar a descarga con un token `DESCARGA_GUIAS`. La descarga conserva la regla heredada del parametro `transportista`, por lo que debe coincidir con el transportista guardado.
