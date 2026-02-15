package com.example.turntimer.model

/**
 * Immutable data class representing a player in the turn-based timer game.
 *
 * @param id Unique identifier for the player
 * @param name Display name of the player
 * @param elapsedMillis Time elapsed for this player in milliseconds (default 0)
 */
data class Player(
    val id: Int,
    val name: String,
    val elapsedMillis: Long = 0L
)
