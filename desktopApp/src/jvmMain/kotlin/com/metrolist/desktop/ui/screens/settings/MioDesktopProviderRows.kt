/**
 * MiMetrolist: filas de cada servicio en la PC con estado "Conectado" y un botón aparte para importar con enlace.
 */

package com.metrolist.desktop.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_connect_app_title
import com.metrolist.shared.generated.resources.mio_missing_close
import com.metrolist.shared.generated.resources.mio_provider_connect
import com.metrolist.shared.generated.resources.mio_provider_connected
import com.metrolist.shared.generated.resources.mio_provider_connected_desc
import com.metrolist.shared.generated.resources.mio_provider_disconnect
import com.metrolist.shared.playlistimport.ImportSourceType
import com.metrolist.shared.ui.icons.MetrolistIcons
import org.jetbrains.compose.resources.stringResource

private val MioConnectedGreen = Color(0xFF2FD66B)

/** Services whose account was opened successfully while the app is running. */
object MioDesktopSessions {
    val connected = mutableStateMapOf<ImportSourceType, Boolean>()
}

/** Row for a connected service: tap to open its library, or sign out. */
internal fun mioDesktopConnectedItem(
    name: String,
    onOpenAccount: () -> Unit,
    onDisconnect: () -> Unit,
): Material3SettingsItem =
    Material3SettingsItem(
        title = { Text(name) },
        description = { Text(stringResource(Res.string.mio_provider_connected_desc)) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MioDesktopPill(stringResource(Res.string.mio_provider_connected), filled = true)
                IconButton(onClick = onDisconnect, modifier = Modifier.size(36.dp)) {
                    Icon(
                        MetrolistIcons.AutoMirrored.Outlined.Logout,
                        contentDescription = stringResource(Res.string.mio_provider_disconnect),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        onClick = onOpenAccount,
    )

/** Lets the user pick which service to connect. */
@Composable
internal fun MioDesktopConnectChooser(
    options: List<Pair<String, () -> Unit>>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.mio_connect_app_title)) },
        text = {
            Column {
                options.forEach { (name, connect) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onDismiss()
                                connect()
                            }.padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        MioDesktopPill(stringResource(Res.string.mio_provider_connect), filled = false)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.mio_missing_close)) }
        },
    )
}

@Composable
private fun MioDesktopPill(
    text: String,
    filled: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (filled) MioConnectedGreen.copy(alpha = 0.16f) else Color.Transparent,
        border = if (filled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            if (filled) {
                Surface(shape = CircleShape, color = MioConnectedGreen, modifier = Modifier.size(8.dp)) {}
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (filled) MioConnectedGreen else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
