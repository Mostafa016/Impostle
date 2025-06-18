package com.example.nsddemo.data.util

import com.example.nsddemo.data.local.network.socket.Server
import com.example.nsddemo.domain.model.Player
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeStringUtf8

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
}