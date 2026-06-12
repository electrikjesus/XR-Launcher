package dev.electrikjesus.xrlauncher.core.input.rayneo

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log
import dev.electrikjesus.xrlauncher.core.input.CompanionPointerBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

enum class RayNeoHeadTrackingState {
    DISCONNECTED,
    PERMISSION_REQUIRED,
    CONNECTING,
    STREAMING,
    ERROR,
}

/**
 * Reads RayNeo glasses IMU over USB HID and feeds workspace head-look.
 * Protocol aligned with [verncat/RayNeo-Air-3S-Pro-OpenVR](https://github.com/verncat/RayNeo-Air-3S-Pro-OpenVR).
 */
object RayNeoHeadTrackingController {
    private const val TAG = "XRLauncher/RayNeoImu"
    private const val ACTION_USB_PERMISSION = "dev.electrikjesus.xrlauncher.RAYNEO_USB_PERMISSION"
    /** Short timeout keeps the read loop responsive (~30 Hz poll cadence on idle). */
    private const val READ_TIMEOUT_MS = 32
    private const val LOG_SAMPLE_INTERVAL = 120

    private val running = AtomicBoolean(false)
    private var readThread: Thread? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var claimedInterface: UsbInterface? = null
    private var inboundEndpoint: UsbEndpoint? = null
    private var outboundEndpoint: UsbEndpoint? = null
    private var registeredContext: Context? = null
    private var permissionReceiverRegistered = false

