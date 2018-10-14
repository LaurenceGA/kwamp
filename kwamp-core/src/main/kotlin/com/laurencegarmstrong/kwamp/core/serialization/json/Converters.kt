package com.laurencegarmstrong.kwamp.core.serialization.json

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.MessageType

object UriConverter : Converter {
    override fun canConvert(cls: Class<*>) = cls == Uri::class.java
    override fun fromJson(jv: JsonValue): Any = false
    override fun toJson(value: Any) = "\"${(value as Uri).text}\""
}

object MessageTypeConverter : Converter {
    override fun canConvert(cls: Class<*>) = cls == MessageType::class.java
    override fun fromJson(jv: JsonValue): Any = false
    override fun toJson(value: Any) = "${(value as MessageType).id}"
}