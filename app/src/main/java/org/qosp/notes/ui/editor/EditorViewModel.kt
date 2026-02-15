package org.qosp.notes.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.msoul.datastore.defaultOf
import org.qosp.notes.data.model.Note
import org.qosp.notes.data.model.NoteColor
import org.qosp.notes.data.model.NoteTask
import org.qosp.notes.data.repo.NoteRepository
import org.qosp.notes.preferences.DateFormat
import org.qosp.notes.preferences.DefaultEditorMode
import org.qosp.notes.preferences.MoveCheckedItems
import org.qosp.notes.preferences.NewNotesSyncable
import org.qosp.notes.preferences.OpenMediaIn
import org.qosp.notes.preferences.PreferenceRepository
import org.qosp.notes.preferences.ShowDate
import org.qosp.notes.preferences.ShowFabChangeMode
import org.qosp.notes.preferences.TimeFormat
import java.time.Instant

class EditorViewModel(
    private val noteRepository: NoteRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    var inEditMode: Boolean = true
    var isNotInitialized = true
    var moveCheckedItems: Boolean = true
    private val noteIdFlow: MutableStateFlow<Long?> = MutableStateFlow(null)
    var selectedRange = 0 to 0

    val data = noteIdFlow
        .filterNotNull()
        .flatMapLatest { noteRepository.getById(it) }
        .filterNotNull()
        .flatMapLatest { note ->
            preferenceRepository.getAll().map { prefs ->
                Data(
                    note = note,
                    dateTimeFormats = prefs.dateFormat to prefs.timeFormat,
                    openMediaInternally = prefs.openMediaIn == OpenMediaIn.INTERNAL,
                    showDates = prefs.showDate == ShowDate.YES,
                    editorFontSize = prefs.editorFontSize.fontSize,
                    showFabChangeMode = prefs.showFabChangeMode == ShowFabChangeMode.FAB,
                    defaultEditorMode = prefs.defaultEditorMode,
                    isInitialized = true,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Data(),
        )

    fun initialize(
        noteId: Long,
        newNoteTitle: String,
        newNoteContent: String?,  // Kept for signature compatibility but ignored or used for title if needed? No, title is separate.
        newNoteAttachments: List<Any>, // Type doesn't matter as we ignore
        newNoteIsList: Boolean,
        newNoteNotebookId: Long?,
    ) {
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) {
                if (noteId > 0L) return@withContext noteId

                noteRepository.insertNote(
                    Note(
                        title = newNoteTitle,
                        // Checklist only, so no content/isList needed as per new model
                    ),
                )
            }

            noteIdFlow.emit(id)

            isNotInitialized = false
            moveCheckedItems = preferenceRepository.get<MoveCheckedItems>().first() == MoveCheckedItems.YES
        }
    }

    fun setNoteTitle(title: String) = update { note ->
        note.copy(
            title = title,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    // Deprecated/Unused but keeping for compilation compatibility with Fragment calls if any remain
    fun setNoteContent(content: String) { /* No-op for checklist only */ }

    fun setColor(color: NoteColor) = update { note ->
        note.copy(
            color = color,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    fun deleteAttachment(attachment: Any) { /* No-op */ }
    fun insertAttachments(vararg attachments: Any) { /* No-op */ }

    fun updateTaskList(list: List<NoteTask>) = update { note ->
        note.copy(
            taskList = list,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    fun toList() { /* Already loop */ }
    fun toTextNote() { /* No-op, enforce list */ }

    private inline fun update(crossinline transform: suspend (Note) -> Note) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = data.value.note ?: return@launch
            val new = transform(note)
            noteRepository.updateNotes(new)
        }
    }

    data class Data(
        val note: Note? = null,
        val dateTimeFormats: Pair<DateFormat, TimeFormat> = defaultOf<DateFormat>() to defaultOf<TimeFormat>(),
        val openMediaInternally: Boolean = true,
        val showDates: Boolean = true,
        val editorFontSize: Int = -1, // -1: not customised, default font size
        val showFabChangeMode: Boolean = true,
        val defaultEditorMode: DefaultEditorMode = defaultOf(),
        val isInitialized: Boolean = false,
        val moveCheckedItems: Boolean = true,
    )
}

private const val TAG = "EditorViewModel"



