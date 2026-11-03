---
name: Jardín Infantil
description: Interfaz serena y cálida para acompañar la jornada de cuidado infantil.
colors:
  primary: "#285943"
  primary-soft: "#DCEADF"
  sky: "#8EB9C4"
  sky-soft: "#E7F0F1"
  honey: "#E9BD5B"
  honey-soft: "#F8EDC8"
  coral: "#D77B67"
  coral-critical: "#B94E44"
  text: "#27352D"
  text-muted: "#596B60"
  background: "#F8F4E9"
  surface: "#FFFFFF"
  border: "#D8DED5"
typography:
  display:
    fontFamily: "sans-serif"
    fontSize: "24sp"
    fontWeight: 700
    lineHeight: 1.2
  headline:
    fontFamily: "sans-serif"
    fontSize: "16sp"
    fontWeight: 700
    lineHeight: 1.3
  body:
    fontFamily: "sans-serif"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: 1.4
  label:
    fontFamily: "sans-serif"
    fontSize: "12sp"
    fontWeight: 700
    lineHeight: 1.3
rounded:
  field: "14dp"
  card: "16dp"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "14dp"
  lg: "18dp"
  xl: "24dp"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "#FFFFFF"
    rounded: "{rounded.field}"
    height: "54dp"
  card:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.card}"
    padding: "14dp"
  text-field:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    rounded: "{rounded.field}"
    height: "54dp"
---

# Design System: Jardín Infantil

## Overview

**Creative North Star: “Sendero del cuidado”**

La interfaz acompaña a cuidadoras y administradora durante una jornada activa. El verde de jardín y el fondo avena sostienen una lectura tranquila; miel, azul agua y coral separan alimentación, entregas y alertas. La identidad combina calidez natural y acentos alegres con la jerarquía compacta de una app operativa.

Las acciones principales se presentan como una ruta breve: llegada, alimentación y entrega. El color siempre acompaña una etiqueta o un icono; la app no comunica estados de avance que todavía no registra.

**Key Characteristics:**
- Fondo avena con texto verde profundo.
- Acentos cálidos y naturales reservados para categorías y estados.
- Componentes Material nativos y textos en español.
- Acciones de la jornada visibles antes de las tareas administrativas.

## Colors

La paleta combina verdes de jardín, azul agua y miel sobre superficies claras para reconocer cada tarea sin recargar la pantalla.

### Primary
- **Verde jardín** (#285943): identidad, botones principales, navegación y acciones prioritarias.
- **Verde suave** (#DCEADF): apoyo y estados tranquilos.

### Secondary
- **Azul agua** (#8EB9C4): categoría de entregas y acciones de salida; **azul tenue** (#E7F0F1) sirve de superficie.
- **Miel** (#E9BD5B): categoría de alimentación; **miel clara** (#F8EDC8) sirve de superficie.

### Tertiary
- **Coral cálido** (#D77B67): acento para avisos.
- **Coral crítico** (#B94E44): errores y situaciones que requieren atención.

### Neutral
- **Avena** (#F8F4E9): fondo principal.
- **Blanco** (#FFFFFF): superficie de tarjetas y campos.
- **Tinta de jardín** (#27352D): texto principal.
- **Verde grisáceo** (#596B60): texto secundario.
- **Borde hoja** (#D8DED5): límites de superficies y divisores.

### Named Rules
**La Regla de Ruta.** Asigna verde a llegada, miel a alimentación y azul a entregas; conserva el texto junto al color.

**La Regla del Acceso Seguro.** El acceso usa cuentas de cuidadora y administradora; los permisos visibles reflejan los que valida la API.

## Typography

**Display Font:** sans-serif del sistema Android (Roboto en la configuración actual)
**Body Font:** sans-serif del sistema Android
**Label/Mono Font:** sans-serif del sistema Android

**Character:** Lectura familiar y directa, con pesos marcados para títulos y acciones. El tamaño usa sp para respetar la escala de texto del dispositivo.

### Hierarchy
- **Display** (bold, 24–26sp): saludo y cifras destacadas del resumen.
- **Headline** (bold, 16sp): encabezados de sección.
- **Title** (bold, 14–15sp): títulos de tarjetas y acciones.
- **Body** (regular, 14sp): campos y explicaciones.
- **Label** (bold, 12–13sp): fechas, estados y etiquetas auxiliares.

## Layout

Las pantallas usan columnas verticales desplazables con 20–24dp de margen exterior. Los bloques relacionados se separan en pasos de 8–24dp; controles de formulario de 54dp dejan espacio para lectura y toque. El inicio coloca saludo, resumen del día, ruta de cuidado y pendientes antes de la navegación inferior. No hay una variante de tableta configurada en los recursos revisados.

## Elevation & Depth

El sistema se apoya en superficies blancas delimitadas por un borde suave, sin sombras declaradas en los fondos revisados. La jerarquía viene del espacio, el contraste de color y el tamaño de los títulos.

## Shapes

Los campos y botones usan esquinas de 14dp. Las tarjetas usan 16dp. Los indicadores circulares se reservan para avatares y símbolos; no se recortan fotos como decoración.

## Components

### Buttons
- **Shape:** rectangular de esquinas suaves (14dp).
- **Primary:** verde jardín con texto blanco; botones de acceso de 54dp.
- **Focus:** conserva el feedback de toque del sistema Android cuando el componente lo permite.

### Cards / Containers
- **Corner Style:** 16dp con superficie blanca y borde hoja de 1dp.
- **Padding:** las acciones principales parten de 14dp y conservan al menos 72dp de alto.

### Inputs
- **Shape:** campos de 54dp con 14dp de radio y borde verde grisáceo.
- **Content:** texto de entrada oscuro y ayuda en verde grisáceo.
- **Password toggle:** objetivo de toque de 48dp y descripción accesible en español.

### Navigation
La navegación inferior usa botones e iconos del sistema visual común, con las etiquetas correspondientes en español.

## Do's and Don'ts
- **Do** mantener visible la llegada, la alimentación y la entrega autorizada en el inicio.
- **Do** acompañar cada estado con texto, no solo color.
- **Do** describir los campos que no traen datos como no reportados.
- **Do** conservar objetivos táctiles cómodos y estados de foco del sistema.
- **Don't** presentar controles ocultos en Android como sustituto de los permisos de la API.
- **Don't** inventar datos personales o resultados de operación.
- **Don't** añadir decoración que compita con las acciones del día.
