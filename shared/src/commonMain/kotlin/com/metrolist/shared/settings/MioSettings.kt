/**
 * MiMetrolist: opciones del estilo personal.
 */

package com.metrolist.shared.settings

/** Color de acento del estilo personal. CANCION toma el color de la carátula que suena. */
enum class MioAccent(
    val argb: Long,
) {
    VERDE(0xFF2FD66BL),
    AZUL(0xFF4D8DFFL),
    MORADO(0xFFA66BFFL),
    ROSA(0xFFFF5FA2L),
    NARANJA(0xFFFF8A3DL),
    ROJO(0xFFFF4D5EL),
    CIAN(0xFF2FD4E0L),
    DORADO(0xFFF5C543L),
    BLANCO(0xFFF2F2F2L),
    CANCION(0xFF2FD66BL),
}

/** Qué tan redondeadas se ven las carátulas, tarjetas y botones. */
enum class MioCorners(
    val scale: Float,
) {
    RECTAS(0.35f),
    SUAVES(0.7f),
    REDONDAS(1f),
    MUY_REDONDAS(1.5f),
}

/** Diseño del reproductor pequeño de abajo. */
enum class MioMiniPlayer {
    TARJETA,
    ORIGINAL,
}

/** Fondo de la barra de navegación inferior. */
enum class MioNavBar {
    DEGRADADO,
    SOLIDA,
    ORIGINAL,
}

/** Qué suena con el despertador. */
enum class MioAlarmSource {
    ME_GUSTA,
    DESCARGADAS,
    TOP,
}
