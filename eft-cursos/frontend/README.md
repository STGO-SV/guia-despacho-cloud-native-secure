# Frontend académico EFT

SPA mínima con Vite, JavaScript y `@azure/msal-browser`. MSAL Browser implementa Authorization Code con PKCE para aplicaciones SPA.

## Configuración

Completa `public/config.js` con los datos públicos de la aplicación B2C. El build lo copia como `dist/config.js`, separado del bundle. No agregues client secrets: una SPA es un cliente público.

- `B2C_CLIENT_ID`
- `B2C_AUTHORITY` con el user flow `B2C_1_guias_signupsignin`
- `B2C_KNOWN_AUTHORITY`, por ejemplo `tenant.b2clogin.com`
- `B2C_REDIRECT_URI=http://localhost:5173`
- `B2C_SCOPE`, scope completo terminado en `access_as_user`
- `BFF_BASE_URL=http://localhost:8080`

Registra `http://localhost:5173` como redirect URI de tipo SPA en Azure AD B2C.

Para el backend real, inicia Compose con `APP_SECURITY_DEMO_ENABLED=false` y configura `AZURE_ISSUER_URI`, `AZURE_JWK_SET_URI`, `AZURE_CLIENT_ID` y `AZURE_ROLES_CLAIM`. El user flow debe devolver `extension_RolGuia` en ID y access tokens.

## Ejecución

```powershell
npm install
npm run test
npm run build
npm run serve
```

Abre `http://localhost:5173`. `npm run dev` también está disponible en un entorno local sin restricciones; `npm run serve` publica exactamente el build validado. Los tokens quedan únicamente en `sessionStorage`; la interfaz nunca imprime el access token.

## Roles

- `GESTION_GUIAS` o `INSTRUCTOR`: Instructor.
- `DESCARGA_GUIAS` o `ESTUDIANTE`: Estudiante.

Sin una configuración B2C real, el frontend puede compilar y cargar, pero el login funcional requiere completar los datos públicos y usar cuentas del tenant.
