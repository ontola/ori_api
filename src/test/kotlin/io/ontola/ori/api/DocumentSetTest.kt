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

import io.kotlintest.matchers.file.shouldContainFile
import io.kotlintest.matchers.file.shouldExist
import io.kotlintest.should
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec
import io.ontola.activitystreams.vocabulary.AS
import io.ontola.ori.api.context.CtxProps
import io.ontola.rdf4j.prop
import io.ontola.rdfUtils.createIRI
import io.ontola.testhelpers.TestContext
import io.ontola.testhelpers.testModel
import java.io.File

class DocumentSetTest : StringSpec({
    "::process" should {
        "should generate the appropriate document structure" {
            val delta = testModel("1008") {
                prop(AS.NAME, "test")
            }

            val ctx = TestContext(CtxProps(iri = createIRI("http://id.openraadsinformatie.nl/1008")))
            val docSet = DocumentSet(ctx, delta.toModel())

            docSet.process()

            ctx.dir().shouldExist()
            ctx.dir() shouldContainFile "latest"

            val latest = ctx.copy(version = "latest").dir()
            for (ext in listOf("nt", "brf", "nq", "jsonld", "zip", "n3", "ttl", "rj", "rdf", "activity.json")) {
                latest shouldContainFile "${ctx.id}.$ext"
            }

            latest shouldContainFile "activity"
            val activity = File("${latest.toPath()}/activity")

            activity shouldContainFile "${ctx.id}.activity.json"
            activity shouldContainFile "${ctx.id}.nq"

            activity shouldContainFile "${ctx.id};page=1.activity.json"
            activity shouldContainFile "${ctx.id};page=1.nq"

            activity shouldContainFile "first.activity.json"
            activity shouldContainFile "first.nq"
            activity shouldContainFile "last.activity.json"
            activity shouldContainFile "last.nq"
        }

        "should lock the directory" {
            val delta = testModel("1009") {
                prop(AS.NAME, "test")
            }

            val ctx = TestContext(CtxProps(iri = createIRI("http://id.openraadsinformatie.nl/1009")))
            val docSet = DocumentSet(ctx, delta.toModel())
            val docSetInterfere = DocumentSet(ctx, delta.toModel())

            docSet.lock.lock()

            docSet.lock shouldNotBe docSetInterfere.lock
        }
    }
})
