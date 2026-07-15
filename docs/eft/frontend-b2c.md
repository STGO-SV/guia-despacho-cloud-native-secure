# Frontend académico con Azure AD B2C

## Arquitectura

`Navegador :5173 -> MSAL Browser -> Azure AD B2C -> access token -> BFF :8080 -> microservicios`

La SPA usa Vite y JavaScript sin framework. `@azure/msal-browser` implementa OAuth 2.0 Authorization Code con PKCE; no se implementa ni habilita flujo implícito.

## Configuración MSAL

Los valores públicos se cargan desde `eft-cursos/frontend/public/config.js`, que el build copia como `dist/config.js` sin integrarlo al bundle:

- `B2C_CLIENT_ID`: ID público de la SPA.
- `B2C_AUTHORITY`: authority completa con `B2C_1_guias_signupsignin`.
- `B2C_KNOWN_AUTHORITY`: dominio `*.b2clogin.com` permitido.
- `B2C_REDIRECT_URI`: `http://localhost:5173`.
- `B2C_SCOPE`: scope completo `access_as_user` de la API.
- `BFF_BASE_URL`: `http://localhost:8080`.

La redirect URI debe registrarse en Azure AD B2C como tipo SPA. No se requiere ni se debe agregar un client secret.

Para que el BFF y los servicios acepten el access token real, inicia Compose con `APP_SECURITY_DEMO_ENABLED=false` y completa también los valores públicos `AZURE_ISSUER_URI`, `AZURE_JWK_SET_URI`, `AZURE_CLIENT_ID` y `AZURE_ROLES_CLAIM=extension_RolGuia`. El modo demo continúa activo por defecto para las pruebas locales sin Azure.

## Login, token y logout

1. La aplicación espera `initialize()` y procesa el retorno de autenticación.
2. `loginPopup` abre el user flow B2C; MSAL realiza Code + PKCE.
3. Antes de cada llamada, `acquireTokenSilent` consulta/renueva el token; solo ante interacción requerida usa popup.
4. El BFF recibe `Authorization: Bearer <access_token>`.
5. `logoutPopup` cierra la sesión B2C y retorna al frontend.

La caché se limita a `sessionStorage`; no se usa `localStorage`, cookies de sesión propias ni archivos. El token nunca se muestra en la interfaz ni se escribe en logs.

## Roles y operaciones

| Claim | Visualización | Operación |
| --- | --- | --- |
| `GESTION_GUIAS` o `INSTRUCTOR` | Instructor | GET cursos, POST curso |
| `DESCARGA_GUIAS` o `ESTUDIANTE` | Estudiante | GET cursos, POST inscripción |

El claim leído inicialmente es `extension_RolGuia`. La pantalla incluye una acción de evidencia: estudiante intenta crear curso, o instructor intenta crear inscripción; el BFF debe responder 403 y el panel lo explica.

## Ejecución

```powershell
cd eft-cursos\frontend
npm install
npm run test
npm run build
npm run serve
```

Abrir `http://localhost:5173`. Build verificado con Vite 7.3.6; cuatro pruebas Vitest aprobadas y `npm audit` sin vulnerabilidades.

En el sandbox de la ruta OneDrive, el optimizador del servidor `vite dev` intentó recorrer fuera del workspace y fue bloqueado. Para la evidencia se sirve el build con `npm run serve`, un servidor Node limitado a `dist/`; esto no afecta el artefacto ni el flujo MSAL en el navegador.

## Evidencia pendiente

La compilación, carga local, UI y CORS pueden validarse sin credenciales. El login real, los 200/201 con identidades B2C, el 403 asociado a esas identidades y el logout requieren completar los valores públicos, registrar la redirect URI y disponer de cuentas de instructor/estudiante del tenant. No se inventan resultados de esa fase.

El user flow debe emitir `extension_RolGuia` también en el ID token para que la interfaz pueda mostrar el rol, y en el access token para que los Resource Servers autoricen la operación.

Referencias oficiales: [MSAL Browser y B2C](https://learn.microsoft.com/en-us/entra/msal/javascript/browser/working-with-b2c), [adquisición silenciosa](https://learn.microsoft.com/en-us/entra/msal/javascript/browser/acquire-token), [caché de MSAL](https://learn.microsoft.com/en-us/entra/msal/javascript/browser/caching).
