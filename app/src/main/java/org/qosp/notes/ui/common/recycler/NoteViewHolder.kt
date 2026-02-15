package org.qosp.notes.ui.common.recycler

import android.content.Context
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.qosp.notes.R
import org.qosp.notes.data.model.Note
import org.qosp.notes.data.model.NoteColor
import org.qosp.notes.databinding.LayoutNoteBinding
import org.qosp.notes.ui.tasks.TasksAdapter
import org.qosp.notes.ui.utils.dp
import org.qosp.notes.ui.utils.resId

class NoteViewHolder(
    private val binding: LayoutNoteBinding,
    listener: NoteRecyclerListener?,
    private val context: Context,
    private val searchMode: Boolean,
    tasksViewPool: RecyclerView.RecycledViewPool,
) : RecyclerView.ViewHolder(binding.root), SelectableViewHolder {

    private val tasksAdapter = TasksAdapter(true, null)

    private val defaultStrokeWidth = 1.dp(context)
    private val selectedStrokeWidth = 2.dp(context)

    init {
        binding.recyclerAttachments.isVisible = false // Always hide attachments
        binding.containerTags.isVisible = false // Always hide tags

        binding.recyclerTasks.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = tasksAdapter
            setRecycledViewPool(tasksViewPool)
        }

        if (listener != null) {
            itemView.setOnClickListener { listener.onItemClick(bindingAdapterPosition, binding) }
            itemView.setOnLongClickListener { listener.onLongClick(bindingAdapterPosition, binding) }
        }
    }

    private fun updateBackgroundColor(color: NoteColor) {
        color.resId(context)?.let { resId ->
            binding.root.setCardBackgroundColor(resId)
        }
    }

    private fun updateIndicatorIcons(note: Note) = with(binding) {
        indicatorNoteHidden.isVisible = false // note.isHidden && !searchMode
        indicatorPinned.isVisible = false // note.isPinned && !searchMode
        indicatorHasReminder.isVisible = false // hasReminders
        indicatorDeleted.isVisible = note.isDeleted && searchMode
        indicatorArchived.isVisible = note.isArchived && searchMode
    }

    private fun setTitle(note: Note) {
        if (note.title.isEmpty()) {
            binding.textViewTitle.isVisible = false
        } else {
            binding.textViewTitle.isVisible = true
            binding.textViewTitle.text = note.title
        }
    }

    private fun setContent(note: Note) = with(binding) {
        val showTasks = true // Always show tasks if any
        val showContent = false // Never show content (it's checklist only)

        recyclerTasks.isVisible = showTasks
        indicatorMoreTasks.isVisible = false
        textViewContent.isVisible = showContent

        if (showTasks) {
            val taskList = note.taskList.takeIf { it.size <= 8 } ?: note.taskList.subList(0, 8).also {
                val moreItems = note.taskList.size - 8
                val showMoreIndicator = moreItems > 0
                indicatorMoreTasks.isVisible = showMoreIndicator
                if (showMoreIndicator) {
                    indicatorMoreTasks.text =
                        context.resources.getQuantityString(R.plurals.more_items, moreItems, moreItems)
                }
            }
            tasksAdapter.submitList(taskList)
        }
    }

    fun runPayloads(note: Note, payloads: List<NoteRecyclerAdapter.Payload>) {
        payloads.forEach {
            when (it) {
                NoteRecyclerAdapter.Payload.TitleChanged -> setTitle(note)
                NoteRecyclerAdapter.Payload.ColorChanged -> updateBackgroundColor(note.color)
                NoteRecyclerAdapter.Payload.ArchivedChanged -> updateIndicatorIcons(note)
                NoteRecyclerAdapter.Payload.DeletedChanged -> updateIndicatorIcons(note)
                NoteRecyclerAdapter.Payload.TasksChanged -> setContent(note)
                else -> {}
            }
        }
    }

    fun bind(note: Note) {
        setContent(note)
        setTitle(note)
        updateBackgroundColor(note.color)
        updateIndicatorIcons(note)
        
        ViewCompat.setTransitionName(binding.root, "editor_${note.id}")
    }

    override fun onSelectedStatusChanged(isSelected: Boolean) {
        binding.root.isChecked = isSelected
        binding.root.strokeWidth = if (isSelected) selectedStrokeWidth else defaultStrokeWidth
    }
}
