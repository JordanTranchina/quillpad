package org.qosp.notes.data.repo

import kotlinx.coroutines.flow.Flow
import me.msoul.datastore.defaultOf
import org.qosp.notes.data.model.Note
import org.qosp.notes.preferences.SortMethod

interface NoteRepository {
    suspend fun insertNote(note: Note): Long
    suspend fun updateNotes(vararg notes: Note)
    suspend fun moveNotesToBin(vararg notes: Note)
    suspend fun restoreNotes(vararg notes: Note)
    suspend fun deleteNotes(vararg notes: Note)
    suspend fun discardEmptyNotes(): Boolean
    suspend fun permanentlyDeleteNotesInBin()

    fun getById(noteId: Long): Flow<Note?>
    fun getDeleted(sortMethod: SortMethod = defaultOf()): Flow<List<Note>>
    fun getArchived(sortMethod: SortMethod = defaultOf()): Flow<List<Note>>
    fun getNonDeleted(sortMethod: SortMethod = defaultOf()): Flow<List<Note>>
    fun getNonDeletedOrArchived(sortMethod: SortMethod = defaultOf()): Flow<List<Note>>
    fun getAll(sortMethod: SortMethod = defaultOf()): Flow<List<Note>>
    fun getAllBlankTitleNotes(): Flow<List<Note>>
}
