package com.rodrigocaceres.autopausa

/**
 * Apps de conductor soportadas. Cada una define:
 *  - su nombre de paquete en Android,
 *  - palabras clave que aparecen en sus notificaciones cuando hay un viaje activo,
 *  - textos de los botones para pausar y para confirmar la pausa.
 *
 * Los textos pueden variar según versión/país de la app; por eso se manejan
 * como listas amplias y en minúsculas (la comparación ignora mayúsculas).
 */
enum class RideApp(
    val packageName: String,
    val displayName: String,
    val tripKeywords: List<String>,
    val pauseButtonTexts: List<String>,
    val confirmButtonTexts: List<String>
) {
    UBER(
        packageName = "com.ubercab.driver",
        displayName = "Uber",
        tripKeywords = listOf(
            "recoger", "recogiendo", "en camino", "dirígete", "dirigete",
            "en viaje", "viaje en curso", "dejar a", "llevando a",
            "picking up", "on trip", "heading to", "dropping off"
        ),
        pauseButtonTexts = listOf(
            "detener nuevas solicitudes", "dejar de recibir solicitudes",
            "desconectarse", "desconectarte", "fuera de línea",
            "stop new requests", "go offline"
        ),
        confirmButtonTexts = listOf(
            "sí", "si", "confirmar", "aceptar", "desconectarse",
            "yes", "confirm", "go offline"
        )
    ),

    DIDI(
        packageName = "com.sdu.didi.gsui",
        displayName = "DiDi",
        tripKeywords = listOf(
            "recoger", "pasajero", "en camino", "en viaje", "viaje en curso",
            "dirígete", "dirigete", "llegando", "destino",
            "picking up", "on trip", "arriving"
        ),
        pauseButtonTexts = listOf(
            "pausar", "pausa", "dejar de aceptar", "fuera de servicio",
            "terminar turno", "salir", "pause", "stop receiving", "go offline"
        ),
        confirmButtonTexts = listOf(
            "sí", "si", "confirmar", "aceptar", "pausar",
            "yes", "confirm", "pause"
        )
    );

    /** Devuelve la otra app: si estoy en viaje con Uber, hay que pausar DiDi, y viceversa. */
    fun other(): RideApp = if (this == UBER) DIDI else UBER

    companion object {
        fun fromPackage(packageName: String): RideApp? =
            entries.firstOrNull { it.packageName == packageName }
    }
}
