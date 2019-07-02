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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.ontola.ori.api

import io.ontola.ori.api.context.ResourceCtx
import org.eclipse.rdf4j.RDF4JException
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import org.eclipse.rdf4j.model.util.Models
import org.eclipse.rdf4j.model.vocabulary.VCARD4
import org.eclipse.rdf4j.rio.RDFFormat
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

/**
 * A resource in the ORI API.
 */
class Document(
    private val docCtx: ResourceCtx<*>,
    internal val data: Model,
    private val baseDir: File
) {
    companion object {
        fun findExisting(docCtx: ResourceCtx<*>, timestamp: String, baseDir: File): Document {
            val d = Document(docCtx.copy(version = timestamp), LinkedHashModel(), baseDir)
            d.read()
            return d
        }
    }

    private val iri = docCtx.iri!!
    internal val version = docCtx.version!!

    private val id: String = iri.stringValue().substring(iri.stringValue().lastIndexOf('/') + 1)
    private val filePath = this.dir()
    private val formats = listOf(
        RDFFormat.NTRIPLES,
        RDFFormat.N3,
        RDFFormat.NQUADS,
        RDFFormat.TURTLE,
        RDFFormat.JSONLD,
        RDFFormat.RDFJSON,
        RDFFormat.BINARY,
        RDFFormat.RDFXML
    )

    val organization: IRI?
        get() {
            val hasOrgName = VCARD4.HAS_ORGANIZATION_NAME
            val org = data
                .find { s -> s.predicate == hasOrgName }
                ?.`object`
                ?: return null

            if (org !is IRI) {
                throw Exception("vcard4:hasOrganizationName is blank node or literal with value '$org'")
            }

            return org
        }

    fun archive() {
        val tmp = createTempFile("ori-api-$id-$version")
        tmp.deleteOnExit()
        ZipUtil.pack(filePath, tmp)

        val archiveName = "${this.id}.zip"
        val archive = File("$filePath/$archiveName")
        if (archive.exists()) {
            archive.delete()
        }
        Files.move(tmp.toPath(), archive.toPath())
    }

    fun asDir(): File {
        return File(String.format("%s/%s/activity", baseDir.absolutePath, version))
    }

    fun dir(): File {
        return File(String.format("%s/%s", baseDir.absolutePath, version))
    }

    fun save(): Document {
        println("Writing subject '$iri' with version '$version'")

        try {
            ensureDirectoryTree(this.dir())
        } catch (e: Exception) {
            EventBus.getBus().publishError(docCtx, e)
        }
        serialize()

        return this
    }

    /**
     * Reads an existing (n-quads) file from disk into the model, overwriting any previous statements.
     */
    private fun read() {
        val nqFile = File("$baseDir/$version/$id.nq")
        val newData = ORio.parseToModel(nqFile.inputStream(), iri.stringValue())

        data.clear()
        data.addAll(newData)
    }

    private fun serialize() {
        val permissions = PosixFilePermissions.fromString("rw-r--r--")
        for (format in formats) {
            val filename = "${this.id}.${format.defaultFileExtension}"
            val file = File("$filePath/$filename")

            try {
                val rdfWriter = ORio.createWriter(format, FileOutputStream(file))
                rdfWriter.handleSingleModel(this.data)
                Files.setPosixFilePermissions(file.toPath(), permissions)
            } catch (e: FileNotFoundException) {
                EventBus.getBus().publishError(docCtx, Exception("Couldn't create file '${file.path}' because '$e'", e))
            } catch (e: RDF4JException) {
                EventBus.getBus().publishError(
                    docCtx,
                    Exception("Error while serializing resource '$id' to ${file.path} because '$e'", e)
                )
            }
        }
    }

    /**
     * Compares two documents by their contents.
     *
     * Will return true even if the versions differ, but the IRI and statements are equal.
     */
    override operator fun equals(other: Any?): Boolean {
        if (other == null || this.javaClass != other.javaClass) {
            return false
        }

        return iri == (other as Document).iri && Models.isomorphic(data, other.data)
    }

    override fun hashCode(): Int {
        return "${iri.hashCode()}${data.hashCode()}".hashCode()
    }
}
