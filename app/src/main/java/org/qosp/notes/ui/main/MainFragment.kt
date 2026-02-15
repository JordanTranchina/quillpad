package org.qosp.notes.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.navigation.NavDirections
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.qosp.notes.R
import org.qosp.notes.databinding.FragmentMainBinding
import org.qosp.notes.databinding.LayoutNoteBinding
import org.qosp.notes.preferences.LayoutMode
import org.qosp.notes.ui.common.AbstractNotesFragment
import org.qosp.notes.ui.utils.navigateSafely
import org.qosp.notes.ui.utils.viewBinding

open class MainFragment : AbstractNotesFragment(R.layout.fragment_main) {
    private val binding by viewBinding(FragmentMainBinding::bind)

    override val currentDestinationId: Int = R.id.fragment_main
    override val model: MainViewModel by viewModel()

    override val recyclerView: RecyclerView
        get() = binding.recyclerMain
    override val swipeRefreshLayout: SwipeRefreshLayout
        get() = binding.layoutSwipeRefresh
    override val snackbarLayout: View
        get() = binding.fabCreateNote
    override val snackbarAnchor: View
        get() = binding.fabCreateNote
    override val emptyIndicator: View
        get() = binding.indicatorNotesEmpty
    override val appBarLayout: AppBarLayout
        get() = binding.layoutAppBar.appBar
    override val toolbar: Toolbar
        get() = binding.layoutAppBar.toolbar
    override val toolbarTitle: String
        get() = getString(R.string.nav_notes)
    override val secondaryToolbar: Toolbar
        get() = binding.layoutAppBar.toolbarSelection
    override val secondaryToolbarMenuRes: Int = R.menu.main_selected_notes

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFab()
        setupBottomAppBar()
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_top, menu)
        mainMenu = menu
        // Removed setHiddenNotesItemActionText
        setLayoutChangeActionIcon()
        // Removed selectSortMethodItem as sorting might be fixed/simplified
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> findNavController().navigateSafely(actionToSearch())
            R.id.action_layout_mode -> toggleLayoutMode()
            // Removed sort actions and hidden notes actions
            R.id.action_select_all -> selectAllNotes()
        }
        return super.onOptionsItemSelected(item)
    }

    open fun actionToEditor(
        transitionName: String,
        noteId: Long,
    ): NavDirections =
        MainFragmentDirections.actionMainToEditor(transitionName)
            .setNoteId(noteId)

    open fun actionToSearch(searchQuery: String = ""): NavDirections =
        MainFragmentDirections.actionMainToSearch().setSearchQuery(searchQuery)

    override fun onNoteClick(noteId: Long, position: Int, viewBinding: LayoutNoteBinding) {
        goToEditor(noteId, sharedElement = viewBinding.root, fromPosition = position)
    }

    override fun onNoteLongClick(noteId: Long, position: Int, viewBinding: LayoutNoteBinding): Boolean {
        showMenuForNote(position)
        return true
    }

    override fun onSelectionChanged(selectedIds: List<Long>) {
        super.onSelectionChanged(selectedIds)

        val inSelectionMode = selectedIds.isNotEmpty()
        binding.bottomAppBar.isVisible = !inSelectionMode
        binding.fabCreateNote.isVisible = !inSelectionMode
    }

    override fun onLayoutModeChanged() {
        super.onLayoutModeChanged()
        setLayoutChangeActionIcon()
    }

    private fun goToEditor(
        noteId: Long? = null,
        fromPosition: Int? = null,
        sharedElement: View,
    ) {
        applyNavToEditorAnimation(fromPosition)
        when (noteId) {
            null -> {
                findNavController().navigateSafely(
                    actionToEditor(
                        transitionName = "editor_create",
                        noteId = 0L,
                    ),
                    FragmentNavigatorExtras(sharedElement to "editor_create")
                )
            }
            else -> {
                findNavController().navigateSafely(
                    actionToEditor("editor_$noteId", noteId),
                    FragmentNavigatorExtras(sharedElement to "editor_$noteId")
                )
            }
        }
    }

    private fun setupFab() {
        ViewCompat.setTransitionName(binding.fabCreateNote, "editor_create")
        binding.fabCreateNote.setOnClickListener { goToEditor(sharedElement = binding.fabCreateNote) }
    }

    private fun setLayoutChangeActionIcon() {
        mainMenu?.findItem(R.id.action_layout_mode)?.apply {
            isVisible = true
            setIcon(if (data.layoutMode == LayoutMode.GRID) R.drawable.ic_list else R.drawable.ic_grid)
        }
    }

    private fun setupBottomAppBar() {
        binding.bottomAppBar.setOnMenuItemClickListener { it ->
            when (it.itemId) {
                // Removed action_create_list as redundant or just create regular note
                else -> false
            }
        }
    }
}
