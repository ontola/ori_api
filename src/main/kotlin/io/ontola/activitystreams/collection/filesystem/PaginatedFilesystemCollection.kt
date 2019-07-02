/*
 * ActivityStreams
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

package io.ontola.activitystreams.collection.filesystem

import io.ontola.activitystreams.*
import io.ontola.activitystreams.Collection
import io.ontola.activitystreams.vocabulary.AS
import io.ontola.ori.api.ORio
import io.ontola.ori.api.ensureDirectoryTree
import io.ontola.ori.api.formatExtension
import io.ontola.ori.api.recreateSymbolicLink
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.rio.RDFFormat
import java.io.File
import java.nio.file.Path
import kotlin.math.floor

inline fun <reified T : ASObject> PaginatedFilesystemCollection<T>.loadWithType() {
    if (!location.exists()) {
        return
    }

    findPages(RDFFormat.NQUADS).forEachIndexed { i, page ->
        val model = ORio.parseToModel(page)
        val obj = modelToObject<T>(model, pageIRI(i + 1))

        if (obj is CollectionPage<*>) {
            obj.items?.forEach { item ->
                items.add(item as T)
            }
        }
    }
}

/**
 * Implements the MutableList interface for the ActivityStreams paginated collection model
 */
class PaginatedFilesystemCollection<T : ASObject>(
    val tFactory: (index: Int) -> T,

    /**  */
    val generateIRI: (path: String?) -> IRI,

    /** Name of the collection, will be included in the file names */
    val name: String,

    /** Directory where the collection is stored */
    val location: File,

    private val pageSize: Int = 100,

    val items: MutableList<T> = MutableList(0, tFactory)
) : MutableList<T> by items {
    /** Formats in which the Activity Stream is serialized */
    private val fileTypes = listOf(ORio.ACTIVITY_JSONLD, RDFFormat.NQUADS)

    /** Main file where the entire streams' Collection is stored */
    fun collectionFile(fileType: RDFFormat? = null) =
        File("${location.path}/$name${formatExtension(fileType)}")

    /** Symlink to the first page of the collection */
    private fun firstLink(fileType: RDFFormat? = null) =
        File("${location.path}/first${formatExtension(fileType)}")

    /** Symlink to the last page of the collection */
    private fun lastLink(fileType: RDFFormat? = null) =
        File("${location.path}/last${formatExtension(fileType)}")

    fun save() {
        ensure()
        storeStream()
        storePages()
        updateLinks()
    }

    private fun ensure() {
        ensureDirectoryTree(location)
    }

    fun findPages(fileType: RDFFormat): Array<File> {
        val pageMatcher = Regex("$name(;page=[0-9]+)${formatExtension(fileType)}")

        return location
            .listFiles { file -> file.name.matches(pageMatcher) }!!
            .sortedArray()
    }

    private fun initPage(number: Int, itemCount: Int): Pair<Resource, Model> {
        val coll = CollectionPage(
            id = pageIRI(number),
            items = listOf(),
            totalItems = itemCount,
            partOf = generateIRI(null),
            first = generateIRI(firstLink().relativeTo(location).toString()),
            last = generateIRI(lastLink().relativeTo(location).toString())
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

    internal fun page(number: Int): Path {
        return File("${location.path}/${pageName(number)}").toPath()
    }

    fun pageIRI(number: Int): IRI {
        return generateIRI(pageName(number))
    }

    private fun pageName(number: Int): String {
        return "$name;page=$number"
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
        val coll = Collection<T>(
            id = generateIRI(null),
            totalItems = items.size,
            first = generateIRI(firstLink().relativeTo(location).toString()),
            last = generateIRI(lastLink().relativeTo(location).toString())
        )
        val (_, streamModel) = objectToModel(coll)
        store(collectionFile().toPath(), streamModel)
    }

    private fun updateLinks() {
        for (fileType in fileTypes) {
            val pages = findPages(fileType)

            val firstPage = pages.firstOrNull()
            if (firstPage != null) {
                recreateSymbolicLink(
                    firstLink(fileType).toPath(),
                    firstPage.relativeTo(location).toPath()
                )
            }

            val lastPage = pages.lastOrNull()
            if (lastPage != null) {
                recreateSymbolicLink(
                    lastLink(fileType).toPath(),
                    lastPage.relativeTo(location).toPath()
                )
            }
        }
    }
}
