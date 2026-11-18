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
import org.jetbrains.compose.resources.stringResource

@Composable
fun MioLibraryTransferHost() {
    val message = MioLibraryTransfer.received ?: return
    AlertDialog(
        onDismissRequest = { MioLibraryTransfer.received = null },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { MioLibraryTransfer.received = null }) { Text(stringResource(Res.string.mio_transfer_ok)) }
        },
    )
}
