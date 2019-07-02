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

package io.ontola.ori.api.context

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.eclipse.rdf4j.model.IRI
import java.io.File

data class CtxProps(
    val cmd: String? = null,
    val record: ConsumerRecord<String, String>? = null,
    val iri: IRI? = null,
    val version: String? = null
)

abstract class ResourceCtx<T : ResourceCtx<T>>(open val ctx: CtxProps = CtxProps()) {
    abstract val cmd: String?

    abstract val iri: IRI?

    abstract val record: ConsumerRecord<String, String>?

    abstract val version: String?

    abstract val id: String?

    abstract fun dir(): File

    abstract fun copy(
        cmd: String? = this.ctx.cmd,
        record: ConsumerRecord<String, String>? = this.ctx.record,
        iri: IRI? = this.ctx.iri,
        version: String? = this.ctx.version
    ): T
}
