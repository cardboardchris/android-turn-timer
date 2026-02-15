package com.example.turntimer

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.turntimer.model.GameState
import com.example.turntimer.ui.GameFragment
import com.example.turntimer.ui.GameSummaryFragment
import com.example.turntimer.ui.PlayerSetupFragment
import com.example.turntimer.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Main Activity serving as a thin Fragment host.
 *
 * Responsibilities:
 * - Hosts a single FragmentContainerView (FrameLayout)
 * - Observes GameViewModel.gameState to drive fragment swaps
 * - Does NOT contain any game logic — purely navigation
 *
 * Fragment mapping:
 * - SETUP → PlayerSetupFragment
 * - PLAYING / PAUSED → GameFragment
 * - FINISHED → GameSummaryFragment
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: GameViewModel by viewModels()
    private var touchDownX: Float = 0f
    private var touchDownY: Float = 0f
    private var lastTurnEndTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load initial fragment only on first creation (not on config change)
        if (savedInstanceState == null) {
            showFragment(PlayerSetupFragment())
        }

        observeGameState()
    }

    /**
     * Observe GameViewModel.gameState and swap fragments accordingly.
     * Checks current fragment type before swapping to avoid redundant transactions.
     */
    private fun observeGameState() {
        lifecycleScope.launch {
            viewModel.gameState.collect { state ->
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

                when (state) {
                    GameState.SETUP -> {
                        if (currentFragment !is PlayerSetupFragment) {
                            showFragment(PlayerSetupFragment())
                        }
                    }
                    GameState.PLAYING, GameState.PAUSED -> {
                        if (currentFragment !is GameFragment) {
                            showFragment(GameFragment())
                        }
                    }
                    GameState.FINISHED -> {
                        if (currentFragment !is GameSummaryFragment) {
                            showFragment(GameSummaryFragment())
                        }
                    }
                }
            }
        }
    }

    /**
     * Replace the current fragment in the container.
     * Does NOT add to back stack — navigation is driven by gameState only.
     *
     * @param fragment The fragment to display
     */
    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = ev.rawX
                touchDownY = ev.rawY
            }
            MotionEvent.ACTION_UP -> {
                // Only act if game is PLAYING
                if (viewModel.gameState.value == GameState.PLAYING) {
                    // Distinguish tap from scroll (30px threshold)
                    val dx = abs(ev.rawX - touchDownX)
                    val dy = abs(ev.rawY - touchDownY)
                    if (dx <= 30f && dy <= 30f) {
                        // Check if touch is on excluded buttons
                        val gameFragment = supportFragmentManager
                            .findFragmentById(R.id.fragment_container) as? GameFragment
                        if (gameFragment != null && !gameFragment.isTouchOnExcludedButton(ev)) {
                            // Debounce: 300ms cooldown
                            val now = System.currentTimeMillis()
                            if (now - lastTurnEndTime >= 300) {
                                lastTurnEndTime = now
                                viewModel.endTurn()
                                gameFragment.triggerHapticFeedback()
                            }
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
