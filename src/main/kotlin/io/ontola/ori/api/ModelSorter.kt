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

import org.eclipse.rdf4j.model.Statement

/**
 * Sorts a Model to output nicely ordered rdf statements.
 *
 * Orders the type statements first, followed by the attributes, and then the associations.
 */
class ModelSorter : Comparator<Statement> {
    override fun compare(p0: Statement, p1: Statement): Int {
        val subject = compareSubject(p0, p1)
        if (subject != 0) {
            return subject
        }

        val type = compareType(p0, p1)
        if (type != 0) {
            return type
        }

        return comparePredicateType(p0, p1)
    }

    private fun compareSubject(p0: Statement, p1: Statement): Int {
        if (p0.subject == p1.subject) {
            return 0
        }

        if (p0.subject.stringValue().startsWith("_:")) {
            return when {
                p1.subject.stringValue().startsWith("_:") -> 0
                else -> -1
            }
        }

        if (p1.subject.stringValue().startsWith("_:")) {
            return 1
        }

        return p0.subject.stringValue().compareTo(p1.subject.stringValue())
    }

    private fun compareType(p0: Statement, p1: Statement): Int {
        val p0IsType = p0.subject.stringValue() == "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
        val p1IsType = p1.subject.stringValue() == "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
        if (p0IsType && p1IsType) {
            return p0.`object`.stringValue().compareTo(p1.`object`.stringValue())
        } else if (p0IsType) {
            return -1
        } else if (p1IsType) {
            return 1
        }

        return 0
    }

    private fun comparePredicateType(p0: Statement, p1: Statement): Int {
        // TODO: check whether object is lit or resource and sort accordingly
        return 0
    }
}
