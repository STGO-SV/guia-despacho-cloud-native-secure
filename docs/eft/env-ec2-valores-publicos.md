# Plantilla de valores públicos para `/opt/eft-cursos/.env.ec2`

Fecha de verificación: 17 de julio de 2026.

Este documento no es un archivo de entorno ejecutable y no contiene secretos reales. Su propósito es separar los valores públicos confirmados de las credenciales que Santiago debe generar y escribir únicamente en EC2.

## Distinción entre scope y roles

| Tipo | Valores |
|---|---|
| Scope OAuth2 solicitado por la SPA | `https://duocssaezcloudnative.onmicrosoft.com/75d470b0-2bfb-4989-9d81-aa1805f3b546/access_as_user` |
| Claim que contiene el rol | `extension_RolGuia` |
| Roles Azure confirmados | `GESTION_GUIAS`, `DESCARGA_GUIAS` |
| Aliases internos admitidos | `INSTRUCTOR`, `ESTUDIANTE` |

`GESTION_GUIAS` y `DESCARGA_GUIAS` no son scopes y no deben aparecer en `B2C_SCOPE`.

## Valores públicos confirmados

| Variable | Valor |
|---|---|
| `AZURE_ISSUER_URI` | `https://duocssaezcloudnative.b2clogin.com/c1b3705d-e099-41bf-9501-b1a51343af10/v2.0/` |
| `AZURE_JWK_SET_URI` | `https://duocssaezcloudnative.b2clogin.com/duocssaezcloudnative.onmicrosoft.com/b2c_1_guias_signupsignin/discovery/v2.0/keys` |
| `AZURE_CLIENT_ID` | `75d470b0-2bfb-4989-9d81-aa1805f3b546` |
| `AZURE_ROLES_CLAIM` | `extension_RolGuia` |
| `B2C_CLIENT_ID` | `b91690e3-e8f3-435c-8aaa-6e8eb7f263ed` |
| `B2C_AUTHORITY` | `https://duocssaezcloudnative.b2clogin.com/duocssaezcloudnative.onmicrosoft.com/B2C_1_guias_signupsignin` |
| `B2C_KNOWN_AUTHORITY` | `duocssaezcloudnative.b2clogin.com` |
| `B2C_REDIRECT_URI` | `https://eft-cursos-ssaez.duckdns.org/` |
| `B2C_SCOPE` | `https://duocssaezcloudnative.onmicrosoft.com/75d470b0-2bfb-4989-9d81-aa1805f3b546/access_as_user` |
| `FRONTEND_ALLOWED_ORIGIN` | `https://eft-cursos-ssaez.duckdns.org` |
| `FRONTEND_PORT` | `8088`, puerto HTTP del frontend publicado solamente en el host EC2 para que Caddy lo consuma |
| `BFF_BASE_URL` | vacío, para usar el proxy Nginx del mismo origen |

El issuer y el JWKS se obtuvieron del documento OIDC público de `B2C_1_guias_signupsignin`. El issuer contiene el identificador GUID del tenant porque ese es el valor literal publicado y emitido como `iss`; no se debe reemplazar por una URI construida con el nombre de la policy.

La configuración local previa usaba también esta variante JWKS válida:

`https://duocssaezcloudnative.b2clogin.com/duocssaezcloudnative.onmicrosoft.com/discovery/v2.0/keys?p=b2c_1_guias_signupsignin`

Ambas variantes respondieron una clave y el mismo `kid`. La plantilla EC2 utiliza literalmente el `jwks_uri` de la metadata.

## Valores que Santiago debe completar

| Variable | Tratamiento |
|---|---|
| `DOCKERHUB_USERNAME` | Usuario u organización que contiene las cuatro imágenes. No es contraseña. |
| `RABBITMQ_USERNAME` | Generar para el broker interno. |
| `RABBITMQ_PASSWORD` | Secreto robusto, distinto de cualquier placeholder. |
| `CURSOS_DB_PASSWORD` | Secreto para la base H2 de cursos. |
| `INSCRIPCIONES_DB_PASSWORD` | Secreto para la base H2 de inscripciones. |

`DOCKERHUB_TOKEN` no pertenece a `.env.ec2`. Solo se usa en GitHub Actions y, si las imágenes son privadas, en un `docker login --password-stdin` ejecutado manualmente en EC2.

`EC2_HOST`, `EC2_USER` y `EC2_SSH_KEY` tampoco pertenecen a `.env.ec2`; permanecen exclusivamente en GitHub Secrets.

