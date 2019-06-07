/*
 * ORI API
 * Copyright (C) 2019, Argu BV
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

import org.eclipse.rdf4j.RDF4JException
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.rio.*
import org.eclipse.rdf4j.rio.helpers.JSONLDMode
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings
import org.zeroturnaround.zip.ZipUtil
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

/**
 * A resource in the ORI API.
 */
class Document(
    private val iri: String,
    private val data: Model,
    val version: String,
    private val baseDir: File
) {

    private val id: String = this.iri.substring(this.iri.lastIndexOf('/') + 1)
    private val subject: Resource = SimpleValueFactory.getInstance().createIRI(iri)
    private val filePath = this.dir()
    private val formats = listOf(
        RDFFormat.NTRIPLES,
        RDFFormat.N3,
        RDFFormat.NQUADS,
        RDFFormat.TURTLE,
        RDFFormat.JSONLD,
        RDFFormat.RDFJSON
    )

    fun dir(): File {
        return File(String.format("%s/%s", baseDir.absolutePath, this.version))
    }

    fun save() {
        System.out.printf("Writing subject '%s' with version '%s'\n", this.subject, this.version)

        ensureDirectory()
        serialize()
        archive()
    }

    private fun archive() {
        val archiveName = "${this.id}.zip"
        val archive = File("$filePath/$archiveName")
        if (archive.exists()) {
            archive.delete()
        }
        ZipUtil.pack(filePath, archive)
        if (ZipUtil.containsEntry(archive, archiveName)) {
            ZipUtil.removeEntry(archive, archiveName)
        }
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
                throw Exception(String.format("Couldn't create directory '%s'", filePath), e)
            }
        }
    }

    private fun serialize() {
        val permissions = PosixFilePermissions.fromString("rw-r--r--")
        for (format in formats) {
            val filename = "${this.id}.${format.defaultFileExtension}"
            val file = File("$filePath/$filename")

            try {
                val rdfWriter = createWriter(format, FileOutputStream(file))
                rdfWriter.handleUsedNamespaces(this.data)
                rdfWriter.startRDF()
                rdfWriter.handleModel(this.data)
                rdfWriter.endRDF()
                Files.setPosixFilePermissions(file.toPath(), permissions)
            } catch (e: FileNotFoundException) {
                System.out.printf("Couldn't create file '%s' because '%s' \n", file.path, e.toString())
            } catch (e: RDF4JException) {
                System.out.printf("Error while serializing resource '$id' because '%s' \n", file.path, e.toString())
            }
        }
    }

    override operator fun equals(other: Any?): Boolean {
        if (other == null || this.javaClass != other.javaClass) {
            return false
        }

        return iri == (other as Document).iri
    }
}
