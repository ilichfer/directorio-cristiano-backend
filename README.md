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
