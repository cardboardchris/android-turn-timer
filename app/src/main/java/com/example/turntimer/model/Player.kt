package com.example.turntimer.model

/**
 * Immutable data class representing a player in the turn-based timer game.
 *
 * @param id Unique identifier for the player
 * @param name Display name of the player
 * @param color ARGB color value for the player (default 0)
 * @param elapsedMillis Time elapsed for this player in milliseconds (default 0)
 */
data class Player(
    val id: Int,
    val name: String,
    val color: Int = 0,
    val elapsedMillis: Long = 0L
) {
    companion object {
        val PALETTE: List<Int> = listOf(
            0xFFE53935.toInt(),  // Red
            0xFF1E88E5.toInt(),  // Blue
            0xFF43A047.toInt(),  // Green
            0xFFFB8C00.toInt(),  // Orange
            0xFF7E57C2.toInt(),  // Purple
            0xFF00897B.toInt(),  // Teal
            0xFFC2185B.toInt(),  // Pink
            0xFFFDD835.toInt()   // Amber
        )

        fun getAvailableColors(usedColors: Set<Int>): List<Int> {
            return PALETTE.filter { it !in usedColors }
        }

        fun getNextAvailableColor(usedColors: Set<Int>): Int {
            return getAvailableColors(usedColors).firstOrNull() ?: PALETTE[0]
        }
    }
}
