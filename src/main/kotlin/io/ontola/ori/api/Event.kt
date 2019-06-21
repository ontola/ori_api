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

import com.github.jsonldjava.core.RDFDataset
import io.ontola.rdfUtils.createIRI
import org.apache.kafka.clients.producer.ProducerRecord
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Resource
import java.nio.charset.Charset
import java.util.*

class InvalidEventException(message: String) : Exception(message)

open class Event(val type: EventType, open val iri: Resource?, val org: IRI?, open val data: Model?) {
    companion object {
        private val config: Properties = ORIContext.getCtx().config
        private val deltaTopic = config.getProperty("ori.api.kafka.topic")
        private val errorTopic = config.getProperty("ori.api.kafka.errorTopic")
        private val updateTopic = config.getProperty("ori.api.kafka.updateTopic")
        private val orgPredicate = RDFDataset.IRI("http://www.w3.org/2006/vcard/ns#hasOrganizationName").toString()

        fun parseRecord(docCtx: DocumentCtx): Event? {
            val record = docCtx.record!!

            return when (record.topic()) {
                deltaTopic -> {
                    val event = ORio.parseToModel(record.value())

                    return DeltaEvent(docCtx, event)
                }
                errorTopic -> {
                    TODO()
                }
                updateTopic -> {
                    val org = record.headers().lastHeader(orgPredicate).value().toString(Charset.defaultCharset())

                    return Event(EventType.UPDATE, createIRI(record.value()), createIRI(org), null)
                }
                else -> null
            }
        }
    }

    open fun process() {
        TODO("This is currently left to the subclasses")
    }

    open fun toRecord(): ProducerRecord<String, String> {
        val record = ProducerRecord<String, String>(
            ORIContext.getCtx().config.getProperty("ori.api.kafka.updateTopic"),
            type.name.toLowerCase(),
            iri?.stringValue()
        )
        if (org != null) {
            record.headers().add(
                RDFDataset.IRI("http://www.w3.org/2006/vcard/ns#hasOrganizationName").toString(),
                org.toString().toByteArray()
            )
        }

        return record
    }
}
