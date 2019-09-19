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
import java.time.Duration
import kotlin.system.exitProcess

/**
 * Processes messages from the 'updates' channel. Updates are downstream from the delta's, containing aggregate
 * information.
 *
 * Due to the fact that we're working with a file system, it's of vital importance concurrency is managed properly.
 * Locking
 */
@ExperimentalCoroutinesApi
suspend fun processUpdates(): Job {
    return withContext(Dispatchers.Default) {
        try {
            val updateTopic = ORIContext.getCtx().config.getProperty("ori.api.kafka.updateTopic")
            val consumer = EventBus.getBus().createSubscriber(topic = updateTopic)

            val records = produceUpdates(consumer)

            return@withContext consumeUpdatesAsync(records)
        } catch (e: Exception) {
            println("Fatal error occurred: ${e.message}")
            e.printStackTrace()
            ORIContext.notify(e)
            exitProcess(1)
        }
    }
}

@ExperimentalCoroutinesApi
private fun CoroutineScope.produceUpdates(
    consumer: KafkaConsumer<String, String>
): ReceiveChannel<ConsumerRecord<String, String>> = produce {
    while (true) {
        for (record in consumer.poll(Duration.ofMillis(50))) {
            send(record)
        }
        delay(100)
    }
}

private fun CoroutineScope.consumeUpdatesAsync(
    channel: ReceiveChannel<ConsumerRecord<String, String>>
) = launch {
    for (record in channel) {
        supervisorScope {
            UpdateProcessor(record).process()
        }
    }
}
