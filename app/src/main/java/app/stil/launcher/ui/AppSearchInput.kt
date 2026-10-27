package app.stil.launcher.ui

import androidx.compose.ui.text.input.TextFieldValue

internal fun isSearchTextEdit(previous: TextFieldValue, next: TextFieldValue): Boolean =
    next.text != previous.text
