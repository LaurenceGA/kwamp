package com.laurencegarmstrong.kwamp.conversations.core

import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.router.core.Realm
import com.laurencegarmstrong.kwamp.router.core.Router

fun defaultRouter() = Router().apply {
    addRealm(Realm(Uri("default")))
}
