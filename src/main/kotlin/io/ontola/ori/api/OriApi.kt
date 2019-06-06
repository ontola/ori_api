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

import java.io.File
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.ArrayList
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.Future

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

    Runtime.getRuntime().addShutdownHook(Thread {
        s.shutdown()
        tasks.forEach { t -> t.cancel(true) }
    })

    val p = DeltaProcessor(config)
    tasks.add(s.submit(p))
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
            (System.getenv("CLUSTER_API_KEY") ?: "")
    )
    config.setProperty(
            "ori.api.kafka.clusterApiSecret",
            (System.getenv("CLUSTER_API_SECRET") ?: "")
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
