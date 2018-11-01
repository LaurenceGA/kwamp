package com.laurencegarmstrong.kwamp.client.core.call

import com.laurencegarmstrong.kwamp.core.messages.Dict

data class CallResult(val arguments: List<Any?>? = null, val argumentsKw: Dict? = null)