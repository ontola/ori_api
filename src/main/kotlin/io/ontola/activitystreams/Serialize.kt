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

package io.ontola.activitystreams

import org.eclipse.rdf4j.model.*
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.model.vocabulary.XMLSchema
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import javax.xml.datatype.XMLGregorianCalendar
import kotlin.reflect.full.declaredMemberProperties

fun objectToModel(obj: ASObject): Pair<Resource, Model> {
    val model = LinkedHashModel()
    val subject = constructSubject(obj.id)

    for (prop in obj::class.declaredMemberProperties) {
        // Setting as:id enables bugs in the json-ld serializer
        if (prop.name == "id") continue

        val rawValue = prop.call(obj) ?: continue
        val predicate = determinePredicate(prop.name)

        model.addAll(unknownToStatements(subject, predicate, rawValue))
    }

    return Pair(subject, model)
}

internal fun constructSubject(id: Resource?): Resource {
    val factory = SimpleValueFactory.getInstance()
    if (id == null) {
        return factory.createBNode()
    }

    return id
}

private fun addList(subject: Resource, predicate: IRI, list: List<*>): List<Statement> {
    val factory = SimpleValueFactory.getInstance()
    val statements = ArrayList<Statement>()
    list.forEach { v ->
        statements.add(factory.createStatement(subject, predicate, unknownToValue(v)))
    }

    return statements
}

private fun unknownToStatements(subject: Resource, predicate: IRI, rawValue: Any): List<Statement> {
    val factory = SimpleValueFactory.getInstance()

    when (rawValue) {
        is Array<*> -> factory.createLiteral("TODO:Array")
        is List<*> -> {
            return addList(subject, predicate, rawValue)
        }
    }

    val value = unknownToValue(rawValue)

    return listOf(factory.createStatement(subject, predicate, value))
}

private fun unknownToValue(rawValue: Any?): Value {
    val factory = SimpleValueFactory.getInstance()

    return when (rawValue) {
        is BigDecimal -> factory.createLiteral(rawValue)
        is BigInteger, is Int -> factory.createLiteral(rawValue.toString(), XMLSchema.INTEGER)
        is BNode -> rawValue
        is Boolean -> factory.createLiteral(rawValue)
        is Date -> factory.createLiteral(rawValue)
        is Double -> factory.createLiteral(rawValue)
        is Float -> factory.createLiteral(rawValue)
        is IRI -> rawValue
        is Long -> factory.createLiteral(rawValue)
        is String -> factory.createLiteral(rawValue)
        is Short -> factory.createLiteral(rawValue)
        is XMLGregorianCalendar -> factory.createLiteral(rawValue)
        else -> factory.createLiteral(rawValue.toString())
    }
}
