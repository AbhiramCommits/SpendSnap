package com.spendsnap.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.spendsnap.data.local.SyncState

@Composable
fun SyncBadge(
    state: SyncState,
    modifier: Modifier = Modifier,
) {
    val (label, background, foreground) =
        when (state) {
            SyncState.PENDING ->
                Triple(
                    "Pending",
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                )

            SyncState.SYNCED ->
                Triple(
                    "Synced",
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                )

            SyncState.FAILED ->
                Triple(
                    "Retry",
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer,
                )
        }

    Surface(
        modifier =
            modifier.semantics {
                contentDescription = "Sync status: $label"
            },
        color = background,
        contentColor = foreground,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = Color.Unspecified,
        )
    }
}
