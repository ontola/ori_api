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

import com.github.jsonldjava.core.RDFDataset
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.TopicPartition
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.rio.RDFFormat
import java.io.StringWriter
import java.lang.Exception
import java.time.Duration
import java.util.*
import java.util.concurrent.Future
import java.util.stream.Collectors

/**
 * Wrapper to manage reading and writing bus data.
 *
 * TODO: Add an interface with `poll` and `send` so kafka can be swapped out.
 */
class EventBus {
    companion object {
        private val eventBus = EventBus()

        fun getBus(): EventBus {
            return eventBus
        }
    }

    private val ctx = ORIContext.getCtx()
    private val apiUpdateProducer = KafkaProducer<String, String>(ctx.kafkaOpts)

    internal fun createSubscriber(waitForConnection: Boolean = false): KafkaConsumer<String, String> {
        val topic = ctx.config.getProperty("ori.api.kafka.topic", "ori-delta")

        System.out.printf(
            "Connecting to kafka on '%s' with group '%s' and topic '%s' \n",
            ctx.kafkaOpts.getProperty("bootstrap.servers"),
            ctx.kafkaOpts.getProperty("group.id"),
            topic
        )

        try {
            val consumer = KafkaConsumer<String, String>(ctx.kafkaOpts)
            consumer.subscribe(Arrays.asList(topic))

            val partitionList = consumer
                .partitionsFor(topic)
                .stream()
                .map { t: PartitionInfo -> Integer.toString(t.partition()) }
                .collect(Collectors.joining(","))

            if (waitForConnection) {
                while (consumer.assignment().size == 0) {
                    consumer.poll(Duration.ofMillis(100))
                }
            }

            println("Subscribed to topic '$topic' with partitions '$partitionList'")


            return consumer
        } catch (e: KafkaException) {
            val c: Throwable? = e.cause
            val message = (c ?: e).message
            throw Exception("[FATAL] Error while creating subscriber: $message\n", e)
        }
    }

    internal fun publishEvent(event: Event): Future<RecordMetadata> {
        return apiUpdateProducer.send(event.toRecord())
    }

    internal fun publishError(type: String, value: Model, org: IRI?): Future<RecordMetadata> {
        val payload = StringWriter()
        val writer = ORio.createWriter(RDFFormat.NQUADS, payload)
        writer.handleSingleModel(value)

        val record = ProducerRecord<String, String>(
            ctx.config.getProperty("ori.api.kafka.errorTopic"),
            type,
            payload.toString()
        )
        if (org != null) {
            record.headers().add(
                RDFDataset.IRI("http://www.w3.org/2006/vcard/ns#hasOrganizationName").toString(),
                org.toString().toByteArray()
            )
        }

        return apiUpdateProducer.send(record)
    }

    internal fun resetTopicToBeginning(
        consumer: KafkaConsumer<String, String>,
        topic: String = ctx.config.getProperty("ori.api.kafka.topic", "ori-delta")
    ) {
        val parts = consumer
            .partitionsFor(topic)
            .map { t -> TopicPartition(t.topic(), t.partition()) }
        consumer.seekToBeginning(parts)
        consumer.commitSync()
    }
}
