package com.laurencegarmstrong.kwamp.core

import io.kotlintest.be
import io.kotlintest.data.forall
import io.kotlintest.matchers.string.beEmpty
import io.kotlintest.should
import io.kotlintest.shouldNot
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

internal class UriTest : StringSpec({
    "Can create valid Uris" {
        forall(
            row("a.b"),
            row("a"),
            row("a.b.c.d.e.f"),
            row("1.2.3.4"),
            row("a.1.2.D.EF.G3"),
            row("!.@.$.%.^.&.*.(.)"),
            row("-.=.,.;")
        ) { text ->
            Uri(text).ensureValid()
        }
    }

    "Exception on invalid Uris" {
        forall(
            row(".a.b"),
            row("a."),
            row("a..f"),
            row("1 2 3 4"),
            row("Not a uri"),
            row("."),
            row(""),
            row(" "),
            row(". ."),
            row("a.#")
        ) { text ->
            shouldThrow<InvalidUriException> {
                Uri(text).ensureValid()
            }
        }
    }

    "Can create valid Uri patterns" {
        forall(
            row("a.b"),
            row("a"),
            row("a.b.c.d.e.f"),
            row("1.2.3.4"),
            row("a.1.2.D.EF.G3"),
            row("!.@.$.%.^.&.*.(.)"),
            row("-.=.,.;"),
            row(".abc"),
            row("..abc"),
            row("a..b..c"),
            row("a..b..c......"),
            row("a..b..c."),
            row("."),
            row("")
        ) { pattern ->
            UriPattern(pattern).ensureValid()
        }
    }

    "Exception on invalid Uri patterns" {
        forall(
            row("1 2 3 4"),
            row("Not a uri"),
            row(" "),
            row(". ."),
            row("a.#")
        ) { pattern ->
            shouldThrow<InvalidUriException> {
                UriPattern(pattern).ensureValid()
            }
        }
    }

    "Can create valid strict Uris" {
        forall(
            row("a.b"),
            row("a"),
            row("a.b.c.d.e.f"),
            row("1.2.3.4")
        ) { text ->
            Uri(text).ensureStrict()
        }
    }

    "Exception on invalid strict Uris" {
        forall(
            row(".a.b"),
            row("a."),
            row("a..f"),
            row("1 2 3 4"),
            row("Not a uri"),
            row("."),
            row(""),
            row(" "),
            row(". ."),
            row("a.#"),
            row("a.1.2.D.EF.G3"),
            row("!.@.$.%.^.&.*.(.)"),
            row("-.=.,.;")
        ) { text ->
            shouldThrow<InvalidUriException> {
                Uri(text).ensureStrict()
            }
        }
    }

    "Can create valid strict Uri patterns" {
        forall(
            row("a.b"),
            row("a"),
            row("a.b.c.d.e.f"),
            row("1.2.3.4"),
            row(".abc"),
            row("..abc"),
            row("a..b..c"),
            row("a..b..c......"),
            row("a..b..c."),
            row("."),
            row("")
        ) { pattern ->
            UriPattern(pattern).ensureStrict()
        }
    }

    "Exception on invalid strict Uri patterns" {
        forall(
            row("1 2 3 4"),
            row("Not a uri"),
            row(" "),
            row(". ."),
            row("a.#"),
            row("a.1.2.D.EF.G3"),
            row("!.@.$.%.^.&.*.(.)"),
            row("-.=.,.;")
        ) { pattern ->
            shouldThrow<InvalidUriException> {
                UriPattern(pattern).ensureStrict()
            }
        }
    }

    "A Uri is equal to the equivalent UriPattern" {
        val testUri = Uri("test.topic")
        val testUriPattern = UriPattern("test.topic")
        testUri.hashCode() should be(testUriPattern.hashCode())
        testUri should be(testUriPattern)

        val testMap = HashMap<UriPattern, String>()
        testMap[testUriPattern] = "test"
        testMap[testUri] shouldNot beEmpty()
    }
})