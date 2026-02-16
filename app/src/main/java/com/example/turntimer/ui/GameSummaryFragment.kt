package com.example.turntimer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.turntimer.adapter.PlayerSummaryAdapter
import com.example.turntimer.databinding.FragmentGameSummaryBinding
import com.example.turntimer.viewmodel.GameViewModel

/**
 * Fragment for the game summary screen shown after a game ends.
 *
 * Features:
 * - Display "Game Summary" title
 * - RecyclerView showing all players with their total accumulated times in turn order
 * - "New Game" button to reset and return to Player Setup
 *
 * Uses shared GameViewModel via activityViewModels() to read final player data.
 * Reads player data ONE-TIME via .value since the game is finished (no continuous collection needed).
 */
class GameSummaryFragment : Fragment() {

    private var _binding: FragmentGameSummaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameViewModel by activityViewModels()
    private lateinit var adapter: PlayerSummaryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEdgeToEdgeInsets()
        setupRecyclerView()
        setupNewGameButton()
        displayResults()
    }

    private fun setupEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val basePadding = (24 * resources.displayMetrics.density).toInt()
            view.setPadding(
                basePadding + systemBars.left,
                basePadding + systemBars.top,
                basePadding + systemBars.right,
                basePadding + systemBars.bottom
            )
            insets
        }
    }

    /**
     * Initialize RecyclerView with PlayerSummaryAdapter and LinearLayoutManager.
     */
    private fun setupRecyclerView() {
        adapter = PlayerSummaryAdapter()
        binding.rvPlayerSummary.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlayerSummary.adapter = adapter
    }

    /**
     * Set up the "New Game" button to reset the game and return to Player Setup.
     * Calls viewModel.resetGame() which sets gameState to SETUP.
     * MainActivity observes gameState and swaps to PlayerSetupFragment.
     */
    private fun setupNewGameButton() {
        binding.btnNewGame.setOnClickListener {
            viewModel.resetGame()
        }
    }

    /**
     * Read final player data ONE-TIME from ViewModel and display in RecyclerView.
     * No continuous collection needed since the game is finished.
     */
    private fun displayResults() {
        val players = viewModel.players.value
        adapter.updatePlayers(players)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
