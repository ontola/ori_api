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

import io.ontola.ori.api.context.ResourceCtx
import io.ontola.rdfUtils.createIRI
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.model.vocabulary.VCARD4
import java.nio.charset.StandardCharsets
import java.util.*

class InvalidEventException(message: String) : Exception(message)

open class Event(
    val type: EventType,
    open val iri: Resource?,
    val org: IRI?,
    open val data: Model?
) {
    companion object {
        private val config: Properties = ORIContext.getCtx().config
        private val deltaTopic = config.getProperty("ori.api.kafka.topic") ?: throw Exception("Delta topic not set")
        private val errorTopic =
            config.getProperty("ori.api.kafka.errorTopic") ?: throw Exception("Error topic not set")
        private val updateTopic =
            config.getProperty("ori.api.kafka.updateTopic") ?: throw Exception("Update topic not set")
        private val orgPredicate = VCARD4.HAS_ORGANIZATION_NAME.stringValue()

        fun parseRecord(docCtx: ResourceCtx<*>): Event? {
            val record = docCtx.record

            return when (record?.topic()) {
                deltaTopic -> {
                    val event = ORio.parseToModel(record.value())

                    return DeltaEvent(docCtx, event)
                }
                errorTopic -> {
                    TODO()
                }
                updateTopic -> parseUpdate(record)
                else -> null
            }
        }

        fun parseUpdate(record: ConsumerRecord<String, String>): Event {
            val org = record
                .headers()
                .lastHeader(orgPredicate)
                .value()
                .toString(StandardCharsets.UTF_8)

            return Event(EventType.UPDATE, createIRI(record.value()), createIRI(org), null)
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
                VCARD4.HAS_ORGANIZATION_NAME.stringValue(),
                org.toString().toByteArray()
            )
        }
        this.data
            ?.find { s -> s.subject == iri && s.predicate == RDF.TYPE }
            ?.let {
                record.headers().add(
                    RDF.TYPE.stringValue(),
                    it.`object`.toString().toByteArray()
                )
            }

        return record
    }
}
