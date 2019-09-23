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

import org.redisson.config.Config
import java.util.*

fun initConfig(): Properties {
    val config = Properties()

    config.setProperty(
        "ori.api.dataDir",
        (System.getenv("DATA_DIR") ?: "${System.getProperty("java.io.tmpdir")}/ori/id")
    )
    config.setProperty(
        "ori.api.apiDir",
        (System.getenv("API_DIR") ?: "${System.getProperty("java.io.tmpdir")}/ori/api/v1")
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
        "ori.api.redis.address",
        (System.getenv("REDIS_ADDRESS") ?: "")
    )
    config.setProperty(
        "ori.api.redis.hostname",
        (System.getenv("REDIS_HOSTNAME") ?: "localhost")
    )
    config.setProperty(
        "ori.api.redis.port",
        (System.getenv("REDIS_PORT") ?: "6379")
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
    config.setProperty(
        "ori.api.kafka.updateTopic",
        (System.getenv("API_UPDATE_TOPIC") ?: "ori-api-update")
    )
    config.setProperty(
        "ori.api.kafka.errorTopic",
        (System.getenv("API_ERROR_TOPIC") ?: "ori-api-error")
    )
    config.setProperty(
        "ori.api.threadCount",
        System.getenv("THREAD_COUNT") ?: "4"
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

fun initKafkaConfig(config: Properties): Properties {
    val kafkaOpts = Properties()

    kafkaOpts.setProperty("bootstrap.servers", config.getProperty("ori.api.kafka.address"))
    kafkaOpts.setProperty("group.id", config.getProperty("ori.api.kafka.group_id"))
    kafkaOpts.setProperty("max.poll.records", "1")
    kafkaOpts.setProperty("session.timeout.ms", "60000")
    kafkaOpts.setProperty("enable.auto.commit", "true")
    kafkaOpts.setProperty("auto.commit.interval.ms", "1000")
    kafkaOpts.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaOpts.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaOpts.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    kafkaOpts.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    kafkaOpts.setProperty("request.timeout.ms", "20000")
    kafkaOpts.setProperty("retry.backoff.ms", "500")

    val clusterApiKey = config.getProperty("ori.api.kafka.clusterApiKey")
    val clusterApiSecret = config.getProperty("ori.api.kafka.clusterApiSecret")
    if (clusterApiKey == null || "".equals(clusterApiKey) || clusterApiSecret == null || "".equals(clusterApiSecret)) {
        println("Either cluster API key or secret was left blank, skipping SASL authentication")
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

    return kafkaOpts
}

fun initRedisConfig(config: Properties): Config {
    val redisOpts = Config()
    val address =
        if (config.getProperty("ori.api.redis.address").isNotEmpty()) {
            config.getProperty("ori.api.redis.address")
        } else {
            val hostname = config.getProperty("ori.api.redis.hostname")
            val port = config.getProperty("ori.api.redis.port")
            "redis://$hostname:$port"
        }

    redisOpts.useSingleServer().apply {
        this.address = address
    }
    return redisOpts
}
