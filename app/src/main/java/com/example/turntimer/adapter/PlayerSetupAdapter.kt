package com.example.turntimer.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.turntimer.databinding.ItemPlayerSetupBinding
import com.example.turntimer.model.Player

/**
 * RecyclerView adapter for the player setup screen.
 * Supports drag-to-reorder via drag handles and player removal.
 *
 * @param onRemoveClick Callback invoked when a player is removed. Takes the player ID.
 * @param onColorSelected Callback invoked when a player's color is selected. Takes the player ID and color Int.
 */
class PlayerSetupAdapter(
    private val onRemoveClick: (Int) -> Unit,
    private val onColorSelected: (Int, Int) -> Unit
) : RecyclerView.Adapter<PlayerSetupAdapter.PlayerViewHolder>() {

    private var players: List<Player> = emptyList()
    private var unavailableColors: Set<Int> = emptySet()
    var itemTouchHelper: ItemTouchHelper? = null

    /**
     * Update the adapter's player list and unavailable colors, then refresh the RecyclerView.
     *
     * @param newPlayers The new list of players to display
     * @param usedColors The set of colors currently in use by players
     */
    fun updatePlayers(newPlayers: List<Player>, usedColors: Set<Int> = emptySet()) {
        players = newPlayers
        unavailableColors = usedColors
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = players.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val binding = ItemPlayerSetupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlayerViewHolder(binding, onRemoveClick, onColorSelected)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(players[position], unavailableColors)
        
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
        private val onRemoveClick: (Int) -> Unit,
        private val onColorSelected: (Int, Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Public drag handle reference for ItemTouchHelper integration.
         */
        val dragHandle: ImageView = binding.ivDragHandle

        /**
         * Bind a player to this ViewHolder.
         *
         * @param player The player data to display
         * @param unavailableColors Set of colors already in use by other players
         */
        fun bind(player: Player, unavailableColors: Set<Int>) {
            binding.tvPlayerName.text = player.name
            binding.btnRemove.setOnClickListener {
                onRemoveClick(player.id)
            }

            // Populate color container with 8 color circles
            val colorContainer = binding.colorContainer
            colorContainer.removeAllViews()

            Player.PALETTE.forEach { color ->
                val colorView = createColorCircle(
                    context = itemView.context,
                    color = color,
                    isSelected = color == player.color,
                    isUnavailable = color in unavailableColors && color != player.color,
                    onColorClick = { onColorSelected(player.id, color) }
                )
                colorContainer.addView(colorView)
            }
        }

        /**
         * Create a single color circle View for the color picker.
         */
        private fun createColorCircle(
            context: android.content.Context,
            color: Int,
            isSelected: Boolean,
            isUnavailable: Boolean,
            onColorClick: () -> Unit
        ): View {
            val circleView = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(28),
                    dpToPx(28)
                ).apply {
                    setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                }
            }

            // Create gradient drawable for circle shape
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)

                // Add white stroke if this is the selected color
                if (isSelected) {
                    setStroke(dpToPx(3), Color.WHITE)
                }
            }

            circleView.background = drawable

            // Dim unavailable colors
            if (isUnavailable) {
                circleView.alpha = 0.3f
                circleView.isEnabled = false
            } else {
                circleView.alpha = 1.0f
                circleView.isEnabled = true
                circleView.setOnClickListener {
                    onColorClick()
                }
            }

            return circleView
        }

        /**
         * Convert dp to pixels.
         */
        private fun dpToPx(dp: Int): Int {
            return (dp * itemView.context.resources.displayMetrics.density).toInt()
        }
    }
}

