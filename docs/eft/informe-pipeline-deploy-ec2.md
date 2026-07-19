# Informe de pipeline y despliegue EC2 del sistema EFT Cursos

Fecha de validación local inicial: 16 de julio de 2026. Actualización B2C/EC2: 17 de julio de 2026. Documentación HTTPS: 18 de julio de 2026.

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
- `docs/eft/env-ec2-valores-publicos.md`: plantilla explicada de valores públicos y campos sensibles pendientes.
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

- `frontend`, con Nginx y puerto de host configurable; la plantilla actual usa 8088 y Compose conserva el fallback 80;
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
- activación S3, región y bucket privado para comprobantes de inscripción;
- origen público del frontend;
- Client ID, authority, known authority, redirect URI y scope públicos de la SPA;
- `BFF_BASE_URL` vacío para usar el proxy del mismo origen.

No se guardan AWS access keys. En EC2, `inscripciones-service` usa
`DefaultCredentialsProvider` y el perfil IAM asociado a la instancia.

## 9. Dominio, DuckDNS y terminación HTTPS

El dominio definitivo de la EFT es:

`https://eft-cursos-ssaez.duckdns.org`

El subdominio configurado en DuckDNS es `eft-cursos-ssaez`. Su registro A debe
apuntar a la IPv4 pública vigente de la instancia EC2. El Learner Lab puede
asignar una IP diferente después de detener o reiniciar el entorno; cuando eso
ocurra, el registro debe actualizarse desde el panel autenticado de DuckDNS.
El token de DuckDNS es secreto y no debe incluirse en este repositorio, comandos
capturados, logs ni evidencias.

Caddy termina TLS y se configura en la instancia mediante
`/etc/caddy/Caddyfile`. La regla relevante es:

```caddyfile
eft-cursos-ssaez.duckdns.org {
    reverse_proxy 127.0.0.1:8088
}
```

Caddy se administra como servicio systemd habilitado para iniciar con la
instancia. Su estado se comprueba sin mostrar información sensible:

```bash
sudo systemctl is-enabled caddy
sudo systemctl is-active caddy
```

Los resultados esperados son `enabled` y `active`, respectivamente. Esta
documentación no sustituye la evidencia tomada directamente desde EC2.

La preparación operativa de `/opt/eft-cursos` requiere mantener `.env.ec2` con
permisos 600, completar allí las credenciales internas, autenticar Docker Hub
si las imágenes son privadas y registrar en Azure B2C el redirect URI exacto
`https://eft-cursos-ssaez.duckdns.org/`.

El workflow copia por SCP `docker-compose.ec2.yml` y `.env.ec2.example`. Nunca copia ni reemplaza `.env.ec2`. La configuración Nginx y la plantilla frontend ya viajan dentro de la imagen frontend.

## 10. Puertos requeridos

`FRONTEND_PORT=8088` hace que Compose publique el puerto 80 del contenedor
frontend en el puerto 8088 del host mediante `${FRONTEND_PORT:-80}:80`. Nginx
dentro del contenedor sirve la SPA y reenvía `/api/` al BFF. Caddy escucha
públicamente en 80/443 y reenvía el tráfico HTTPS a `127.0.0.1:8088`.

Puertos de entrada recomendados en el Security Group:

- TCP 22: solo desde la IP administrativa de Santiago o del runner si se adopta esa política;
- TCP 443: público para `https://eft-cursos-ssaez.duckdns.org`;
- TCP 80: público para la redirección HTTP a HTTPS y la validación ACME de Caddy;
- TCP 8088: no requiere una regla pública; Caddy lo consume desde la misma instancia.

No deben abrirse públicamente 8080, 8081, 8082, 5672 ni 15672. Esos servicios se comunican por la red Docker. RabbitMQ Management queda instalado por exigencia académica de la imagen, pero no expuesto.

## 11. Configuración B2C para la URL pública actual

Los valores públicos confirmados son:

