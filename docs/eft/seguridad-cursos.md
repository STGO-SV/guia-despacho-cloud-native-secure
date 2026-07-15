# Seguridad académica

Los tres servicios reutilizan el patrón OAuth2 Resource Server:

- issuer configurable;
- JWKS configurable;
- audience validada;
- claim de rol configurable;
- conversión a `ROLE_`;
- sesiones stateless;
- respuestas 401 y 403;
- regla final `denyAll`;
- health público.

Compatibilidad provisional:

| Rol nuevo | Rol B2C actual equivalente | Permiso |
| --- | --- | --- |
| `INSTRUCTOR` | `GESTION_GUIAS` | Gestionar cursos y consultar inscripciones |
| `ESTUDIANTE` | `DESCARGA_GUIAS` | Consultar cursos y crear/consultar inscripción |

No se crean usuarios ni se modifica Azure en esta fase. Para una demostración real se usarán variables locales y tokens temporales, nunca secretos versionados.

