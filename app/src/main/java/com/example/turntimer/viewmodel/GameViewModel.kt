package com.example.turntimer.viewmodel

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.turntimer.model.GameState
import com.example.turntimer.model.Player
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * ViewModel for managing multi-player turn-based game state and timer logic.
 *
 * Features:
 * - Up to 5 players with individual cumulative timers and assigned colors
 * - Coroutine-based ticker using wall-clock timestamps for accurate elapsed time
 * - StateFlow-based reactive state management
 * - Pause/resume support
 * - Player color persistence to SharedPreferences
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModel(application: Application) : AndroidViewModel(application) {

    // ========== STATE (StateFlow) ==========

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _activePlayerIndex = MutableStateFlow(0)
    val activePlayerIndex: StateFlow<Int> = _activePlayerIndex.asStateFlow()

    private val _gameState = MutableStateFlow(GameState.SETUP)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // ========== INTERNAL STATE ==========

    private var _turnStartTimestamp: Long = 0L
    private var _accumulatedBeforeTurn: Long = 0L
    private var nextPlayerId = 0

    // ========== SHARED PREFERENCES ==========

    private companion object {
        private const val PREFS_NAME = "turn_timer_prefs"
        private const val KEY_PLAYER_NAMES = "player_names"
    }

    private val prefs by lazy {
        getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ========== TIMER TICKER (Coroutine Flow) ==========

    private val tickerFlow = flow {
        while (true) {
            delay(100) // 100ms tick interval (10 FPS)
            emit(Unit)
        }
    }

    init {
        // Load saved players with colors
        val savedPlayers = loadSavedPlayers()
        _players.value = savedPlayers.mapIndexed { index, (name, color) ->
            Player(id = index, name = name, color = color)
        }.also { players ->
            nextPlayerId = players.size
        }

        // Auto-start/stop ticker based on game state
        _gameState
            .flatMapLatest { state ->
                if (state == GameState.PLAYING) tickerFlow else emptyFlow()
            }
            .onEach { updateActivePlayerElapsedTime() }
            .launchIn(viewModelScope)
    }

    /**
     * Update the active player's elapsed time based on wall-clock timestamp.
     * Called on each ticker tick when game is PLAYING.
     */
    private fun updateActivePlayerElapsedTime() {
        val currentElapsed = System.currentTimeMillis() - _turnStartTimestamp
        val updatedPlayers = _players.value.mapIndexed { index, player ->
            if (index == _activePlayerIndex.value) {
                player.copy(elapsedMillis = _accumulatedBeforeTurn + currentElapsed)
            } else {
                player
            }
        }
        _players.value = updatedPlayers
    }

    // ========== PUBLIC API ==========

    /**
     * Add a new player to the game (max 5 players).
     * Auto-assigns the next available color from Player.PALETTE.
     *
     * @param name Player name (whitespace will be trimmed)
     * @return true if added successfully, false if rejected (empty name or max players reached)
     */
    fun addPlayer(name: String): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return false
        if (_players.value.size >= 5) return false

        val assignedColor = Player.getNextAvailableColor(getUsedColors())
        val newPlayer = Player(
             id = nextPlayerId++,
             name = trimmedName,
             color = assignedColor,
             elapsedMillis = 0L
         )
         _players.value += newPlayer
        return true
    }

    /**
     * Remove a player by their ID.
     *
     * @param playerId The player's unique ID
     */
    fun removePlayer(playerId: Int) {
        _players.value = _players.value.filter { it.id != playerId }
    }

    /**
     * Move a player from one position to another (for drag-to-reorder).
     *
     * @param fromIndex Source index
     * @param toIndex Destination index
     */
    fun movePlayer(fromIndex: Int, toIndex: Int) {
        val currentList = _players.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val player = currentList.removeAt(fromIndex)
            currentList.add(toIndex, player)
            _players.value = currentList
        }
    }

    /**
     * Check if the game can start (requires at least 2 players).
     *
     * @return true if >= 2 players
     */
    fun canStartGame(): Boolean = _players.value.size >= 2

    /**
     * Start the game (transition to PLAYING state).
     * Saves current player names and colors.
     * Sets active player to index 0 and records the turn start timestamp.
     */
    fun startGame() {
        if (!canStartGame()) return
        savePlayers()
        _activePlayerIndex.value = 0
        _turnStartTimestamp = System.currentTimeMillis()
        _accumulatedBeforeTurn = 0L
        _gameState.value = GameState.PLAYING
    }

    /**
     * End the current player's turn and advance to the next player.
     * Accumulates elapsed time for the current player before advancing.
     */
    fun endTurn() {
        if (_gameState.value != GameState.PLAYING) return

        // 1. Accumulate current turn's elapsed time FIRST
        val finalElapsed = System.currentTimeMillis() - _turnStartTimestamp
        val updatedPlayers = _players.value.mapIndexed { index, player ->
            if (index == _activePlayerIndex.value) {
                player.copy(elapsedMillis = _accumulatedBeforeTurn + finalElapsed)
            } else {
                player
            }
        }
        _players.value = updatedPlayers

        // 2. Advance to next player (wrapping)
        _activePlayerIndex.value = (_activePlayerIndex.value + 1) % _players.value.size

        // 3. Record new turn start with UPDATED player's accumulated time
        _turnStartTimestamp = System.currentTimeMillis()
        _accumulatedBeforeTurn = _players.value[_activePlayerIndex.value].elapsedMillis
    }

    /**
     * Pause the game (transition to PAUSED state).
     * Accumulates elapsed time for the active player before pausing.
     */
    fun pauseGame() {
        if (_gameState.value != GameState.PLAYING) return

        // Accumulate elapsed time before pausing
        val finalElapsed = System.currentTimeMillis() - _turnStartTimestamp
        val updatedPlayers = _players.value.mapIndexed { index, player ->
            if (index == _activePlayerIndex.value) {
                player.copy(elapsedMillis = _accumulatedBeforeTurn + finalElapsed)
            } else {
                player
            }
        }
        _players.value = updatedPlayers
        _accumulatedBeforeTurn = updatedPlayers[_activePlayerIndex.value].elapsedMillis

        _gameState.value = GameState.PAUSED
    }

    /**
     * Resume the game from PAUSED state (transition to PLAYING state).
     * Records a new turn start timestamp to prevent time jump.
     */
    fun resumeGame() {
        if (_gameState.value != GameState.PAUSED) return
        _turnStartTimestamp = System.currentTimeMillis()
        _gameState.value = GameState.PLAYING
    }

    /**
     * End the game (transition to FINISHED state).
     * Accumulates any remaining elapsed time for the active player.
     */
    fun endGame() {
        if (_gameState.value == GameState.PLAYING) {
            // Accumulate remaining elapsed time
            val finalElapsed = System.currentTimeMillis() - _turnStartTimestamp
            val updatedPlayers = _players.value.mapIndexed { index, player ->
                if (index == _activePlayerIndex.value) {
                    player.copy(elapsedMillis = _accumulatedBeforeTurn + finalElapsed)
                } else {
                    player
                }
            }
            _players.value = updatedPlayers
        }
        _gameState.value = GameState.FINISHED
    }

    /**
     * Reset the game state (clear all players, return to SETUP).
     * Loads saved players with their colors for the next game.
     */
    fun resetGame() {
        val savedPlayers = loadSavedPlayers()
        _players.value = savedPlayers.mapIndexed { index, (name, color) ->
            Player(id = index, name = name, color = color)
        }.also { players ->
            nextPlayerId = players.size
        }
        _activePlayerIndex.value = 0
        _gameState.value = GameState.SETUP
        _turnStartTimestamp = 0L
        _accumulatedBeforeTurn = 0L
    }

    // ========== UTILITY ==========

    /**
     * Get the set of colors currently in use by players.
     *
     * @return Set of ARGB color integers
     */
    fun getUsedColors(): Set<Int> {
        return _players.value.map { it.color }.toSet()
    }

    /**
     * Change a player's color.
     * Enforces uniqueness: rejects change if the new color is already in use by another player.
     *
     * @param playerId The player's unique ID
     * @param newColor New ARGB color value
     * @return true if color changed successfully, false if color is already in use
     */
    fun changePlayerColor(playerId: Int, newColor: Int): Boolean {
        val player = _players.value.find { it.id == playerId } ?: return false
        
        // Check if newColor is already used by another player
        val usedColors = getUsedColors() - player.color
        if (newColor in usedColors) return false
        
        val updatedPlayers = _players.value.map { p ->
            if (p.id == playerId) p.copy(color = newColor) else p
        }
        _players.value = updatedPlayers
        return true
    }

    /**
     * Save current players (names and colors) to SharedPreferences.
     * Format: JSONArray of JSONObjects with "name" and "color" fields.
     */
    private fun savePlayers() {
        val jsonArray = JSONArray()
        _players.value.forEach { player ->
            val jsonObj = JSONObject().apply {
                put("name", player.name)
                put("color", player.color)
            }
            jsonArray.put(jsonObj)
        }
         prefs.edit { putString(KEY_PLAYER_NAMES, jsonArray.toString()) }
    }

    /**
     * Load saved players from SharedPreferences.
     * Handles migration: tries new JSONObject format first, falls back to legacy string array format.
     *
     * @return List of Pair<name, color> tuples
     */
    private fun loadSavedPlayers(): List<Pair<String, Int>> {
        val savedJson = prefs.getString(KEY_PLAYER_NAMES, null) ?: return emptyList()
        
        return try {
            val jsonArray = JSONArray(savedJson)
            val result = mutableListOf<Pair<String, Int>>()
            
            for (i in 0 until jsonArray.length()) {
                try {
                    val jsonObj = jsonArray.getJSONObject(i)
                    val name = jsonObj.getString("name")
                    val color = jsonObj.getInt("color")
                    result.add(Pair(name, color))
                } catch (e: JSONException) {
                    // Fall back to legacy string format
                    val name = jsonArray.getString(i)
                    val color = Player.PALETTE[i % Player.PALETTE.size]
                    result.add(Pair(name, color))
                }
            }
            result
        } catch (e: JSONException) {
            emptyList()
        }
    }

    /**
     * Format elapsed time in milliseconds to MM:SS format.
     *
     * @param millis Elapsed time in milliseconds
     * @return Formatted string in "MM:SS" format
     */
    fun formatTime(millis: Long): String {
        val minutes = millis / 60000
        val seconds = (millis / 1000) % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
