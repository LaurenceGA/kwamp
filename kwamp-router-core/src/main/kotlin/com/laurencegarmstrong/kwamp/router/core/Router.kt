package com.laurencegarmstrong.kwamp.router.core

import com.laurencegarmstrong.kwamp.core.Connection
import com.laurencegarmstrong.kwamp.core.Uri
import java.util.concurrent.ConcurrentHashMap

class Router(private val strictUris: Boolean = false) {
    private val realms = ConcurrentHashMap<Uri, Realm>()

    suspend fun registerConnection(connection: Connection) = SessionEstablisher(
        realms,
        connection.apply {
            setStrictUris(strictUris)
        }
    ).establish()

    fun addRealm(realm: Realm) {
        strictIfRequired(realm)
        if (realm.uri !in realms) realms[realm.uri] = realm
        else throw IllegalArgumentException("Realm already exists")
    }

    private fun strictIfRequired(realm: Realm) {
        if (strictUris) realm.uri.ensureStrict()
    }
}