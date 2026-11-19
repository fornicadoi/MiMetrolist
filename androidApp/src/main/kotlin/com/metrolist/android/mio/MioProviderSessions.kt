/**
 * MiMetrolist: cuentas de otras apps conectadas mientras la app está abierta,
 * para volver a ver tus listas sin iniciar sesión otra vez.
 */

package com.metrolist.android.mio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object MioProviderSessions {
    var appleMusicToken by mutableStateOf<String?>(null)
    var deezerArl by mutableStateOf<String?>(null)
    var tidalSession by mutableStateOf<Pair<String, String>?>(null)
    var amazonSession by mutableStateOf<Pair<String, String>?>(null)
}
