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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.ontola.ori.api.context

import com.google.common.base.Splitter
import io.ontola.ori.api.ORIContext
import io.ontola.ori.api.getDigester
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.eclipse.rdf4j.model.IRI
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

/** To keep track of the state while processing a record into a file on disk. */
class DocumentCtx(override val ctx: CtxProps) : ResourceCtx<DocumentCtx>(ctx) {
    override val cmd: String? = ctx.cmd

    override val iri: IRI? = ctx.iri

    override val record: ConsumerRecord<String, String>? = ctx.record

    override val version: String? = ctx.version

    override val id: String? = ctx.iri?.stringValue()?.substring(ctx.iri.stringValue().lastIndexOf('/') + 1)

    override fun copy(
        cmd: String?,
        record: ConsumerRecord<String, String>?,
        iri: IRI?,
        version: String?
    ): DocumentCtx {
        return DocumentCtx(
            ctx.copy(
                cmd = cmd,
                record = record,
                iri = iri,
                version = version
            )
        )
    }

    /** The directory of the document. Points to the container folder or a version if set */
    override fun dir(): File {
        var filePath = "${ORIContext.getCtx().config.getProperty("ori.api.dataDir")}/${dirPath()}"
        if (ctx.version != null) {
            filePath += "/${ctx.version}"
        }
        return File(filePath)
    }

    private fun dirPath(): String {
        val md5sum = digester.digest(id?.toByteArray())
        val hashedId = String.format("%032x", BigInteger(1, md5sum))

        return Splitter.fixedLength(16).split(hashedId).joinToString("/")
    }

    companion object {
        private val digester: MessageDigest = getDigester()
    }
}
