/*
 * ActivityStreams
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

package io.ontola.activitystreams.vocabulary

import io.ontola.rdfUtils.createIRI
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Namespace
import org.eclipse.rdf4j.model.impl.SimpleNamespace

/**
 * Constants for the Schema.org namespace.
 *
 * @see [Schema.org](https://schema.org/)
 */
object SCHEMA {
    const val NAMESPACE = "http://schema.org/"

    /**
     * Recommended prefix for the Schema.org namespace: "schema"
     */
    const val PREFIX = "schema"

    val CREATOR = createIRI(NAMESPACE, "creator")

    val NAME = createIRI(NAMESPACE, "name")

    /**
     * An immutable [Namespace] constant that represents the RDF namespace.
     */
    val NS: Namespace = SimpleNamespace(PREFIX, NAMESPACE)

    fun custom(term: String): IRI {
        return createIRI("$NAMESPACE$term")
    }
}
