# Informe de pipeline y despliegue EC2 del sistema EFT Cursos

Fecha de validación local: 16 de julio de 2026.

## 1. Estado inicial del workflow

El workflow heredado tenía un único job llamado `deploy` y se ejecutaba solamente ante `push` a `eft-cursos`. Sus pasos eran:

1. checkout;
2. Java 17;
3. `./mvnw clean package` sobre el proyecto raíz;
4. login en Docker Hub con `DOCKERHUB_USERNAME` y `DOCKERHUB_TOKEN`;
5. build y push de una sola imagen desde el Dockerfile raíz;
6. conexión SSH y ejecución de un único contenedor en EC2.

El `pom.xml` raíz corresponde al artefacto monolítico `guia-despacho`. No compilaba explícitamente el reactor `eft-cursos/pom.xml`, cuyos módulos son `cursos-service`, `inscripciones-service` y `bff-service`.

## 2. Causa de `missing server host`

`appleboy/ssh-action` recibió vacío el parámetro `host`, porque `${{ secrets.EC2_HOST }}` no estaba definido o no estaba disponible para esa ejecución. El error ocurre antes de iniciar una conexión SSH.

El workflow nuevo valida `EC2_HOST`, `EC2_USER` y `EC2_SSH_KEY` antes de SCP o SSH. Si falta cualquiera, el job termina indicando solamente el nombre faltante, sin imprimir valores sensibles ni intentar conectarse.

## 3. Referencias heredadas al monolito

Se retiraron del workflow las siguientes referencias antiguas:

- build desde el Dockerfile raíz;
- imagen `DOCKERHUB_USERNAME/guia-despacho:latest`;
- contenedor único `guia-despacho`;
- `docker stop`, `docker rm` y `docker run` monolíticos;
- archivo `/opt/guia-despacho/app.env`;
- volumen `/mnt/efs/guias`;
- despliegue de un único puerto 8080 sin los demás componentes EFT.

El Dockerfile y el `pom.xml` monolíticos siguen existiendo en la raíz porque pertenecen al proyecto anterior, pero el pipeline EFT ya no los utiliza.

## 4. Archivos modificados o añadidos

- `.github/workflows/deploy.yml`: pruebas, publicación multiimagen y deploy condicionado.
- `.gitignore`: permite versionar `.env.ec2.example`, manteniendo ignorado `.env.ec2`.
- `eft-cursos/.dockerignore`: excluye dependencias, builds, variables y archivos ajenos del contexto Docker.
- `eft-cursos/docker-compose.ec2.yml`: stack cloud basado en imágenes publicadas.
- `eft-cursos/.env.ec2.example`: nombres y placeholders sin valores reales.
- `eft-cursos/frontend/Dockerfile`: build Vite y runtime Nginx.
- `eft-cursos/frontend/nginx.conf`: SPA, health y proxy interno hacia el BFF.
- `eft-cursos/frontend/config.template.js`: plantilla de configuración pública B2C.
- `eft-cursos/frontend/40-render-config.sh`: generación runtime de `config.js`.
- `docs/eft/informe-pipeline-deploy-ec2.md`: este informe.

No se modificó `eft-cursos/frontend/public/config.js`.

## 5. Imágenes Docker definidas

El pipeline construye y publica cuatro imágenes independientes:

| Componente | Imagen |
|---|---|
| BFF | `DOCKERHUB_USERNAME/eft-cursos-bff` |
| Cursos | `DOCKERHUB_USERNAME/eft-cursos-cursos` |
| Inscripciones | `DOCKERHUB_USERNAME/eft-cursos-inscripciones` |
| Frontend | `DOCKERHUB_USERNAME/eft-cursos-frontend` |

Cada imagen recibe dos tags:

- `latest`, útil como referencia manual;
- `sha-GITHUB_SHA`, inmutable para desplegar exactamente la revisión que superó las pruebas.

RabbitMQ no se construye ni publica. El compose usa directamente `rabbitmq:4.1-management`.

## 6. Estrategia del frontend

La SPA se compila con Node 22 y Vite en una etapa Docker. El resultado estático se sirve con Nginx en una imagen independiente.

Nginx publica el único puerto web del stack y reenvía `/api/` a `bff-service:8080`. Por esta razón `BFF_BASE_URL` queda vacío en EC2 y el navegador usa el mismo origen; los puertos backend no deben exponerse públicamente.

Al arrancar el contenedor, `40-render-config.sh` genera `/usr/share/nginx/html/config.js` desde variables públicas B2C. Así se puede cambiar URL, redirect URI o aplicación SPA sin reconstruir la imagen y sin modificar el `public/config.js` local.

## 7. Estructura del compose EC2

`docker-compose.ec2.yml` contiene:

