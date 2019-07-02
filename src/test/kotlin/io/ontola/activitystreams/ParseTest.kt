/*
 * ActivityStreams
 * Copyright (C), Argu BV
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.ontola.activitystreams

import io.kotlintest.properties.assertAll
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.WordSpec
import io.ontola.activitystreams.vocabulary.AS
import io.ontola.rdf4j.iri
import io.ontola.rdf4j.model
import io.ontola.rdf4j.prop
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.model.vocabulary.RDF

class ParseTest : WordSpec({
    val factory = SimpleValueFactory.getInstance()

    "modelToObject" should {
        "not parse empty model" {
            val iri = factory.createBNode()
            val e = shouldThrow<Exception> {
                modelToObject<Object>(LinkedHashModel(), iri)
            }

            e.message shouldBe "No explicit type given for $iri"
        }

        "parse filled models" {
            val modelId = "https://id.openraadsinformatie.nl/1100".iri()
            val attachment1 = factory.createBNode()
            val attachment2 = "https://id.openraadsinformatie.nl/1101".iri()
            val attachment3 = factory.createBNode()

            val model = model {
                iri = modelId

                prop(RDF.TYPE, AS.COLLECTION_PAGE)
                prop(AS.NAME, "Test")
                prop(AS.ITEMS) {
                    +model {
                        iri = attachment1
                        prop(RDF.TYPE, AS.DOCUMENT)
                        prop(AS.NAME, "Attachment 1")
                    }
                    +model {
                        iri = attachment2
                        prop(RDF.TYPE, AS.DOCUMENT)
                        prop(AS.NAME, "Attachment 2")
                    }
                    +model {
                        iri = attachment3
                        prop(RDF.TYPE, AS.DOCUMENT)
                        prop(AS.NAME, "Attachment 3")
                    }
                }
            }.toModel()

            val attachmentList = listOf(
                Object(id = attachment1, type = AS.DOCUMENT, name = "Attachment 1"),
                Object(id = attachment2, type = AS.DOCUMENT, name = "Attachment 2"),
                Object(id = attachment3, type = AS.DOCUMENT, name = "Attachment 3")
            )

            val obj = modelToObject<CollectionPage<ASObject>>(model, modelId)

            if (obj !is CollectionPage<*>) {
                throw Exception("Parser gives wrong type")
            }

            obj.id shouldBe modelId
            obj.type shouldBe AS.COLLECTION_PAGE
            obj.name shouldBe "Test"
            obj.items shouldBe attachmentList
        }
    }

    "literalToNative" should {
        "parse strings" {
            assertAll { value: String ->
                literalToNative(factory.createLiteral(value)) shouldBe value
            }
        }

        "parse integers" {
            assertAll { value: Int ->
                literalToNative(factory.createLiteral(value)) shouldBe value.toBigInteger()
            }
        }

        "parse booleans" {
            assertAll { value: Boolean ->
                literalToNative(factory.createLiteral(value)) shouldBe value
            }
        }
    }
})
