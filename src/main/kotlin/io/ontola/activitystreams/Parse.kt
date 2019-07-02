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

import com.fasterxml.jackson.databind.util.StdDateFormat
import io.ontola.activitystreams.vocabulary.AS
import org.eclipse.rdf4j.model.*
import org.eclipse.rdf4j.model.util.Models
import org.eclipse.rdf4j.model.vocabulary.XMLSchema
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubtypeOf

/** Map an resource {subject} in a model to an ASObject */
fun <T : ASObject> modelToObject(model: Model, subject: Resource): ASObject {
    val obj = createObject<T>(model, subject)

    for (prop in obj::class.declaredMemberProperties) {
        val propValue = activityProp(model, subject, prop)
        if (prop is KMutableProperty<*> && propValue != null) {
            try {
                prop.setter.call(obj, propValue)
            } catch (e: IllegalArgumentException) {
                println("Wrong argument '$propValue' of type '${propValue::class.qualifiedName}' for '${prop.name}'")
            }
        }
    }

    return obj
}

private fun <T : ASObject> createObject(model: Model, subject: Resource): ASObject {
    return when (val typeValue = activityProp(model, subject, JSONLDResource::type)) {
        AS.ACCEPT,
        AS.ADD,
        AS.ANNOUNCE,
        AS.ARRIVE,
        AS.BLOCK,
        AS.CREATE,
        AS.DELETE,
        AS.DISLIKE,
        AS.FLAG,
        AS.FOLLOW,
        AS.IGNORE,
        AS.INVITE,
        AS.JOIN,
        AS.LEAVE,
        AS.LIKE,
        AS.LISTEN,
        AS.MOVE,
        AS.OFFER,
        AS.QUESTION,
        AS.REJECT,
        AS.READ,
        AS.REMOVE,
        AS.TENTATIVE_REJECT,
        AS.TENTATIVE_ACCEPT,
        AS.TRAVEL,
        AS.UNDO,
        AS.UPDATE,
        AS.VIEW,
        AS.ACTIVITY -> Activity(id = subject)

        AS.COLLECTION -> Collection<T>(id = subject)

        AS.COLLECTION_PAGE -> CollectionPage<T>(id = subject)

        AS.ARTICLE,
        AS.AUDIO,
        AS.DOCUMENT,
        AS.EVENT,
        AS.IMAGE,
        AS.NOTE,
        AS.PAGE,
        AS.PLACE,
        AS.PROFILE,
        AS.RELATIONSHIP,
        AS.TOMBSTONE,
        AS.VIDEO,
        AS.OBJECT -> Object(id = subject)

        null -> throw Exception("No explicit type given for $subject")

        else -> throw Exception("Unknown type $typeValue for $subject")
    }
}

// Kotlin reflection constraints, see https://discuss.kotlinlang.org/t/should-add-better-support-for-ktype/1495/2
private fun listType(): List<ASObject>? = TODO()

private fun <T, R> activityProp(model: Model, subject: Resource, prop: KProperty1<T, R>): Any? {
    val propFilter = { stmt: Statement -> stmt.subject == subject && stmt.predicate == determinePredicate(prop.name) }

    if (prop.returnType.isSubtypeOf(::listType.returnType)) {
        return model
            .filter(propFilter)
            .map { stmt -> propToNative(model, subject, stmt.`object`) }
    }

    val obj = model.find(propFilter)?.`object`

    return propToNative(model, subject, obj)
}

private fun propToNative(model: Model, subject: Resource, obj: Value?): Any? {
    return when (obj) {
        is Literal -> literalToNative(obj)
        is IRI -> {
            if (obj != subject && Models.subjectIRIs(model).contains(obj))
                modelToObject<ASObject>(model, obj)
            else
                obj
        }
        is BNode -> {
            if (Models.subjectBNodes(model).contains(obj))
                modelToObject<ASObject>(model, obj)
            else
                obj
        }
        else -> obj?.stringValue()
    }
}

internal fun literalToNative(literal: Literal): Any {
    return when (literal.datatype) {
        XMLSchema.INT,
        XMLSchema.INTEGER,
        XMLSchema.UNSIGNED_INT,
        XMLSchema.NON_POSITIVE_INTEGER,
        XMLSchema.NEGATIVE_INTEGER,
        XMLSchema.NON_NEGATIVE_INTEGER,
        XMLSchema.POSITIVE_INTEGER,
        XMLSchema.UNSIGNED_INT
        -> literal.integerValue()
        XMLSchema.BOOLEAN -> literal.booleanValue()
        XMLSchema.SHORT, XMLSchema.UNSIGNED_SHORT -> literal.shortValue()
        XMLSchema.LONG, XMLSchema.UNSIGNED_LONG -> literal.longValue()
        XMLSchema.DECIMAL -> literal.decimalValue()
        XMLSchema.FLOAT -> literal.floatValue()
        XMLSchema.DOUBLE -> literal.doubleValue()
        XMLSchema.DATE, XMLSchema.DATETIME, XMLSchema.TIME -> StdDateFormat().parse(literal.calendarValue().toString())
        else -> literal.stringValue()
    }
}
