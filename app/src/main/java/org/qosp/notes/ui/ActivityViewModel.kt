package org.qosp.notes.ui

import android.net.Uri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withContext
import org.qosp.notes.data.model.Note


import org.qosp.notes.data.repo.NoteRepository
import org.qosp.notes.preferences.GroupNotesWithoutNotebook
import org.qosp.notes.preferences.LayoutMode
import org.qosp.notes.preferences.NoteDeletionTime
import org.qosp.notes.preferences.PreferenceRepository
import org.qosp.notes.preferences.SortMethod
import org.qosp.notes.preferences.SortNavdrawerNotebooksMethod
import org.qosp.notes.preferences.SortTagsMethod
import org.qosp.notes.ui.utils.Toaster
import java.time.Instant

class ActivityViewModel(
    private val noteRepository: NoteRepository,

    private val preferenceRepository: PreferenceRepository,



    private val toaster: Toaster,
) : ViewModel() {



    fun discardEmptyNotesAsync() = viewModelScope.async(Dispatchers.IO) { noteRepository.discardEmptyNotes() }

    fun deleteNotesPermanently(vararg notes: Note) = viewModelScope.launch(Dispatchers.IO) {
        noteRepository.deleteNotes(*notes)
    }

    fun deleteNotes(vararg notes: Note) {
        viewModelScope.launch(Dispatchers.IO) {

            when (preferenceRepository.get<NoteDeletionTime>().first()) {
                NoteDeletionTime.INSTANTLY -> {
                    noteRepository.deleteNotes(*notes)
                }

                else -> {
                    noteRepository.moveNotesToBin(*notes)
                }
            }
        }
    }

    fun restoreNotes(vararg notes: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { noteRepository.restoreNotes(*notes) }
            if (result.isFailure) {
                toaster.showLong(result.exceptionOrNull()?.message ?: "Error restoring notes")
            }
        }
    }

    fun archiveNotes(vararg notes: Note) =
        update(*notes) { it.copy(isArchived = true, modifiedDate = Instant.now().epochSecond) }

    fun unarchiveNotes(vararg notes: Note) =
        update(*notes) { it.copy(isArchived = false, modifiedDate = Instant.now().epochSecond) }





    fun duplicateNotes(vararg notes: Note) = notes.forEachAsync { note ->
        val oldId = note.id
        val cloned = note.copy(
            id = 0L,
            creationDate = Instant.now().epochSecond,
            modifiedDate = Instant.now().epochSecond,
            deletionDate = if (note.isDeleted) Instant.now().epochSecond else null
        )

        val newId = noteRepository.insertNote(cloned)
        // tagRepository.copyTags(oldId, newId) // Legacy tags copy removed/disabled
    }

    fun setLayoutMode(layoutMode: LayoutMode) {
        viewModelScope.launch(Dispatchers.IO) { preferenceRepository.set(layoutMode) }
    }

    fun setSortMethod(method: SortMethod) {
        viewModelScope.launch(Dispatchers.IO) { preferenceRepository.set(method) }
    }

    fun setSortTagsMethod(method: SortTagsMethod) {
        viewModelScope.launch(Dispatchers.IO) { preferenceRepository.set(method) }
    }

    fun setSortNavdrawerNotebooksMethod(method: SortNavdrawerNotebooksMethod) {
        viewModelScope.launch(Dispatchers.IO) { preferenceRepository.set(method) }
    }



    private inline fun update(
        vararg notes: Note,
        crossinline transform: suspend (Note) -> Note,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val notes = notes
            .map { transform(it) }
            .toTypedArray()

        noteRepository.updateNotes(*notes)
    }

    private inline fun Array<out Note>.forEachAsync(crossinline block: suspend CoroutineScope.(Note) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { forEach { block(it) } }
    }
}
