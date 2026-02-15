package org.qosp.notes.ui.editor

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.qosp.notes.R
import org.qosp.notes.data.model.Note
import org.qosp.notes.data.model.NoteTask
import org.qosp.notes.databinding.FragmentEditorBinding
import org.qosp.notes.ui.common.BaseFragment
import org.qosp.notes.ui.tasks.TaskRecyclerListener
import org.qosp.notes.ui.tasks.TaskViewHolder
import org.qosp.notes.ui.tasks.TasksAdapter
import org.qosp.notes.ui.utils.dp
import org.qosp.notes.ui.utils.getDrawableCompat
import org.qosp.notes.ui.utils.hideKeyboard
import org.qosp.notes.ui.utils.liftAppBarOnScroll
import org.qosp.notes.ui.utils.resolveAttribute
import org.qosp.notes.ui.utils.shareNote
import org.qosp.notes.ui.utils.viewBinding

private typealias Data = EditorViewModel.Data

class EditorFragment : BaseFragment(R.layout.fragment_editor) {
    private val binding by viewBinding(FragmentEditorBinding::bind)
    private val model: EditorViewModel by viewModel()

    private val args: EditorFragmentArgs by navArgs()
    private var snackbar: Snackbar? = null
    private var mainMenu: Menu? = null
    private var isNoteDeleted: Boolean = false

    private var data = Data()

    private lateinit var tasksAdapter: TasksAdapter

    override val hasDefaultAnimation = false
    override val toolbar: Toolbar
        get() = binding.toolbar

