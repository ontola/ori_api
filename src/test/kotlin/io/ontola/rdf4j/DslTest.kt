/*
 * Ontola RDF4j Helpers
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

package io.ontola.rdf4j

import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.ontola.activitystreams.vocabulary.AS
import io.ontola.ori.api.ORio
import io.ontola.testhelpers.asResource
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.rio.RDFFormat
import java.io.StringWriter

class DSLTest : StringSpec({
    val exIRI = "http://example.com/example".iri()
    val nestedIRI = "http://example.com/example/nested".iri()

    "defines a blank resource" {
        val resource = model { iri = exIRI }

        resource.toModel() shouldHaveSize 0
    }

    "accepts data properties" {
        val resource = model {
            iri = exIRI

            prop(AS.NAME, "Test")
        }

        resource.toModel() shouldHaveSize 1
        val st = resource.toModel().first()
        st.subject shouldBe exIRI
        st.predicate shouldBe AS.NAME
        st.`object` shouldBe "Test".xsdString()
    }

    "accepts object properties" {
        val resource = model {
            iri = exIRI

            prop(AS.NAME) model {
                iri = nestedIRI
            }
        }

        resource.toModel() shouldHaveSize 1
        val st = resource.toModel().first()
        st.subject shouldBe exIRI
        st.predicate shouldBe AS.NAME
        st.`object` shouldBe nestedIRI
    }

    "created nested resources" {
        val resource = model {
            iri = exIRI

            prop(AS.NAME, "Test")
            prop(AS.ATTACHMENTS) {
                +model {
                    iri = "http://example.com/attachment/1".iri()

                    prop(AS.NAME, "Attachment 1")
                    prop(AS.AUTHOR) model {
                        prop("http://schema.org/name".iri(), "Peter Peterson")
                        prop(RDF.TYPE, "http://schema.org/Person".iri())
                    }
                    prop(AS.HREF, iri) // the url
                }

                +model {
                    prop(AS.NAME, "Attachment 2 (blank node)")
                }

                +model {
                    prop(AS.NAME, "Attachment 3 (blank node)")
                }
            }
        }

        val example = resource.toModel()

        example shouldHaveSize 11

        val out = StringWriter()
        val writer = ORio.createWriter(RDFFormat.TURTLE, out)
        writer.handleSingleModel(example)

        val turtle = out.toString()

        turtle shouldBe "/io/ontola/rdf4j/DslTest/nestedResources.ttl".asResource()
    }
})
