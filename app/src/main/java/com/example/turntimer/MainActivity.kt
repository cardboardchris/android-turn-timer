package com.example.turntimer

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import com.example.turntimer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var timer: CountDownTimer? = null
    private var currentPlayer = 1
    private var timeLeftInMillis: Long = 60000 // Default 60 seconds
    private var defaultTimeInMillis: Long = 60000
    private var isTimerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        updateUI()
    }

    private fun setupListeners() {
        binding.btnStartPause.setOnClickListener {
            if (isTimerRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        binding.btnReset.setOnClickListener {
            resetTimer()
        }

        binding.btnNextTurn.setOnClickListener {
            nextTurn()
        }

        binding.btnSetTime30.setOnClickListener {
            setDefaultTime(30000)
        }

        binding.btnSetTime60.setOnClickListener {
            setDefaultTime(60000)
        }

        binding.btnSetTime120.setOnClickListener {
            setDefaultTime(120000)
        }
    }

    private fun startTimer() {
        timer = object : CountDownTimer(timeLeftInMillis, 10) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerDisplay()
            }

            override fun onFinish() {
                isTimerRunning = false
                timeLeftInMillis = 0
                updateUI()
                // Optionally add sound or vibration here
            }
        }.start()

        isTimerRunning = true
        updateUI()
    }

    private fun pauseTimer() {
        timer?.cancel()
        isTimerRunning = false
        updateUI()
    }

    private fun resetTimer() {
        timer?.cancel()
        timeLeftInMillis = defaultTimeInMillis
        isTimerRunning = false
        updateUI()
    }

    private fun nextTurn() {
        timer?.cancel()
        currentPlayer++
        timeLeftInMillis = defaultTimeInMillis
        isTimerRunning = false
        updateUI()
    }

    private fun setDefaultTime(millis: Long) {
        defaultTimeInMillis = millis
        if (!isTimerRunning) {
            timeLeftInMillis = millis
            updateUI()
        }
    }

    private fun updateUI() {
        updateTimerDisplay()
        binding.tvCurrentPlayer.text = "Player $currentPlayer"
        binding.btnStartPause.text = if (isTimerRunning) "PAUSE" else "START"
        
        // Update button backgrounds to show selected time
        binding.btnSetTime30.alpha = if (defaultTimeInMillis == 30000L) 1.0f else 0.5f
        binding.btnSetTime60.alpha = if (defaultTimeInMillis == 60000L) 1.0f else 0.5f
        binding.btnSetTime120.alpha = if (defaultTimeInMillis == 120000L) 1.0f else 0.5f
    }

    private fun updateTimerDisplay() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        val millis = (timeLeftInMillis % 1000) / 10

        binding.tvTimer.text = String.format("%02d:%02d.%02d", minutes, seconds, millis)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
