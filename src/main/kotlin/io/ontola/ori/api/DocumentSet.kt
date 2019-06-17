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

import com.google.common.base.Splitter

import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.Files
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

import org.eclipse.rdf4j.model.*
import org.eclipse.rdf4j.model.impl.LinkedHashModel

class DocumentSet(
    private val docCtx: DocumentCtx,
    private val delta: Model = LinkedHashModel()
) {
    private val iri = docCtx.iri!!
    private val config: Properties = ORIContext.getCtx().config

    companion object {
        val versionStringMatcher = Regex("\\d{8}T\\d{4}")
        private val digester: MessageDigest = getDigester()
    }

    private val id: String = iri.substring(iri.lastIndexOf('/') + 1)
    private val hashKeys: Iterable<String>

    init {
        val md5sum = digester.digest(id.toByteArray())
        val hashedId = String.format("%032x", BigInteger(1, md5sum))
        hashKeys = Splitter.fixedLength(4).split(hashedId)
    }

    private fun baseDir(): File {
        return File("${config.getProperty("ori.api.dataDir")}/${hashKeys.joinToString("/")}")
    }

    fun deltaAdd(s: Statement): Boolean {
        return delta.add(s.subject, s.predicate, s.`object`)
    }

    fun anyObject(o: Resource): Boolean {
        return delta.any { stmt -> stmt.`object` == o }
    }

    fun process() {
        println("Processing deltaevent, $iri")
        ensureDirectoryTree()
        val latestVersion = findLatestDocument()
        val newVersion = initNewVersion()

        val event = when {
            latestVersion == null -> EventType.CREATE
            latestVersion != newVersion -> EventType.UPDATE
            else -> return
        }

        newVersion.save()
        updateLatest(newVersion)
        publishBlocking(event, newVersion)
    }

    private fun initNewVersion(): Document {
        val versionStamp = SimpleDateFormat("yyyyMMdd'T'HHmm").format(Date())

        return Document(
            docCtx.copy(version = versionStamp),
            this.delta,
            this.baseDir()
        )
    }

    private fun ensureDirectoryTree() {
        val filePath = this.baseDir()
        if (!filePath.exists()) {
            val dirPerms = PosixFilePermissions.fromString("rwxr-xr-x")
            try {
                Files.createDirectories(
                    filePath.toPath(),
                    PosixFilePermissions.asFileAttribute(dirPerms)
                )
            } catch (e: IOException) {
                EventBus.getBus().publishError(docCtx, Exception("Couldn't create hash directory tree '$filePath'", e))
            }
        }
    }

    private fun findLatestDocument(): Document? {
        val timestampMatcher = versionStringMatcher

        val version = baseDir()
            .list { dir: File, name: String -> dir.isDirectory && name.matches(timestampMatcher) }
            .sortedArray()
            .lastOrNull()

        if (version.isNullOrEmpty()) {
            return null
        }

        return Document.findExisting(docCtx, version, baseDir())
    }

    /** Publish an action to the bus for further processing */
    private fun publishBlocking(type: EventType, version: Document) {
        EventBus
            .getBus()
            .publishEvent(Event(type, iri, version.organization, null))
            .get()
    }

    private fun updateLatest(nextLatest: Document) {
        try {
            val latestDir = File(String.format("%s/%s", this.baseDir(), "latest"))
            Files.deleteIfExists(latestDir.toPath())
            // The link needs to be relative to work across volume mounts
            Files.createSymbolicLink(
                latestDir.toPath(),
                nextLatest.dir().relativeTo(this.baseDir()).toPath()
            )
            println("Made ${nextLatest.version} latest")
        } catch (e: IOException) {
            EventBus.getBus().publishError(
                docCtx,
                Exception("Error while marking '${nextLatest.version}' as latest for resource '$iri'; ${e.message}", e)
            )
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
