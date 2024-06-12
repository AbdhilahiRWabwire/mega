package mega.privacy.android.shared.original.core.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * TextField parameter provider for compose previews.
 */
class TextFieldProvider : PreviewParameterProvider<TextFieldState> {
    override val values = listOf(
        TextFieldState(),
        TextFieldState(text = "Text goes here"),
        TextFieldState(text = "Error text", error = "Error goes here")
    ).asSequence()
}

/**
 * Data class defining the possible TextField states.
 */
data class TextFieldState(
    val text: String = "",
    val error: String? = null,
)