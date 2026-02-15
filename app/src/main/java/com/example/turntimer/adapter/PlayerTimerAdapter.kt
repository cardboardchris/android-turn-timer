package com.example.turntimer.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.turntimer.R
import com.example.turntimer.databinding.ItemPlayerTimerBinding
import com.example.turntimer.model.Player

/**
 * RecyclerView adapter for displaying players and their cumulative timers during a game.
 * Highlights the active player with a green background.
 */
class PlayerTimerAdapter : RecyclerView.Adapter<PlayerTimerAdapter.PlayerTimerViewHolder>() {

    private var players: List<Player> = emptyList()
    private var activePlayerIndex: Int = -1

    /**
     * Update the adapter's player list and active player index, then refresh the RecyclerView.
     *
     * @param newPlayers The new list of players to display
     * @param activeIndex The index of the currently active player (for highlighting)
     */
    fun updatePlayers(newPlayers: List<Player>, activeIndex: Int) {
        players = newPlayers
        activePlayerIndex = activeIndex
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = players.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerTimerViewHolder {
        val binding = ItemPlayerTimerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlayerTimerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayerTimerViewHolder, position: Int) {
        holder.bind(players[position], position == activePlayerIndex)
    }

    /**
     * ViewHolder for a player timer item.
     */
    class PlayerTimerViewHolder(
        private val binding: ItemPlayerTimerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Bind a player to this ViewHolder.
         *
         * @param player The player data to display
         * @param isActive Whether this player is the currently active player
         */
        fun bind(player: Player, isActive: Boolean) {
            binding.tvPlayerName.text = player.name
            binding.tvPlayerTime.text = formatTime(player.elapsedMillis)

            // Set color dot background
            (binding.viewColorDot.background.mutate() as GradientDrawable).setColor(player.color)

            // Highlight active player with bold text (instead of background color)
            if (isActive) {
                binding.tvPlayerName.setTypeface(null, Typeface.BOLD)
                binding.itemRoot.setBackgroundColor(Color.TRANSPARENT)
            } else {
                binding.tvPlayerName.setTypeface(null, Typeface.NORMAL)
                binding.itemRoot.setBackgroundColor(Color.TRANSPARENT)
            }
        }

        /**
         * Format elapsed time in milliseconds to MM:SS format.
         *
         * @param millis Elapsed time in milliseconds
         * @return Formatted string in "MM:SS" format
         */
        private fun formatTime(millis: Long): String {
            val minutes = millis / 60000
            val seconds = (millis / 1000) % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }
}
