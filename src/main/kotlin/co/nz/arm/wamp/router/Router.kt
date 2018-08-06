package co.nz.arm.wamp.router

import co.nz.arm.wamp.Connection
import co.nz.arm.wamp.Uri
import co.nz.arm.wamp.messages.*
import java.util.concurrent.ConcurrentHashMap

class Router {
    private val realms = ConcurrentHashMap<Uri, Realm>()

    suspend fun registerConnection(connection: Connection) = SessionEstablisher(realms, connection).establish()

    private suspend fun sendProtocolViolation(connection: Connection, message: String) = connection.apply {
        send(Abort("{}", Uri(message)))
        close(message)
    }

    fun addRealm(realm: Realm) {
        if (realm.uri !in realms) realms[realm.uri] = realm
        else throw RuntimeException("Realm already exists")
    }
}