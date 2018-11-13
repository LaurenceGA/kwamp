package com.laurencegarmstrong.kwamp.client.core.call

import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.Dict

data class CallResult(val arguments: List<Any?>? = null, val argumentsKw: Dict? = null)

val DEFAULT_INVOCATION_ERROR = Uri("error.invocation_failed")

data class CallException(
    val arguments: List<Any?>? = null,
    val argumentsKw: Dict? = null,
    val error: Uri = DEFAULT_INVOCATION_ERROR
) : Exception()