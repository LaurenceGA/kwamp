package co.nz.arm.wamp

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

data class Uri(val uri: String) {
    object UriConverter : Converter {
        override fun canConvert(cls: Class<*>) = cls == Uri::class.java
        override fun fromJson(jv: JsonValue): Any = false
        override fun toJson(value: Any) = "\"${(value as Uri).uri}\""
    }

    object UriJsonAdapter : JsonAdapter<Uri>() {
        override fun fromJson(reader: JsonReader?): Uri? = null
        @ToJson
        override fun toJson(writer: JsonWriter?, uri: Uri?) {
            writer!!.value(uri!!.uri)
        }
    }
}

