package org.qosp.notes.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class NoteTask(val id: Long, var content: String, var isDone: Boolean) : Parcelable
