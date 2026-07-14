SISTEMA DE GESTIÓN DE GUÍAS DE DESPACHO - EVIDENCIAS

FUNCIONALIDADES VERIFICADAS

- Spring Boot funcional y aplicación Docker healthy.
- RabbitMQ real con exchange directo, cola principal y cola de errores durables.
- Productor y consumidor Java, mensajes JSON persistentes, tres intentos finitos y DLQ.
- Persistencia local en GUIA_PROCESADA_RABBIT e idempotencia por eventoId único.
- Spring Security como OAuth2 Resource Server: issuer, audience, roles, 401, 403 y denyAll.
- Azure AD B2C real con PKCE y roles GESTION_GUIAS y DESCARGA_GUIAS.
- AWS S3 real: bucket privado, subida automática, PDF, AES256 y descarga con hash idéntico.
- Maven: 37 pruebas, 0 fallos, 0 errores, 0 omitidas y BUILD SUCCESS.

CAPTURAS PENDIENTES DE INSERTAR

1. Arquitectura general.
2. docker-compose ps y /actuator/health.
3. Claims B2C redactados de ambos roles.
4. Respuestas Postman 201, 202, 200, 401 y 403.
5. Exchange, cola principal, consumidor y DLQ en RabbitMQ Management.
6. Logs de reintentos, persistencia e idempotencia.
7. S3: key, Content-Type, AES256, PDF y SHA-256.
8. Maven con 37 pruebas y BUILD SUCCESS.

LIMITACIONES REALES

- Oracle Cloud real no fue verificado. Solo H2 local en modo compatible con Oracle.
- API Gateway no fue desplegado.
- EC2 no fue auditado por fallo de credenciales temporales; no se modificaron instancias.
- El video y sus capturas todavía deben grabarse.

EJECUCIÓN MÍNIMA

1. Crear un .env local a partir de .env.example sin versionarlo.
2. Ejecutar: docker-compose up -d --build
3. Verificar: docker-compose ps
4. Verificar: GET http://localhost:8080/actuator/health
5. Ejecutar pruebas: .\mvnw.cmd clean verify
6. Usar la colección Postman con tokens frescos obtenidos mediante PKCE.

SEGURIDAD

Este paquete no debe contener .env, respaldos, target, logs, archivos PKCE/DPAPI,
credenciales AWS, contraseñas, client secrets, access/refresh tokens ni cookies.
Los valores sensibles deben permanecer en variables locales o servicios de secretos.
