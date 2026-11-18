package com.metrolist.desktop.deviceconnect

import com.metrolist.shared.deviceconnect.DeviceConnectDeviceType
import com.metrolist.shared.deviceconnect.DeviceConnectManager
import com.metrolist.shared.mio.MioLibraryTransfer

object DesktopDeviceConnect {
    val manager =
        DeviceConnectManager(localDeviceType = DeviceConnectDeviceType.DESKTOP).also { MioLibraryTransfer.manager = it }
}
