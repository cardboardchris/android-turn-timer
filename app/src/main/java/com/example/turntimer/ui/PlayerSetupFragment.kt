package com.example.turntimer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.turntimer.R
import com.example.turntimer.adapter.PlayerSetupAdapter
import com.example.turntimer.databinding.FragmentPlayerSetupBinding
import com.example.turntimer.model.Player
import com.example.turntimer.viewmodel.GameViewModel
import kotlinx.coroutines.launch

class PlayerSetupFragment : Fragment() {

    private var _binding: FragmentPlayerSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameViewModel by activityViewModels()
    private lateinit var adapter: PlayerSetupAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupButtons()
        setupInputValidation()
        observePlayers()
    }

    private fun setupRecyclerView() {
        adapter = PlayerSetupAdapter(
            onRemoveClick = { playerId ->
                viewModel.removePlayer(playerId)
            },
            onColorSelected = { playerId, color ->
                viewModel.changePlayerColor(playerId, color)
            }
        )

        binding.rvPlayers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlayers.adapter = adapter

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun isLongPressDragEnabled() = false

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                
                viewModel.movePlayer(from, to)
                adapter.notifyItemMoved(from, to)
                
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }

        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.rvPlayers)
        adapter.itemTouchHelper = itemTouchHelper
    }

    private fun setupButtons() {
        binding.btnAddPlayer.setOnClickListener {
            val name = binding.etPlayerName.text.toString()
            if (viewModel.addPlayer(name)) {
                binding.etPlayerName.text?.clear()
            }
        }

        binding.btnStartGame.setOnClickListener {
            viewModel.startGame()
        }
    }

    private fun setupInputValidation() {
        binding.etPlayerName.doAfterTextChanged {
            updateAddButtonState()
        }
    }

    private fun observePlayers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.players.collect { players ->
                val usedColors = viewModel.getUsedColors()
                adapter.updatePlayers(players, usedColors)
                updateUI(players)
            }
        }
    }

    private fun updateUI(players: List<Player>) {
        binding.tvPlayerCount.text = getString(R.string.player_count, players.size)
        updateAddButtonState()
        binding.btnStartGame.isEnabled = viewModel.canStartGame()
    }

    private fun updateAddButtonState() {
        val inputText = binding.etPlayerName.text.toString()
        val playerCount = viewModel.players.value.size
        binding.btnAddPlayer.isEnabled = inputText.isNotBlank() && playerCount < 5
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
