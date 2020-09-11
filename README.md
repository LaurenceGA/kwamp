# Kotlin Web Application Messaging Protocol (KWAMP)
This is an implementation of the [Web Application Messaging Protocol](https://wamp-proto.org/) (WAMP) written in Kotlin.

The provides facilities for routed Remote Procdeure Calls (RPC) and Publish and Subscribe (PubSub). 

It's based on the [WAMP spec](https://wamp-proto.org/_static/gen/wamp_latest.html).

So far this project aims to fulfil the WAMP basic profile.

## Getting the libraries
Under repositories add:
```groovy
repositories {
    mavenCentral()
    jcenter()   // Required
    maven { url 'https://jitpack.io' }
}
```

### Router
Under dependencies:
```groovy
dependencies {
    implementation("com.github.LaurenceGA.kwamp:kwamp-router-core:1.0.5")
}
```

### Client
Under dependencies:
```groovy
dependencies {
    implementation("com.github.LaurenceGA.kwamp:kwamp-client-core:1.0.5")
}
```

## Usage
KWAMP is mostly transport independent (it must be a valid [WAMP transport](https://wamp-proto.org/_static/gen/wamp_latest.html#transports)). E.G raw socket or web socket.

To use the KWAMP client or router you can use the kwamp-client-core or kwamp-router-core packages respectively.
They just need to be hooked into a transport (doing this can be seen in kwamp-client-example and kwamp-router-example respectively).

(At some point these integrations should become actual packages in this project which can be used by themselves.)

### Router
The router supports the WAMP basic profile.
To create a router:
```kotlin
val router = Router()
```

A client won't be able to connect to it unless it has a [Realm](https://wamp-proto.org/_static/gen/wamp_latest.html#realms-sessions-and-transports) (WAMP message routing domain):
```kotlin
router.addRealm(Realm(Uri("myRealm")))
```

Connections then must be registered with the router in order for them to communicate with it:
```kotlin
val incoming = Channel<ByteArray>()
val outgoing = Channel<ByteArray>()

val connection = Connection(
    incoming,
    outgoing,
    { message ->
        // callback when router closes a connection
        // so you can flush and close the underlying transport
    },
    messageSerializer = JsonMessageSerializer() // default message serializer
)

router.registerConnection(connection)
```

You then just need to ensure that the incoming and outgoing channels are hooked into the input/output of the underlying transport.
```kotlin
launch {    // In another thread so it doesn't block
    outgoing.consumeEach { message ->
        // forward on anything the router wants to send over the connection to the transport here
    }
}

// Send anything the transport receives to the incoming channel
transportIncoming.consumeEach { message ->
    incoming.send(/* transport message as byte array */)
}
```

### Client
A client is created with:
```kotlin
val incoming = Channel<ByteArray>()
val outgoing = Channel<ByteArray>()
val client = ClientImpl(wampIncoming, wampOutgoing, Uri("<REALM_URI_HERE>"))
val sessionId = client.getSessionId()
```

Then you just need to hook up incoming and outgoing channels with the input/output of the underlying channel (the same as is done above with the router).

#### Registering a procedure
To register a procedure with the router the client is connected to use:
```kotlin
val registrationHandle = client.register(Uri("<PROCEDURE_URI_HERE>")) { arguments, argumentsKw ->
    // Do something here and return a CallResult object
    CallResult(listOf(1, 2, 3), mapOf("one" to 1))
    // Or just CallResult() if you want empty return
}
```

#### Unregistering a procedure
Unregistering is a blocking procedure. Using the registration handle from when you registered:
```kotlin
registrationHandle.unregister()
```

#### Calling a procedure
To call a procedure use:
```kotlin
val call = client.call(Uri("<PROCEDURE_URI_HERE>"))
```

This produces a `DeferredCallResult`.  
You can then use this to await the result.
```kotlin
val result = call.await()
```

If there is an error executing the procedure then this will throw an exception.

#### Subscribing to a topic
To subscribe to a topic use:
```kotlin
val subscriptionHandle = client.subscribe("<PROCEDURE_URI_HERE>") { arguments, argumentsKw ->
    // Event callback
    // Do something with event arguments here...
}
```

#### Unsubscribing to a topic
Unsubscribing is a blocking procedure. Using the subscription handle from when you subscribed:
```kotlin
subscriptionHandle.unsubscribe()
```

#### Publishing to a topic
To publish to a topic, a client can use:
```kotlin
client.publish(Uri("<TOPIC_URI_HERE>"), publishArguments, publishArgumentsKw)
```
where `publishArguments` is of type `List<Any?>?` and publishArgumentsKw is of type `Map<String, Any?>?`.

If you also want it acknowledged, `publish()` has an extra optional argument:
```kotlin
client.publish(testTopic, publishArguments, publishArgumentsKw) { id ->
    // Do something. Id is the ID of the publication
}
```

#### Disconnecting from a router
Disconnecting is a blocking procedure used like so:
```kotlin
client.disconnect()
```
The client will say goodbye to the router and wait for it to say goodbye back.
The function also returns the goodbye reason sent from the router if you want it.

## In this repo
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
