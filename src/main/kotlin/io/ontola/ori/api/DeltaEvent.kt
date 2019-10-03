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
import io.ontola.rdfUtils.getQueryParameter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.rdf4j.model.*
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import java.net.URI
import java.util.*

private typealias PartitionMap = HashMap<Resource, List<Statement>>
private typealias PartitionEntry = Map.Entry<Resource, List<Statement>>

class DeltaEvent(
    private val docCtx: ResourceCtx<*>,
    override val data: Model = LinkedHashModel()
) : Event(EventType.DELTA, null, null, data) {
    private val config: Properties = ORIContext.getCtx().config

    override fun process() {
        try {
            runBlocking {
                val partitions = partition()
                printlnWithThread("Processing delta event with %s partitions \n", partitions.size)
                for ((iri, delta) in partitions) {
                    launch {
                        try {
                            delta.process()
                        } catch(e: Exception) {
                            printlnWithThread("Exception while processing partition %s: '%s'\n", iri, e.toString())
                            EventBus.getBus().publishError(docCtx, e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            printlnWithThread("Exception while parsing delta event: '%s'\n", e.toString())
            EventBus.getBus().publishError(docCtx, e)
        }
    }

    private fun printlnWithThread(message: String, vararg opts: Any?) {
        val msg = String.format(message, *opts)
        val separator = if (msg.startsWith("[")) "" else " "
        val template = "[%s]$separator%s\n"

        System.out.printf(template, Thread.currentThread().name, msg)
    }

    /** Partitions a delta into separately processable slices. */
    internal fun partition(): Map<IRI, DocumentSet> {
        val subjectBuckets = this.splitByDocument(data)
        return partitionPerDocument(subjectBuckets)
    }

    /**
     * Partitions a model by target graph / base document. Any fragment in the IRI will be trimmed, for it cannot be
     * queried from the web as a document.
     *
     * The context's `graph` query parameter takes precedence over the subject.
     */
    private fun splitByDocument(model: Model): PartitionMap {
        val partitions = PartitionMap()

        // TODO: implement an RDFHandler which does this while parsing
        for (s: Statement in model) {
            val doc = this.doc(s.context?.let { getQueryParameter(s.context, "graph") } ?: s.subject)
            val stmtList = partitions[doc]
            if (!stmtList.isNullOrEmpty()) {
                partitions[doc] = stmtList.plus(s)
            } else {
                partitions[doc] = listOf(s)
            }
        }

        return partitions
    }

    /** Organizes anonymous resources into the bucket which refers to them */
    private fun partitionPerDocument(buckets: PartitionMap): Map<IRI, DocumentSet> {
        val bNodeForestReferences = HashMap<BNode, Resource>()
        val forests = buckets
            .filterKeys { key -> key is IRI }
            .map { bucket ->
                val iri = bucket.key as IRI
                val model = LinkedHashModel()
                bucket.value.forEach { statement ->
                    val obj = statement.`object`
                    if (obj is BNode) {
                        bNodeForestReferences[obj] = statement.subject
                    }

                    model.add(statement)
                }

                iri to DocumentSet(docCtx.copy(iri = iri), model)
            }
            .toMap()

        buckets
            .filterKeys { key -> key is BNode }
            .forEach { bucket ->
                val homeForest = bNodeForestReferences[bucket.key]
                if (homeForest == null) {
                    handleDanglingNode(bucket)
                } else {
                    forests[homeForest]!!.addAll(bucket.value)
                }
            }

        return forests
    }

    private fun handleDanglingNode(bucket: PartitionEntry) {
        val danglingResource = LinkedHashModel(data.filter { s -> s.subject == bucket.key })
        EventBus.getBus().publishError("dangling-resource", danglingResource, null)
    }

    private fun doc(subject: Resource): Resource {
        if (subject is BNode) {
            return subject
        }

        val subj = URI(subject.stringValue())

        return createIRI(URI(
            subj.scheme,
            subj.userInfo,
            subj.host,
            subj.port,
            subj.path,
            subj.query,
            null
        ).toString())
    }
}
