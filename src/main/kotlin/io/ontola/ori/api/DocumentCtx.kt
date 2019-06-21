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

package io.ontola.ori.api

import com.google.common.base.Splitter
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.eclipse.rdf4j.model.IRI
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

/** To keep track of the state while processing a record into a file on disk. */
data class DocumentCtx(
    val cmd: String?,
    val record: ConsumerRecord<String, String>? = null,
    val iri: IRI? = null,
    val version: String? = null
) {
    companion object {
        private val digester: MessageDigest = getDigester()
    }

    /** The directory of the document. Points to the container folder or a version if set */
    fun dir(): File {
        var filePath = "${ORIContext.getCtx().config.getProperty("ori.api.dataDir")}/${dirPath()}"
        if (version != null) {
            filePath += "/$version"
        }
        return File(filePath)
    }

    fun id(): String {
        if (iri == null) {
            throw Exception("'id' called before an iri was set")
        }
        return iri.stringValue().substring(iri.stringValue().lastIndexOf('/') + 1)
    }

    private fun dirPath(): String {
        val md5sum = digester.digest(id().toByteArray())
        val hashedId = String.format("%032x", BigInteger(1, md5sum))

        return Splitter.fixedLength(4).split(hashedId).joinToString("/")
    }
}
