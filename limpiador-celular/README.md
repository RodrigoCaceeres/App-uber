# Limpiador de Celular

App Android nativa (Kotlin) para revisar qué apps ocupan más espacio y cuáles no usás hace tiempo, y desinstalarlas — siempre con tu confirmación.

## Qué hace

- Lista las apps instaladas (excluyendo apps de sistema puras, para no arriesgar el funcionamiento del celular).
- Muestra el tamaño aproximado de cada app, ordenadas de mayor a menor.
- Si le das acceso a "Datos de uso" (opcional), muestra hace cuántos días usaste cada app.
- Botón "Desinstalar" por app: dispara el diálogo nativo de Android para desinstalar. **Android siempre te pide confirmar ahí**, esta app nunca borra nada por sí sola ni en segundo plano.
- Header con resumen de almacenamiento usado/total del dispositivo.

## Qué NO hace (limitaciones reales de Android)

- No borra caché de otras apps: Android no permite que una app de terceros limpie la caché de otra sin root.
- No desinstala apps de sistema "puras" (las que vienen de fábrica y no se pueden actualizar/desinstalar sin root): se filtran intencionalmente.
- No automatiza nada sin que vos confirmes: cada desinstalación pasa por el diálogo del sistema operativo.

## Cómo compilarla e instalarla

1. Instalá [Android Studio](https://developer.android.com/studio).
2. Abrí la carpeta `limpiador-celular/` como proyecto ("Open").
3. Si Android Studio pide generar el wrapper de Gradle (no está incluido en este repo para no subir binarios), aceptá la opción "Create Gradle Wrapper", o corré una vez `gradle wrapper --gradle-version 8.5` si tenés Gradle instalado.
4. Conectá tu celular Android por USB con "Depuración USB" activada (Ajustes > Opciones de desarrollador), o generá el APK con `Build > Build APK(s)` e instalalo manualmente.
5. Ejecutá la app (Run ▶) o instalá el APK generado en `app/build/outputs/apk/`.

## Permisos que pide

- `QUERY_ALL_PACKAGES`: para poder listar todas las apps instaladas (requerido desde Android 11).
- `PACKAGE_USAGE_STATS`: permiso especial, no se concede con el diálogo normal. La app te lleva directo a Ajustes para activarlo manualmente (banner amarillo en la pantalla principal). Es opcional: sin él, la app funciona igual pero no muestra "hace cuántos días no la usás".
- `REQUEST_DELETE_PACKAGES`: para poder abrir el diálogo nativo de desinstalación.

Ninguno de estos permisos le da a la app la capacidad de borrar nada sin tu confirmación explícita.
