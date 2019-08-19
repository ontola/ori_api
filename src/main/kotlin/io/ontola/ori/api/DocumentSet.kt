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
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*

class DocumentSet(
    private val docCtx: ResourceCtx<*>,
    private val delta: Model = LinkedHashModel()
) {
    private val iri = docCtx.iri!!
    private val baseDir = docCtx.dir()
    internal val lock = createLock(docCtx.dir())

    companion object {
        val versionStringFormat = SimpleDateFormat("yyyyMMdd'T'HHmm")
        val versionStringMatcher = Regex("\\d{8}T\\d{4}")
    }

    fun addAll(s: List<Statement>): Boolean {
        return delta.addAll(s)
    }

    fun deltaAdd(s: Statement): Boolean {
        return delta.add(s.subject, s.predicate, s.`object`)
    }

    fun anyObject(o: Resource): Boolean {
        return delta.any { stmt -> stmt.`object` == o }
    }

    suspend fun process() {
        lock.withLock {
            try {
                ensureDirectoryTree(baseDir)
            } catch (e: Exception) {
                EventBus.getBus().publishError(docCtx, e)
                return@withLock
            }
            val latestVersion = findLatestDocument()
            val newVersion = initNewVersion()

            val eventType = when {
                latestVersion == null -> EventType.CREATE
                latestVersion != newVersion -> EventType.UPDATE
                else -> return@withLock
            }
            val org = try {
                newVersion.organization
            } catch (e: Exception) {
                EventBus
                    .getBus()
                    .publishError(this.docCtx, e)
                null
            }
            val event = Event(eventType, iri, org, newVersion.data)

            newVersion.save()
            updateActivityStream(event, newVersion, latestVersion)
            newVersion.archive()
            updateLatest(newVersion)
            publishBlocking(event)
        }
    }

    internal fun findLatestDocument(): Document? {
        val timestampMatcher = versionStringMatcher

        val version = baseDir
            .list { dir: File, name: String -> dir.isDirectory && name.matches(timestampMatcher) }
            ?.sortedArray()
            ?.lastOrNull()

        if (version.isNullOrEmpty()) {
            return null
        }

        return Document.findExisting(docCtx, version, baseDir)
    }

    internal fun initNewVersion(): Document {
        // TODO: pass the old verion to kickstart, then apply the delta
        val versionStamp = versionStringFormat.format(Date())

        return Document(
            docCtx.copy(version = versionStamp),
            delta,
            baseDir
        )
    }

    /** Publish an action to the bus for further processing */
    private fun publishBlocking(event: Event) {
        EventBus
            .getBus()
            .publishEvent(event)
            .get()
    }

    private fun updateLatest(nextLatest: Document) {
        try {
            val latestDir = docCtx.copy(version = "latest").dir()
            Files.deleteIfExists(latestDir.toPath())
            // The link needs to be relative to work across volume mounts
            Files.createSymbolicLink(
                latestDir.toPath(),
                nextLatest.dir().relativeTo(baseDir).toPath()
            )
        } catch (e: IOException) {
            EventBus.getBus().publishError(
                docCtx,
                Exception("Error while marking '${nextLatest.version}' as latest for resource '$iri'; ${e.message}", e)
            )
        }
    }

    private fun updateActivityStream(event: Event, newVersion: Document, oldVersion: Document?) {
        val ctx = docCtx.copy(version = newVersion.version)
        try {
            val stream = DocumentActivityStream(ctx)
            if (oldVersion != null) {
                val oldStream = DocumentActivityStream(docCtx)
                stream.load(oldStream)
            }
            stream.append(event)
            stream.save()
        } catch (e: Exception) {
            EventBus.getBus().publishError(ctx, e)
        }
    }

    override operator fun equals(other: Any?): Boolean {
        if (other == null || this.javaClass != other.javaClass) {
            return false
        }

        return iri == (other as DeltaEvent).iri
    }

    override fun hashCode(): Int {
        return iri.hashCode()
    }
}
