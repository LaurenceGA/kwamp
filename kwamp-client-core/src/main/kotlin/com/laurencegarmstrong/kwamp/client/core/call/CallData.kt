package com.laurencegarmstrong.kwamp.client.core.call

import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.Dict
import com.laurencegarmstrong.kwamp.core.messages.Error
import com.laurencegarmstrong.kwamp.core.messages.MessageType

data class CallResult(val arguments: List<Any?>? = null, val argumentsKw: Dict? = null)

data class CallError(val error: Uri, val details: Dict, val arguments: List<Any?>?, val argumentsKw: Dict?) :
    Throwable(message = error.text)

internal fun Error.toCallException() =
    if (requestType == MessageType.CALL)
        CallError(
            error,
            details,
            arguments,
            argumentsKw
        ) else throw IllegalArgumentException("Request message type must be CALL to to convert to call exception, but got $requestType")
