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

import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.RDFWriter
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.rio.WriterConfig
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings
import org.eclipse.rdf4j.rio.helpers.JSONLDMode
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings
import java.io.OutputStream
import java.io.Writer

/**
 * Rio wrapper to only include namespaces mentioned in the data.
 *
 * Base Rio implementations will include _all_ namespaces added by RDFWriter::handleNamespace, which results in a large
 * list of unused namespaces.
 */
class ORio(private val writer: RDFWriter) : RDFWriter by writer {

    init {
        val writerConfig = WriterConfig()
        writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, true)

        if (writer.rdfFormat == RDFFormat.JSONLD) {
            writerConfig.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT)
            writerConfig.set(JSONLDSettings.USE_NATIVE_TYPES, true)
            writerConfig.set(JSONLDSettings.HIERARCHICAL_VIEW, true)
        }

        writer.writerConfig = writerConfig
    }

    fun handleUsedNamespaces(statements: Model) {
        val usedNamespaces = HashSet<String>()
        val namespacesIRIs = reverseNamespaces.keys
        statements.forEach { s ->
            arrayOf(s.subject, s.predicate, s.`object`).forEach { resource ->
                val match = namespacesIRIs.find { nsIRI -> resource.stringValue().startsWith(nsIRI) }
                if (!match.isNullOrEmpty()) {
                    usedNamespaces.add(match)
                }
            }
        }

        usedNamespaces.forEach { ns -> writer.handleNamespace(reverseNamespaces[ns], ns) }
    }

    fun handleSingleModel(model: Model) {
        handleUsedNamespaces(model)
        startRDF()
        handleModel(model)
        endRDF()
    }

    fun handleModel(model: Model) {
        model
            .stream()
            .sorted(ModelSorter())
            .forEach(writer::handleStatement)
    }

    companion object {
        fun createWriter(format: RDFFormat, out: OutputStream): ORio {
            val rdfWriter = Rio.createWriter(format, out)

            return ORio(rdfWriter)
        }

        fun createWriter(format: RDFFormat, writer: Writer): ORio {
            val rdfWriter = Rio.createWriter(format, writer)

            return ORio(rdfWriter)
        }

        private fun reverseNSMap(): Map<String, String> {
            val nsMap = HashMap<String, String>()

            nsMap["https://argu.co/ns/meta#"] = "ameta"
            nsMap["https://argu.co/ns/core#"] = "argu"
            nsMap["https://www.w3.org/ns/activitystreams#"] = "as"
            nsMap["http://bibframe.org/vocab/"] = "bibframe"
            nsMap["http://purl.org/ontology/bibo/"] = "bibo"
            nsMap["http://purl.org/vocab/bio/0.1/"] = "bio"
            nsMap["http://creativecommons.org/ns#"] = "cc"
            nsMap["http://dbpedia.org/ontology/"] = "dbo"
            nsMap["http://dbpedia.org/property/"] = "dbp"
            nsMap["http://dbpedia.org/resource/"] = "dbpedia"
            nsMap["http://purl.org/dc/terms/"] = "dc"
            nsMap["http://www.w3.org/ns/dcat#"] = "dcat"
            nsMap["http://purl.org/dc/dcmitype/"] = "dctype"
            nsMap["http://example.com/ns#"] = "ex"
            nsMap["http://www.example.com/"] = "example"
            nsMap["http://hl7.org/fhir/"] = "fhir"
            nsMap["http://hl7.org/fhir/STU3/"] = "fhir3"
            nsMap["http://xmlns.com/foaf/0.1/"] = "foaf"
            nsMap["http://www.w3.org/2003/01/geo/wgs84_pos#"] = "geo"
            nsMap["http://www.w3.org/2011/http#"] = "http"
            nsMap["http://www.w3.org/2007/ont/http#"] = "http07"
            nsMap["http://www.w3.org/2007/ont/httph#"] = "httph"
            nsMap["http://www.w3.org/ns/hydra/core#"] = "hydra"
            nsMap["http://www.iana.org/assignments/link-relations/"] = "ianalr"
            nsMap["http://www.w3.org/2007/ont/link#"] = "link"
            nsMap["http://purl.org/link-lib/"] = "ll"
            nsMap["https://argu.co/voc/mapping/"] = "mapping"
            nsMap["https://argu.co/ns/meeting/"] = "meeting"
            nsMap["http://www.semanticdesktop.org/ontologies/2007/04/02/ncal#"] = "ncal"
            nsMap["http://www.w3.org/ns/opengov#"] = "opengov"
            nsMap["http://www.w3.org/ns/org#"] = "org"
            nsMap["https://id.openraadsinformatie.nl/"] = "orid"
            nsMap["http://www.w3.org/2002/07/owl#"] = "owl"
            nsMap["http://purl.org/pav/"] = "pav"
            nsMap["http://www.w3.org/ns/person#"] = "person"
            nsMap["http://www.w3.org/ns/prov#"] = "prov"
            nsMap["http://purl.org/linked-data/cube#"] = "qb"
            nsMap["http://www.w3.org/1999/02/22-rdf-syntax-ns#"] = "rdf"
            nsMap["http://www.w3.org/2000/01/rdf-schema#"] = "rdfs"
            nsMap["http://schema.org/"] = "schema"
            nsMap["http://www.w3.org/ns/shacl#"] = "sh"
            nsMap["http://www.w3.org/2004/02/skos/core#"] = "skos"
            nsMap["http://www.wikidata.org/entity/"] = "wd"
            nsMap["https://www.wikidata.org/wiki/Special:EntityData/"] = "wdata"
            nsMap["http://www.wikidata.org/reference/"] = "wdref"
            nsMap["http://www.wikidata.org/prop/"] = "wdp"
            nsMap["http://www.wikidata.org/entity/statement/"] = "wds"
            nsMap["http://www.wikidata.org/prop/direct/"] = "wdt"
            nsMap["http://www.wikidata.org/value/"] = "wdv"
            nsMap["http://www.w3.org/2000/xmlns/"] = "xmlns"
            nsMap["http://www.w3.org/2001/XMLSchema#"] = "xsd"

            return nsMap
        }

        val reverseNamespaces = reverseNSMap()
    }
}