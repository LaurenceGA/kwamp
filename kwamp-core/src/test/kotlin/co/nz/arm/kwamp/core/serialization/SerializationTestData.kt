package co.nz.arm.kwamp.core.serialization

import co.nz.arm.kwamp.core.Uri
import co.nz.arm.kwamp.core.messages.*
import io.kotlintest.tables.row

private val detailsDict = mapOf("details" to "detail")
private const val detailsDictRaw = "{\"details\": \"detail\"}"
private val optionsDict = mapOf("options" to "option")
private const val optionsDictRaw = "{\"options\": \"option\"}"
val messageData = listOf(
    row(
        Hello(Uri("testRealm"), detailsDict),
        "[1, \"testRealm\", $detailsDictRaw]"
    ),
    row(
        Welcome(123, detailsDict),
        "[2, 123, $detailsDictRaw]"
    ),
    row(
        Abort(detailsDict, Uri("reason")),
        "[3, $detailsDictRaw, \"reason\"]"
    ),
    row(
        Goodbye(detailsDict, Uri("reason")),
        "[6, $detailsDictRaw, \"reason\"]"
    ),
    row(
        Error(5, 123, detailsDict, Uri("error")),
        "[8, 5, 123, $detailsDictRaw, \"error\"]"
    ),
//            row(Error(5, 123, "details", Uri("error"), listOf("arg1", 2, null)),
//                    "[8, 5, 123, \"details\", \"error\", [\"arg1\", 2, null]]"),
//                row(Error(5, 123, "details", Uri("error"), listOf("arg1", 2, null), mapOf("arg3" to "val3", "arg4" to 4)),
//                        "[8, 5, 123, \"details\", \"error\", [\"arg1\", 2, null], {\"arg3\": \"val3\", \"arg4\": 4}]"),
    row(
        Error(5, 123, detailsDict, Uri("error"), listOf("arg1", 2.0), mapOf("arg3" to "val3", "arg4" to 4.0)),
        "[8, 5, 123, $detailsDictRaw, \"error\", [\"arg1\", 2.0], {\"arg3\": \"val3\", \"arg4\": 4.0}]"
    ),
    row(
        Publish(123, optionsDict, Uri("topic")),
        "[16, 123, $optionsDictRaw, \"topic\"]"
    ),
    row(
        Published(123, 456),
        "[17, 123, 456]"
    ),
    row(
        Subscribe(123, optionsDict, Uri("topic")),
        "[32, 123, $optionsDictRaw, \"topic\"]"
    ),
    row(
        Subscribed(123, 456),
        "[33, 123, 456]"
    ),
    row(
        Unsubscribe(123, 456),
        "[34, 123, 456]"
    ),
    row(
        Unsubscribed(123),
        "[35, 123]"
    ),
    row(
        Event(123, 456, detailsDict),
        "[36, 123, 456, $detailsDictRaw]"
    ),
    row(
        Call(123, optionsDict, Uri("procedure")),
        "[48, 123, $optionsDictRaw, \"procedure\"]"
    ),
    row(
        Result(123, detailsDict),
        "[50, 123, $detailsDictRaw]"
    ),
    row(
        Register(123, optionsDict, Uri("procedure")),
        "[64, 123, $optionsDictRaw, \"procedure\"]"
    ),
    row(
        Registered(123, 456),
        "[65, 123, 456]"
    ),
    row(
        Unregister(123, 456),
        "[66, 123, 456]"
    ),
    row(
        Unregistered(123),
        "[67, 123]"
    ),
    row(
        Invocation(123, optionsDict),
        "[68, 123, $optionsDictRaw]"
    ),
    row(
        Yield(123, optionsDict),
        "[70, 123, $optionsDictRaw]"
    )
)