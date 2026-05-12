package com.delve.hungrywalrus.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Single-line caption used by the Summaries screen to communicate whether today is
 * included in the rolling window. Design spec §5.8 / §3.14 element 4.
 *
 * - When [includesToday] is false: "Today excluded -- updates after 20:00".
 * - When [includesToday] is true: "Includes today".
 *
 * Rendered in `labelSmall` / `onSurfaceVariant` to keep the hint low-noise. This is
 * the only UI surface that explicitly mentions the 20:00 cutoff; intentionally a
 * plain caption rather than a banner or modal.
 */
@Composable
fun RollingWindowHint(
    includesToday: Boolean,
    modifier: Modifier = Modifier,
) {
    val text = if (includesToday) {
        "Includes today"
    } else {
        "Today excluded -- updates after 20:00"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
