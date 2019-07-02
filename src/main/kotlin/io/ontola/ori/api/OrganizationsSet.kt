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

import io.ontola.activitystreams.ASObject
import io.ontola.activitystreams.Object
import io.ontola.activitystreams.collection.filesystem.PaginatedFilesystemCollection
import io.ontola.activitystreams.collection.filesystem.loadWithType
import io.ontola.activitystreams.vocabulary.AS
import io.ontola.rdfUtils.createIRI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.rio.RDFFormat
import java.io.File

private val defaultAPIDir = File(ORIContext.getCtx().config.getProperty("ori.api.apiDir")).canonicalFile

/** Manipulates the API's 'organizations collection' which holds all organizations stored in the API. */
class OrganizationsSet(private val apiDir: File = defaultAPIDir) : Set<ASObject> {
    internal val collectionDir = File("$apiDir/$collectionName")
    private val collection = PaginatedFilesystemCollection(
        tFactory = { Object(type = AS.DOCUMENT) },
        generateIRI = { path: String? ->
            createIRI(
                listOfNotNull("https://api.openraadsinformatie.nl/v1/$collectionName", path)
                    .joinToString("/")
            )
        },
        name = collectionName,
        location = collectionDir
    )

    init {
        collection.loadWithType()
    }

    private fun indexLink(fileType: RDFFormat? = null) =
        File("$collectionDir/index${formatExtension(fileType)}")

    private fun collectionIndexLink(fileType: RDFFormat? = null) =
        File("$apiDir/$collectionName${formatExtension(fileType)}").canonicalFile

    override val size: Int
        get() = collection.size

    override fun contains(element: ASObject): Boolean {
        return collection.contains(element)
    }

    override fun isEmpty(): Boolean {
        return collection.isEmpty()
    }

    override fun iterator(): Iterator<ASObject> {
        return collection.iterator()
    }

    override fun containsAll(elements: Collection<ASObject>): Boolean {
        return collection.containsAll(elements)
    }

    fun add(id: IRI) {
        val organization = Object(
            type = AS.DOCUMENT,
            id = id
        )
        if (!collection.contains(organization)) {
            collection += organization
        }
    }

    fun save() {
        collection.save()
        updateLinks()
    }

    private fun updateLinks() {
        for (format in listOf(ORio.ACTIVITY_JSONLD, RDFFormat.NQUADS)) {
            if (!indexLink(format).exists()) {
                recreateSymbolicLink(
                    indexLink(format).toPath(),
                    collection.collectionFile(format).relativeTo(collectionDir).toPath()
                )

                recreateSymbolicLink(
                    collectionIndexLink(format).toPath(),
                    collection.collectionFile(format).relativeTo(apiDir).toPath()
                )
            }
        }
    }

    companion object {
        const val collectionName = "organizations"

        suspend fun withLock(
            dir: File = File("$defaultAPIDir/$collectionName"),
            action: () -> Unit
        ) = withContext(Dispatchers.IO) {
            createLock(dir).withLock(action)
        }
    }
}
