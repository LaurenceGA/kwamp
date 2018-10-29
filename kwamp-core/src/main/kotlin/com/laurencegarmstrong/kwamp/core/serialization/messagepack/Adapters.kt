package com.laurencegarmstrong.kwamp.core.serialization.messagepack

import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.UriPattern
import com.laurencegarmstrong.kwamp.core.messages.MessageType
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

object UriPatternJsonAdapter : JsonAdapter<UriPattern>() {
    override fun fromJson(reader: JsonReader?): UriPattern? = null
    @ToJson
    override fun toJson(writer: JsonWriter?, uriPattern: UriPattern?) {
        writer!!.value(uriPattern!!.text)
    }
}

object UriJsonAdapter : JsonAdapter<Uri>() {
    override fun fromJson(reader: JsonReader?): Uri? = null
    @ToJson
    override fun toJson(writer: JsonWriter?, uri: Uri?) = UriPatternJsonAdapter.toJson(writer, uri)
}

object MessageTypeJsonAdapter : JsonAdapter<MessageType>() {
    override fun fromJson(reader: JsonReader?): MessageType? = null
    @ToJson
    override fun toJson(writer: JsonWriter?, messageType: MessageType?) {
        writer!!.value(messageType!!.id)
    }
}