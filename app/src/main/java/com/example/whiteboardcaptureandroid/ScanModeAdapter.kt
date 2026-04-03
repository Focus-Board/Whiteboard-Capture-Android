package com.example.whiteboardcaptureandroid

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.whiteboardcaptureandroid.databinding.ItemScanModeBinding

class ScanModeAdapter(
    private val modes: List<ScanMode>,
    private val onModeSelected: (ScanMode) -> Unit
) : RecyclerView.Adapter<ScanModeAdapter.ModeViewHolder>() {

    private var selectedPosition = 0

    inner class ModeViewHolder(val binding: ItemScanModeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(mode: ScanMode, isSelected: Boolean) {
            binding.modeTitle.text = mode.displayName

            if (isSelected) {
                binding.modeCard.strokeColor = Color.parseColor("#4CAF50")
                binding.modeCard.strokeWidth = 6
                binding.modeCard.setCardBackgroundColor(Color.parseColor("#1A4CAF50"))
            } else {
                binding.modeCard.strokeColor = Color.parseColor("#555555")
                binding.modeCard.strokeWidth = 4
                binding.modeCard.setCardBackgroundColor(Color.parseColor("#222222"))
            }

            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onModeSelected(mode)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModeViewHolder {
        val binding = ItemScanModeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ModeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModeViewHolder, position: Int) {
        holder.bind(modes[position], position == selectedPosition)
    }

    override fun getItemCount() = modes.size

    fun setSelectedMode(mode: ScanMode) {
        val newPosition = modes.indexOf(mode)
        if (newPosition != -1 && newPosition != selectedPosition) {
            val previousPosition = selectedPosition
            selectedPosition = newPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
        }
    }
}