- issuer exacto: `https://duocssaezcloudnative.b2clogin.com/c1b3705d-e099-41bf-9501-b1a51343af10/v2.0/`;
- JWKS literal de la metadata: `https://duocssaezcloudnative.b2clogin.com/duocssaezcloudnative.onmicrosoft.com/b2c_1_guias_signupsignin/discovery/v2.0/keys`;
- audience/API Client ID: `75d470b0-2bfb-4989-9d81-aa1805f3b546`;
- SPA Client ID: `b91690e3-e8f3-435c-8aaa-6e8eb7f263ed`;
- scope: `https://duocssaezcloudnative.onmicrosoft.com/75d470b0-2bfb-4989-9d81-aa1805f3b546/access_as_user`;
- redirect URI: `https://eft-cursos-ssaez.duckdns.org/`;
- origen CORS, sin barra final: `https://eft-cursos-ssaez.duckdns.org`.

`GESTION_GUIAS` y `DESCARGA_GUIAS` son roles leídos desde `extension_RolGuia`; no son scopes OAuth2. Los aliases `INSTRUCTOR` y `ESTUDIANTE` siguen admitidos internamente.

El issuer contiene el GUID del tenant porque ese es el valor literal publicado por la metadata OIDC y emitido en `iss`. La policy aparece en la URL de metadata, en el JWKS y normalmente en `tfp`/`acr`, no dentro del issuer. Los resource servers validan issuer exacto, firma y audience, pero actualmente no añaden un validador explícito del claim de policy.

Si posteriormente cambia el dominio DNS, Azure B2C, `B2C_REDIRECT_URI` y `FRONTEND_ALLOWED_ORIGIN` deben actualizarse juntos. No se modificó ninguna configuración remota Azure durante esta preparación.

## 12. Condiciones que controlan el deploy

- `push` a `eft-cursos`: ejecuta pruebas y publica imágenes; no despliega.
- `push` a `main`: ejecuta pruebas y publica imágenes; no despliega.
- `workflow_dispatch` con `deploy=false`: prueba y publica, sin deploy.
- `workflow_dispatch` con `deploy=true`: habilita deploy desde la revisión seleccionada solamente si `build_images` termina correctamente.

El job usa el environment GitHub `production`. Se recomienda configurarlo con required reviewers como segunda aprobación humana antes de acceder a secretos EC2. Ningún `push` ejecuta el job `deploy`.

El deploy usa siempre `sha-GITHUB_SHA`, ejecuta `docker compose pull`, `up -d --remove-orphans` y `ps`. No usa el tag mutable `latest` para una ejecución automatizada.

## 13. Pruebas ejecutadas y resultados

- parseo de `.github/workflows/deploy.yml` con SnakeYAML: correcto;
- `docker compose --env-file eft-cursos/.env.ec2.example -f eft-cursos/docker-compose.ec2.yml config -q`: correcto;
- metadata OIDC pública: issuer y `jwks_uri` obtenidos correctamente;
- JWKS de metadata y variante `?p=`: una clave en cada respuesta y mismo `kid`;
- los tres resource servers locales coincidieron en issuer, JWKS, audience y claim;
- renderizado con `IMAGE_TAG=sha-validation`: las cuatro imágenes EFT usan el tag SHA y RabbitMQ conserva su imagen oficial;
- `npm test`: 8 pruebas aprobadas;
- `npm run build`: correcto, 146 módulos transformados;
- reactor `eft-cursos/pom.xml test`: BUILD SUCCESS, 44 pruebas aprobadas (6 cursos, 27 inscripciones y 11 BFF);
- build local de `bff`, `cursos`, `inscripciones` y `frontend`: correcto;
- ejecución temporal de la imagen frontend: estado `healthy`, `/healthz` HTTP 200 y `config.js` generado con los valores públicos EC2;
- `git diff --check`: correcto.

No se publicó ninguna imagen, no se ejecutó el workflow remoto y no se intentó conectar a EC2.

Santiago confirmó por separado que el perfil IAM de EC2 permite
`sts get-caller-identity`, listar el bucket `guia-despacho-ssaezv-dcn` y subir
un archivo de prueba con AWS CLI. Esa evidencia valida el host y el bucket, pero
la primera carga realizada desde el contenedor `inscripciones-service` debe
comprobarse manualmente después de reconstruir y desplegar la imagen nueva.

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
curl --fail --silent http://127.0.0.1:8088/healthz
sudo systemctl is-enabled caddy
sudo systemctl is-active caddy
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

