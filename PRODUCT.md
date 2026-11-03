# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Cuidadoras que registran asistencia, entregas a acudientes autorizados y complementación alimentaria durante la jornada. Administradora que consulta y gestiona niños, inventario y reportes.

## Product Purpose

Aplicación Android para apoyar la gestión diaria de un jardín infantil. El resultado buscado es registrar y consultar la información operativa con rapidez y reducir errores en las llegadas, salidas y entregas.

## Operating Context

Uso diario por el personal del jardín, principalmente desde un teléfono Android. El flujo central es consultar niños, registrar su llegada y entregar cada niño a un acudiente autorizado. También se registran complementos alimentarios y se consulta inventario e informes.

## Capabilities and Constraints

La app nativa usa Kotlin, vistas XML y Material 3, y consume una API HTTP. Las funcionalidades incluyen acceso con cuentas por rol, lista y perfil de niños, llegadas, entregas a acudientes autorizados, historial diario, complementación, inventario, reposición, historial de movimientos, administración y reportes mensuales. La API Node.js/MongoDB, mantenida en un repositorio separado, valida las sesiones y conserva los registros. No inventar información de niños ni acudientes; mostrar campos no suministrados como no reportados.

## Brand Commitments

Nombre actual: Jardín Infantil. El usuario solicita combinar una identidad natural y cálida, colorida y juguetona, con una presentación minimalista y serena. Mantener lenguaje claro en español y una sensación de cuidado apropiada para el personal y las familias.

## Evidence on Hand

Código Android en `app/`, API Node.js/MongoDB en un repositorio separado, `README.md` y `GUIA_ANDROID_API.md`. La autenticación local y la persistencia están implementadas; el despliegue productivo aún requiere configuración segura de secretos, HTTPS y MongoDB.

## Product Principles

- Priorizar los flujos diarios de llegada y entrega.
- Mantener la autorización de acudientes visible y comprensible.
- Mostrar estados y errores junto a la acción que los provoca.
- Presentar la información de niños con cuidado y sin inventar datos.
- Mantener las tareas administrativas fáciles de localizar.


