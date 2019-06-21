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

import io.ontola.activitystreams.vocabulary.AS
import org.eclipse.rdf4j.model.Resource
import java.util.*

interface ASCollection : ASObject {
    var items: List<Object>?
    var totalItems: Number?
    var first: Resource?
    var last: Resource?
    var current: String?
}

data class Collection(
    override var id: Resource? = null,
    override var type: Resource? = AS.COLLECTION,

    override var items: List<Object>? = null,
    override var totalItems: Number? = null,
    override var first: Resource? = null,
    override var last: Resource? = null,
    override var current: String? = null,

    // From Object
    override var attachment: String? = null,
    override var attributedTo: String? = null,
    override var audience: String? = null,
    override var content: String? = null,
    override var context: String? = null,
    override var contentMap: String? = null,
    override var name: String? = null,
    override var nameMap: String? = null,
    override var endTime: String? = null,
    override var generator: String? = null,
    override var icon: String? = null,
    override var image: String? = null,
    override var inReplyTo: String? = null,
    override var location: String? = null,
    override var preview: String? = null,
    override var published: Date? = null,
    override var replies: String? = null,
    override var startTime: String? = null,
    override var summary: String? = null,
    override var summaryMap: String? = null,
    override var tag: String? = null,
    override var updated: String? = null,
    override var url: String? = null,
    override var to: String? = null,
    override var bto: String? = null,
    override var cc: String? = null,
    override var bcc: String? = null,
    override var mediaType: String? = null,
    override var duration: String? = null
) : ASCollection
