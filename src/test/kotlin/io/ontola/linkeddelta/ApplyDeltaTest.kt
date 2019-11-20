/*
 * Linked delta
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

package io.ontola.linkeddelta

import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.specs.WordSpec
import io.ontola.activitystreams.vocabulary.SCHEMA
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.model.vocabulary.RDF

class ApplyDeltaTest : WordSpec({
    val factory = SimpleValueFactory.getInstance()

    "applyDelta" should {
        fun getStatement(graph: String): Statement {
            return factory.createStatement(
                factory.createIRI("http://test.org/"),
                SCHEMA.NAME,
                factory.createLiteral("test"),
                factory.createIRI(graph)
            )
        }

        "handle plain graph name" {
            val statement = getStatement("http://purl.org/linked-delta/replace")

            val current = LinkedHashModel()
            val delta = LinkedHashModel()
            delta.add(statement)

            applyDelta(processors, current, delta) shouldHaveSize 1
        }

        "handle query parameters" {
            val statement = getStatement("http://purl.org/linked-delta/replace?graph=https%3A//id.openraadsinformatie.nl/440906")

            val current = LinkedHashModel()
            val delta = LinkedHashModel()
            delta.add(statement)

            applyDelta(processors, current, delta) shouldHaveSize 1
        }

        "has safe replacement" {
            val resource = factory.createIRI("http://test.org/")
            val c1 = factory.createStatement(resource, SCHEMA.NAME, factory.createLiteral("test"))
            val c2 = factory.createStatement(resource, SCHEMA.CREATOR, factory.createIRI("http://test.org/person/1"))
            val c2b = factory.createStatement(resource, SCHEMA.CREATOR, factory.createIRI("http://test.org/person/3"))
            val c3 = factory.createStatement(resource, RDF.TYPE, SCHEMA.custom("Thing"))

            val current = LinkedHashModel(listOf(c1, c2, c2b, c3))

            val d1 = factory.createStatement(
                resource,
                SCHEMA.CREATOR,
                factory.createIRI("http://test.org/person/2"),
                factory.createIRI("http://purl.org/linked-delta/replace")
            )
            val d2 = factory.createStatement(
                resource,
                SCHEMA.custom("description"),
                factory.createLiteral("desc"),
                factory.createIRI("http://purl.org/linked-delta/add")
            )

            val delta = LinkedHashModel(listOf(d1, d2))

            applyDelta(processors, current, delta) shouldHaveSize 4
        }
    }
})
