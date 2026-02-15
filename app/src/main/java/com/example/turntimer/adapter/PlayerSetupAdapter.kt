package com.example.turntimer.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.turntimer.databinding.ItemPlayerSetupBinding
import com.example.turntimer.model.Player

/**
 * RecyclerView adapter for the player setup screen.
 * Supports drag-to-reorder via drag handles and player removal.
 *
 * @param onRemoveClick Callback invoked when a player is removed. Takes the player ID.
 */
class PlayerSetupAdapter(
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<PlayerSetupAdapter.PlayerViewHolder>() {

    private var players: List<Player> = emptyList()
    var itemTouchHelper: ItemTouchHelper? = null

    /**
     * Update the adapter's player list and refresh the RecyclerView.
     *
     * @param newPlayers The new list of players to display
     */
    fun updatePlayers(newPlayers: List<Player>) {
        players = newPlayers
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = players.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val binding = ItemPlayerSetupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlayerViewHolder(binding, onRemoveClick)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(players[position])
        
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                itemTouchHelper?.startDrag(holder)
            }
            false
        }
    }

    /**
     * ViewHolder for a player setup item.
     * Exposes the drag handle for ItemTouchHelper integration.
     */
    class PlayerViewHolder(
        private val binding: ItemPlayerSetupBinding,
        private val onRemoveClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Public drag handle reference for ItemTouchHelper integration.
         */
        val dragHandle: ImageView = binding.ivDragHandle

        /**
         * Bind a player to this ViewHolder.
         *
         * @param player The player data to display
         */
        fun bind(player: Player) {
            binding.tvPlayerName.text = player.name
            binding.btnRemove.setOnClickListener {
                onRemoveClick(player.id)
            }
        }
    }
}
