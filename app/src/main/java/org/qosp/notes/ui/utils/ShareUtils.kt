package org.qosp.notes.ui.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import org.qosp.notes.data.model.Note

fun shareNote(context: Context, note: Note) {
    val sendIntent: Intent = Intent().apply {
        val textContent = note.taskListToString(withCheckmarks = true)
        putExtra(
            Intent.EXTRA_TITLE,
            note.title
        )
        putExtra(
            Intent.EXTRA_TEXT,
            textContent,
        )
        action = Intent.ACTION_SEND
        type = "text/plain"
    }

    val chooser = Intent.createChooser(sendIntent, null)
    ContextCompat.startActivity(context, chooser, null)
}