    private val _state = MutableStateFlow(RayNeoHeadTrackingState.DISCONNECTED)
    val state: StateFlow<RayNeoHeadTrackingState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val sampleCount = AtomicInteger(0)

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            if (device == null) return
            if (granted && running.get()) {
                connectLocked(context.applicationContext, device)
            } else {
                _state.value = RayNeoHeadTrackingState.PERMISSION_REQUIRED
                _lastError.value = "USB permission denied for RayNeo glasses"
            }
        }
    }

    fun findRayNeoDevice(context: Context): UsbDevice? {
        val usbManager = context.getSystemService(UsbManager::class.java) ?: return null
        return usbManager.deviceList.values.firstOrNull { device ->
            device.vendorId == RayNeoUsbConstants.VENDOR_ID &&
                device.productId == RayNeoUsbConstants.PRODUCT_ID
        }
    }

    fun isRayNeoAttached(context: Context): Boolean = findRayNeoDevice(context) != null

    fun start(context: Context) {
        val appContext = context.applicationContext
        registerPermissionReceiver(appContext)
        running.set(true)
        sampleCount.set(0)
        val device = findRayNeoDevice(appContext)
        if (device == null) {
            _state.value = RayNeoHeadTrackingState.DISCONNECTED
            _lastError.value = "RayNeo glasses not found on USB"
            return
        }
        val usbManager = appContext.getSystemService(UsbManager::class.java) ?: return
        if (!usbManager.hasPermission(device)) {
            _state.value = RayNeoHeadTrackingState.PERMISSION_REQUIRED
            requestPermission(appContext, device)
            return
        }
        connectLocked(appContext, device)
    }

    fun stop() {
        running.set(false)
        readThread?.interrupt()
        readThread = null
        releaseUsb()
        _state.value = RayNeoHeadTrackingState.DISCONNECTED
    }

    fun shutdown(context: Context) {
        stop()
        if (permissionReceiverRegistered) {
            runCatching { context.applicationContext.unregisterReceiver(permissionReceiver) }
            permissionReceiverRegistered = false
            registeredContext = null
        }
    }

    private fun registerPermissionReceiver(context: Context) {
        if (permissionReceiverRegistered) return
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        context.registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        permissionReceiverRegistered = true
        registeredContext = context.applicationContext
    }

    private fun requestPermission(context: Context, device: UsbDevice) {
        val usbManager = context.getSystemService(UsbManager::class.java) ?: return
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(context.packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        usbManager.requestPermission(device, pendingIntent)
    }

    private fun connectLocked(context: Context, device: UsbDevice) {
        releaseUsb()
        _state.value = RayNeoHeadTrackingState.CONNECTING
        _lastError.value = null

        val usbManager = context.getSystemService(UsbManager::class.java) ?: return
        val hidInterface = (0 until device.interfaceCount)
            .map(device::getInterface)
            .firstOrNull { it.interfaceClass == RayNeoUsbConstants.HID_INTERFACE_CLASS }
            ?: run {
                fail("No HID interface on RayNeo device")
                return
            }

        val connection = usbManager.openDevice(device) ?: run {
            fail("Unable to open RayNeo USB device")
            return
        }

        if (!connection.claimInterface(hidInterface, true)) {
            connection.close()
            fail("Unable to claim RayNeo HID interface (another app may own it)")
            return
        }

        val inEndpoint = findEndpoint(hidInterface, UsbConstants.USB_DIR_IN)
        val outEndpoint = findEndpoint(hidInterface, UsbConstants.USB_DIR_OUT)
        if (inEndpoint == null) {
            connection.releaseInterface(hidInterface)
            connection.close()
            fail("RayNeo HID IN endpoint missing")
            return
        }

        usbConnection = connection
        claimedInterface = hidInterface
        inboundEndpoint = inEndpoint
        outboundEndpoint = outEndpoint

        sendCommand(RayNeoImuProtocol.buildImuOnCommand())

        readThread = Thread(
            { readLoop() },
            "RayNeoImuReader",
        ).also { it.start() }

        _state.value = RayNeoHeadTrackingState.STREAMING
        Log.i(TAG, "RayNeo IMU streaming started (endpoint type=${inEndpoint.type})")
    }

    private fun readLoop() {
        val connection = usbConnection ?: return
        val endpoint = inboundEndpoint ?: return
        val buffer = ByteArray(RayNeoUsbConstants.FRAME_SIZE)
        var lastDeviceTick: Long? = null
        var lastWallNs = System.nanoTime()

        while (running.get() && !Thread.currentThread().isInterrupted) {
            val read = connection.bulkTransfer(
                endpoint,
                buffer,
                buffer.size,
                READ_TIMEOUT_MS,
            )
            if (read < RayNeoUsbConstants.FRAME_SIZE) continue

            val sample = RayNeoImuProtocol.parseInboundFrame(buffer, read) ?: continue
            val nowNs = System.nanoTime()
            val wallDeltaSec = ((nowNs - lastWallNs).coerceAtLeast(1L)) / 1_000_000_000f
            lastWallNs = nowNs

            val tickDeltaSec = lastDeviceTick?.let { previousTick ->
                if (sample.tick > previousTick) {
                    (sample.tick - previousTick).toFloat() / 1000f
                } else {
                    null
                }
            }
            lastDeviceTick = sample.tick

            val deltaSec = (tickDeltaSec ?: wallDeltaSec).coerceIn(0.001f, 0.05f)
            CompanionPointerBus.applyGlassesImuSample(
                gyroXDps = sample.gyroXDps,
                gyroYDps = sample.gyroYDps,
                gyroZDps = sample.gyroZDps,
                deltaTimeSec = deltaSec,
            )

            val count = sampleCount.incrementAndGet()
            if (count == 1 || count % LOG_SAMPLE_INTERVAL == 0) {
                Log.d(
                    TAG,
                    "IMU samples=$count dt=${"%.3f".format(deltaSec)}s " +
                        "gyro=(${sample.gyroXDps}, ${sample.gyroYDps}, ${sample.gyroZDps}) dps",
                )
            }
        }
    }

    private fun sendCommand(frame: ByteArray) {
        val connection = usbConnection ?: return
        val endpoint = outboundEndpoint
        if (endpoint != null) {
            connection.bulkTransfer(endpoint, frame, frame.size, READ_TIMEOUT_MS)
        } else {
            connection.controlTransfer(
                0x21,
                0x09,
                0x0200,
                claimedInterface?.id ?: 0,
                frame,
                frame.size,
                READ_TIMEOUT_MS,
            )
        }
    }

    private fun releaseUsb() {
        runCatching { sendCommand(RayNeoImuProtocol.buildImuOffCommand()) }
        claimedInterface?.let { iface ->
            usbConnection?.releaseInterface(iface)
        }
        usbConnection?.close()
        usbConnection = null
        claimedInterface = null
        inboundEndpoint = null
        outboundEndpoint = null
    }

    private fun findEndpoint(iface: UsbInterface, direction: Int): UsbEndpoint? {
        for (index in 0 until iface.endpointCount) {
            val endpoint = iface.getEndpoint(index)
            if (endpoint.direction == direction) return endpoint
        }
        return null
    }

    private fun fail(message: String) {
        Log.w(TAG, message)
        _lastError.value = message
        _state.value = RayNeoHeadTrackingState.ERROR
        releaseUsb()
    }
}
