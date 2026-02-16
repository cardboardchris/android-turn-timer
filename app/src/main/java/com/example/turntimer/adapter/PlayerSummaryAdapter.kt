package com.example.turntimer.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.turntimer.databinding.ItemPlayerSummaryBinding
import com.example.turntimer.model.Player
import java.util.Locale

/**
 * RecyclerView adapter for displaying player summary results after a game ends.
 * Shows each player's position (turn order), name, and total accumulated time.
 */
class PlayerSummaryAdapter : RecyclerView.Adapter<PlayerSummaryAdapter.PlayerSummaryViewHolder>() {

    private var players: List<Player> = emptyList()

    /**
     * Update the adapter's player list and refresh the RecyclerView.
     *
     * @param newPlayers The final list of players with their accumulated times
     */
    fun updatePlayers(newPlayers: List<Player>) {
        players = newPlayers
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = players.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerSummaryViewHolder {
        val binding = ItemPlayerSummaryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlayerSummaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayerSummaryViewHolder, position: Int) {
        holder.bind(players[position], position)
    }

    /**
     * ViewHolder for a player summary item.
     */
    class PlayerSummaryViewHolder(
        private val binding: ItemPlayerSummaryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Bind a player to this ViewHolder.
         *
         * @param player The player data to display
         * @param position The position index (used for display number)
         */
        fun bind(player: Player, position: Int) {
            binding.tvPlayerPosition.text = "${position + 1}."
            binding.tvPlayerName.text = player.name
            binding.tvPlayerTime.text = formatTime(player.elapsedMillis)

            // Set color dot background
            (binding.viewColorDot.background.mutate() as GradientDrawable).setColor(player.color)
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
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