- `.env.ec2` debe conservarse exclusivamente en EC2, con permisos 600 y fuera de capturas o logs;
- el redirect URI HTTPS debe permanecer registrado en Azure B2C exactamente con la barra final;
- el dominio depende de que DuckDNS apunte a la IPv4 pública vigente de EC2; un cambio de IP interrumpe DNS, HTTPS y el despliegue hasta actualizar DuckDNS y `EC2_HOST`;
- H2 con volumen es suficiente para la demostración académica, pero no reemplaza una base administrada para alta disponibilidad;
- `latest` es mutable, aunque el deploy automatizado mitiga este riesgo usando el tag SHA;
- si Docker Hub es privado, EC2 necesita autenticación de lectura;
- las Actions de terceros están referenciadas por tag y no por SHA inmutable;
- aunque solo la ejecución manual con `deploy=true` habilita deploy, conviene proteger `main` y configurar aprobación en el environment `production`;
- Spring valida issuer, firma y audience, pero no valida explícitamente `tfp`/`acr` contra `B2C_1_guias_signupsignin`;
- el pipeline no puede comprobar localmente el contenido correcto de `.env.ec2` ni el registro remoto del redirect URI.

## 16. Operaciones Git

Durante este trabajo no se ejecutó commit, push, merge, checkout, restore, discard ni otra operación Git de escritura.

## 17. Secretos y configuración local

No se guardaron secretos reales, claves privadas, JWT, tokens de acceso, contraseñas reales ni AWS keys. El archivo `.env.ec2.example` contiene exclusivamente nombres y placeholders.

No se modificó `eft-cursos/frontend/public/config.js`; la configuración cloud se genera dentro del contenedor frontend desde `config.template.js`.

## 18. Auditoría de configuración EC2 y B2C del 17 de julio de 2026

### Error corregido

`.env.ec2.example` confundía el rol `GESTION_GUIAS` con un scope. Se reemplazó por el scope real `access_as_user` y se añadieron comentarios que separan valores públicos, datos del operador y secretos que deben generarse.

### Fuentes de verificación

- `frontend/public/config.js`: SPA Client ID, authority, known authority y scope previamente validados;
- los tres contenedores resource server locales: issuer, JWKS, audience y claim activos, consultando solo esas cuatro variables públicas;
- metadata OIDC pública de `B2C_1_guias_signupsignin`: issuer y `jwks_uri` literales;
- ambos endpoints JWKS, que respondieron una clave y el mismo `kid`;
- configuración Spring Security: `JwtValidators.createDefaultWithIssuer`, `NimbusJwtDecoder.withJwkSetUri`, `AudienceValidator` y `JwtRolesConverter`.

### Configuración runtime del frontend

El build Vite incluye inicialmente `public/config.js`, pero no es la configuración final de EC2. Al arrancar el contenedor, Compose entrega las variables públicas al frontend y `40-render-config.sh` reemplaza `/usr/share/nginx/html/config.js` usando `config.template.js`. Nginx sirve el resultado con `Cache-Control: no-store`.

`BFF_BASE_URL` permanece vacío. La SPA llama rutas `/api/...` del mismo origen y Nginx las reenvía internamente a `http://bff-service:8080`. Los backends usan `http://cursos-service:8081` y `http://inscripciones-service:8082`; no usan `localhost`.

### Compose EC2 confirmado

- usa `DOCKERHUB_USERNAME` e `IMAGE_TAG`;
- entrega issuer, JWKS, audience y claim a los tres backends;
- entrega Client ID SPA, authority, known authority, redirect URI, scope y BFF base al frontend;
- publica únicamente `${FRONTEND_PORT:-80}:80`;
- no publica RabbitMQ, BFF, cursos ni inscripciones;
- habilita S3 para `inscripciones-service` mediante `.env.ec2`; el valor por defecto de la aplicación y Compose sigue siendo `false`.

La prueba runtime detectó que BusyBox `wget` no alcanzaba Nginx mediante `localhost` en esta imagen Alpine, aunque `127.0.0.1` sí respondía. Se corrigieron los healthchecks del Dockerfile y del compose a `http://127.0.0.1/healthz`; la imagen reconstruida alcanzó el estado `healthy`.

### Archivo remoto requerido

