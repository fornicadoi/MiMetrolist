/**
 * MiMetrolist: filas de cada servicio con estado "Conectado" y un botón aparte para importar con enlace.
 */

package com.metrolist.android.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_provider_connect
import com.metrolist.shared.generated.resources.mio_provider_connected
import com.metrolist.shared.generated.resources.mio_provider_connected_desc
import com.metrolist.shared.generated.resources.mio_provider_disconnect
import com.metrolist.shared.generated.resources.mio_provider_disconnected_desc
import com.metrolist.shared.generated.resources.mio_provider_link_desc
import com.metrolist.shared.generated.resources.mio_provider_link_title
import com.metrolist.shared.ui.icons.MetrolistIcons
import org.jetbrains.compose.resources.stringResource

private val MioConnectedGreen = Color(0xFF2FD66B)

@Composable
internal fun mioProviderItems(
    name: String,
    connected: Boolean,
    enabled: Boolean,
    onOpenAccount: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onImportLink: () -> Unit,
): List<SettingsSubScreenItem> =
    listOf(
        SettingsSubScreenItem(
            title = name,
            description =
                stringResource(
                    if (connected) Res.string.mio_provider_connected_desc else Res.string.mio_provider_disconnected_desc,
                ),
            enabled = enabled,
            trailingContent = {
                if (connected) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MioStatusPill(stringResource(Res.string.mio_provider_connected), filled = true)
                        IconButton(onClick = onDisconnect, enabled = enabled, modifier = Modifier.size(40.dp)) {
                            Icon(
                                MetrolistIcons.AutoMirrored.Outlined.Logout,
                                contentDescription = stringResource(Res.string.mio_provider_disconnect),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    MioStatusPill(stringResource(Res.string.mio_provider_connect), filled = false)
                }
            },
            onClick = if (connected) onOpenAccount else onConnect,
        ),
        SettingsSubScreenItem(
            title = stringResource(Res.string.mio_provider_link_title, name),
            description = stringResource(Res.string.mio_provider_link_desc),
            icon = MetrolistIcons.Outlined.Link,
            enabled = enabled,
            onClick = onImportLink,
        ),
    )

@Composable
private fun MioStatusPill(
    text: String,
    filled: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (filled) MioConnectedGreen.copy(alpha = 0.16f) else Color.Transparent,
        border = if (filled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (filled) {
                Box(Modifier.size(8.dp).padding(0.dp)) {
                    Surface(shape = CircleShape, color = MioConnectedGreen, modifier = Modifier.size(8.dp)) {}
                }
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
