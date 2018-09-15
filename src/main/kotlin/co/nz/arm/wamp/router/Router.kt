package co.nz.arm.wamp.router

import co.nz.arm.wamp.core.Connection
import co.nz.arm.wamp.core.Uri
import co.nz.arm.wamp.messages.*
import java.util.concurrent.ConcurrentHashMap

class Router {
    private val realms = ConcurrentHashMap<Uri, Realm>()

    suspend fun registerConnection(connection: Connection) = SessionEstablisher(realms, connection).establish()

    fun addRealm(realm: Realm) {
        if (realm.uri !in realms) realms[realm.uri] = realm
        else throw RuntimeException("Realm already exists")
    }
}