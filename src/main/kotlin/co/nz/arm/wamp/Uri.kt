package co.nz.arm.wamp

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue

data class Uri(val uri: String) {
    object UriConverter : Converter {
        override fun canConvert(cls: Class<*>) = cls == Uri::class.java
        override fun fromJson(jv: JsonValue): Any = false
        override fun toJson(value: Any) = "\"${(value as Uri).uri}\""
    }
}

