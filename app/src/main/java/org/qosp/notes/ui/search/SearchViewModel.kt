package org.qosp.notes.ui.search

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.qosp.notes.data.model.Note
import org.qosp.notes.data.repo.NoteRepository
import org.qosp.notes.preferences.PreferenceRepository
import org.qosp.notes.preferences.SortMethod
import org.qosp.notes.ui.common.AbstractNotesViewModel

class SearchViewModel(
    noteRepository: NoteRepository,
    preferenceRepository: PreferenceRepository,
) : AbstractNotesViewModel(preferenceRepository) {
    private val searchKeyData: MutableStateFlow<String> = MutableStateFlow("")

    var isFirstLoad = true

    override val provideNotes = { sortMethod: SortMethod ->
        searchKeyData.debounce(300).flatMapLatest { searchKey ->
            noteRepository
                .getAll(sortMethod)
                .map { notes ->
                    getSearchResults(
                        searchKey.trim(),
                        notes,
                    )
                }
        }
    }

    private fun getSearchResults(
        searchKey: String,
        notes: List<Note>,
    ): List<Note> = notes.filter { note ->
        fun String.matches(): Boolean = contains(searchKey, true)

        note.taskList.any { it.content.matches() } || note.title.matches()
    }

    fun setSearchQuery(query: String) = viewModelScope.launch {
        searchKeyData.emit(query)
    }
}
