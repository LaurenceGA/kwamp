package com.laurencegarmstrong.kwamp.core

open class UriPattern(val text: String) {
    // A Uri Pattern can contain empty components. E.G "a.b..d"
    protected open val relaxedRegex = Regex("""^(([^\s.#]+\.)|\.)*([^\s.#]+)?$""")
    protected open val strictRegex = Regex("""^(([0-9a-z_]+\.)|\.)*([0-9a-z_]+)?$""")

    protected fun matchesRegex(regex: Regex) {
        if (regex doesNotMatch text) throw InvalidUriException("${this::class.simpleName} much match regular expression ${regex.pattern}")
    }

    fun ensureValid() = matchesRegex(relaxedRegex)
    fun ensureStrict() = matchesRegex(strictRegex)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UriPattern) return false

        return text == other.text
    }

    override fun hashCode(): Int {
        return text.hashCode()
    }

    override fun toString(): String {
        return text
    }
}

class Uri(text: String) : UriPattern(text) {
    override val relaxedRegex = Regex("""^([^\s.#]+\.)*([^\s.#]+)$""")
    override val strictRegex = Regex("""^([0-9a-z_]+\.)*([0-9a-z_]+)$""")
}

infix fun Regex.doesNotMatch(text: String) = !(this matches text)