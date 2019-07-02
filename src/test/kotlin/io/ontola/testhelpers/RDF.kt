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

package io.ontola.testhelpers

import io.ontola.rdf4j.ResourceModel
import io.ontola.rdf4j.model
import io.ontola.rdfUtils.createIRI

fun testModel(id: String, init: (ResourceModel.() -> Unit)): ResourceModel {
    return model {
        iri = createIRI("http://id.openraadsinformatie.nl/$id")
        defaultContext = createIRI("http://purl.org/link-lib/supplant")

        init()
    }
}
