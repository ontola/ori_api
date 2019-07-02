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

import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import io.ontola.ori.api.context.CtxProps
import io.ontola.rdf4j.iri
import io.ontola.testhelpers.TestContext
import io.ontola.testhelpers.deltaDocument

class EventTest : WordSpec({
    "parseRecord" should {
        "default null" {
            val doc = TestContext(CtxProps(iri = "http://id.openraadsinformatie.nl/1002".iri()))
            val e = Event.parseRecord(doc)

            e.shouldBeNull()
        }

        "parse delta events" {
            val doc = deltaDocument("/io/ontola/ori/api/EventTest/deltaCreateOrganization.nq")
            val e = Event.parseRecord(doc)

            e.shouldBeInstanceOf<DeltaEvent>()
            (e as DeltaEvent).type shouldBe EventType.DELTA
        }
    }
})
