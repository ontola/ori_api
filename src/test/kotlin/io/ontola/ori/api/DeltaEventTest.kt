/*
 * ORI API
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

package io.ontola.ori.api

import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.ontola.activitystreams.vocabulary.AS
import io.ontola.activitystreams.vocabulary.SCHEMA
import io.ontola.rdf4j.model
import io.ontola.rdf4j.prop
import io.ontola.rdfUtils.createIRI
import io.ontola.testhelpers.deltaDocument
import io.ontola.testhelpers.emptyContext
import io.ontola.testhelpers.nqResourceToModel
import io.ontola.testhelpers.testModel
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.model.vocabulary.RDF

class DeltaEventTest : StringSpec({
    "::partition" should {
        "partition single resource" {
            val model = testModel("1000") {
                prop(AS.NAME, "test")
            }.toModel()

            val event = DeltaEvent(emptyContext(), model)
            val partitioned = event.partition()

            partitioned.values shouldHaveSize 1
        }

        "partition single resource with blank nodes" {
            val model = testModel("1000") {
                prop(AS.ACTOR) model {
                    prop(AS.NAME, "Some actor")
                }
            }.toModel()

            val event = DeltaEvent(emptyContext(), model)
            val partitioned = event.partition()

            partitioned.values shouldHaveSize 1
        }

        "partition single resource with rdf list" {
            val model = nqResourceToModel("/io/ontola/ori/api/DeltaEventTest/singleWithBN.nq")

            val factory = SimpleValueFactory.getInstance()
            val quad = { s: Resource, p: IRI, o: Value, g: IRI -> factory.createStatement(s, p, o, g) }
            val ori10 = createIRI("https://id.openraadsinformatie.nl/10")
            val ori11 = createIRI("https://id.openraadsinformatie.nl/11")
            val oriOther = createIRI("https://id.openraadsinformatie.nl/other")
            val listRoot = createIRI("https://id.openraadsinformatie.nl/10#listRoot")
            val listIntermediate = factory.createBNode("list1")
            val listLeaf = factory.createBNode("list2")
            val supplant = createIRI("http://purl.org/link-lib/supplant")

            val event = DeltaEvent(emptyContext(), model)
            val partitioned = event.partition()
            val keys = partitioned.keys.toList()
            val values = partitioned.values.toList()

            values shouldHaveSize 3

            keys[0] shouldBe ori10
            keys[1] shouldBe ori11
            keys[2] shouldBe oriOther

            val firstDelta = values.first().delta
            val supplantIn10 = createIRI("http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2F10")

            firstDelta shouldContain quad(ori10, SCHEMA.NAME, factory.createLiteral("Test"), supplantIn10)
            firstDelta shouldContain quad(ori10, SCHEMA.custom("tags"), listRoot, supplantIn10)
            firstDelta shouldContain quad(listRoot, RDF.REST, listIntermediate, supplantIn10)
            firstDelta shouldContain quad(listLeaf, RDF.FIRST, factory.createLiteral("Value 2"), supplantIn10)
            firstDelta shouldContain quad(listLeaf, RDF.REST, RDF.NIL, supplantIn10)

            val secondDelta = values[1].delta
            secondDelta shouldContain quad(ori11, SCHEMA.NAME, factory.createLiteral("Different subject no graph"), supplant)

            val otherDelta = values[2].delta
            val supplantInOther = createIRI("http://purl.org/link-lib/supplant?graph=https%3A%2F%2Fid.openraadsinformatie.nl%2Fother")
            otherDelta shouldContain quad(
                ori10,
                SCHEMA.custom("error"),
                factory.createLiteral("Same subject with different delta graph"),
                supplantInOther
            )
        }

        "partition multiple resources" {
            val model1 = testModel("1001") {
                prop(AS.NAME, "test1")
            }
            val model2 = testModel("1002") {
                prop(AS.NAME, "test2")
            }
            val model3 = testModel("1003") {
                prop(AS.NAME, "test3")
            }

            val model = LinkedHashModel(model1.toModel() + model2.toModel() + model3.toModel())

            val event = DeltaEvent(emptyContext(), model)
            val partitioned = event.partition()

            partitioned.values shouldHaveSize 3
        }
    }
})
