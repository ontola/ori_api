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

interface ASObject : JSONLDResource {
    var attachment: String?
    var attributedTo: String?
    var audience: String?
    var content: String?
    var context: String?
    var contentMap: String?
    var name: String?
    var nameMap: String?
    var endTime: String?
    var generator: String?
    var icon: String?
    var image: String?
    var inReplyTo: String?
    var location: String?
    var preview: String?
    var published: Date?
    var replies: String?
    var startTime: String?
    var summary: String?
    var summaryMap: String?
    var tag: String?
    var updated: String?
    var url: String?
    var to: String?
    var bto: String?
    var cc: String?
    var bcc: String?
    var mediaType: String?
    var duration: String?
}

data class Object(
    override var id: Resource? = null,
    override var type: Resource? = AS.OBJECT,

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
) : ASObject
