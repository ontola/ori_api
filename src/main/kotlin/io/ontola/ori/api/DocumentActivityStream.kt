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
import io.ontola.activitystreams.Activity
import io.ontola.activitystreams.collection.filesystem.PaginatedFilesystemCollection
import io.ontola.activitystreams.collection.filesystem.loadWithType
import io.ontola.activitystreams.vocabulary.AS
import io.ontola.ori.api.context.ResourceCtx
import io.ontola.rdfUtils.createIRI
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.rio.RDFFormat
import java.io.File
import java.nio.file.Files

class DocumentActivityStream(private val docCtx: ResourceCtx<*>) {
    /** Directory where the Activity Stream data is stored */
    private val asDirectory = File(docCtx.dir().path + "/activity")

    /** Symlink to resolve the stream via content-negotiation */
    private fun streamLink(fileType: RDFFormat? = null) =
        File("${docCtx.dir().path}/${docCtx.id}${formatExtension(fileType)}")

    private val collection = PaginatedFilesystemCollection<ASObject>(
        tFactory = { Activity() },
        generateIRI = { path: String? -> iri(path ?: "") },
        name = docCtx.id!!,
        location = asDirectory
    )

    init {
        collection.loadWithType()
    }

    /** Add an event to the activity stream. */
    fun append(event: Event) {
        try {
            val eventActivity = Activity(
                type = createIRI(AS.NAMESPACE, event.type.name.toLowerCase().capitalize()),
                target = docCtx.iri,
                published = DocumentSet.versionStringFormat
                    .parse(docCtx.version)
            )
            collection += eventActivity
        } catch (e: Exception) {
            println(e)
            ORIContext.notify(e)
        }
    }

    fun append(activity: Activity) {
        collection += activity
    }

    fun save() {
        collection.save()
        updateLinks()
    }

    fun load(other: DocumentActivityStream) {
        collection += other.collection
    }

    private fun iri(suffix: String = ""): IRI {
        val iriStr = arrayOf("${docCtx.iri}/activity", suffix)
            .filterNot(String::isNullOrBlank)
            .joinToString("/")

        return createIRI(iriStr)
    }

    private fun updateLinks() {
        if (!streamLink(ORio.ACTIVITY_JSONLD).exists()) {
            Files.createSymbolicLink(
                streamLink(ORio.ACTIVITY_JSONLD).toPath(),
                collection.collectionFile(ORio.ACTIVITY_JSONLD).relativeTo(docCtx.dir()).toPath()
            )
        }
    }
}
