package org.qosp.notes.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.Instant

@Entity(tableName = "notes")
@Serializable
data class NoteEntity(
    val title: String,
    val taskList: List<NoteTask>,
    val isArchived: Boolean,
    val isDeleted: Boolean,
    val creationDate: Long,
    val modifiedDate: Long,
    val deletionDate: Long?,
    val color: NoteColor,
    @PrimaryKey(autoGenerate = true)
    val id: Long,
)

@Serializable
@Parcelize
data class Note(
    val title: String = "",
    val taskList: List<NoteTask> = listOf(),
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val creationDate: Long = Instant.now().epochSecond,
    val modifiedDate: Long = Instant.now().epochSecond,
    val deletionDate: Long? = null,
    val color: NoteColor = NoteColor.Default,
    val id: Long = 0L,
) : Parcelable {

    fun isEmpty(): Boolean {
        // A checklist note is empty if title is blank and no tasks
        return title.isBlank() && taskList.isEmpty()
    }

    // Legacy conversion helpers might be removed or simplified. 
    // Keeping simple string conversion for search/preview if needed.
    
    fun taskListToString(withCheckmarks: Boolean = false): String {
        return taskList.joinToString("\n") {
            val prefix = when {
                withCheckmarks -> if (it.isDone) "☑ " else "☐ "
                else -> ""
            }
            "$prefix${it.content.trim()}"
        }
    }

    fun toEntity(): NoteEntity = NoteEntity(
        title = title,
        taskList = taskList,
        isArchived = isArchived,
        isDeleted = isDeleted,
        creationDate = creationDate,
        modifiedDate = modifiedDate,
        deletionDate = deletionDate,
        color = color,
        id = id
    )
}
