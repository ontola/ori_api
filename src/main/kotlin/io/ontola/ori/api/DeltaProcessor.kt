/*
 * ORI API
 * Copyright (C) 2019, Argu BV
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

import java.io.*
import java.time.Duration
import java.util.*
import java.util.stream.Collectors

import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.PartitionInfo
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import org.eclipse.rdf4j.rio.*
import org.eclipse.rdf4j.rio.helpers.StatementCollector

class DeltaProcessor(private val config: Properties) : Runnable {
  companion object {
    private const val deserializerClass: String = "org.apache.kafka.common.serialization.StringDeserializer"
  }

  override fun run() {
    printlnWithThread("Started thread")
    val consumer = oriDeltaSubscriber()
    val baseDocument = config.getProperty("ori.api.baseIRI")

    val t = Thread.currentThread()

    while (!t.isInterrupted) {
      try {
        val records: ConsumerRecords<String, String> = consumer.poll(Duration.ofMillis(100))
        for (record in records) {
          try {
            this.printlnWithThread("[start][orid:%s] Processing message", record.timestamp())
            val rdfParser = Rio.createParser(RDFFormat.NQUADS)
            val deltaEvent = LinkedHashModel()
            rdfParser.setRDFHandler(StatementCollector(deltaEvent))

            try {
              StringReader(record.value()).use {
                rdfParser.parse(it, baseDocument)
                partitionDelta(deltaEvent)
                  .forEach(DeltaEvent::process)
              }
            } catch (e: Exception) {
              this.printlnWithThread("Exception while parsing delta event: '%s'\n", e.toString())
              e.printStackTrace()
            }

            this.printlnWithThread("[end][orid:%s] Done with message\n", record.timestamp())
          } catch (e: Exception) {
            this.printlnWithThread("Exception while processing delta event: '%s'\n", e.toString())
            e.printStackTrace()
          }
        }
      } catch (e: Exception) {
        this.printlnWithThread("Exception while listening for events: '%s'\n", e.toString())
        e.printStackTrace()
        t.interrupt()
      }
    }
  }

  private fun printlnWithThread(message: String, vararg opts: Any?) {
    val msg = String.format(message, *opts)
    val separator = if (msg.startsWith("[")) "" else " "
    val template = "[%s]$separator%s\n"

    System.out.printf(template, Thread.currentThread().name, msg)
  }

  private fun oriDeltaSubscriber(): KafkaConsumer<String, String> {
    val kafkaOpts = Properties()
    kafkaOpts.setProperty("bootstrap.servers", config.getProperty("ori.api.kafka.address"))
    kafkaOpts.setProperty("group.id", config.getProperty("ori.api.kafka.group_id"))
    kafkaOpts.setProperty("enable.auto.commit", "true")
    kafkaOpts.setProperty("auto.commit.interval.ms", "1000")
    kafkaOpts.setProperty("key.deserializer", deserializerClass)
    kafkaOpts.setProperty("value.deserializer", deserializerClass)
    kafkaOpts.setProperty("request.timeout.ms", "20000")
    kafkaOpts.setProperty("retry.backoff.ms", "500")

    val clusterApiKey = config.getProperty("ori.api.kafka.clusterApiKey")
    val clusterApiSecret = config.getProperty("ori.api.kafka.clusterApiSecret")
    if (clusterApiKey == null || "".equals(clusterApiKey) || clusterApiSecret == null || "".equals(clusterApiSecret)) {
      System.out.println("Either cluster API key or secret was left blank, skipping SASL authentication")
    } else {
      kafkaOpts.setProperty("ssl.endpoint.identification.algorithm", "https")
      kafkaOpts.setProperty("sasl.mechanism", "PLAIN")
      val jaasConfig = String.format(
          "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
          clusterApiKey,
          clusterApiSecret
      )
      kafkaOpts.setProperty("sasl.jaas.config", jaasConfig)
      kafkaOpts.setProperty("security.protocol", "SASL_SSL")
    }

    val topic = config.getProperty("ori.api.kafka.topic", "ori-delta")

    printlnWithThread("Connecting to kafka on '%s' with group '%s' and topic '%s' \n",
        kafkaOpts.get("bootstrap.servers"),
        kafkaOpts.get("group.id"),
        topic)

    try {
      val consumer = KafkaConsumer<String, String>(kafkaOpts)
      consumer.subscribe(Arrays.asList(topic))

      val partitionList = consumer
          .partitionsFor(topic)
          .stream()
          .map { t: PartitionInfo -> Integer.toString(t.partition()) }
          .collect(Collectors.joining("," ))

      this.printlnWithThread("Subscribed to topic '%s' with partitions '%s'", topic, partitionList)

      return consumer
    } catch (e: KafkaException) {
      val c: Throwable? = e.cause
      val message = (c ?: e).message
      this.printlnWithThread(String.format("[FATAL] Error while creating subscriber: %s", message))
      Thread.currentThread().interrupt()

      return null as KafkaConsumer<String, String>
    }
  }

  /**
   * Partitions a delta into separate models for processing.
   */
  private fun partitionDelta(deltaEvent: Model): List<DeltaEvent> {
    val parts = ArrayList<DeltaEvent>()
    // TODO: implement an RDFHandler which does this while parsing
    for (s: Statement in deltaEvent) {
      if (!s.context.toString().equals(config.getProperty("ori.api.supplantIRI"))) {
        this.printlnWithThread("Expected supplant statement, got %s", s.context)
        continue
      }
      val target = DeltaEvent(s.getSubject().toString(), config)
      if (!parts.contains(target)) {
        parts.add(target)
      }
      val index = parts.indexOf(target)
      if (index == -1) {
        throw Error(String.format("Index '%s' out of bounds for '%s' with '%s' items (%s)",
          index,
          s.subject.toString(),
          parts.size,
          parts[0].iri))
      }
      parts.get(index).deltaAdd(s.getSubject(), s.getPredicate(), s.getObject())
    }

    return parts
  }
}
