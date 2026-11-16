package com.studyos.cainter.ui.dashboard

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studyos.cainter.R
import com.studyos.cainter.data.local.entity.TodoItem
import com.studyos.cainter.databinding.ItemTodoBinding

class TodoAdapter(
    private val onToggleDone: (TodoItem, Boolean) -> Unit,
    private val onDelete: (TodoItem) -> Unit
) : ListAdapter<TodoItem, TodoAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTodoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(private val binding: ItemTodoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TodoItem) {
            binding.tvTodoTitle.text = item.title

            // Remove listener before setting checked state to avoid circular events
            binding.cbTodoCompleted.setOnCheckedChangeListener(null)
            binding.cbTodoCompleted.isChecked = item.isCompleted

            val context = itemView.context
            if (item.isCompleted) {
                binding.tvTodoTitle.paintFlags =
                    binding.tvTodoTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvTodoTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.text_tertiary_light)
                )
                binding.cardTodoItem.alpha = 0.65f
            } else {
                binding.tvTodoTitle.paintFlags =
                    binding.tvTodoTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvTodoTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.text_primary_light)
                )
                binding.cardTodoItem.alpha = 1.0f
            }

            binding.cbTodoCompleted.setOnCheckedChangeListener { _, isChecked ->
                onToggleDone(item, isChecked)
            }

            binding.btnDeleteTodo.setOnClickListener {
                onDelete(item)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TodoItem>() {
            override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
