package de.tu_darmstadt.polarhrserver.powermeter

import kotlin.experimental.and

class HexHelper {
    companion object {
        //8bit
        fun byteToUnsignedInt(b: Byte): Int {
            return (b and 0xFF.toByte()).toInt()
        }

        //16bit
        fun bytesToUnsignedInt(msb: Byte, lsb: Byte): Int {
            return byteToUnsignedInt(lsb) + (byteToUnsignedInt(msb) shl 8)
        }

        fun bytesToSignedInt(msb: Byte, lsb: Byte): Int =
            unsignedToSigned(bytesToUnsignedInt(msb, lsb), 16)

        fun byteToSignedInt(b: Byte): Int = unsignedToSigned(byteToUnsignedInt(b), 8)

        private fun unsignedToSigned(unsigned: Int, size: Int = 8): Int {
            var u = unsigned
            if (u and (1 shl size - 1) != 0) {
                u = -1 * ((1 shl size - 1) - (u and (1 shl size - 1) - 1))
            }
            return u
        }
    }
}


