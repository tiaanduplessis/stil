package app.stil.launcher.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.*
import org.junit.Test

class AppSearchInputTest {
    @Test fun `typing pasting and deleting committed text can launch`() {
        assertTrue(isCommittedSearchEdit(TextFieldValue(), TextFieldValue("Maps")))
        assertTrue(isCommittedSearchEdit(TextFieldValue("Maps!"), TextFieldValue("Maps")))
    }

    @Test fun `cursor changes and repeated values do not launch`() {
        val value = TextFieldValue("Maps", selection = TextRange(4))
        assertFalse(isCommittedSearchEdit(value, value.copy(selection = TextRange(1))))
        assertFalse(isCommittedSearchEdit(value, value))
    }

    @Test fun `IME composition waits for commitment even when the text stays the same`() {
        val composing = TextFieldValue("Maps", composition = TextRange(0, 4))
        assertFalse(isCommittedSearchEdit(TextFieldValue(), composing))
        assertFalse(isCommittedSearchEdit(composing, composing.copy(text = "Map", composition = TextRange(0, 3))))
        assertTrue(isCommittedSearchEdit(composing, composing.copy(composition = null)))
    }
}
