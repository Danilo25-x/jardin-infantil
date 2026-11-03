# Guía para ejecutar Jardín Infantil

Android se conecta a una API Node.js/MongoDB alojada en un repositorio separado. El servidor duplicado en Ktor se retiró; Android usa únicamente la API externa.

## Iniciar la API

1. Abre una terminal en la carpeta local del repositorio de la API.
2. Configura las variables locales que indica el README del backend. No publiques `.env` ni credenciales.
3. Asegúrate de que MongoDB esté disponible en la dirección de `MONGODB_URI`.
4. Ejecuta `npm install`, luego `npm run seed` y `npm start`.
5. Comprueba `http://localhost:3000/api/health`.

El servidor crea una administradora y una cuidadora de desarrollo al sembrar datos. En producción se requiere `JWT_SECRET` propio y `SEED_DEMO_USERS=0`.

## Ejecutar Android

1. Abre la carpeta del proyecto en Android Studio y sincroniza Gradle.
2. Inicia un emulador Android. La URL predeterminada `http://10.0.2.2:3000/api` llega al servidor del computador.
3. Ejecuta la configuración `app` e inicia sesión con una cuenta de desarrollo.

Para un teléfono físico, configura `API_BASE_URL` con la IP LAN del computador, por ejemplo `-PAPI_BASE_URL=http://192.168.1.20:3000/api`; teléfono y computador deben estar en la misma red. Para publicar, configura HTTPS.

## Contrato utilizado

| Flujo Android | Endpoint |
|---|---|
| Acceso y sesión | `POST /api/auth/login`, `GET /api/auth/me` |
| Lista, perfil, crear, editar y retirar niños | `GET/POST/PUT/DELETE /api/children` |
| Llegada, entrega e historial | `POST /api/attendance`, `PATCH /api/attendance/:id/checkout`, `GET /api/attendance?date=` |
| Alimentación y existencias | `POST/GET /api/aid-deliveries`, `GET /api/inventory` |
| Reposición e historial de stock | `POST /api/inventory/restock`, `GET /api/inventory/movements` |
| Reporte diario o mensual | `GET /api/reports/summary?date=` o `?month=` |

## Permisos y datos

La API valida el token de sesión y el rol. La administradora gestiona perfiles, inventario y reportes; la cuidadora consulta niños y registra operaciones de jornada. La baja de un niño es lógica para conservar su historial. La aplicación no almacena contraseñas. Los documentos de acudientes solo se muestran parcialmente, aunque se envían completos al verificar una entrega.

MongoDB conserva los registros al reiniciar el servidor. `npm run seed` vuelve a cargar los datos de ejemplo y borra los datos operativos de esa base; úsalo únicamente cuando quieras restablecer el entorno local.

