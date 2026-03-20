package com.mostafa.impostle.data.local.network.socket

sealed class MessageEvent(
    open val clientId: String,
    open val data: String,
) {
    data class Received(
        override val clientId: String,
        override val data: String,
    ) : MessageEvent(clientId, data)

    data class Sent(
        override val clientId: String,
        override val data: String,
    ) : MessageEvent(clientId, data)
}
