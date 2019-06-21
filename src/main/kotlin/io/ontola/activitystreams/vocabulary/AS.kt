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
import org.eclipse.rdf4j.model.Namespace
import org.eclipse.rdf4j.model.impl.SimpleNamespace

/**
 * Constants for RDF primitives and for the ActivityStreams 2.0 Core namespace.
 *
 * @see [ActivityStreams 2.0 Vocabulary Specification](https://www.w3.org/TR/activitystreams-vocabulary/#types)
 */
object AS {
    const val NAMESPACE = "https://www.w3.org/ns/activitystreams#"

    /**
     * Recommended prefix for the ActivityStreams namespace: "as"
     */
    const val PREFIX = "as"

    val ACTOR = createIRI(NAMESPACE, "actor")

    val ATTRIBUTED_TO = createIRI(NAMESPACE, "attributedTo")

    val ATTACHMENT = createIRI(NAMESPACE, "attachment")

    val ATTACHMENTS = createIRI(NAMESPACE, "attachments")

    val AUTHOR = createIRI(NAMESPACE, "author")

    val BCC = createIRI(NAMESPACE, "bcc")

    val BTO = createIRI(NAMESPACE, "bto")

    val CC = createIRI(NAMESPACE, "cc")

    val CONTEXT = createIRI(NAMESPACE, "context")

    val CURRENT = createIRI(NAMESPACE, "current")

    val FIRST = createIRI(NAMESPACE, "first")

    val GENERATOR = createIRI(NAMESPACE, "generator")

    val ICON = createIRI(NAMESPACE, "icon")

    val IMAGE_PROP = createIRI(NAMESPACE, "image")

    val IN_REPLY_TO = createIRI(NAMESPACE, "inReplyTo")

    val ITEMS = createIRI(NAMESPACE, "items")

    val LAST = createIRI(NAMESPACE, "last")

    val LOCATION = createIRI(NAMESPACE, "location")

    val NEXT = createIRI(NAMESPACE, "next")

    val OBJECT_PROP = createIRI(NAMESPACE, "object")

    val ONEOF = createIRI(NAMESPACE, "oneOf")

    val ANY_OF = createIRI(NAMESPACE, "anyOf")

    val PREV = createIRI(NAMESPACE, "prev")

    val PREVIEW = createIRI(NAMESPACE, "preview")

    val PROVIDER = createIRI(NAMESPACE, "provider")

    val REPLIES = createIRI(NAMESPACE, "replies")

    val RESULT = createIRI(NAMESPACE, "result")

    val AUDIENCE = createIRI(NAMESPACE, "audience")

    val PART_OF = createIRI(NAMESPACE, "partOf")

    val TAG = createIRI(NAMESPACE, "tag")

    val TAGS = createIRI(NAMESPACE, "tags")

    val TARGET = createIRI(NAMESPACE, "target")

    val ORIGIN = createIRI(NAMESPACE, "origin")

    val INSTRUMENT = createIRI(NAMESPACE, "instrument")

    val TO = createIRI(NAMESPACE, "to")

    val URL = createIRI(NAMESPACE, "url")

    val SUBJECT = createIRI(NAMESPACE, "subject")

    val RELATIONSHIP_PROP = createIRI(NAMESPACE, "relationship")

    val DESCRIBES = createIRI(NAMESPACE, "describes")

    val FORMER_TYPE = createIRI(NAMESPACE, "formerType")

    val ACCURACY = createIRI(NAMESPACE, "accuracy")

    val ALTITUDE = createIRI(NAMESPACE, "altitude")

    val CONTENT = createIRI(NAMESPACE, "content")

    val NAME = createIRI(NAMESPACE, "name")

    val DOWNSTREAM_DUPLICATES = createIRI(NAMESPACE, "downstreamDuplicates")

    val DURATION = createIRI(NAMESPACE, "duration")

    val END_TIME = createIRI(NAMESPACE, "endTime")

    val HEIGHT = createIRI(NAMESPACE, "height")

    val HREF = createIRI(NAMESPACE, "href")

    val HREFLANG = createIRI(NAMESPACE, "hreflang")

    val ID = createIRI(NAMESPACE, "id")

    val LATITUDE = createIRI(NAMESPACE, "latitude")

    val LONGITUDE = createIRI(NAMESPACE, "longitude")

    val MEDIA_TYPE = createIRI(NAMESPACE, "mediaType")

    val OBJECT_TYPE = createIRI(NAMESPACE, "objectType")

    val PUBLISHED = createIRI(NAMESPACE, "published")

    val RADIUS = createIRI(NAMESPACE, "radius")

    val RATING = createIRI(NAMESPACE, "rating")

    val REL = createIRI(NAMESPACE, "rel")

    val START_INDEX = createIRI(NAMESPACE, "startIndex")