    private val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(UP or DOWN, LEFT or RIGHT) {
        override fun isLongPressDragEnabled() = false
        override fun isItemViewSwipeEnabled() = model.inEditMode
        override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = 0.5F
        override fun getSwipeEscapeVelocity(defaultValue: Float) = 3 * defaultValue
        override fun getSwipeVelocityThreshold(defaultValue: Float) = defaultValue / 3

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val tasks = tasksAdapter.tasks.toMutableList()
            tasks.removeAt(viewHolder.bindingAdapterPosition)
            tasksAdapter.submitList(tasks)
            model.updateTaskList(tasksAdapter.tasks)
            // notifyItemRemoved is called by DiffUtil in submitList usually, but here we modify list then submit?
            // TasksAdapter.submitList creates a new list diff. 
            // Better to let adapter handle it or standard way. 
            // For now sticking to logic: modify list -> submit
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            tasksAdapter.moveItem(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
            model.updateTaskList(tasksAdapter.tasks)
            return true
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean,
        ) {
            when (actionState) {
                ACTION_STATE_DRAG -> {
                    val top = viewHolder.itemView.top + dY
                    val bottom = top + viewHolder.itemView.height
                    if (top > 0 && bottom < recyclerView.height) {
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    }
                }

                ACTION_STATE_SWIPE -> {
                    val newDx = dX / 3
                    val p = Paint().apply { color = context?.resolveAttribute(R.attr.colorTaskSwipe) ?: Color.RED }
                    val itemView = viewHolder.itemView
                    val icon = context?.getDrawableCompat(R.drawable.ic_indicator_delete_task)?.toBitmap()
                    val height = itemView.bottom - itemView.top
                    val size = (24).dp(requireContext())

                    if (dX < 0) {
                        val background = RectF(
                            itemView.right.toFloat() + newDx,
                            itemView.top.toFloat(),
                            itemView.right.toFloat(),
                            itemView.bottom.toFloat()
                        )
                        c.drawRect(background, p)

                        val iconRect = RectF(
                            background.right - size - 16.dp(requireContext()),
                            background.top + (height - size) / 2,
                            background.right - 16.dp(requireContext()),
                            background.bottom - (height - size) / 2,
                        )
                        if (icon != null) c.drawBitmap(icon, null, iconRect, p)
                    } else if (dX > 0) {
                        val background = RectF(
                            itemView.left.toFloat(),
                            itemView.top.toFloat(),
                            newDx,
                            itemView.bottom.toFloat()
                        )
                        c.drawRect(background, p)
                        val iconRect = RectF(
                            background.left + 16.dp(requireContext()),
                            background.top + (height - size) / 2,
                            background.left + size + 16.dp(requireContext()),
                            background.bottom - (height - size) / 2,
                        )
                        if (icon != null) c.drawBitmap(icon, null, iconRect, p)
                    }
                    super.onChildDraw(c, recyclerView, viewHolder, newDx, dY, actionState, isCurrentlyActive)
                }
            }
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            (viewHolder as TaskViewHolder?)?.let { vh ->
                vh.isBeingMoved = true
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            (viewHolder as TaskViewHolder?)?.let {
                if (it.isBeingMoved) it.isBeingMoved = false
            }
            model.updateTaskList(tasksAdapter.tasks)
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = 300L
            scrimColor = Color.TRANSPARENT
            requireContext().resolveAttribute(R.attr.colorBackground)?.let { setAllContainerColors(it) }
        }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply { duration = 300L }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        data = Data()

        if (model.isNotInitialized) {
            model.initialize(
                noteId = args.noteId,
                newNoteTitle = args.newNoteTitle,
                newNoteContent = args.newNoteContent,
                newNoteAttachments = emptyList(),
                newNoteIsList = true,
                newNoteNotebookId = null
            )
        }

        setupTasksRecycler()
        observeData()
        setupEditTexts()
        setupListeners()

        toolbar.setTitleTextColor(Color.TRANSPARENT)
        ViewCompat.setTransitionName(binding.root, args.transitionName)
        binding.scrollView.liftAppBarOnScroll(
            binding.layoutAppBar,
            requireContext().resources.getDimension(R.dimen.app_bar_elevation)
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.editor_top, menu)
        this.mainMenu = menu

        lifecycleScope.launch {
            model.data.first().note?.let { setupMenuItems(it) }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        data.note?.let { note ->
            when (item.itemId) {
                R.id.action_archive_note -> {
                    if (note.isArchived) activityModel.unarchiveNotes(note) else activityModel.archiveNotes(note)
                    showMessage(getString(R.string.indicator_archive_note))
                    activity?.onBackPressed()
                }

                R.id.action_delete_note -> {
                    activityModel.deleteNotes(note)
                    showMessage(getString(R.string.indicator_moved_note_to_bin))
                    activity?.onBackPressed()
                }

                R.id.action_restore_note -> {
                    activityModel.restoreNotes(note)
                    activity?.onBackPressed()
                }

                R.id.action_delete_permanently_note -> {
                    activityModel.deleteNotesPermanently(note)
                    showMessage(getString(R.string.indicator_deleted_note_permanently))
                    activity?.onBackPressed()
                }

                R.id.action_share -> {
                    shareNote(requireContext(), note)
                }
                
                R.id.action_uncheck_all_tasks -> {
                    uncheckAllTasks()
                    true
                }

                R.id.action_remove_all_checked_tasks -> {
                    removeAllCheckedTasks()
                    true
                }

                else -> false
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        snackbar?.dismiss()
        itemTouchHelper.attachToRecyclerView(null)
        tasksAdapter.listener = null
        super.onDestroyView()
    }

    private fun jumpToNextTaskOrAdd(fromPosition: Int) {
        val next = tasksAdapter.tasks.getOrNull(fromPosition + 1)
        if (next == null || next.content.isNotEmpty()) {
            addTask(fromPosition + 1)
            return
        }
        (binding.recyclerTasks.findViewHolderForAdapterPosition(fromPosition + 1) as TaskViewHolder).requestFocus()
    }

    private fun setupTasksRecycler() {
        tasksAdapter = TasksAdapter(
            false,
            object : TaskRecyclerListener {
                override fun onDrag(viewHolder: TaskViewHolder) {
                    itemTouchHelper.startDrag(viewHolder)
                }

                override fun onTaskStatusChanged(position: Int, isDone: Boolean) {
                    updateTask(position = position, isDone = isDone)
                    if (isDone && data.moveCheckedItems) {
                        tasksAdapter.moveItem(position, tasksAdapter.tasks.lastIndex)
                        model.updateTaskList(tasksAdapter.tasks)
                    }
                }

                override fun onTaskContentChanged(position: Int, content: String) {
                    updateTask(position = position, content = content)
                }

                override fun onNext(position: Int) {
                    jumpToNextTaskOrAdd(position)
                }
            },
        )

        binding.recyclerTasks.apply {
            isVisible = true
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = tasksAdapter
            itemTouchHelper.attachToRecyclerView(this)
        }
    }

    private fun setupEditTexts() = with(binding) {
        editTextTitle.apply {
            imeOptions = EditorInfo.IME_ACTION_NEXT
            setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)

            setOnEditorActionListener { _, actionId, _ ->
                when {
                    actionId == EditorInfo.IME_ACTION_NEXT -> {
                        jumpToNextTaskOrAdd(-1)
                        true
                    }
                    else -> false
                }
            }

            doOnTextChanged { text, _, _, _ ->
                if (data.note == null) return@doOnTextChanged
                model.setNoteTitle(text.toString().trim())
            }
        }

        // Used to clear focus and hide the keyboard when touching outside of the edit texts
        linearLayout.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) root.hideKeyboard()
        }
    }

    private fun setupMenuItems(note: Note) = mainMenu?.run {
        findItem(R.id.action_restore_note)?.isVisible = note.isDeleted
        findItem(R.id.action_delete_permanently_note)?.isVisible = note.isDeleted
        findItem(R.id.action_delete_note)?.isVisible = !note.isDeleted
        
        findItem(R.id.action_archive_note)?.apply {
            title = if (note.isArchived) getString(R.string.action_unarchive) else getString(R.string.action_archive)
            isVisible = !note.isDeleted
        }

        findItem(R.id.action_uncheck_all_tasks)?.apply {
            isVisible = !note.isDeleted
        }

        findItem(R.id.action_remove_all_checked_tasks)?.apply {
            isVisible = !note.isDeleted
        }
    }

    private fun observeData() = with(binding) {
        lifecycleScope.launch {
            model.data.collect { data ->
                if (data.note == null && data.isInitialized) {
                     findNavController().navigateUp()
                     return@collect
                }

                val currentNote = data.note
                if (currentNote == null) return@collect

                this@EditorFragment.data = data
                model.inEditMode = true

                this@EditorFragment.isNoteDeleted = currentNote.isDeleted

                if (editTextTitle.text.toString() != currentNote.title) {
                    editTextTitle.setText(currentNote.title)
                }
                
                tasksAdapter.submitList(currentNote.taskList)
            }
        }
    }

    private fun setupListeners() = with(binding) {
        toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun addTask(position: Int) {
        val tasks = tasksAdapter.tasks.toMutableList()
        tasks.add(position, NoteTask(0L, "", false))
        tasksAdapter.submitList(tasks)
        // Focus logic would need to happen after list submission and binding
    }

    private fun updateTask(position: Int, content: String? = null, isDone: Boolean? = null) {
        if (position >= 0 && position < tasksAdapter.tasks.size) {
            val task = tasksAdapter.tasks[position]
            content?.let { task.content = it }
            isDone?.let { task.isDone = it }
            model.updateTaskList(tasksAdapter.tasks)
         }
    }

    private fun uncheckAllTasks() {
        val tasks = tasksAdapter.tasks.onEach { it.isDone = false }
        model.updateTaskList(tasks)
        tasksAdapter.submitList(tasks)
        tasksAdapter.notifyDataSetChanged()
    }

    private fun removeAllCheckedTasks() {
        val tasks = tasksAdapter.tasks.filter { !it.isDone }
        model.updateTaskList(tasks)
        tasksAdapter.submitList(tasks)
    }

    private fun showMessage(msg: String) {
        val view = binding.root
        snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_SHORT)
        snackbar?.show()
    }
}
