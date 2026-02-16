package com.example.turntimer.ui

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.turntimer.adapter.PlayerTimerAdapter
import com.example.turntimer.databinding.FragmentGameBinding
import com.example.turntimer.model.GameState
import com.example.turntimer.model.Player
import com.example.turntimer.viewmodel.GameViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
 * - Haptic vibration on turn changes
 * - FLAG_KEEP_SCREEN_ON during active play
 * - Back button confirmation dialog during active game
 *
 * Uses shared GameViewModel via activityViewModels() for state management.
 */
class GameFragment : Fragment() {

    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameViewModel by activityViewModels()
    private lateinit var adapter: PlayerTimerAdapter

    private var backgroundAnimator: ValueAnimator? = null
    private var currentBackgroundColor: Int = 0xFF1E1E1E.toInt()

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
        setupBackButtonHandler()
        
        // If game is already PLAYING, set background immediately (rotation case)
        if (viewModel.gameState.value == GameState.PLAYING) {
            val activePlayer = viewModel.players.value.getOrNull(viewModel.activePlayerIndex.value)
            if (activePlayer != null) {
                updateBackgroundColor(activePlayer, animate = false)
            }
        }
        
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
     * Set up button click handlers for Pause/Resume and End Game.
     */
    private fun setupButtons() {
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
     * Set up back button interception with confirmation dialog.
     * Uses OnBackPressedCallback which is automatically removed when the view is destroyed.
     */
    private fun setupBackButtonHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("End Game?")
                        .setMessage("Are you sure you want to end the current game?")
                        .setPositiveButton("End Game") { _, _ -> viewModel.endGame() }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        )
    }

    /**
     * Trigger a short haptic vibration (100ms) on turn changes.
     * Handles API level differences:
     * - API 31+ (S): Uses VibratorManager
     * - API 26-30 (O): Uses Vibrator with VibrationEffect
     * - API 24-25: Uses deprecated vibrate(long) method
     */
    internal fun triggerHapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    /**
     * Check if a touch event occurred on either the Pause/Resume or End Game button.
     * Used by MainActivity to exclude button taps from the tap-to-end-turn gesture.
     */
    internal fun isTouchOnExcludedButton(event: MotionEvent): Boolean {
        return isTouchOnView(event, binding.btnPauseResume) ||
               isTouchOnView(event, binding.btnEndGame)
    }

    private fun isTouchOnView(event: MotionEvent, view: View): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val x = event.rawX
        val y = event.rawY
        return x >= location[0] && x <= location[0] + view.width &&
               y >= location[1] && y <= location[1] + view.height
    }

    /**
     * Observe StateFlows from GameViewModel and update UI reactively.
     *
     * Three separate collectors:
     * 1. players + activePlayerIndex: Update active player display and RecyclerView
     * 2. gameState: Toggle Pause/Resume button text and manage FLAG_KEEP_SCREEN_ON
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
                // Animate background color on turn change
                if (activeIndex in players.indices) {
                    updateBackgroundColor(players[activeIndex], animate = true)
                }
            }
        }

        // Observe gameState to toggle button text and screen-on flag
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.gameState.collect { state ->
                updateButtonText(state)
                updateKeepScreenOn(state)
                
                // Show tap hint only during PLAYING state
                binding.tvTapHint.visibility = if (state == GameState.PLAYING) View.VISIBLE else View.GONE
                
                // Set background immediately on game start
                if (state == GameState.PLAYING) {
                    val activePlayer = viewModel.players.value.getOrNull(viewModel.activePlayerIndex.value)
                    if (activePlayer != null) {
                        updateBackgroundColor(activePlayer, animate = false)
                    }
                }
                
                // Revert background on game end
                if (state == GameState.FINISHED) {
                    binding.rootLayout.setBackgroundColor(0xFF1E1E1E.toInt())
                    updateTextContrast(0xFF1E1E1E.toInt())
                    currentBackgroundColor = 0xFF1E1E1E.toInt()
                }
            }
        }
    }

    /**
     * Update UI elements for active player and RecyclerView.
     *
     * @param players Current list of players
     * @param activeIndex Index of the currently active player
     */
     private fun updatePlayerUI(players: List<Player>, activeIndex: Int) {
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

    /**
     * Animate or set root layout background color based on game state.
     *
     * @param player Player whose color to apply
     * @param animate Whether to animate the transition (300ms with FastOutSlowInInterpolator)
     */
    private fun updateBackgroundColor(player: Player, animate: Boolean) {
        if (animate && viewModel.gameState.value == GameState.PLAYING) {
            backgroundAnimator?.cancel()
            backgroundAnimator = ValueAnimator.ofArgb(currentBackgroundColor, player.color).apply {
                duration = 300
                interpolator = FastOutSlowInInterpolator()
                addUpdateListener { animator ->
                    val color = animator.animatedValue as Int
                    binding.rootLayout.setBackgroundColor(color)
                    updateTextContrast(color)
                }
                start()
            }
            currentBackgroundColor = player.color
        } else {
            backgroundAnimator?.cancel()
            binding.rootLayout.setBackgroundColor(player.color)
            updateTextContrast(player.color)
            currentBackgroundColor = player.color
        }
    }

    /**
     * Adjust text colors based on background luminance for accessibility.
     *
     * @param backgroundColor Background color to calculate luminance from
     */
    private fun updateTextContrast(backgroundColor: Int) {
        val luminance = ColorUtils.calculateLuminance(backgroundColor)
        if (luminance > 0.5) {
            // Light background → dark text
            binding.tvActivePlayerName.setTextColor(Color.BLACK)
            binding.tvActivePlayerTimer.setTextColor(Color.BLACK)
            binding.tvAllPlayersLabel.setTextColor(0xFF333333.toInt())
            binding.tvTapHint.setTextColor(0xFF555555.toInt())
            binding.btnPauseResume.setTextColor(Color.BLACK)
            binding.btnEndGame.setTextColor(Color.BLACK)
            binding.btnEndGame.strokeColor = ColorStateList.valueOf(Color.BLACK)
        } else {
            // Dark background → white text
            binding.tvActivePlayerName.setTextColor(Color.WHITE)
            binding.tvActivePlayerTimer.setTextColor(Color.WHITE)
            binding.tvAllPlayersLabel.setTextColor(0xFFAAAAAA.toInt())
            binding.tvTapHint.setTextColor(0xFFAAAAAA.toInt())
            binding.btnPauseResume.setTextColor(Color.WHITE)
            binding.btnEndGame.setTextColor(Color.WHITE)
            binding.btnEndGame.strokeColor = ColorStateList.valueOf(Color.WHITE)
        }
    }

    /**
     * Toggle FLAG_KEEP_SCREEN_ON based on game state.
     * Screen stays on during PLAYING, flag is cleared during PAUSED/FINISHED/SETUP.
     *
     * @param state Current game state
     */
    private fun updateKeepScreenOn(state: GameState) {
        activity?.window?.let { window ->
            when (state) {
                GameState.PLAYING -> window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else -> window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear FLAG_KEEP_SCREEN_ON to prevent battery drain
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Cancel running animator to prevent leaks
        backgroundAnimator?.cancel()
        _binding = null
    }
}
