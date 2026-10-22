package dev.techda.sahatisukari.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

@Composable
internal fun SahatiPageTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Bold,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth().testTag("sahati-page-title"),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = fontWeight,
        textAlign = TextAlign.Start,
    )
}
