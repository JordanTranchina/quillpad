package org.qosp.notes.data.repo

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import me.msoul.datastore.defaultOf
import org.qosp.notes.data.dao.NoteDao
import org.qosp.notes.data.model.Note
import org.qosp.notes.data.model.NoteEntity
import org.qosp.notes.preferences.SortMethod
import java.time.Instant

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
) : NoteRepository {

    private val tag = NoteRepositoryImpl::class.java.simpleName

    override suspend fun insertNote(note: Note): Long {
        Log.d(tag, "insertNote: Creating note '${note.title}'")
        return noteDao.insert(note.toEntity())
    }

    override suspend fun updateNotes(vararg notes: Note) {
        notes.forEach { note ->
            Log.d(tag, "updateNote: Updating note ID=${note.id}, title='${note.title}'")
            noteDao.update(note.toEntity())
        }
    }

    override suspend fun moveNotesToBin(vararg notes: Note) {
        Log.d(tag, "moveNotesToBin: Moving ${notes.size} notes to bin")
        val entities = notes.map { it.toEntity().copy(isDeleted = true, deletionDate = Instant.now().epochSecond) }
            .toTypedArray<NoteEntity>()
        noteDao.update(*entities)
    }

    override suspend fun restoreNotes(vararg notes: Note) {
        Log.d(tag, "restoreNotes: Restoring ${notes.size} notes from bin")
        val array = notes
            .map { it.toEntity().copy(isDeleted = false, deletionDate = null) }
            .toTypedArray()
        noteDao.update(*array)
    }

    override suspend fun deleteNotes(vararg notes: Note) {
        Log.d(tag, "deleteNotes: Permanently deleting ${notes.size} notes")
        val array = notes.map { it.toEntity() }.toTypedArray()
        noteDao.delete(*array)
    }

    override suspend fun discardEmptyNotes(): Boolean {
        val notes = noteDao.getAllBlankTitleNotes().first().filter { it.isEmpty() }.toTypedArray()
        Log.d(tag, "discardEmptyNotes: Found ${notes.size} empty notes to discard")
        deleteNotes(*notes)
        return notes.isNotEmpty()
    }

    override suspend fun permanentlyDeleteNotesInBin() {
        Log.d(tag, "permanentlyDeleteNotesInBin")
        noteDao.permanentlyDeleteNotesInBin()
    }

    override fun getById(noteId: Long): Flow<Note?> {
        return noteDao.getById(noteId)
    }

    override fun getDeleted(sortMethod: SortMethod): Flow<List<Note>> {
        return noteDao.getDeleted(sortMethod)
    }

    override fun getArchived(sortMethod: SortMethod): Flow<List<Note>> {
        return noteDao.getArchived(sortMethod)
    }

    override fun getNonDeleted(sortMethod: SortMethod): Flow<List<Note>> {
        return noteDao.getNonDeleted(sortMethod)
    }

    override fun getNonDeletedOrArchived(sortMethod: SortMethod): Flow<List<Note>> {
        return noteDao.getNonDeletedOrArchived(sortMethod)
    }

    override fun getAll(sortMethod: SortMethod): Flow<List<Note>> {
        return noteDao.getAll(sortMethod)
    }
    
    override fun getAllBlankTitleNotes(): Flow<List<Note>> {
        return noteDao.getAllBlankTitleNotes()
    }
}
