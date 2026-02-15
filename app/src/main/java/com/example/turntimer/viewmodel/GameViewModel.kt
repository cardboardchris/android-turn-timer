package com.example.turntimer.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.turntimer.model.GameState
import com.example.turntimer.model.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONException

/**
 * ViewModel for managing multi-player turn-based game state and timer logic.
 *
 * Features:
 * - Up to 5 players with individual cumulative timers
 * - Coroutine-based ticker using wall-clock timestamps for accurate elapsed time
 * - StateFlow-based reactive state management
 * - Pause/resume support
 */
class GameViewModel(application: android.app.Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "turn_timer_prefs"
        private const val KEY_PLAYER_NAMES = "player_names"
    }

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

    // ========== TIMER TICKER (Coroutine Flow) ==========

    private val tickerFlow = flow {
        while (true) {
            delay(100) // 100ms tick interval (10 FPS)
            emit(Unit)
        }
    }

    init {
        // Auto-start/stop ticker based on game state
        _gameState
            .flatMapLatest { state ->
                if (state == GameState.PLAYING) tickerFlow else emptyFlow()
            }
            .onEach { updateActivePlayerElapsedTime() }
            .launchIn(viewModelScope)

        // Load saved player names on initialization
        val savedNames = loadSavedPlayerNames()
        if (savedNames.isNotEmpty()) {
            _players.value = savedNames.mapIndexed { index, name ->
                Player(id = index, name = name)
            }
            nextPlayerId = savedNames.size
        }
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
     *
     * @param name Player name (whitespace will be trimmed)
     * @return true if added successfully, false if rejected (empty name or max players reached)
     */
    fun addPlayer(name: String): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return false
        if (_players.value.size >= 5) return false

        val newPlayer = Player(
            id = nextPlayerId++,
            name = trimmedName,
            elapsedMillis = 0L
        )
        _players.value = _players.value + newPlayer
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
     * Save current player names to SharedPreferences.
     * Only saves names, not turn order or elapsed time.
     */
    private fun savePlayerNames() {
        val names = _players.value.map { it.name }
        val jsonArray = JSONArray(names)
        getApplication<android.app.Application>()
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PLAYER_NAMES, jsonArray.toString())
            .apply()
    }

    /**
     * Load saved player names from SharedPreferences.
     * Returns empty list if no saved data or JSON parsing fails.
     *
     * @return List of player names
     */
    private fun loadSavedPlayerNames(): List<String> {
        val prefs = getApplication<android.app.Application>()
            .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PLAYER_NAMES, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    /**
     * Start the game (transition to PLAYING state).
     * Sets active player to index 0 and records the turn start timestamp.
     */
    fun startGame() {
        if (!canStartGame()) return
        savePlayerNames()
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
     */
    fun resetGame() {
        val savedNames = loadSavedPlayerNames()
        _players.value = savedNames.mapIndexed { index, name ->
            Player(id = index, name = name)
        }
        _activePlayerIndex.value = 0
        _gameState.value = GameState.SETUP
        _turnStartTimestamp = 0L
        _accumulatedBeforeTurn = 0L
        nextPlayerId = savedNames.size
    }

    // ========== UTILITY ==========

    /**
     * Format elapsed time in milliseconds to MM:SS format.
     *
     * @param millis Elapsed time in milliseconds
     * @return Formatted string in "MM:SS" format
     */
    fun formatTime(millis: Long): String {
        val minutes = millis / 60000
        val seconds = (millis / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