- `frontend`, con Nginx y puerto público configurable, por defecto 80;
- `bff-service`, accesible solo dentro de la red Docker;
- `cursos-service`, accesible solo dentro de la red Docker;
- `inscripciones-service`, accesible solo dentro de la red Docker;
- `rabbitmq:4.1-management`, sin puertos publicados;
- red bridge `eft-network` para comunicación interna y salida necesaria hacia Azure B2C;
- healthchecks para los cinco servicios;
- `restart: unless-stopped`;
- dependencias condicionadas por health;
- volúmenes para RabbitMQ y las bases H2 académicas de cursos e inscripciones.

El compose utiliza únicamente `image:`; no contiene `build:` local.

## 8. Variables y secretos necesarios

### GitHub Secrets

Obligatorios para build y publicación:

- `DOCKERHUB_USERNAME`;
- `DOCKERHUB_TOKEN`, token de acceso y nunca contraseña de la cuenta.

Obligatorios solamente cuando se habilita el job de deploy:

- `EC2_HOST`;
- `EC2_USER`;
- `EC2_SSH_KEY`, clave privada completa.

No se añadieron otros secretos GitHub. Las variables de aplicación se mantienen en `/opt/eft-cursos/.env.ec2`, fuera del repositorio y fuera de los logs de Actions.

### Variables de EC2

El listado completo está en `.env.ec2.example` y comprende:

- usuario Docker Hub y tag de imagen;
- issuer, JWKS, audience y claim de roles;
- credenciales internas RabbitMQ;
- URLs, usuarios y contraseñas de las dos bases H2;
- región y nombre de bucket, con subida S3 deshabilitada;
- origen público del frontend;
- Client ID, authority, known authority, redirect URI y scope públicos de la SPA;
- `BFF_BASE_URL` vacío para usar el proxy del mismo origen.

No se necesitan AWS access keys mientras `AWS_S3_UPLOAD_ENABLED=false`.

## 9. Preparación requerida en EC2

Antes de habilitar el deploy se necesita:

1. instancia activa con IP elástica o DNS estable;
2. Docker Engine y el plugin Docker Compose instalados;
3. usuario SSH autorizado para ejecutar Docker;
4. directorio `/opt/eft-cursos` existente y escribible por `EC2_USER`;
5. archivo `/opt/eft-cursos/.env.ec2` completo y con permisos 600;
6. login previo en Docker Hub si los repositorios de imágenes son privados;
7. Security Group configurado con los puertos mínimos;
8. redirect URI y origen público registrados en Azure B2C.

El workflow copia por SCP `docker-compose.ec2.yml` y `.env.ec2.example`. Nunca copia ni reemplaza `.env.ec2`. La configuración Nginx y la plantilla frontend ya viajan dentro de la imagen frontend.

## 10. Puertos requeridos

Puertos de entrada recomendados en el Security Group:

- TCP 22: solo desde la IP administrativa de Santiago o del runner si se adopta esa política;
- TCP 80: público para la demostración HTTP;
- TCP 443: público cuando se incorpore TLS.

No deben abrirse públicamente 8080, 8081, 8082, 5672 ni 15672. Esos servicios se comunican por la red Docker. RabbitMQ Management queda instalado por exigencia académica de la imagen, pero no expuesto.

## 11. Cambios B2C cuando exista URL pública

Cuando Santiago disponga de IP o DNS deberá:

1. registrar como redirect URI de la SPA la URL exacta, por ejemplo `https://cursos.ejemplo.cl/`;
2. establecer el mismo valor en `B2C_REDIRECT_URI` dentro de `.env.ec2`;
3. establecer el origen exacto, sin barra final, en `FRONTEND_ALLOWED_ORIGIN`;
4. conservar en backend el issuer exacto emitido por la política B2C;
5. conservar el `jwks_uri` exacto de esa política;
6. usar en `AZURE_CLIENT_ID` el Client ID/audience de la API, no necesariamente el Client ID de la SPA;
7. usar en `B2C_CLIENT_ID` el Client ID público de la SPA;
8. conservar `AZURE_ROLES_CLAIM=extension_RolGuia`;
9. verificar que `B2C_SCOPE` sea el scope expuesto por la API, por ejemplo el correspondiente a `GESTION_GUIAS`;
10. si cambia de HTTP a HTTPS, actualizar redirect URI y origen en Azure y `.env.ec2` de forma conjunta.

No se cambió ningún redirect URI real durante esta preparación.

## 12. Condiciones que controlan el deploy

- `push` a `eft-cursos`: ejecuta pruebas y publica imágenes; no despliega.
- `push` a `main`: ejecuta pruebas, publica imágenes y habilita el job de deploy.
- `workflow_dispatch` con `deploy=false`: prueba y publica, sin deploy.
- `workflow_dispatch` con `deploy=true`: prueba, publica y habilita deploy desde la revisión seleccionada.

