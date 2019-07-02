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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.rdf4j.model.*
import org.eclipse.rdf4j.model.impl.LinkedHashModel
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
                for (delta in partition()) {
                    launch {
                        delta.process()
                    }
                }
            }
        } catch (e: Exception) {
            EventBus.getBus().publishError(docCtx, e)
            printlnWithThread("Exception while parsing delta event: '%s'\n", e.toString())
            e.printStackTrace()
        }
    }

    private fun printlnWithThread(message: String, vararg opts: Any?) {
        val msg = String.format(message, *opts)
        val separator = if (msg.startsWith("[")) "" else " "
        val template = "[%s]$separator%s\n"

        System.out.printf(template, Thread.currentThread().name, msg)
    }

    /** Partitions a delta into separately processable slices. */
    internal fun partition(): Collection<DocumentSet> {
        val subjectBuckets = this.splitBySubject(data)
        val forest = partitionPerDocument(subjectBuckets)

        return forest.values
    }

    private fun splitBySubject(model: Model): PartitionMap {
        val partitions = PartitionMap()

        // TODO: implement an RDFHandler which does this while parsing
        for (s: Statement in model) {
            if (!s.context?.toString().equals(config.getProperty("ori.api.supplantIRI"))) {
                printlnWithThread("Expected supplant statement, got %s", s.context)
                continue
            }

            val stmtList = partitions[s.subject]
            if (!stmtList.isNullOrEmpty()) {
                partitions[s.subject] = stmtList.plus(s)
            } else {
                partitions[s.subject] = listOf(s)
            }
        }

        return partitions
    }

    /** Organizes anonymous resources into the bucket which refers to them */
    private fun partitionPerDocument(buckets: PartitionMap): Map<IRI, DocumentSet> {
        val bNodeForestReferences = HashMap<BNode, IRI>()

        val forests = buckets
            .filterKeys { key -> key is IRI }
            .map { bucket ->
                val iri = bucket.key as IRI
                val docSet = DocumentSet(docCtx.copy(iri = iri))
                bucket.value.forEach { statement ->
                    val obj = statement.`object`
                    if (obj is BNode) bNodeForestReferences[obj] = statement.subject as IRI

                    docSet.deltaAdd(statement)
                }

                iri to docSet
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
}
