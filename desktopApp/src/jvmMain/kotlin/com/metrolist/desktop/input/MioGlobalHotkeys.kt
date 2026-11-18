/**
 * MiMetrolist: atajos de teclado que funcionan aunque la app no tenga el foco (solo Windows).
 */

@file:Suppress("FunctionName")

package com.metrolist.desktop.input

import com.metrolist.desktop.state.app.UiState
import com.metrolist.desktop.state.playback.PlayerState
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.state.SongLikeState
import com.metrolist.shared.utils.OsUtil
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import javax.swing.SwingUtilities

private interface MioHotkeyUser32 : Library {
    fun RegisterHotKey(
        hWnd: Pointer?,
        id: Int,
        fsModifiers: Int,
        vk: Int,
    ): Boolean

    fun UnregisterHotKey(
        hWnd: Pointer?,
        id: Int,
    ): Boolean

    fun GetMessageW(
        lpMsg: WinUser.MSG,
        hWnd: Pointer?,
        wMsgFilterMin: Int,
        wMsgFilterMax: Int,
    ): Int

    fun PostThreadMessageW(
        idThread: Int,
        msg: Int,
        wParam: WinDef.WPARAM?,
        lParam: WinDef.LPARAM?,
    ): Boolean
}

private interface MioHotkeyKernel32 : Library {
    fun GetCurrentThreadId(): Int
}

private const val MOD_ALT = 0x0001
private const val MOD_CONTROL = 0x0002
private const val MOD_NOREPEAT = 0x4000
private const val WM_HOTKEY = 0x0312
private const val WM_QUIT = 0x0012

private class MioHotkey(
    val vk: Int,
    val action: () -> Unit,
)

object MioGlobalHotkeys {
    // Ctrl+Alt plus: Space, Left, Right, Up, Down, K, L.
    private val hotkeys =
        mapOf(
            1 to MioHotkey(0x20) { PlayerState.togglePlay() },
            2 to MioHotkey(0x25) { PlayerState.skipPrevious() },
            3 to MioHotkey(0x27) { PlayerState.skipNext() },
            4 to MioHotkey(0x26) { PlayerState.setVolumeLevel(PlayerState.volume + 0.1f) },
            5 to MioHotkey(0x28) { PlayerState.setVolumeLevel(PlayerState.volume - 0.1f) },
            6 to MioHotkey(0x4B) { PlayerState.currentTrack?.let(SongLikeState::toggleLike) },
            7 to MioHotkey(0x4C) {
                Settings.shared.mioLyricsFloating = true
                UiState.showLyricsDrawer = !UiState.showLyricsDrawer
            },
        )

    private var thread: Thread? = null

    @Volatile private var threadId = 0

    @Volatile var failedKeys: Int = 0
        private set

    fun update(enabled: Boolean) {
        if (enabled) start() else stop()
    }

    private fun start() {
        if (!OsUtil.isWindows || thread != null) return
        thread =
            Thread({ runLoop() }, "mio-global-hotkeys").apply {
                isDaemon = true
                start()
            }
    }

    private fun stop() {
        val id = threadId
        thread = null
        threadId = 0
        if (id != 0) {
            runCatching { Native.load("user32", MioHotkeyUser32::class.java).PostThreadMessageW(id, WM_QUIT, null, null) }
        }
    }

    private fun runLoop() {
        val registered = mutableListOf<Int>()
        try {
            val user32 = Native.load("user32", MioHotkeyUser32::class.java)
            threadId = Native.load("kernel32", MioHotkeyKernel32::class.java).GetCurrentThreadId()
            hotkeys.forEach { (id, hotkey) ->
                if (user32.RegisterHotKey(null, id, MOD_CONTROL or MOD_ALT or MOD_NOREPEAT, hotkey.vk)) registered += id
            }
            // Keys another program already owns cannot be registered.
            failedKeys = hotkeys.size - registered.size
            try {
                val message = WinUser.MSG()
                while (user32.GetMessageW(message, null, 0, 0) > 0) {
                    if (message.message == WM_HOTKEY) {
                        hotkeys[message.wParam.toInt()]?.let { hotkey -> SwingUtilities.invokeLater { hotkey.action() } }
                    }
                }
            } finally {
                registered.forEach { user32.UnregisterHotKey(null, it) }
            }
        } catch (_: Throwable) {
            failedKeys = hotkeys.size
        } finally {
            if (thread === Thread.currentThread()) {
                thread = null
                threadId = 0
            }
        }
    }
}
