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

import io.ontola.ori.api.context.CtxProps
import io.ontola.ori.api.context.ResourceCtx
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.eclipse.rdf4j.model.IRI
import java.io.File

private fun getId(iri: IRI?): String {
    return iri?.stringValue()?.split("/")?.last() ?: ""
}

class TestContext(
    ctx: CtxProps,
    private val dir: File = createTempDir(getId(ctx.iri))
) : ResourceCtx<TestContext>(ctx) {
    override val cmd: String? = ctx.cmd

    override val id: String? = getId(ctx.iri)

    override val iri: IRI? = ctx.iri

    override val record: ConsumerRecord<String, String>? = ctx.record

    override val version: String? = ctx.version

    init {
        dir.deleteOnExit()
    }

    override fun copy(
        cmd: String?,
        record: ConsumerRecord<String, String>?,
        iri: IRI?,
        version: String?
    ): TestContext {
        return TestContext(
            ctx.copy(
                cmd = cmd,
                record = record,
                iri = iri,
                version = version
            ),
            dir()
        )
    }

    override fun dir(): File {
        var path = dir.path
        if (version != null) path += "/$version"
        return File(path)
    }
}
