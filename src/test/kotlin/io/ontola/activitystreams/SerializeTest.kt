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

import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import io.ontola.rdf4j.iri
import org.eclipse.rdf4j.model.BNode
import org.eclipse.rdf4j.model.impl.SimpleValueFactory

class SerializeTest : WordSpec({
    val namedResource = "https://id.openraadsinformatie.nl/1005".iri()

    "constructSubject" should {
        "handle null" {
            constructSubject(null).shouldBeInstanceOf<BNode>()
        }

        "handle IRIs" {
            constructSubject(namedResource) shouldBe namedResource
        }

        "preserve bnode ids" {
            val bnode = SimpleValueFactory.getInstance().createBNode("testId")
            constructSubject(bnode) shouldBe bnode
        }
    }
})
