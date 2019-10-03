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

package io.ontola.testhelpers

import io.ontola.ori.api.ORio
import io.ontola.ori.api.context.CtxProps
import io.ontola.ori.api.context.DocumentCtx
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.eclipse.rdf4j.model.Model

fun emptyContext(): DocumentCtx {
    return DocumentCtx(CtxProps())
}

fun nqResourceToModel(testResource: String): Model {
    val body = testResource.asResource()
    return ORio.parseToModel(body)
}

fun deltaDocument(testResource: String): DocumentCtx {
    return DocumentCtx(
        CtxProps(
            record = ConsumerRecord(
                "ori-delta",
                0,
                0,
                "update",
                testResource.asResource()
            )
        )
    )
}
