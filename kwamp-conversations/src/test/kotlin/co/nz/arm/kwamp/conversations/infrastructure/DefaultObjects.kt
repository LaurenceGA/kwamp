package co.nz.arm.kwamp.conversations.infrastructure

import co.nz.arm.kwamp.core.Uri
import co.nz.arm.kwamp.router.Realm
import co.nz.arm.kwamp.router.Router

fun defaultRouter() = Router().apply { addRealm(Realm(Uri("default"))) }
