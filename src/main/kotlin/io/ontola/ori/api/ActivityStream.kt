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

import io.ontola.activitystreams.*
import io.ontola.activitystreams.Collection
import io.ontola.activitystreams.vocabulary.AS
import io.ontola.rdfUtils.createIRI
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.rio.RDFFormat
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.floor

class ActivityStream(private val docCtx: DocumentCtx) {
    /** Formats in which the Activity Stream is serialized */
    private val fileTypes = listOf(ORio.ACTIVITY_JSONLD, RDFFormat.NQUADS)

    /** Directory where the Activity Stream data is stored */
    private val asDirectory = File(docCtx.dir().path + "/activity")

    /** Main file where the entire streams' Collection is stored */
    private fun streamFile(fileType: RDFFormat? = null) =
        File("${asDirectory.path}/${docCtx.id()}${formatExtension(fileType)}")

    /** Symlink to resolve the stream via content-negotiation */
    private fun streamLink(fileType: RDFFormat? = null) =
        File("${docCtx.dir().path}/${docCtx.id()}${formatExtension(fileType)}")

    /** Symlink to the first page of the collection */
    private fun firstLink(fileType: RDFFormat? = null) =
        File("${asDirectory.path}/first${formatExtension(fileType)}")

    /** Symlink to the last page of the collection */
    private fun lastLink(fileType: RDFFormat? = null) =
        File("${asDirectory.path}/last${formatExtension(fileType)}")

    private val items = ArrayList<ASObject>()
    private val pageSize = 100

    init {
        ensure()
        load()
    }

    /** Add an event to the activity stream. */
    fun append(event: Event) {
        val eventActivity = Activity(
            type = createIRI(AS.NAMESPACE, event.type.name.toLowerCase().capitalize()),
            target = docCtx.iri,
            published = DocumentSet.versionStringFormat.parse(docCtx.version)
        )
        items.add(eventActivity)
    }

    /** Adds all activities in {streamDir} to this stream */
    fun load(streamDir: File = asDirectory) {
        findPages(streamDir, RDFFormat.NQUADS).forEachIndexed { i, page ->
            val model = ORio.parseToModel(page)
            val obj = modelToObject(model, pageIRI(i + 1))

            if (obj is CollectionPage) {
                items.addAll(obj.items ?: emptyList())
            }
        }
    }

    fun save() {
        storeStream()
        storePages()
        updateLinks()
    }

    private fun ensure(): ActivityStream {
        ensureDirectoryTree(asDirectory)

        return this
    }

    private fun findPages(dir: File = asDirectory, fileType: RDFFormat): Array<File> {
        val pageMatcher = Regex("${docCtx.id()}(;page=[0-9]+)${formatExtension(fileType)}")

        return dir
            .listFiles { file -> file.name.matches(pageMatcher) }
            .sortedArray()
    }

    private fun initPage(number: Int, itemCount: Int): Pair<Resource, Model> {
        val coll = CollectionPage(
            id = pageIRI(number),
            items = listOf(),
            totalItems = itemCount,
            partOf = iri(),
            first = iri(firstLink().relativeTo(asDirectory).toString()),
            last = iri(lastLink().relativeTo(asDirectory).toString())
        )
        if (number > 1) {
            coll.prev = pageIRI(number - 1)
        }
        val hasNext = floor(items.size / pageSize.toFloat()) > number
        if (hasNext) {
            coll.next = pageIRI(number + 1)
        }
        return objectToModel(coll)
    }

    private fun iri(suffix: String = ""): IRI {
        val iriStr = arrayOf("${docCtx.iri}/activity", suffix)
            .filterNot(String::isNullOrBlank)
            .joinToString("/")

        return createIRI(iriStr)
    }

    private fun page(number: Int): Path {
        return File("${asDirectory.path}/${pageName(number)}").toPath()
    }

    private fun pageIRI(number: Int): IRI {
        return iri(pageName(number))
    }

    private fun pageName(number: Int): String {
        return "${docCtx.id()};page=$number"
    }

    private fun store(basePath: Path, model: Model) {
        val asLDFile = File("$basePath.activity.json")
        if (asLDFile.exists()) {
            asLDFile.delete()
        }
        asLDFile.createNewFile()
        val writer = ORio.createWriter(RDFFormat.JSONLD, asLDFile.bufferedWriter())
        writer.setContext("https://www.w3.org/ns/activitystreams#")
        writer.startRDF()
        writer.handleModel(model)
        writer.endRDF()

        val nqFile = File("$basePath.nq")
        val nqWriter = ORio.createWriter(RDFFormat.NQUADS, nqFile.bufferedWriter())
        nqWriter.handleSingleModel(model)
    }

    private fun storePages() {
        items.chunked(pageSize).forEachIndexed { i, pageItems ->
            val number = i + 1
            val (pageIRI, pageModel) = initPage(number, pageItems.size)

            pageItems.forEach { item ->
                val (eventNode, eventModel) = objectToModel(item)
                pageModel += eventModel
                pageModel.add(
                    pageIRI,
                    AS.ITEMS,
                    eventNode
                )
            }

            store(page(number), pageModel)
        }
    }

    private fun storeStream() {
        val coll = Collection(
            id = iri(),
            totalItems = items.size,
            first = iri(firstLink().relativeTo(asDirectory).toString()),
            last = iri(lastLink().relativeTo(asDirectory).toString())
        )
        val (_, streamModel) = objectToModel(coll)
        store(streamFile().toPath(), streamModel)
    }

    private fun updateLinks() {
        for (fileType in fileTypes) {
            val pages = findPages(asDirectory, fileType)

            val firstPage = pages.firstOrNull()
            if (firstPage != null) {
                if (firstLink(fileType).exists()) {
                    firstLink(fileType).delete()
                }
                Files.createSymbolicLink(
                    firstLink(fileType).toPath(),
                    firstPage.relativeTo(asDirectory).toPath()
                )
            }

            val lastPage = pages.lastOrNull()
            if (lastPage != null) {
                if (lastLink(fileType).exists()) {
                    lastLink(fileType).delete()
                }
                Files.createSymbolicLink(
                    lastLink(fileType).toPath(),
                    lastPage.relativeTo(asDirectory).toPath()
                )
            }

        }
        if (!streamLink(ORio.ACTIVITY_JSONLD).exists()) {
            Files.createSymbolicLink(
                streamLink(ORio.ACTIVITY_JSONLD).toPath(),
                streamFile(ORio.ACTIVITY_JSONLD).relativeTo(docCtx.dir()).toPath()
            )
        }
    }
}