    val START_TIME = createIRI(NAMESPACE, "startTime")

    val SUMMARY = createIRI(NAMESPACE, "summary")

    val TOTAL_ITEMS = createIRI(NAMESPACE, "totalItems")

    val UNITS = createIRI(NAMESPACE, "units")

    val UPDATED = createIRI(NAMESPACE, "updated")

    val UPSTREAM_DUPLICATES = createIRI(NAMESPACE, "upstreamDuplicates")

    val VERB = createIRI(NAMESPACE, "verb")

    val WIDTH = createIRI(NAMESPACE, "width")

    val DELETED = createIRI(NAMESPACE, "deleted")

    val ACCEPT = createIRI(NAMESPACE, "Accept")

    val ACTIVITY = createIRI(NAMESPACE, "Activity")

    val BLOCK = createIRI(NAMESPACE, "Block")

    val INTRANSITIVE_ACTIVITY = createIRI(NAMESPACE, "IntransitiveActivity")

    val ADD = createIRI(NAMESPACE, "Add")

    val ANNOUNCE = createIRI(NAMESPACE, "Announce")

    val APPLICATION = createIRI(NAMESPACE, "Application")

    val ARRIVE = createIRI(NAMESPACE, "Arrive")

    val ARTICLE = createIRI(NAMESPACE, "Article")

    val AUDIO = createIRI(NAMESPACE, "Audio")

    val COLLECTION = createIRI(NAMESPACE, "Collection")

    val COLLECTION_PAGE = createIRI(NAMESPACE, "CollectionPage")

    val ORDERED_COLLECTION_PAGE = createIRI(NAMESPACE, "OrderedCollectionPage")

    val RELATIONSHIP = createIRI(NAMESPACE, "Relationship")

    val CREATE = createIRI(NAMESPACE, "Create")

    val DELETE = createIRI(NAMESPACE, "Delete")

    val DISLIKE = createIRI(NAMESPACE, "Dislike")

    val DOCUMENT = createIRI(NAMESPACE, "Document")

    val EVENT = createIRI(NAMESPACE, "Event")

    val FLAG = createIRI(NAMESPACE, "Flag")

    val FOLLOW = createIRI(NAMESPACE, "Follow")

    val GROUP = createIRI(NAMESPACE, "Group")

    val IGNORE = createIRI(NAMESPACE, "Ignore")

    val IMAGE = createIRI(NAMESPACE, "Image")

    val INVITE = createIRI(NAMESPACE, "Invite")

    val JOIN = createIRI(NAMESPACE, "Join")

    val LEAVE = createIRI(NAMESPACE, "Leave")

    val LIKE = createIRI(NAMESPACE, "Like")

    val VIEW = createIRI(NAMESPACE, "View")

    val LISTEN = createIRI(NAMESPACE, "Listen")

    val READ = createIRI(NAMESPACE, "Read")

    val MOVE = createIRI(NAMESPACE, "Move")

    val TRAVEL = createIRI(NAMESPACE, "Travel")

    val LINK = createIRI(NAMESPACE, "Link")

    val MENTION = createIRI(NAMESPACE, "Mention")

    val NOTE = createIRI(NAMESPACE, "Note")

    val OBJECT = createIRI(NAMESPACE, "Object")

    val OFFER = createIRI(NAMESPACE, "Offer")

    val ORDERED_COLLECTION = createIRI(NAMESPACE, "OrderedCollection")

    val ORDERED_ITEMS = createIRI(NAMESPACE, "OrderedItems")

    val PAGE = createIRI(NAMESPACE, "Page")

    val PERSON = createIRI(NAMESPACE, "Person")

    val ORGANIZATION = createIRI(NAMESPACE, "Organization")

    val PROFILE = createIRI(NAMESPACE, "Profile")

    val PLACE = createIRI(NAMESPACE, "Place")

    val QUESTION = createIRI(NAMESPACE, "Question")

    val REJECT = createIRI(NAMESPACE, "Reject")

    val REMOVE = createIRI(NAMESPACE, "Remove")

    val SERVICE = createIRI(NAMESPACE, "Service")

    val TENTATIVE_ACCEPT = createIRI(NAMESPACE, "TentativeAccept")

    val TENTATIVE_REJECT = createIRI(NAMESPACE, "TentativeReject")

    val TOMBSTONE = createIRI(NAMESPACE, "Tombstone")

    val UNDO = createIRI(NAMESPACE, "Undo")

    val UPDATE = createIRI(NAMESPACE, "Update")

    val VIDEO = createIRI(NAMESPACE, "Video")

    /**
     * An immutable [Namespace] constant that represents the RDF namespace.
     */
    val NS: Namespace = SimpleNamespace(PREFIX, NAMESPACE)
}
