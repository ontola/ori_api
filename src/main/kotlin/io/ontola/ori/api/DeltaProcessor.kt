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

import java.io.*
import java.util.*
import kotlinx.coroutines.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.eclipse.rdf4j.model.BNode
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import org.eclipse.rdf4j.rio.*
import org.eclipse.rdf4j.rio.helpers.StatementCollector
import java.util.ArrayList

class DeltaProcessor(
    private val record: ConsumerRecord<String, String>
) {
    private val config: Properties = ORIContext.getCtx().config

    fun process() {
        try {
            printlnWithThread("[start][orid:${record.timestamp()}] Processing message")
            val baseDocument = config.getProperty("ori.api.baseIRI")
            val rdfParser = Rio.createParser(RDFFormat.NQUADS)
            val deltaEvent = LinkedHashModel()
            rdfParser.setRDFHandler(StatementCollector(deltaEvent))

            try {
                StringReader(record.value()).use {
                    rdfParser.parse(it, baseDocument)
                    runBlocking {
                        for (delta in partitionDelta(deltaEvent)) {
                            launch {
                                delta.process()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                printlnWithThread("Exception while parsing delta event: '%s'\n", e.toString())
                e.printStackTrace()
            }

            printlnWithThread("[end][orid:%s] Done with message\n", record.timestamp())
        } catch (e: Exception) {
            printlnWithThread("Exception while processing delta event: '%s'\n", e.toString())
            e.printStackTrace()
        }
    }

    private fun printlnWithThread(message: String, vararg opts: Any?) {
        val msg = String.format(message, *opts)
        val separator = if (msg.startsWith("[")) "" else " "
        val template = "[%s]$separator%s\n"

        System.out.printf(template, Thread.currentThread().name, msg)
    }

    /**
     * Partitions a delta into separate models for processing.
     */
    private fun partitionDelta(deltaEvent: Model): MutableCollection<DeltaEvent> {
        val partitions = HashMap<Resource, List<Statement>>()

        // TODO: implement an RDFHandler which does this while parsing
        for (s: Statement in deltaEvent) {
            if (!s.context?.toString().equals(config.getProperty("ori.api.supplantIRI"))) {
                this.printlnWithThread("Expected supplant statement, got %s", s.context)
                continue
            }

            val stmtList = partitions[s.subject]
            if (!stmtList.isNullOrEmpty()) {
                partitions[s.subject] = stmtList.plus(s)
            } else {
                partitions[s.subject] = listOf(s)
            }
        }

        val forest = HashMap<String, DeltaEvent>()
        while (partitions.isNotEmpty()) {
            val removals = ArrayList<Resource>()
            for ((key, value) in partitions) {
                if (key is BNode) {
                    val delta = forest.values.find { event -> event.anyObject(key) }
                    if (delta != null) {
                        value.forEach { stmt -> delta.deltaAdd(stmt) }
                        removals.add(key)
                    }
                } else if (!forest.containsKey(key.stringValue())) {
                    val store = DeltaEvent(key.stringValue())
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
