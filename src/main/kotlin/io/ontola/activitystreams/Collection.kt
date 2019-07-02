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
import java.time.Duration
import java.util.*

interface ASCollection<T : ASObject> : ASObject {
    var items: List<T>?
    var totalItems: Number?
    var first: Resource?
    var last: Resource?
    var current: String?
}

data class Collection<T : ASObject>(
    override var id: Resource? = null,
    override var type: Resource? = AS.COLLECTION,

    override var items: List<T>? = null,
    override var totalItems: Number? = null,
    override var first: Resource? = null,
    override var last: Resource? = null,
    override var current: String? = null,

    // From Object
    override var attachment: Collection<ASObject>? = null,
    override var attributedTo: Collection<ASObject>? = null,
    override var audience: ASObject? = null,
    override var content: String? = null,
    override var context: String? = null,
    override var contentMap: Any? = null,
    override var name: String? = null,
    override var nameMap: Any? = null,
    override var endTime: Date? = null,
    override var generator: ASObject? = null,
    override var icon: ASObject? = null,
    override var image: Collection<ASObject>? = null,
    override var inReplyTo: ASObject? = null,
    override var location: ASObject? = null,
    override var preview: ASObject? = null,
    override var published: Date? = null,
    override var replies: Collection<ASObject>? = null,
    override var startTime: Date? = null,
    override var summary: String? = null,
    override var summaryMap: Any? = null,
    override var tag: Collection<ASObject>? = null,
    override var updated: Date? = null,
    override var url: Resource? = null,
    override var to: Resource? = null,
    override var bto: Resource? = null,
    override var cc: Resource? = null,
    override var bcc: Resource? = null,
    override var mediaType: String? = null,
    override var duration: Duration? = null
) : ASCollection<T>
