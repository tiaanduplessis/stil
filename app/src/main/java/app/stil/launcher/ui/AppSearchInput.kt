package app.stil.launcher.ui

import androidx.compose.ui.text.input.TextFieldValue

internal fun isCommittedSearchEdit(previous: TextFieldValue, next: TextFieldValue): Boolean =
    next.composition == null && (next.text != previous.text || previous.composition != null)
