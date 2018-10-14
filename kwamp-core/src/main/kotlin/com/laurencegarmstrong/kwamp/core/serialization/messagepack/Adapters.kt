package com.laurencegarmstrong.kwamp.core.serialization.messagepack

import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.MessageType
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

object UriJsonAdapter : JsonAdapter<Uri>() {
    override fun fromJson(reader: JsonReader?): Uri? = null
    @ToJson
    override fun toJson(writer: JsonWriter?, uri: Uri?) {
        writer!!.value(uri!!.text)
    }
}

object MessageTypeJsonAdapter : JsonAdapter<MessageType>() {
    override fun fromJson(reader: JsonReader?): MessageType? = null
    @ToJson
    override fun toJson(writer: JsonWriter?, messageType: MessageType?) {
        writer!!.value(messageType!!.id)
    }
}