# directorio-cristiano-backend

## Login con Google

Por defecto (`GOOGLE_CLIENT_ID` vacío) el endpoint `POST /api/v1/auth/google` solo acepta la
vía de demostración (`demoEmail`/`demoDisplayName`, ver `GOOGLE_DEMO_MODE`). Para activar el
login real con Google:

1. En [Google Cloud Console](https://console.cloud.google.com/), crea un proyecto (o usa uno
   existente) y configura la pantalla de consentimiento OAuth.
2. Crea una credencial **OAuth client ID** de tipo *Web application*.
3. En "Orígenes de JavaScript autorizados" agrega `http://localhost:5173` (desarrollo) y el
   dominio de producción del frontend.
4. Copia el Client ID a la variable de entorno `GOOGLE_CLIENT_ID` de este backend, y al mismo
   valor en `VITE_GOOGLE_CLIENT_ID` del frontend (`directorio-cristiano-front/.env`).
5. Reinicia ambos servicios. No se necesita client secret: se usa el flujo de ID token de
   Google Identity Services, verificado con `GoogleIdTokenVerifier`.

Detalle completo en `../specs/001-redesign-google-login/quickstart.md` y
`../specs/001-redesign-google-login/contracts/post-auth-google.md`.

## Perfiles y moderación

Detalle completo en `../specs/002-dual-profiles-approval/` (spec, contratos y quickstart).

### Perfiles

Cada cuenta empieza como **cliente** (`user_type = buyer`): explora el directorio, ve los datos
de contacto y deja testimonios. Al aceptar el acuerdo de honestidad pasa a ser también
**emprendedor** (`entrepreneur`) y puede crear negocios, sin perder lo de cliente. Aparte, una
cuenta puede tener el permiso de **moderador** (`users.is_moderator`), que da acceso a
`/api/v1/moderation/**` (`ROLE_MODERATOR`).

### Estados del negocio

| Estado | Qué significa | Visible al público |
|---|---|---|
| `draft` | Borrador; puede estar incompleto | No |
| `in_review` | Enviado; espera la decisión de un moderador | No |
| `published` | Aprobado | Sí |
| `paused` | El dueño lo ocultó; vuelve a publicarse sin revisión | No |
| `suspended` | Un moderador lo suspendió con motivo; el dueño no puede editarlo | No |

Las ediciones de un negocio publicado no tocan la ficha visible: quedan en una solicitud
pendiente (`business_change_requests`) hasta que un moderador la aprueba. Los cambios de precio
se publican al instante. Al suspender, la solicitud pendiente se cancela; al reactivar, el
negocio vuelve con su última versión aprobada y sus testimonios. Cada decisión queda en
`moderation_events`.

### Designar moderadores

Con la variable `APP_MODERATOR_EMAILS` (correos separados por coma). Las cuentas que ya existen
se marcan al arrancar el backend; las que se registren después, al crearse:

```powershell
$env:APP_MODERATOR_EMAILS="moderador@ejemplo.com,otra@ejemplo.com"
```

Alternativa por SQL, para una cuenta ya creada:

```sql
UPDATE users SET is_moderator = true WHERE email = 'moderador@ejemplo.com';
```

Un moderador no puede aprobar, rechazar, suspender ni reactivar sus propios negocios.

### Avisos al emprendedor

Al aprobar, rechazar o suspender, el dueño recibe un aviso. **Sin SMTP configurado, los avisos
solo se escriben en el log** (y el emprendedor los ve en "Mi negocio"). Para enviarlos también
por correo:

| Variable | Default | Uso |
|---|---|---|
| `MAIL_ENABLED` | `false` | `true` para enviar correos |
| `MAIL_HOST` | — | Servidor SMTP |
| `MAIL_PORT` | `587` | Puerto (STARTTLS) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | — | Credenciales SMTP |
| `MAIL_FROM` | — | Remitente; si se omite, lo pone el servidor SMTP |

Si el envío falla, se registra una advertencia y la decisión de moderación igual queda guardada.
