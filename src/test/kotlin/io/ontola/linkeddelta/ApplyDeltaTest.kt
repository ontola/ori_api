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
    }
})
