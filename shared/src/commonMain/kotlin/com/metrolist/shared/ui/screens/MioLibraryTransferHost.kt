/**
 * MiMetrolist: aviso cuando llega la biblioteca de otro dispositivo.
 */

package com.metrolist.shared.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_transfer_ok
import com.metrolist.shared.mio.MioLibraryTransfer
import com.metrolist.shared.mio.MioNotice
import org.jetbrains.compose.resources.stringResource

@Composable
fun MioLibraryTransferHost() {
    val received = MioLibraryTransfer.received
    val message = received ?: MioNotice.message ?: return
    val dismiss = {
        if (received != null) MioLibraryTransfer.received = null else MioNotice.message = null
    }
    AlertDialog(
        onDismissRequest = dismiss,
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = dismiss) { Text(stringResource(Res.string.mio_transfer_ok)) }
        },
    )
}
