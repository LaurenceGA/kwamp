package com.laurencegarmstrong.kwamp.core

enum class WampClose(uri: String) {
    // The Peer is shutting down completely - used as a GOODBYE (or ABORT) reason.
    SYSTEM_SHUTDOWN("wamp.close.system_shutdown"),
    // The Peer want to leave the realm - used as a GOODBYE reason.
    CLOSE_REALM("wamp.close.close_realm"),
    // A Peer acknowledges ending of a session - used as a GOODBYE reply reason.
    GOODBYE_AND_OUT("wamp.close.goodbye_and_out");

    val uri = Uri(uri)
}