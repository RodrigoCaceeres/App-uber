# AutoPausa 🚗⏸️

App Android para conductores que trabajan con **Uber** y **DiDi** a la vez.
Cuando inicias un viaje en una de las dos apps, AutoPausa **pausa
automáticamente las solicitudes en la otra**, para que no te lleguen viajes
que no puedes aceptar.

## ¿Cómo funciona?

Ni Uber ni DiDi ofrecen una API pública para conductores, así que la
automatización se hace directamente en tu teléfono:

1. **Detección del viaje** — Un *Notification Listener* lee las notificaciones
   de Uber Driver y DiDi Conductor. Cuando aparece la notificación persistente
   de un viaje activo (por ejemplo «Recoger a…», «En viaje»), AutoPausa sabe
   que estás ocupado.
2. **Pausa automática** — Un *Servicio de Accesibilidad* abre la otra app,
   busca en pantalla el botón de «pausar / detener nuevas solicitudes», lo
   pulsa (y confirma el diálogo si aparece) y te devuelve a la app del viaje.
3. **Fin del viaje** — Cuando la notificación del viaje desaparece, AutoPausa
   te avisa para que reactives las solicitudes. La reconexión se deja como
   paso manual a propósito: reconectarte solo podría mandarte viajes cuando
   aún no estás listo.

Todo ocurre en tu teléfono; la app no envía datos a ningún servidor.

## Instalación

1. Abre el proyecto en **Android Studio** (Hedgehog o más reciente).
2. Conecta tu teléfono con depuración USB activada y pulsa **Run**, o genera
   el APK con `Build > Build APK(s)` e instálalo.
3. Abre AutoPausa y concede los dos permisos que pide la pantalla principal:
   - **Acceso a notificaciones** (para detectar viajes).
   - **Servicio de accesibilidad** (para pulsar el botón de pausa por ti).
4. Deja activado el interruptor «Pausa automática activada». Listo.

## Apps soportadas

| App | Paquete Android |
|---|---|
| Uber Driver | `com.ubercab.driver` |
| DiDi Conductor | `com.sdu.didi.gsui` |

Las palabras clave de detección y los textos de los botones están en
`app/src/main/java/com/rodrigocaceres/autopausa/RideApp.kt`. Si Uber o DiDi
cambian los textos de su interfaz en una actualización, basta con ajustar esas
listas.

## Limitaciones importantes

- **Es automatización de pantalla, no una integración oficial.** Si Uber o
  DiDi rediseñan sus pantallas o cambian los textos, puede dejar de encontrar
  el botón. En ese caso AutoPausa te avisa con una notificación para que
  pauses manualmente — nunca falla en silencio.
- Los textos incluidos están pensados para las apps **en español
  (Latinoamérica)**; si tu teléfono está en otro idioma, agrega los textos
  correspondientes en `RideApp.kt`.
- El uso de servicios de accesibilidad para automatizar otras apps puede ir
  contra los términos de servicio de Uber/DiDi. Apps comerciales similares
  (p. ej. Mystro) funcionan así desde hace años, pero úsala bajo tu propio
  criterio.
- Requiere Android 8.0 (API 26) o superior.

## Estructura del código

```
app/src/main/java/com/rodrigocaceres/autopausa/
├── RideApp.kt                        # Apps soportadas, palabras clave y textos de botones
├── TripNotificationListener.kt       # Detecta inicio/fin de viaje por notificaciones
├── AutoPauseAccessibilityService.kt  # Abre la otra app y pulsa el botón de pausa
├── Prefs.kt                          # Estado compartido (app en viaje, interruptor, último evento)
└── MainActivity.kt                   # Pantalla de permisos y estado
```
