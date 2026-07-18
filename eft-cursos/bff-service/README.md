# BFF de cursos

## Encabezados de respuestas downstream

El BFF reconstruye las respuestas recibidas de `cursos-service` e
`inscripciones-service`. Por ese motivo no reenvía encabezados hop-by-hop ni
`Content-Length`: esos encabezados pertenecen a una conexión concreta y
Spring/Tomcat debe generarlos para la respuesta que entrega el BFF.

La copia directa de `Transfer-Encoding` provocaba que el BFF expusiera dos
variantes del encabezado (`Transfer-Encoding` y `transfer-encoding`). Nginx
rechazaba esa respuesta como inválida y devolvía HTTP 502. La sanitización común
se aplica a todos los endpoints proxy, compara nombres sin distinguir
mayúsculas de minúsculas y conserva encabezados funcionales como
`Content-Type`.
