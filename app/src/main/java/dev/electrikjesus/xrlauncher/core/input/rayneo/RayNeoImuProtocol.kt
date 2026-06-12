package dev.electrikjesus.xrlauncher.core.input.rayneo

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class RayNeoImuSample(
    val gyroXDps: Float,
    val gyroYDps: Float,
    val gyroZDps: Float,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val temperatureC: Float = 0f,
    val tick: Long = 0L,
)

object RayNeoImuProtocol {
    fun buildCommand(command: Byte, value: Byte = 0, payload: ByteArray = ByteArray(0)): ByteArray {
        val frame = ByteArray(RayNeoUsbConstants.FRAME_SIZE)
        frame[0] = RayNeoUsbConstants.OUTBOUND_MAGIC
        frame[1] = command
        frame[2] = value
        payload.copyInto(frame, destinationOffset = 3, endIndex = payload.size.coerceAtMost(52))
        return frame
    }

    fun buildImuOnCommand(): ByteArray = buildCommand(RayNeoUsbConstants.CMD_IMU_ON)

    fun buildImuOffCommand(): ByteArray = buildCommand(RayNeoUsbConstants.CMD_IMU_OFF)

    fun parseInboundFrame(frame: ByteArray, length: Int = frame.size): RayNeoImuSample? {
        if (length < RayNeoUsbConstants.FRAME_SIZE) return null
        if (frame[0] != RayNeoUsbConstants.INBOUND_MAGIC) return null
        if (frame[1] != RayNeoUsbConstants.ACK_IMU_DATA) return null
        return RayNeoImuSample(
            accelX = readFloatLE(frame, 4),
            accelY = readFloatLE(frame, 8),
            accelZ = readFloatLE(frame, 12),
            gyroXDps = readFloatLE(frame, 16),
            gyroYDps = readFloatLE(frame, 20),
            gyroZDps = readFloatLE(frame, 24),
            temperatureC = readFloatLE(frame, 28),
            tick = readUInt32LE(frame, 40),
        )
    }

    private fun readFloatLE(frame: ByteArray, offset: Int): Float =
        ByteBuffer.wrap(frame, offset, 4).order(ByteOrder.LITTLE_ENDIAN).float

    private fun readUInt32LE(frame: ByteArray, offset: Int): Long =
        ByteBuffer.wrap(frame, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFF_FFFFL
}
