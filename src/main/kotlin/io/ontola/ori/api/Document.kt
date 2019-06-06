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
class Document(private val iri: String,
               private val data: Model,
               val version: String,
               private val baseDir: File) {

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
        val rdfWriter = Rio.createWriter(format, FileOutputStream(file))
        this.handleNamespaces(rdfWriter)
        if (format == RDFFormat.JSONLD) {
          val jsonldConfig = WriterConfig()
          jsonldConfig.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT)
          jsonldConfig.set(JSONLDSettings.USE_NATIVE_TYPES, true)
          jsonldConfig.set(JSONLDSettings.HIERARCHICAL_VIEW, true)
          rdfWriter.setWriterConfig(jsonldConfig)
        }
        rdfWriter.startRDF()
        for (s in this.data.filter(this.subject, null, null)) {
          rdfWriter.handleStatement(s)
        }
        rdfWriter.endRDF()
        Files.setPosixFilePermissions(file.toPath(), permissions)
      } catch (e: FileNotFoundException) {
        System.out.printf("Couldn't create file '%s' because '%s' \n", file.path, e.toString())
      }
    }
  }

  override operator fun equals(other: Any?): Boolean {
    if (other == null || this.javaClass != other.javaClass) {
      return false
    }

    return iri == (other as Document).iri
  }

  private fun handleNamespaces(h: RDFHandler) {
    h.handleNamespace("ameta", "https://argu.co/ns/meta#")
    h.handleNamespace("argu", "https://argu.co/ns/core#")
    h.handleNamespace("as", "https://www.w3.org/ns/activitystreams#")
    h.handleNamespace("bibframe", "http://bibframe.org/vocab/")
    h.handleNamespace("bibo", "http://purl.org/ontology/bibo/")
    h.handleNamespace("bio", "http://purl.org/vocab/bio/0.1/")
    h.handleNamespace("cc", "http://creativecommons.org/ns#")
    h.handleNamespace("dbo", "http://dbpedia.org/ontology/")
    h.handleNamespace("dbp", "http://dbpedia.org/property/")
    h.handleNamespace("dbpedia", "http://dbpedia.org/resource/")
    h.handleNamespace("dc", "http://purl.org/dc/terms/")
    h.handleNamespace("dcat", "http://www.w3.org/ns/dcat#")
    h.handleNamespace("dctype", "http://purl.org/dc/dcmitype/")
    h.handleNamespace("ex", "http://example.com/ns#")
    h.handleNamespace("example", "http://www.example.com/")
    h.handleNamespace("fhir", "http://hl7.org/fhir/")
    h.handleNamespace("fhir3", "http://hl7.org/fhir/STU3")
    h.handleNamespace("foaf", "http://xmlns.com/foaf/0.1/")
    h.handleNamespace("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#")
    h.handleNamespace("http", "http://www.w3.org/2011/http#")
    h.handleNamespace("http07", "http://www.w3.org/2007/ont/http#")
    h.handleNamespace("httph", "http://www.w3.org/2007/ont/httph#")
    h.handleNamespace("hydra", "http://www.w3.org/ns/hydra/core#")
    h.handleNamespace("ianalr", "http://www.iana.org/assignments/link-relations/")
    h.handleNamespace("link", "http://www.w3.org/2007/ont/link#")
    h.handleNamespace("ll", "http://purl.org/link-lib/")
    h.handleNamespace("mapping", "https://argu.co/voc/mapping/")
    h.handleNamespace("meeting", "https://argu.co/ns/meeting/")
    h.handleNamespace("ncal", "http://www.semanticdesktop.org/ontologies/2007/04/02/ncal#")
    h.handleNamespace("opengov", "http://www.w3.org/ns/opengov#")
    h.handleNamespace("org", "http://www.w3.org/ns/org#")
    h.handleNamespace("orid", "https://id.openraadsinformatie.nl/")
    h.handleNamespace("owl", "http://www.w3.org/2002/07/owl#")
    h.handleNamespace("pav", "http://purl.org/pav/")
    h.handleNamespace("person", "http://www.w3.org/ns/person#")
    h.handleNamespace("prov", "http://www.w3.org/ns/prov#")
    h.handleNamespace("qb", "http://purl.org/linked-data/cube#")
    h.handleNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
    h.handleNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
    h.handleNamespace("schema", "http://schema.org/")
    h.handleNamespace("sh", "http://www.w3.org/ns/shacl#")
    h.handleNamespace("skos", "http://www.w3.org/2004/02/skos/core#")
    h.handleNamespace("wd", "http://www.wikidata.org/entity/")
    h.handleNamespace("wdata", "https://www.wikidata.org/wiki/Special:EntityData/")
    h.handleNamespace("wdref", "http://www.wikidata.org/reference/")
    h.handleNamespace("wdp", "http://www.wikidata.org/prop/")
    h.handleNamespace("wds", "http://www.wikidata.org/entity/statement/")
    h.handleNamespace("wdt", "http://www.wikidata.org/prop/direct/")
    h.handleNamespace("wdv", "http://www.wikidata.org/value/")
    h.handleNamespace("xmlns", "http://www.w3.org/2000/xmlns/")
    h.handleNamespace("xsd", "http://www.w3.org/2001/XMLSchema")
   }
}
