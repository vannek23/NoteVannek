package com.example.notev

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class NoteAdapter(
    private val onClick: (Note) -> Unit,
    private val onLongClick: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.tvDate.text = note.date
        holder.tvTitle.text = note.title
        holder.tvSum.text = if (note.totalSum > 0) note.totalSum.toInt().toString() else ""

        holder.itemView.setOnClickListener { onClick(note) }
        holder.itemView.setOnLongClickListener {
            onLongClick(note)
            true
        }
    }

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvNoteDate)
        val tvTitle: TextView = view.findViewById(R.id.tvNoteTitle)
        val tvSum: TextView = view.findViewById(R.id.tvNoteSum)

        init {
            itemView.addPressScaleAnimation()
        }
    }
}

class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
    override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
        return oldItem == newItem
    }
}