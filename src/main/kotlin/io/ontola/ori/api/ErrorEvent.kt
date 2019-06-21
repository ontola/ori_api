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

import org.apache.kafka.clients.producer.ProducerRecord
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ErrorEvent(private val docCtx: DocumentCtx, private val e: Exception) :
    Event(EventType.ERROR, docCtx.iri, null, null) {

    override fun toRecord(): ProducerRecord<String, String> {
        val error = ProducerRecord<String, String>(
            ORIContext.getCtx().config.getProperty("ori.api.kafka.errorTopic"),
            "RecordException",
            e.message
        )
        if (docCtx.cmd != null) {
            error.headers().add("cmd", docCtx.cmd.toByteArray())
        }

        if (docCtx.record != null) {
            val record = docCtx.record
            val recordKey = "tpt-${record.topic()}-${record.partition()}-${record.timestamp()}"
            error.headers().add("recordKey", recordKey.toByteArray())
        }

        if (docCtx.iri != null) {
            error.headers().add("iri", docCtx.iri.stringValue().toByteArray())
        }

        if (docCtx.version != null) {
            error.headers().add("version", docCtx.version.toByteArray())
        }

        val stackTrace = ByteArrayOutputStream()
        PrintStream(stackTrace).use {
            e.printStackTrace(it)
        }
        error.headers().add("stackTrace", stackTrace.toByteArray())

        return error
    }
}
