package com.yourname.ayanami.learn.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourname.ayanami.learn.data.local.NativeLanguage
import com.yourname.ayanami.learn.ui.localization.LocalAppStrings

@Composable
fun NativeLanguageSelector(
    selectedLanguage: NativeLanguage,
    onLanguageSelected: (NativeLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = strings.nativeLanguage,
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NativeLanguage.entries.forEach { language ->
                val buttonModifier = Modifier.weight(1f)
                if (language == selectedLanguage) {
                    Button(
                        onClick = { onLanguageSelected(language) },
                        modifier = buttonModifier
                    ) {
                        Text(language.displayName(selectedLanguage))
                    }
                } else {
                    OutlinedButton(
                        onClick = { onLanguageSelected(language) },
                        modifier = buttonModifier
                    ) {
                        Text(language.displayName(selectedLanguage))
                    }
                }
            }
        }
    }
}
