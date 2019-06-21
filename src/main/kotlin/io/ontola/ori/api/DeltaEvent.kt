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

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.rdf4j.model.*
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import java.util.*

class DeltaEvent(
    private val docCtx: DocumentCtx,
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
    private fun partition(): MutableCollection<DocumentSet> {
        val partitions = HashMap<Resource, List<Statement>>()
        // TODO: implement an RDFHandler which does this while parsing
        for (s: Statement in data) {
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

        val forest = HashMap<String, DocumentSet>()
        while (partitions.isNotEmpty()) {
            val removals = ArrayList<Resource>()
            for ((key, value) in partitions) {
                if (key is BNode) {
                    val delta = forest.values.find { event -> event.anyObject(key) }
                    if (delta == null) {
                        removals.add(key)
                        val danglingResource = LinkedHashModel(data.filter { s -> s.subject == key })
                        EventBus.getBus().publishError("dangling-resource", danglingResource, null)
                        continue
                    }
                    value.forEach { stmt -> delta.deltaAdd(stmt) }
                    removals.add(key)
                } else if (!forest.containsKey(key.stringValue())) {
                    val store = DocumentSet(docCtx.copy(iri = key as IRI))
                    for (statement in value) {
                        if (statement.`object` is BNode) {
                            val nodeData = statement.getObject()
                            if (nodeData != null) {
                                partitions[nodeData]!!.forEach { bStmt -> store.deltaAdd(bStmt) }
                            }
                        }
                        store.deltaAdd(statement)
                    }
                    forest[key.stringValue()] = store
                    removals.add(key)
                }
            }
            removals.forEach { r -> partitions.remove(r) }
        }

        return forest.values
    }
}
