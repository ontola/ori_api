/*
 * RDF Utils
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

package io.ontola.rdfUtils

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.eclipse.rdf4j.model.BNode
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.impl.SimpleValueFactory

private val factory = SimpleValueFactory.getInstance()

fun createIRI(iri: String): IRI {
    return factory.createIRI(iri)
}

fun tryCreateIRI(iri: String): IRI? {
    return try {
        factory.createIRI(iri)
    } catch (e: Exception) {
        null
    }
}

fun createIRI(ns: String, term: String): IRI {
    return factory.createIRI("$ns$term")
}

fun getQueryParameter(iri: Resource, parameter: String): IRI? {
    if (iri is BNode) {
        return null
    }

    return iri
        .stringValue()
        .toHttpUrlOrNull()
        ?.queryParameter(parameter)
        ?.let { graph -> createIRI(graph) }
}
