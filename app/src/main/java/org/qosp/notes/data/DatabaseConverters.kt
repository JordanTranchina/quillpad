package org.qosp.notes.data

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.qosp.notes.data.model.NoteColor
import org.qosp.notes.data.model.NoteTask

object DatabaseConverters {
    @TypeConverter
    fun jsonFromTasks(tasks: List<NoteTask>): String = Json.encodeToString(tasks)

    @TypeConverter
    fun tasksFromJson(json: String): List<NoteTask> = Json.decodeFromString(json)

    @TypeConverter
    fun jsonFromColorEnum(color: NoteColor): String = Json.encodeToString(color)

    @TypeConverter
    fun colorEnumFromJson(json: String): NoteColor = Json.decodeFromString(json)
}
