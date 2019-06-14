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

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.io.File
import java.lang.Exception
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.Duration
import java.util.*
import kotlin.system.exitProcess

/**
 * Listens to kafka streams and processes delta streams into the file system.
 *
 * TODO: Add activity streams to allow viewing which resources have changed
 * TODO: Add error handling service
 */
@ExperimentalCoroutinesApi
fun main(args: Array<String>) = runBlocking {
    val ctx = ORIContext.getCtx()

    printInitMessage(ctx.config)

    ensureOutputFolder(ctx.config)
    val threadCount = Integer.parseInt(ctx.config.getProperty("ori.api.threadCount"))

    if (args.isNotEmpty() && args[0] == "--clean-old-versions") {
        cleanOldVersionsAsync().await()
        exitProcess(0)
    }

    try {
        val consumer = EventBus.getBus().createSubscriber()
        if (args.isNotEmpty() && args[0] == "--from-beginning") {
            EventBus.getBus().resetTopicToBeginning(consumer)
        }

        val records = produceRecords(consumer)

        repeat(threadCount) { consumeRecords(records) }
    } catch (e: Exception) {
        println("Fatal error occurred: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}

@ExperimentalCoroutinesApi
fun CoroutineScope.produceRecords(consumer: KafkaConsumer<String, String>): ReceiveChannel<ConsumerRecord<String, String>> =
    produce {
        while (true) {
            for (record in consumer.poll(Duration.ofMillis(0))) {
                send(record)
                delay(100)
            }
        }
    }

fun CoroutineScope.consumeRecords(channel: ReceiveChannel<ConsumerRecord<String, String>>) = launch {
    for (record in channel) {
        launch { DeltaProcessor(record).process() }
    }
}

fun ensureOutputFolder(settings: Properties) {
    val baseDirectory = File(settings.getProperty("ori.api.dataDir"))
    if (!baseDirectory.exists()) {
        baseDirectory.mkdirs()
    }
}

fun printInitMessage(p: Properties) {
    println("================================================")
    println("Starting ORI API\n")
    val keys = p.keys()
    while (keys.hasMoreElements()) {
        val key = keys.nextElement() as String
        val value = p.get(key)
        println(key.substring("ori.api.".length) + ": " + value)
    }
    println("================================================")
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
        println("[FATAL] No MD5 MessageDigest algorithm support, exiting")
        System.exit(1)
    }

    return digester as MessageDigest
}
