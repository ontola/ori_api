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

import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.should
import io.kotlintest.specs.StringSpec
import io.ontola.activitystreams.vocabulary.AS
import io.ontola.rdf4j.model
import io.ontola.rdf4j.prop
import io.ontola.testhelpers.emptyContext
import io.ontola.testhelpers.testModel
import org.eclipse.rdf4j.model.impl.LinkedHashModel

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
