# Revisión funcional posterior al MegaZIP 32

**Fuente actual:** `catequesis_app-main (3).zip`; comparación con `catequesisapp(2).zip` (antigua). Código inspeccionado, sin acceso al teléfono, al backend autenticado ni a Android SDK en este entorno.

## Comprobaciones de la copia recibida

- Contiene `network_security_config.xml`, la raíz oficial SSL.com RSA 2022, configuración HTTPS, nuevo formulario Contactar y mensaje específico para correo duplicado.
- Es la base de MegaZIP 33; no hay que aplicar 31/32 por separado sobre esta copia.
- La app antigua enviaba formularios de oración por `email_contact/register_contact_email`; la copia 32 ya los lleva al mismo contrato. La recepción todavía debe verificarse en backoffice.

## Errores encontrados y corregidos en MegaZIP 33

| Flujo | Comportamiento detectado | Corrección |
| --- | --- | --- |
| Registro de cuenta | Si el POST de registro tenía éxito pero fallaba el inicio automático, la app permanecía en «Crear cuenta». Un segundo toque provocaba correo duplicado. | Distingue cuenta creada del inicio fallido: pasa a «Iniciar sesión» y avisa al usuario sin repetir el registro. |
| Respuestas PHP (Cursos, acceso, Contactar, chat, progreso y calendario) | Algunos endpoints volcaban `message` del servidor sin filtrar, exponiendo SQLSTATE, avisos de PHP o rutas del hosting. | Mensajes legibles conservando respuestas normales; se ocultan detalles internos y errores técnicos de conexión. |
| Chat Firebase | Se podían ver mensajes de excepción Firebase tal cual. | Explica en español los fallos de carga/publicación y mantiene el reintento pendiente existente. |
| Cuenta: cursos aprobados | El perfil antiguo permitía consultar cursos aprobados y desplegar sus clases; el nuevo solo ofrecía sincronización. | Vuelve a listar los cursos aprobados y las clases relacionadas, después de sincronizar cuando hay conexión. |
| Cuenta: certificados | La app antigua listaba cursos aprobados, sincronizaba y abría `/certificate/certificate_course.php?user=...&course=...` por HTTP. | Lista cursos aprobados; tras sincronizar, abre la misma ruta y parámetros por HTTPS. Verificar generación real y posibles redirecciones del sitio. |
| Ayúdanos | Faltaban oración, solicitud de catequista y donaciones. | Restaura texto antiguo de oración, comprobación de **todos** los cursos aprobados, confirmación/POST `request_cateq/register` con `Authorization`, respuestas `status=1/2` y enlace PayPal antiguo. Probar con dos cuentas de prueba. |

## Funciones aún sin equivalencia completa

1. **Perfil:** editar datos y foto, eliminar cuenta y eliminar chat correspondiente, reiniciar progreso local con confirmación. Estas operaciones tocan datos de cuenta; requieren comprobar las respuestas actuales del backend y Firebase antes de habilitarlas.
2. **Información y Ayuda:** pantalla de versión, actualización explícita de contenido, política de privacidad, «Para qué sirve», los tres textos de ayuda, enlace y contenidos de cada opción.
3. **Ajustes:** activar noticias, descarga explícita sin internet, tamaño del texto, opciones extra de lectura y sonido de juegos. El catálogo actual tiene caché parcial, que no equivale a la descarga completa de la app antigua.
4. **Idiomas:** la vieja utilizaba `LocaleHelper` y recursos en varios idiomas; la nueva conserva muchas cadenas en español.
5. **Inicio Google/Facebook:** ausente en la nueva; los clientes OAuth anteriores requieren renovación según los errores observados en la web.
6. **Menú global y navegación:** la app antigua mostraba opciones desde el menú principal; el menú actual vive dentro de Cursos, y aún faltan entradas de Información, Ayuda y Ajustes.
7. **Paridad de juegos y notificaciones:** siguen pendientes ensayos con datos reales: scoring, repetición de test, efectos, sonidos, PDFs y chat con dos usuarios.

**Importante:** no interpretar este inventario como certificación de que lo ya implementado funciona en producción. Pruebas de compilación y endpoints POST/archivo de certificado requieren Android Studio y el hosting real.

## Prueba inmediata tras aplicar ZIP 33

1. Android Studio: Sync Project with Gradle Files, Build > Make Project y Run sobre Android 13.
2. Cuenta: correo nuevo -> Crear cuenta -> inicio; repetir el registro -> mensaje legible; probar contraseña incorrecta y sin internet.
3. Cuenta con curso aprobado: «Mis cursos aprobados», clases y «Mis certificados»; descargar y verificar HTTPS y contenido del archivo.
4. Cursos > menú > «Ayúdanos»: leer oración; verificar cuenta incompleta sin enviar; en una cuenta que haya aprobado todos, confirmar la solicitud y comprobar backoffice.
5. Chat/Contacto/Calendario: comprobar que errores y respuestas normales no muestran SQL ni detalles internos.

Para pasar a la réplica visual faltan tanto los flujos enumerados como las pruebas anteriores.