La estructura exacta y los valores públicos se documentan en `docs/eft/env-ec2-valores-publicos.md`. Santiago debe completar manualmente:

- `DOCKERHUB_USERNAME`;
- `RABBITMQ_USERNAME`;
- `RABBITMQ_PASSWORD`;
- `CURSOS_DB_PASSWORD`;
- `INSCRIPCIONES_DB_PASSWORD`.

No se creó `.env.ec2` en el repositorio. El workflow seguirá copiando `.env.ec2.example`: es una referencia no sensible y su actualización no toca el archivo real. Mantener la copia ayuda a diagnosticar cambios de esquema de variables; el operador no debe volver a ejecutar `cp` sobre un `.env.ec2` ya creado.

### Confirmaciones

- no se leyeron ni mostraron valores de GitHub Secrets;
- no se guardaron contraseñas, claves privadas, JWT ni tokens;
- no se modificó `frontend/public/config.js`;
- no se copiaron archivos a EC2;
- no se ejecutó GitHub Actions;
- no hubo conexión SSH/SCP ni otra conexión a EC2;
- no se ejecutó commit, push ni otra operación Git de escritura.

## 19. Procedimiento ante cambio de IP pública de EC2

1. Iniciar el Learner Lab y esperar que la instancia EC2 quede disponible.
2. Obtener la nueva **IPv4 pública** desde la consola de EC2. No usar la IP
   privada ni un hostname antiguo.
3. Actualizar el subdominio `eft-cursos-ssaez` desde el panel de DuckDNS para
   que apunte a esa IPv4. No copiar ni capturar el token de DuckDNS.
4. Actualizar solamente el secreto `EC2_HOST` en GitHub Actions con el nuevo
   host o IPv4. `EC2_USER` y `EC2_SSH_KEY` no cambian por una reasignación de IP
   y no deben modificarse.
5. Verificar la propagación DNS desde PowerShell:

   ```powershell
   Resolve-DnsName eft-cursos-ssaez.duckdns.org -Type A
   ```

   La dirección devuelta debe coincidir con la IPv4 pública vigente de EC2.
6. Verificar la respuesta HTTPS y el certificado:

   ```powershell
   curl.exe -I https://eft-cursos-ssaez.duckdns.org/
   ```

7. Solo después de esas comprobaciones, ejecutar manualmente el workflow con
   `deploy=true` si es necesario desplegar una nueva revisión.

El dominio, `B2C_REDIRECT_URI` y `FRONTEND_ALLOWED_ORIGIN` permanecen iguales
cuando solo cambia la IP; no se deben reemplazar por la dirección numérica.

## 20. Evidencia manual del workflow CI/CD

El workflow que debe respaldar la evidencia se llama
**EFT Cursos - Build, Publish and Deploy** y está definido en
`.github/workflows/deploy.yml`. Su ejecución exitosa debe mostrar:

1. `Test backend and frontend`: pruebas Maven, pruebas npm y build Vite;
2. cuatro jobs verdes `Build and push ...`: construcción y publicación de las
   imágenes BFF, cursos, inscripciones y frontend;
3. `Deploy EFT stack on EC2`: validación de secretos, copia de los archivos de
   despliegue y ejecución de Docker Compose, únicamente para una ejecución
   manual con `deploy=true`.

El repositorio no contiene un enlace ni un ID verificable de una ejecución
remota concreta, por lo que no se registra uno en este informe. Santiago debe
agregar manualmente capturas de una ejecución realmente verde. Ubicación
recomendada si se incorporan como evidencia versionada:

- `docs/eft/evidencias/workflow-cicd-resumen-verde.png`: vista completa de los
  jobs y su estado;
- `docs/eft/evidencias/workflow-cicd-deploy-verde.png`: detalle del job de
  despliegue, sin expandir ni mostrar secretos.

Después de guardar las capturas, pueden insertarse inmediatamente bajo este
párrafo con enlaces Markdown relativos. La captura debe dejar visibles el
nombre del workflow, la rama/revisión, los jobs verdes y la fecha; puede ocultar
el actor u otros datos personales. No debe mostrar tokens, claves SSH,
contraseñas, el contenido de `.env.ec2` ni valores de GitHub Secrets.
