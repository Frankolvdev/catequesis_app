# Auditoría funcional Catequesis: aplicación antigua y nueva

**Referencias:** `catequesisapp(2).zip` (Java/XML antiguo) y `catequesis_app-main (2).zip` (Kotlin/Compose nuevo). Inspección estática de 164 clases Java y 32 archivos Kotlin de `app/src/main/java`, menús, recursos y manifiestos. No equivale a pruebas de todos los flujos con backend, Firebase y dispositivo.

**Resultado:** todavía no hay paridad funcional. Los juegos y cursos tienen implementaciones en el proyecto nuevo, pero hay funciones de perfil, menús secundarios, ajustes y ayuda que aún no aparecen. Antes de ajustar todas las interfaces hay que cerrar estos huecos y probarlos con una cuenta real de prueba.

| Flujo antiguo y referencia | Estado en el ZIP nuevo recibido | Conclusión / siguiente verificación |
| --- | --- | --- |
| Inicio, año/género, paleta (`InitConfig`, `PreferencesStorage`) | `InitialSetupScreen`/`ProfileSettings` | Implementado en código; comprobar persistencia tras reiniciar y equivalencia de textos/colores. |
| Cursos, clases, temas, lecciones, anexos, metas y actividades (`CourseFragment`, `ClassCourseActivity`, `ContentCatequesisActivity`) | `CatalogScreen`, `CourseRepository`, `ThemeReadingScreen` | Hay flujos correspondientes; pendiente recorrido real con distintos cursos, progreso y recursos descargados. |
| Examen y actividades: imágenes, Quiz, match, verdadero/falso, ahorcado, enigma, crucigrama, pizarra | Pantallas específicas en `game/` y `quiz/` | Presentes en código; comparar scoring, sonidos, reinicios, confirmaciones y navegación juego por juego. |
| Progreso local, cursos aprobados y sincronización (`ClassCourse`, `TestClass`, `CourseApproved`) | `ClassProgressStore`, `ProgressSyncRepository` | Hay almacenamiento y sincronización; comprobar equivalencia de aprobaciones, dispositivo antiguo y dos dispositivos. |
| Crear cuenta: correo repetido (`User.registerUserVolley`) | `AuthRepository` imprimía `message` sin filtrar | El backend rechaza duplicado; la app antigua solo registraba el error en Logcat. Corregido en MegaZIP 32 con texto legible y sin SQL. |
| Validación de formulario (`UserRegistration`) | `AccountScreen` | Existen validaciones, pero no coinciden la ubicación ni todos los textos; pendiente revisar campo por campo en la etapa visual/UX. |
| Contactar (`ContactsActivity`, `Email_contact`) | Ausente en el ZIP recibido | En MegaZIP 32 se restaura el formulario y POST HTTPS `email_contact/register_contact_email`; requiere prueba real del servidor. |
| Oraciones por intención / al Papa (`AskPrayersFragment`) | Antes del fix abría correo del teléfono | En MegaZIP 32 usa Contactar y los asuntos originales; verificar recepción en backoffice. |
| Formación desde Fe (`VelaFragment`) | Antes del fix abría correo del teléfono | En MegaZIP 32 abre Contactar con el tema «Deseo asistir a charlas o retiros». |
| Calendario litúrgico (`CalendarLiturgicalFragment`) | `FaithScreen` consulta API | Presente; validar años sin eventos, idioma, orden y distintas fechas. |
| Noticias RSS (`RssFragment`) | `NewsScreen` | Presente con fuente/caché actuales; contrastar enlace, actualización y modo sin internet. |
| Chat PHP/Firebase (`ChatListFragment`, `ChatFragment`, `Message`) | `ChatScreen`/`ChatRepository` | Hay integración; falta probar conversaciones anteriores, alta nueva, recepción y reintentos con dos cuentas. |
| Perfil: ver/editar usuario/foto (`ProfileActivity`, `UserControlActivity`, `UserDataFragment`, `PersonDataFragment`) | `AccountScreen` solo muestra sesión y sincronización | **Falta editar datos/foto y subpantallas antiguas**. No confundir mostrar email con perfil completo. |
| Perfil: cursos aprobados y certificados (`CourseApprovedActivity`, `CertificateCourseActivity`) | Datos de aprobación local, sin pantallas de lista/certificados equivalentes | **Faltan acceso y generación/visualización de certificados**. |
| Perfil: reiniciar progreso del dispositivo (`ProfileActivity.onRestartCourse`) | Sin control equivalente | **Falta**; el viejo borraba tres archivos locales tras confirmación. La app nueva además sincroniza con servidor: diseñar sin borrar progreso remoto involuntariamente. |
| Perfil: eliminar cuenta (`ProfileActivity.onDeleteUser`, `User.deleteUserVolley`) | Sin control equivalente | **Falta**; exige API, confirmación, cierre de sesión y limpieza del chat asociado. |
| Menú principal (`menu_main.xml`) | Catálogo ofrece solo Cuenta y, tras MegaZIP 32, Contactar | Faltan accesos a Información, Ajustes, Ayuda, Ayúdanos; en la vieja el menú era global. |
| Información: versión, actualizar contenido, privacidad, para qué sirve (`InformationActivity`) | Sin equivalentes completos | **Faltan**; actualización de contenido debe refrescar datos sin eliminar progreso. |
| Ayuda: funcionamiento, aprobación, certificado (`HelpActivity`) | Sin pantallas equivalentes | **Faltan** los tres contenidos y navegación. |
| Ayúdanos: oración, solicitud de catequista, donación (`HelpmeActivity`, `Request_cateq`) | Sin equivalentes | **Faltan** solicitud autenticada con comprobación de cursos y estados previos; oración y enlace de donación. |
| Ajustes: noticias, actualizar app, descargas, tamaño de letra, lectura, sonido (`SettingActivity`) | Sin pantalla equivalente | **Faltan** ajustes y efecto real en las pantallas; revisar las preferencias antiguas y adaptación Android actual. |
| Devocionario y enlaces de oración (`DevocionarioActivity`, `IWantToPrayFragment`) | `PrayerScreen` tiene páginas HTML locales/enlaces | Parcial; comprobar que todos los textos se muestran y rutas externas vigentes. |
| Idioma/región (`LocaleHelper` y recursos alternativos) | La app nueva mantiene muchos textos en español | **Falta** equivalencia multilingüe si sigue siendo parte del producto. |
| Google/Facebook login (`LoginActivity`) | No integrado en `AccountScreen` | **Falta** y los clientes OAuth de web anteriores estaban caídos; requiere credenciales de proyecto activas y HTTPS. |
| SSL en Android 13 | El ZIP nuevo enviado no incluye MegaZIP 31 | MegaZIP 32 incluye de nuevo la CA oficial específica de Catequesis y el manifiesto, de forma acumulativa. |

## Límite y orden de cierre

1. Aplicar MegaZIP 32 y probar Cursos, registro repetido y envío de contacto real (revisar el mensaje en backoffice). La raíz adicional del ZIP 31 va incluida.
2. Completar funciones faltantes de Perfil (edición, aprobados, certificado, reinicio, eliminación) y probar cada operación contra API/Firebase.
3. Completar Información, Ajustes, Ayuda, Ayúdanos e idioma; probar estados sin internet y persistencia.
4. Probar el recorrido de todos los juegos, chat y sincronización; solo entonces entrar a la réplica visual pantalla por pantalla.

Ningún endpoint que escriba datos (contacto, borrado, aprobación, solicitud) se puede declarar probado solo con inspección de código. Para capturar diferencias visibles se necesitan ejecuciones en Android con el servidor actual.
