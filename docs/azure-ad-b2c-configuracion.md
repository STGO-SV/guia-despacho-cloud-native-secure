# Configuracion Azure AD B2C

Este backend funciona como OAuth2 Resource Server. No inicia sesion, no solicita tokens y no necesita client secret. Solo valida access tokens enviados por clientes como Postman, una app web o API Gateway.

No guardar client secrets, access tokens, refresh tokens ni credenciales reales en el repositorio.

## Datos Confirmados

| Elemento | Valor |
| --- | --- |
| API registrada | `guia-despacho-api` |
| Aplicacion cliente para pruebas | `guia-despacho-postman` |
| Flujo de usuario | `B2C_1_guias_signupsignin` |
| Scope emitido | `access_as_user` |
| Claim personalizado de roles | `extension_RolGuia` |
| Roles validos | `GESTION_GUIAS`, `DESCARGA_GUIAS` |

El tenant B2C exacto se obtiene desde `AZURE_ISSUER_URI`. No se versiona el issuer real en el repositorio.

En Azure AD B2C el `iss` del token puede no coincidir con la URL de metadatos del user flow. Por eso la API valida el issuer exacto del token con `AZURE_ISSUER_URI`, pero obtiene las llaves publicas desde `AZURE_JWK_SET_URI`.

## Variables De Entorno

| Variable | Proposito |
| --- | --- |
| `AZURE_ISSUER_URI` | Issuer esperado del access token emitido por Azure AD B2C. Se valida exactamente contra el claim `iss`. |
| `AZURE_JWK_SET_URI` | URL de llaves publicas JWKS del user flow. Spring Security la usa para validar la firma del token. |
| `AZURE_CLIENT_ID` | Audiencia esperada de la API `guia-despacho-api`. Debe coincidir con el claim `aud` del access token. |
| `AZURE_ROLES_CLAIM` | Nombre del claim desde donde se leen los roles. El valor confirmado es `extension_RolGuia`. |
| `AWS_REGION` | Region AWS usada por el cliente S3. |
| `AWS_S3_BUCKET` | Bucket S3 existente donde se suben las guias. |
| `APP_EFS_PATH` | Ruta local o montada donde se generan temporalmente los PDF. |

Ejemplo ficticio:

```env
AZURE_ISSUER_URI=https://tenant-ficticio.b2clogin.com/tenant-id-ficticio/v2.0/
AZURE_JWK_SET_URI=https://tenant-ficticio.b2clogin.com/tenant-ficticio.onmicrosoft.com/B2C_1_flujo/discovery/v2.0/keys
AZURE_CLIENT_ID=00000000-0000-0000-0000-000000000000
AZURE_ROLES_CLAIM=extension_RolGuia
AWS_REGION=us-east-1
AWS_S3_BUCKET=bucket-ficticio
APP_EFS_PATH=./efs-local
```

## Ejecutar Localmente En PowerShell

Usar marcadores de posicion para issuer y client ID reales:

```powershell
$env:AZURE_ISSUER_URI="https://<tenant>.b2clogin.com/<tenant-id>/v2.0/"
$env:AZURE_JWK_SET_URI="https://<tenant>.b2clogin.com/<tenant>.onmicrosoft.com/B2C_1_guias_signupsignin/discovery/v2.0/keys"
$env:AZURE_CLIENT_ID="<client-id-api-guia-despacho-api>"
$env:AZURE_ROLES_CLAIM="extension_RolGuia"
$env:AWS_REGION="us-east-1"
$env:AWS_S3_BUCKET="bucket-ficticio"
$env:APP_EFS_PATH="./efs-local"
.\mvnw.cmd spring-boot:run
```

## Endpoints Y Roles

| Metodo | Endpoint | Rol requerido |
| --- | --- | --- |
| `POST` | `/api/guias` | `GESTION_GUIAS` |
| `GET` | `/api/guias/{id}` | `GESTION_GUIAS` |
| `POST` | `/api/guias/{id}/subir-s3` | `GESTION_GUIAS` |
| `GET` | `/api/guias/{id}/descargar` | `DESCARGA_GUIAS` |
| `PUT` | `/api/guias/{id}` | `GESTION_GUIAS` |
| `DELETE` | `/api/guias/{id}` | `GESTION_GUIAS` |
| `GET` | `/api/guias?transportista=...&fecha=...` | `GESTION_GUIAS` |
| `POST` | `/api/procesamiento/guias/{id}` | `GESTION_GUIAS` |
| `GET` | `/api/procesamiento/colas` | `GESTION_GUIAS` |

`GET /actuator/health` es la unica ruta publica y no entrega datos de negocio.

`GESTION_GUIAS` no entrega permiso automatico para descargar. Para descargar se requiere `DESCARGA_GUIAS`.

El parametro `transportista` del endpoint de descarga se conserva temporalmente como regla de negocio heredada. La autorizacion principal depende del rol.

## Pruebas Esperadas

| Escenario | Resultado esperado |
| --- | --- |
| Solicitud sin token | `401 Unauthorized` |
| Token valido sin rol requerido | `403 Forbidden` |
| Token valido con `GESTION_GUIAS` en endpoints de gestion | Acceso autorizado |
| Token valido con `DESCARGA_GUIAS` en descarga | Acceso autorizado |
| Token vencido, alterado o con audiencia incorrecta | `401 Unauthorized` |

## Inspeccionar Un JWT

Para revisar un token sin pegarlo en archivos del proyecto:

1. Copiar el access token solo en una herramienta temporal como jwt.ms o jwt.io.
2. Revisar el header y payload.
3. No subir capturas ni tokens reales al repositorio.
4. No usar tokens vencidos para pruebas de integracion.

Claims que debemos verificar en un access token:

- `iss`: debe coincidir con `AZURE_ISSUER_URI`.
- la firma debe validarse con las llaves publicas publicadas en `AZURE_JWK_SET_URI`.
- `aud`: debe contener `AZURE_CLIENT_ID`.
- `exp`: fecha de expiracion futura.
- `nbf`: fecha desde la cual el token es valido.
- `sub`: identificador del sujeto.
- `scp`: debe incluir o corresponder al scope `access_as_user` si Azure lo emite.
- `extension_RolGuia`: debe contener `GESTION_GUIAS` o `DESCARGA_GUIAS`.

Usar un access token para llamar a la API. No usar un ID token como bearer token de la API.

## Cambiar Temporalmente El Claim De Roles

El valor confirmado es:

```env
AZURE_ROLES_CLAIM=extension_RolGuia
```

Si Azure AD B2C emite los roles en otro custom claim durante pruebas, cambiar temporalmente `AZURE_ROLES_CLAIM`, reiniciar la aplicacion y confirmar que el payload del access token contiene ese claim:

```json
{
  "extension_RolGuia": "GESTION_GUIAS"
}
```

El backend no convierte scopes en roles y no concede roles por defecto. Si el claim configurado no existe o viene vacio, el usuario autenticado recibira `403 Forbidden` para endpoints protegidos por rol.
