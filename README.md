# Kotlin Web Application Messaging Protocol (KWAMP)
This is an implementation of the [Web Application Messaging Protocol](https://wamp-proto.org/) (WAMP) written in Kotlin.

It's based on the [WAMP RFC](https://wamp-proto.org/static/rfc/draft-oberstet-hybi-crossbar-wamp.html).

## In this project
### KWAMP Router Core
Transport agnostic WAMP router implementation logic using Kotlin coroutines that routes client messages.

### KWAMP Router Example
An example usage of KWAMP router that uses [KTOR](https://ktor.io/) [websockets](https://ktor.io/servers/features/websockets.html) to host the router.

### KWAMP Client Core
A transport agnostic WAMP client implementation that interfaces with a WAMP router.

### KWAMP Client Example
An example usage of KWAMP client that uses the [KTOR](https://ktor.io/) [websocket client](https://ktor.io/clients/websockets.html) to communicate to a WAMP router.

### KWAMP Router and client Conversations
A set of example conversations with a mocked out router or client.
