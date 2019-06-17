package io.ontola.ori.api

import org.apache.kafka.clients.consumer.ConsumerRecord

/** To keep track of the state while processing a record into a file on disk. */
data class DocumentCtx(
    val cmd: String?,
    val record: ConsumerRecord<String, String>? = null,
    val iri: String? = null,
    val version: String? = null
)
