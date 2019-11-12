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

import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import io.ontola.activitystreams.vocabulary.SCHEMA
import io.ontola.rdfUtils.createIRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.impl.SimpleValueFactory

class BaseProcessorTest : WordSpec({
    val factory = SimpleValueFactory.getInstance()

    class TestProcessor : BaseProcessor() {
        override val graphIRI = createIRI("http://purl.org/linked-delta/test")

        override fun process(current: Model, delta: Model, st: Statement): DeltaProcessorResult {
            return DeltaProcessorResult(
                emptyList(),
                emptyList(),
                emptyList()
            )
        }
    }

    "match" should {
        fun getStatement(graph: String): Statement {
            return factory.createStatement(
                factory.createIRI("http://test.org/"),
                SCHEMA.NAME,
                factory.createLiteral("test"),
                factory.createIRI(graph)
            )
        }

        "handle plain graph name" {
            val statement = getStatement("http://purl.org/linked-delta/test")
            TestProcessor().match(statement) shouldBe true
        }

        "handle query parameters" {
            val statement = getStatement("http://purl.org/linked-delta/test?graph=https%3A//id.openraadsinformatie.nl/440906")
            TestProcessor().match(statement) shouldBe true
        }
    }
})
