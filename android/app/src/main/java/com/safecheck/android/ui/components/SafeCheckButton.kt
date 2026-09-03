package com.safecheck.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safecheck.android.ui.theme.SafeCheckTheme

/** Primary full-width action button in the SafeCheck style. */
@Composable
fun SafeCheckButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(text)
    }
}

/** Secondary/outlined action for less prominent choices. */
@Composable
fun SafeCheckOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(text)
    }
}

@Preview
@Composable
private fun SafeCheckButtonPreview() {
    SafeCheckTheme {
        SafeCheckButton(text = "Check for risk", onClick = {})
    }
}
