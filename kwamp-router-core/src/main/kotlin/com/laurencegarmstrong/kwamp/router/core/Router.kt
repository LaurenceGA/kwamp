package com.laurencegarmstrong.kwamp.router.core

import com.laurencegarmstrong.kwamp.core.Connection
import com.laurencegarmstrong.kwamp.core.Uri
import java.util.concurrent.ConcurrentHashMap

class Router(private val strictUris: Boolean = false) {
    private val realms = ConcurrentHashMap<Uri, Realm>()
    private val messageSender = MessageSender()
    private val sessionEstablisher = SessionEstablisher(realms, messageSender)

    fun registerConnection(connection: Connection) = sessionEstablisher.establish(
        connection.apply {
            setStrictUris(strictUris)
        }
    )

    fun addRealm(realmUri: Uri) {
        ensureStrictUriIfRequired(realmUri)
        if (!realms.containsKey(realmUri)) {
            realms[realmUri] = constructRealm(realmUri)
        } else {
            throw IllegalArgumentException("Realm already exists")
        }
    }

    private fun constructRealm(realmUri: Uri) = Realm(realmUri, messageSender)

    private fun ensureStrictUriIfRequired(realmUri: Uri) {
        if (strictUris) realmUri.ensureStrict()
    }
}