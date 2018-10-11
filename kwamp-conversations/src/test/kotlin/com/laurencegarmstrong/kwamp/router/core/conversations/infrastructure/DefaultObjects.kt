package com.laurencegarmstrong.kwamp.router.core.conversations.infrastructure

import co.nz.arm.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.router.core.Realm
import com.laurencegarmstrong.kwamp.router.core.Router

fun defaultRouter() = Router().apply {
    addRealm(Realm(Uri("default")))
}
