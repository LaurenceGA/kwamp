package com.laurencegarmstrong.kwamp.core.serialization.json

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.laurencegarmstrong.kwamp.core.UriPattern
import com.laurencegarmstrong.kwamp.core.messages.MessageType

object UriConverter : Converter {
    override fun canConvert(cls: Class<*>) = UriPattern::class.java.isAssignableFrom(cls)
    override fun fromJson(jv: JsonValue): Any = false
    override fun toJson(value: Any) = "\"${(value as UriPattern).text}\""
}

object MessageTypeConverter : Converter {
    override fun canConvert(cls: Class<*>) = cls == MessageType::class.java
    override fun fromJson(jv: JsonValue): Any = false
    override fun toJson(value: Any) = "${(value as MessageType).id}"
}