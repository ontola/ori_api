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

import com.github.jsonldjava.core.RDFDataset
import org.eclipse.rdf4j.RDF4JException
import org.eclipse.rdf4j.model.IRI
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.model.util.Models
import org.eclipse.rdf4j.rio.*
import org.eclipse.rdf4j.rio.helpers.StatementCollector
import org.zeroturnaround.zip.ZipUtil
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

/**
 * A resource in the ORI API.
 */
class Document(
    private val docCtx: DocumentCtx,
    private val data: Model,
    private val baseDir: File
) {
    companion object {
        fun findExisting(docCtx: DocumentCtx, timestamp: String, baseDir: File): Document {
            val d = Document(docCtx.copy(version = timestamp), LinkedHashModel(), baseDir)
            d.read()
            return d
        }
    }

    private val iri = docCtx.iri!!
    internal val version = docCtx.version!!

    private val id: String = iri.substring(iri.lastIndexOf('/') + 1)
    private val subject: Resource = SimpleValueFactory.getInstance().createIRI(iri)
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
            val hasOrgName = RDFDataset.IRI("http://www.w3.org/2006/vcard/ns#hasOrganizationName")
            return data
                .find { s -> s.predicate == hasOrgName }
                ?.`object` as IRI?
        }

    fun dir(): File {
        return File(String.format("%s/%s", baseDir.absolutePath, version))
    }


    fun save() {
        println("Writing subject '$subject' with version '$version'")

        ensureDirectory()
        serialize()
        archive()
    }

    private fun archive() {
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

    private fun ensureDirectory() {
        val filePath = this.dir()
        if (!filePath.exists()) {
            val dirPerms = PosixFilePermissions.fromString("rwxr-xr-x")
            try {
                Files.createDirectory(
                    filePath.toPath(),
                    PosixFilePermissions.asFileAttribute(dirPerms)
                )
            } catch (e: IOException) {
                val dirException = Exception(String.format("Couldn't create directory '%s'", filePath), e)
                EventBus.getBus().publishError(docCtx, dirException)
            }
        }
    }

    /**
     * Reads an existing (n-quads) file from disk into the model, overwriting any previous statements.
     */
    private fun read() {
        val newData = LinkedHashModel()
        val rdfParser = Rio.createParser(RDFFormat.NQUADS)
        val nqFile = File("$baseDir/$version/$id.nq")

        rdfParser.setRDFHandler(StatementCollector(newData))
        rdfParser.parse(nqFile.inputStream(), iri)

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
