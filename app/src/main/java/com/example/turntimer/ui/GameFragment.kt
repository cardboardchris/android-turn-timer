package com.example.turntimer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.turntimer.adapter.PlayerTimerAdapter
import com.example.turntimer.databinding.FragmentGameBinding
import com.example.turntimer.model.GameState
import com.example.turntimer.viewmodel.GameViewModel
import kotlinx.coroutines.launch

/**
 * Fragment for the active game screen.
 *
 * Features:
 * - Display active player name and timer prominently
 * - RecyclerView showing all players with their cumulative times
 * - Turn management: End Turn button to advance to next player
 * - Pause/Resume button that toggles based on game state
 * - End Game button to finish the game
 *
 * Uses shared GameViewModel via activityViewModels() for state management.
 */
class GameFragment : Fragment() {

    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameViewModel by activityViewModels()
    private lateinit var adapter: PlayerTimerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupButtons()
        observeGameState()
    }

    /**
     * Initialize RecyclerView with PlayerTimerAdapter and LinearLayoutManager.
     */
    private fun setupRecyclerView() {
        adapter = PlayerTimerAdapter()
        binding.rvPlayerTimers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlayerTimers.adapter = adapter
    }

    /**
     * Set up button click handlers for End Turn, Pause/Resume, and End Game.
     */
    private fun setupButtons() {
        binding.btnEndTurn.setOnClickListener {
            viewModel.endTurn()
        }

        binding.btnPauseResume.setOnClickListener {
            when (viewModel.gameState.value) {
                GameState.PLAYING -> viewModel.pauseGame()
                GameState.PAUSED -> viewModel.resumeGame()
                else -> {} // No-op for SETUP/FINISHED states
            }
        }

        binding.btnEndGame.setOnClickListener {
            viewModel.endGame()
        }
    }

    /**
     * Observe StateFlows from GameViewModel and update UI reactively.
     *
     * Three separate collectors:
     * 1. players + activePlayerIndex: Update active player display and RecyclerView
     * 2. gameState: Toggle Pause/Resume button text
     */
    private fun observeGameState() {
        // Observe players and activePlayerIndex together
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.players.collect { players ->
                val activeIndex = viewModel.activePlayerIndex.value
                updatePlayerUI(players, activeIndex)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activePlayerIndex.collect { activeIndex ->
                val players = viewModel.players.value
                updatePlayerUI(players, activeIndex)
            }
        }

        // Observe gameState to toggle button text
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.gameState.collect { state ->
                updateButtonText(state)
            }
        }
    }

    /**
     * Update UI elements for active player and RecyclerView.
     *
     * @param players Current list of players
     * @param activeIndex Index of the currently active player
     */
    private fun updatePlayerUI(players: List<com.example.turntimer.model.Player>, activeIndex: Int) {
        if (players.isEmpty() || activeIndex !in players.indices) return

        val activePlayer = players[activeIndex]
        binding.tvActivePlayerName.text = activePlayer.name
        binding.tvActivePlayerTimer.text = viewModel.formatTime(activePlayer.elapsedMillis)
        adapter.updatePlayers(players, activeIndex)
    }

    /**
     * Update Pause/Resume button text based on game state.
     *
     * @param state Current game state
     */
    private fun updateButtonText(state: GameState) {
        binding.btnPauseResume.text = when (state) {
            GameState.PLAYING -> "PAUSE"
            GameState.PAUSED -> "RESUME"
            else -> "PAUSE" // Default for SETUP/FINISHED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
