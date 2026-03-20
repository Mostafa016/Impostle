package com.mostafa.impostle.data.util

import android.util.Log
import com.mostafa.impostle.core.util.Debugging.TAG
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.errors.EOFException
import io.ktor.utils.io.errors.IOException
import io.ktor.utils.io.readFully
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.withContext

private const val MAX_PAYLOAD_LENGTH = 10 * 1024 * 1024

object KtorSocketUtil {
    // Write: [Int Length] + [JSON Bytes]
    suspend fun ByteWriteChannel.writePacket(json: String) =
        withContext(Dispatchers.IO) {
            json.toByteArray(Charsets.UTF_8).also { bytes ->
                writeInt(bytes.size)
                writeFully(bytes)
            }
            flush()
        }

    // Read: Read [Int Length] -> Read N Bytes -> Decode String
    suspend fun ByteReadChannel.readPacket(): String? =
        withContext(Dispatchers.IO) {
            if (isClosedForRead) return@withContext null
            return@withContext try {
                // Read the length (4 bytes)
                // discard() logic or checks might be needed if stream is corrupt,
                // but for now, reading int is standard.
                val length = readInt()
                if (length > MAX_PAYLOAD_LENGTH) throw IOException("Packet too large")
                val packet = ByteArray(length)
                readFully(packet)
                String(packet, Charsets.UTF_8)
            } catch (e: CancellationException) {
                throw e
            } catch (e: EOFException) {
                null // Socket closed cleanly
            } catch (e: ClosedReceiveChannelException) {
                null
            } catch (e: Exception) {
                Log.e(TAG, "SocketUtil: readPacket: ${e.message}", e)
                throw IOException("Socket read error", e)
            }
        }
}