## Contenido esperado

Santiago puede copiar `.env.ec2.example` y reemplazar únicamente los marcadores `CHANGE_ME_*` y `DOCKERHUB_USER`. El resultado debe tener esta forma:

```dotenv
DOCKERHUB_USERNAME=<COMPLETAR_USUARIO_U_ORGANIZACION>
IMAGE_TAG=latest

AZURE_ISSUER_URI=https://duocssaezcloudnative.b2clogin.com/c1b3705d-e099-41bf-9501-b1a51343af10/v2.0/
AZURE_JWK_SET_URI=https://duocssaezcloudnative.b2clogin.com/duocssaezcloudnative.onmicrosoft.com/b2c_1_guias_signupsignin/discovery/v2.0/keys
AZURE_CLIENT_ID=75d470b0-2bfb-4989-9d81-aa1805f3b546
AZURE_ROLES_CLAIM=extension_RolGuia

RABBITMQ_USERNAME=<GENERAR_USUARIO_INTERNO>
RABBITMQ_PASSWORD=<GENERAR_SECRETO_RABBITMQ>
RABBITMQ_LISTENER_AUTO_STARTUP=true

CURSOS_DB_URL=jdbc:h2:file:/data/cursos
CURSOS_DB_USERNAME=sa
CURSOS_DB_PASSWORD=<GENERAR_SECRETO_CURSOS>
INSCRIPCIONES_DB_URL=jdbc:h2:file:/data/inscripciones
INSCRIPCIONES_DB_USERNAME=sa
INSCRIPCIONES_DB_PASSWORD=<GENERAR_SECRETO_INSCRIPCIONES>

FRONTEND_ALLOWED_ORIGIN=https://eft-cursos-ssaez.duckdns.org
FRONTEND_PORT=8088

B2C_CLIENT_ID=b91690e3-e8f3-435c-8aaa-6e8eb7f263ed
B2C_AUTHORITY=https://duocssaezcloudnative.b2clogin.com/duocssaezcloudnative.onmicrosoft.com/B2C_1_guias_signupsignin
B2C_KNOWN_AUTHORITY=duocssaezcloudnative.b2clogin.com
B2C_REDIRECT_URI=https://eft-cursos-ssaez.duckdns.org/
B2C_SCOPE=https://duocssaezcloudnative.onmicrosoft.com/75d470b0-2bfb-4989-9d81-aa1805f3b546/access_as_user
BFF_BASE_URL=
```

S3 está deshabilitado mediante `AWS_S3_UPLOAD_ENABLED=false` en el compose. `AWS_REGION` y `AWS_S3_BUCKET` son opcionales y los defaults no activan ninguna llamada AWS. No se requieren access keys.

## Creación manual en EC2

```bash
cd /opt/eft-cursos
cp .env.ec2.example .env.ec2
chmod 600 .env.ec2
nano .env.ec2
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml config -q
```

Antes de guardar, Santiago debe reemplazar todos los marcadores:

```bash
grep -nE 'CHANGE_ME|DOCKERHUB_USER|<COMPLETAR|<GENERAR' /opt/eft-cursos/.env.ec2
```

El comando `grep` debe terminar sin resultados. No debe imprimirse el contenido completo de `.env.ec2` en logs, capturas ni mensajes.

## Configuración runtime del frontend

`npm run build` copia el `public/config.js` local al artefacto, pero la imagen de producción lo reemplaza al arrancar:

1. Compose entrega las variables `B2C_*` y `BFF_BASE_URL` al contenedor frontend.
2. `40-render-config.sh` aplica `envsubst` sobre `config.template.js`.
3. Se genera `/usr/share/nginx/html/config.js`.
4. Nginx sirve ese archivo con `Cache-Control: no-store`.
5. `BFF_BASE_URL` vacío hace que la SPA llame `/api/...` en el mismo origen.
6. Nginx reenvía `/api/` a `http://bff-service:8080`.

Por tanto, no es necesario modificar `frontend/public/config.js` para EC2.

El dominio HTTPS, la configuración de DuckDNS y Caddy, los puertos del Security
Group y el procedimiento ante un cambio de IP se documentan en
[`informe-pipeline-deploy-ec2.md`](informe-pipeline-deploy-ec2.md). Este archivo
solo describe variables de aplicación; no debe contener el token de DuckDNS,
claves SSH ni secretos de GitHub o EC2.