El job usa el environment GitHub `production`. Se recomienda configurarlo con required reviewers para que incluso un push a `main` necesite aprobación humana antes de acceder a secretos EC2.

El deploy usa siempre `sha-GITHUB_SHA`, ejecuta `docker compose pull`, `up -d --remove-orphans` y `ps`. No usa el tag mutable `latest` para una ejecución automatizada.

## 13. Pruebas ejecutadas y resultados

- parseo de `.github/workflows/deploy.yml` con SnakeYAML: correcto;
- `docker compose --env-file eft-cursos/.env.ec2.example -f eft-cursos/docker-compose.ec2.yml config -q`: correcto;
- renderizado con `IMAGE_TAG=sha-validation`: las cuatro imágenes EFT usan el tag SHA y RabbitMQ conserva su imagen oficial;
- `npm test`: 8 pruebas aprobadas;
- `npm run build`: correcto, 146 módulos transformados;
- `.\mvnw.cmd -f eft-cursos\pom.xml test`: BUILD SUCCESS, 33 pruebas aprobadas;
- build local de `bff`, `cursos`, `inscripciones` y `frontend`: correcto;
- ejecución temporal de la imagen frontend: `/healthz` HTTP 200 y `config.js` generado en runtime;
- `git diff --check`: correcto.

No se publicó ninguna imagen, no se ejecutó el workflow remoto y no se intentó conectar a EC2.

## 14. Comandos exactos para continuar

### Preparar la instancia una vez

Después de instalar Docker Engine y Docker Compose según la distribución de la instancia:

```bash
sudo mkdir -p /opt/eft-cursos
sudo chown "$USER":"$USER" /opt/eft-cursos
cd /opt/eft-cursos
cp .env.ec2.example .env.ec2
chmod 600 .env.ec2
nano .env.ec2
```

Si las imágenes Docker Hub son privadas, iniciar sesión sin escribir el token en la línea de comandos:

```bash
read -rs DOCKERHUB_TOKEN
printf '%s' "$DOCKERHUB_TOKEN" | docker login --username DOCKERHUB_USER --password-stdin
unset DOCKERHUB_TOKEN
```

Validar y arrancar manualmente:

```bash
cd /opt/eft-cursos
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml config -q
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml pull
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml up -d --remove-orphans
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml ps
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml logs --tail 100 --no-color
```

Comprobar desde EC2:

```bash
curl --fail --silent http://localhost/healthz
```

### Ejecutar Actions manualmente

Solo build y publicación:

```bash
gh workflow run deploy.yml --ref eft-cursos -f deploy=false
```

Deploy explícito, únicamente después de preparar EC2 y Azure B2C:

```bash
gh workflow run deploy.yml --ref eft-cursos -f deploy=true
```

### Validaciones locales reproducibles

```powershell
.\mvnw.cmd -f eft-cursos\pom.xml test
Set-Location eft-cursos\frontend
npm test
npm run build
Set-Location ..\..
docker compose --env-file eft-cursos\.env.ec2.example -f eft-cursos\docker-compose.ec2.yml config -q
git diff --check
```

## 15. Riesgos pendientes

- faltan host, usuario, clave SSH e instancia EC2 confirmados;
- falta completar `.env.ec2` con valores reales fuera de Git;
- falta registrar la URL pública en Azure B2C;
- HTTP no protege el tráfico; para una entrega accesible por Internet se recomienda DNS y TLS;
- la IP pública dinámica rompería redirect URI y CORS; se recomienda Elastic IP o DNS estable;
- H2 con volumen es suficiente para la demostración académica, pero no reemplaza una base administrada para alta disponibilidad;
- `latest` es mutable, aunque el deploy automatizado mitiga este riesgo usando el tag SHA;
- si Docker Hub es privado, EC2 necesita autenticación de lectura;
- las Actions de terceros están referenciadas por tag y no por SHA inmutable;
- un push a `main` habilita deploy; debe protegerse `main` y configurar aprobación en el environment `production`;
- el pipeline no puede comprobar localmente la existencia de secretos GitHub ni el estado real de EC2.

## 16. Operaciones Git

Durante este trabajo no se ejecutó commit, push, merge, checkout, restore, discard ni otra operación Git de escritura.

## 17. Secretos y configuración local

No se guardaron secretos reales, claves privadas, JWT, tokens de acceso, contraseñas reales ni AWS keys. El archivo `.env.ec2.example` contiene exclusivamente nombres y placeholders.

No se modificó `eft-cursos/frontend/public/config.js`; la configuración cloud se genera dentro del contenedor frontend desde `config.template.js`.
