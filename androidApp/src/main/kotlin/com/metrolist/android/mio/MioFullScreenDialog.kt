/**
 * MiMetrolist: diálogo que cubre toda la pantalla, incluidas las barras del sistema.
 */

package com.metrolist.android.mio

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Draws behind the status and navigation bars; the content handles the insets itself. */
@Composable
fun MioFullScreenDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        content = content,
    )
}
