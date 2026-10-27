package app.stil.launcher.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.*
import org.junit.Test

class AppSearchInputTest {
    @Test fun `typing pasting and deleting committed text can launch`() {
        assertTrue(isSearchTextEdit(TextFieldValue(), TextFieldValue("Maps")))
        assertTrue(isSearchTextEdit(TextFieldValue("Maps!"), TextFieldValue("Maps")))
    }

    @Test fun `cursor changes and repeated values do not launch`() {
        val value = TextFieldValue("Maps", selection = TextRange(4))
        assertFalse(isSearchTextEdit(value, value.copy(selection = TextRange(1))))
        assertFalse(isSearchTextEdit(value, value))
    }

    @Test fun `composing keyboards can launch without committing the word`() {
        val composing = TextFieldValue("Maps", composition = TextRange(0, 4))
        assertTrue(isSearchTextEdit(TextFieldValue(), composing))
        assertTrue(isSearchTextEdit(composing, composing.copy(text = "Map", composition = TextRange(0, 3))))
        assertFalse(isSearchTextEdit(composing, composing.copy(composition = null)))
    }
}
