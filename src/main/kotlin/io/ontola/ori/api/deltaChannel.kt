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

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.lang.Exception
import java.time.Duration
import kotlin.system.exitProcess

@ExperimentalCoroutinesApi
fun processDeltas(docCtx: DocumentCtx, fromBeginning: Boolean) = runBlocking {
    val ctx = ORIContext.getCtx()
    val threadCount = Integer.parseInt(ctx.config.getProperty("ori.api.threadCount"))
    try {
        val consumer = EventBus.getBus().createSubscriber(fromBeginning)
        if (fromBeginning) {
            EventBus.getBus().resetTopicToBeginning(consumer)
        }

        val records = produceDeltas(consumer)

        repeat(threadCount) { consumeDeltas(docCtx, records) }
    } catch (e: Exception) {
        println("Fatal error occurred: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}

@ExperimentalCoroutinesApi
private fun CoroutineScope.produceDeltas(consumer: KafkaConsumer<String, String>): ReceiveChannel<ConsumerRecord<String, String>> =
    produce {
        while (true) {
            for (record in consumer.poll(Duration.ofMillis(0))) {
                send(record)
                delay(100)
            }
        }
    }

private fun CoroutineScope.consumeDeltas(docCtx: DocumentCtx, channel: ReceiveChannel<ConsumerRecord<String, String>>) =
    launch {
        for (record in channel) {
            launch { DeltaProcessor(docCtx.copy(record = record)).process() }
        }
    }
