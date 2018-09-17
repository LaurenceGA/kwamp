package co.nz.arm.kwamp.router

import co.nz.arm.kwamp.core.Connection
import co.nz.arm.kwamp.core.Uri
import java.util.concurrent.ConcurrentHashMap

class Router {
    private val realms = ConcurrentHashMap<Uri, Realm>()

    suspend fun registerConnection(connection: Connection) = SessionEstablisher(realms, connection).establish()

    fun addRealm(realm: Realm) {
        if (realm.uri !in realms) realms[realm.uri] = realm
        else throw RuntimeException("Realm already exists")
    }
}