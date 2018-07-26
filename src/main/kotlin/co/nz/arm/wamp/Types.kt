package co.nz.arm.wamp

import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel

class URI(uri: String)

class Connection(incoming: ReceiveChannel<Frame>, outgoing: SendChannel<Frame>)