package org.qosp.notes.ui.common.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

import org.qosp.notes.data.model.Note
import org.qosp.notes.databinding.LayoutNoteBinding

class NoteRecyclerAdapter(
    var listener: NoteRecyclerListener?,
) : ExtendedListAdapter<Note, NoteViewHolder>(DiffCallback()) {

    private var allItems = listOf<Note>()
    private var visibleItems = listOf<Note>()
    var searchMode: Boolean = false

    private val tasksViewPool = RecyclerView.RecycledViewPool()



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = LayoutNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(
            binding = binding,
            listener = listener,
            context = parent.context,
            searchMode = searchMode,
            tasksViewPool = tasksViewPool,
        )
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int, payloads: MutableList<Any>) {
        val flattenedPayloads = payloads.filterIsInstance<List<Payload>>().flatten()

        if (flattenedPayloads.isEmpty()) {
            return super.onBindViewHolder(holder, position, payloads)
        }

        holder.runPayloads(getItem(position), flattenedPayloads)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val note: Note = getItem(position)
        holder.bind(note)
    }

    override fun getItemId(position: Int) = getItem(position).id

    override fun submitList(list: List<Note>?) {
        if (list != null) {
            allItems = list
            visibleItems = list // No hidden notes support

            super.submitList(visibleItems)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: Note, newItem: Note): Any? {
            val payloads = mutableListOf<Payload>()
            if (oldItem.title != newItem.title) payloads.add(Payload.TitleChanged)

            if (oldItem.color != newItem.color) payloads.add(Payload.ColorChanged)
            if (oldItem.isArchived != newItem.isArchived) payloads.add(Payload.ArchivedChanged)
            if (oldItem.isDeleted != newItem.isDeleted) payloads.add(Payload.DeletedChanged)
            if (oldItem.taskList != newItem.taskList) payloads.add(Payload.TasksChanged)

            return payloads.takeIf { it.isNotEmpty() }
        }
    }

    enum class Payload {
        TitleChanged,
        ArchivedChanged,
        DeletedChanged,

        ColorChanged,
        TasksChanged,
    }
}
