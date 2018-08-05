package co.nz.arm.wamp.router

import co.nz.arm.wamp.Connection
import co.nz.arm.wamp.URI
import io.netty.util.internal.ConcurrentSet

class Realm(val uri: URI) {
    private val sessions = ConcurrentSet<WampSession>()

    fun join(connection: Connection) = sessions.add(WampSession(connection))
}

class WampSession(private val connection: Connection)