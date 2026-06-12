package dev.electrikjesus.xrlauncher.core.input.rayneo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RayNeoImuProtocolTest {
    @Test
    fun buildImuOnCommand_matchesRayNeoProtocol() {
        val frame = RayNeoImuProtocol.buildImuOnCommand()
        assertEquals(RayNeoUsbConstants.OUTBOUND_MAGIC, frame[0])
        assertEquals(RayNeoUsbConstants.CMD_IMU_ON, frame[1])
        assertEquals(0, frame[2].toInt())
    }

    @Test
    fun parseInboundFrame_readsGyroSample() {
        val frame = ByteArray(RayNeoUsbConstants.FRAME_SIZE)
        frame[0] = RayNeoUsbConstants.INBOUND_MAGIC
        frame[1] = RayNeoUsbConstants.ACK_IMU_DATA
        writeFloatLE(frame, 16, 1.5f)
        writeFloatLE(frame, 20, -2.25f)
        writeFloatLE(frame, 24, 0.5f)

        val sample = RayNeoImuProtocol.parseInboundFrame(frame)
        assertNotNull(sample)
        assertEquals(1.5f, sample!!.gyroXDps, 0.001f)
        assertEquals(-2.25f, sample.gyroYDps, 0.001f)
        assertEquals(0.5f, sample.gyroZDps, 0.001f)
    }

    @Test
    fun parseInboundFrame_rejectsNonImuPacket() {
        val frame = ByteArray(RayNeoUsbConstants.FRAME_SIZE)
        frame[0] = RayNeoUsbConstants.INBOUND_MAGIC
        frame[1] = 0xC8.toByte()
        assertNull(RayNeoImuProtocol.parseInboundFrame(frame))
    }

    private fun writeFloatLE(frame: ByteArray, offset: Int, value: Float) {
        ByteBuffer.wrap(frame, offset, 4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value)
    }
}
