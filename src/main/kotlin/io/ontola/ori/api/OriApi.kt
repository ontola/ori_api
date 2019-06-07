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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.ontola.ori.api

import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.PartitionInfo
import java.io.File
import java.lang.Exception
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.stream.Collectors

/**
 * Listens to kafka streams and processes delta streams into the file system.
 *
 * TODO: Add activity streams to allow viewing which resources have changed
 * TODO: Add error handling service
 */
fun main() {
    val config = initConfig()
    printInitMessage(config)

    ensureOutputFolder(config)

    val threadCount = Integer.parseInt(System.getenv("THREAD_COUNT") ?: "1")
    val s = Executors.newFixedThreadPool(threadCount)
    val tasks = ArrayList<Future<*>>()

    var interrupted = false
    Runtime.getRuntime().addShutdownHook(Thread {
        s.shutdown()
        tasks.forEach { t -> t.cancel(true) }
        interrupted = true
    })

    try {
        val consumer = oriDeltaSubscriber(config)

        while (!interrupted) {
            val records: ConsumerRecords<String, String> = consumer.poll(Duration.ofMillis(100))
            for (record in records) {
                val p = DeltaProcessor(record, config)
                tasks.add(s.submit(p))
            }
        }
    } catch (e: Exception) {
        System.out.println("Fatal error occurred: ${e.message}")
        e.printStackTrace()
        System.exit(1)
    }
}

fun ensureOutputFolder(settings: Properties) {
    val baseDirectory = File(settings.getProperty("ori.api.dataDir"))
    if (!baseDirectory.exists()) {
        baseDirectory.mkdirs()
    }
}

fun printInitMessage(p: Properties) {
    System.out.println("================================================")
    System.out.printf("Starting ORI API\n\n")
    val keys = p.keys()
    while (keys.hasMoreElements()) {
        val key = keys.nextElement() as String
        val value = p.get(key)
        System.out.println(key.substring("ori.api.".length) + ": " + value)
    }
    System.out.println("================================================")
}

fun initConfig(): Properties {
    val config = Properties()

    config.setProperty(
        "ori.api.dataDir",
        (System.getenv("DATA_DIR") ?: "${System.getProperty("java.io.tmpdir")}/id")
    )
    config.setProperty(
        "ori.api.baseIRI",
        (System.getenv("BASE_IRI") ?: "https://id.openraadsinformatie.nl")
    )
    config.setProperty(
        "ori.api.supplantIRI",
        (System.getenv("SUPPLANT_IRI") ?: "http://purl.org/link-lib/supplant")
    )
    config.setProperty(
        "ori.api.kafka.clusterApiKey",
        (System.getenv("KAFKA_USERNAME") ?: "")
    )
    config.setProperty(
        "ori.api.kafka.clusterApiSecret",
        (System.getenv("KAFKA_PASSWORD") ?: "")
    )
    config.setProperty(
        "ori.api.kafka.group_id",
        (System.getenv("KAFKA_GROUP_ID") ?: "ori_api")
    )
    config.setProperty(
        "ori.api.kafka.hostname",
        (System.getenv("KAFKA_HOSTNAME") ?: "localhost")
    )
    config.setProperty(
        "ori.api.kafka.port",
        (System.getenv("KAFKA_PORT") ?: "9092")
    )
    config.setProperty(
        "ori.api.kafka.address",
        (System.getenv("KAFKA_ADDRESS") ?: "")
    )
    config.setProperty(
        "ori.api.kafka.topic",
        (System.getenv("DELTA_TOPIC") ?: "ori-delta")
    )
    val hostname = config.getProperty("ori.api.kafka.hostname")
    val port = config.getProperty("ori.api.kafka.port")
    var address = config.getProperty("ori.api.kafka.address")
    if (address == null || address.isEmpty()) {
        address = "$hostname:$port"
    }
    config.setProperty("ori.api.kafka.address", address)

    return config
}

fun oriDeltaSubscriber(config: Properties): KafkaConsumer<String, String> {
    val kafkaOpts = Properties()
    kafkaOpts.setProperty("bootstrap.servers", config.getProperty("ori.api.kafka.address"))
    kafkaOpts.setProperty("group.id", config.getProperty("ori.api.kafka.group_id"))
    kafkaOpts.setProperty("enable.auto.commit", "true")
    kafkaOpts.setProperty("auto.commit.interval.ms", "1000")
    kafkaOpts.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaOpts.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
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

    System.out.printf(
        "Connecting to kafka on '%s' with group '%s' and topic '%s' \n",
        kafkaOpts.getProperty("bootstrap.servers"),
        kafkaOpts.getProperty("group.id"),
        topic
    )

    try {
        val consumer = KafkaConsumer<String, String>(kafkaOpts)
        consumer.subscribe(Arrays.asList(topic))

        val partitionList = consumer
            .partitionsFor(topic)
            .stream()
            .map { t: PartitionInfo -> Integer.toString(t.partition()) }
            .collect(Collectors.joining(","))

        System.out.printf("Subscribed to topic '%s' with partitions '%s'", topic, partitionList)

        return consumer
    } catch (e: KafkaException) {
        val c: Throwable? = e.cause
        val message = (c ?: e).message
        throw Exception("[FATAL] Error while creating subscriber: $message\n", e)
    }
}

/**
 * Hard check for an MD5 digester.
 * No fallback is used since that could cause inconsistent results when multiple hash methods are mixed.
 */
fun getDigester(): MessageDigest {
    var digester: MessageDigest? = null
    try {
        digester = MessageDigest.getInstance("MD5")
    } catch (e: NoSuchAlgorithmException) {
        System.out.println("[FATAL] No MD5 MessageDigest algorithm support, exiting")
        System.exit(1)
    }

    return digester as MessageDigest
}
