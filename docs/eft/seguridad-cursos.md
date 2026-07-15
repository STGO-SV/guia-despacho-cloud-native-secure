# Seguridad académica de cursos

## Backend

Los tres servicios usan OAuth2 Resource Server con issuer, JWK, audiencia y claim de rol configurables; sesiones stateless; respuestas JSON 401/403; regla final `denyAll`; y health público.

| Rol nuevo | Rol B2C actual equivalente | Permiso |
| --- | --- | --- |
| `INSTRUCTOR` | `GESTION_GUIAS` | Crear cursos y consumir mensajes académicos |
| `ESTUDIANTE` | `DESCARGA_GUIAS` | Consultar cursos y crear inscripciones |

## Frontend y PKCE

La SPA usa la biblioteca oficial `@azure/msal-browser`, que implementa Authorization Code con PKCE. Configura `knownAuthorities` para B2C, intenta `acquireTokenSilent` en cada llamada y guarda artefactos solamente en `sessionStorage`. No contiene secretos y no imprime tokens.

## CORS

El BFF registra CORS únicamente para `/api/bff/**` y un origen exacto:

```text
FRONTEND_ALLOWED_ORIGIN=http://localhost:5173
```

Permite métodos GET, POST y OPTIONS; headers `Authorization` y `Content-Type`; no utiliza `*`; y `allowCredentials` es falso porque la autenticación viaja en Bearer, no en cookies. Una prueba MockMvc confirma que el origen local recibe `Access-Control-Allow-Origin` y que un origen distinto obtiene 403.

## Respuestas visibles

- 401: sesión ausente o token inválido; la UI solicita iniciar sesión nuevamente.
- 403: token válido pero rol insuficiente; la UI explica el rechazo.

No se crean usuarios ni se modifica Azure desde el proyecto. La evidencia B2C real requiere intervención humana y valores públicos del tenant.

Al cambiar de la evidencia local al tenant real debe definirse `APP_SECURITY_DEMO_ENABLED=false`; de lo contrario, el decodificador controlado solo reconocerá los tokens académicos locales. También deben configurarse issuer, JWK, audiencia y claim con los valores públicos reales.
