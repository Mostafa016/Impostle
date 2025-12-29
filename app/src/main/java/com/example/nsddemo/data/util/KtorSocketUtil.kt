package com.example.nsddemo.data.util

import com.example.nsddemo.data.local.network.socket.Server
import com.example.nsddemo.domain.model.Player
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.errors.EOFException
import io.ktor.utils.io.errors.IOException
import io.ktor.utils.io.readFully
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeStringUtf8

private const val MAX_PAYLOAD_LENGTH = 10 * 1024 * 1024

object KtorSocketUtil {
    // TODO: Remove this once the network layer is complete
    suspend fun sendUtf8LineToAllPlayers(messageFun: (Player) -> String) {
        for ((clientConnection, player) in Server.clients) {
            clientConnection.output.writeLineUtf8(messageFun(player))
        }
    }

    suspend fun ByteWriteChannel.writeLineUtf8(string: String) =
        writeStringUtf8(string.appendNewLine())

    private fun String.appendNewLine(): String {
        return this + '\n'
    }

    // Write: [Int Length] + [JSON Bytes]
    suspend fun ByteWriteChannel.writePacket(json: String) {
        json.toByteArray(Charsets.UTF_8).also { bytes ->
            writeInt(bytes.size)
            writeFully(bytes)
        }
        flush()
    }

    // Read: Read [Int Length] -> Read N Bytes -> Decode String
    suspend fun ByteReadChannel.readPacket(): String? {
        if (isClosedForRead) return null
        try {
            // Read the length (4 bytes)
            // discard() logic or checks might be needed if stream is corrupt,
            // but for now, reading int is standard.
            val length = readInt()
            if (length > MAX_PAYLOAD_LENGTH) throw IOException("Packet too large")
            val packet = ByteArray(length)
            readFully(packet)
            return String(packet, Charsets.UTF_8)
        } catch (e: EOFException) {
            return null // Socket closed cleanly
        } catch (e: Exception) {
            throw IOException("Socket read error", e)
        }
    }
}