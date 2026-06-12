package dev.electrikjesus.xrlauncher.core.input.rayneo

/** USB identifiers for RayNeo AR Glasses on Pixel 8 + SmartGlasses Desktop Mode. */
object RayNeoUsbConstants {
    const val VENDOR_ID = 7099
    const val PRODUCT_ID = 44880

    const val HID_INTERFACE_CLASS = 3
    const val FRAME_SIZE = 64

    const val OUTBOUND_MAGIC: Byte = 0x66
    const val INBOUND_MAGIC: Byte = 0x99.toByte()

    const val CMD_IMU_ON: Byte = 1
    const val CMD_IMU_OFF: Byte = 2
    const val ACK_IMU_DATA: Byte = 0x65
}
