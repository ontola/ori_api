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

package io.ontola.activitystreams.collection.filesystem

import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.file.beNonEmptyDirectory
import io.kotlintest.matchers.file.exist
import io.kotlintest.matchers.file.shouldHaveName
import io.kotlintest.matchers.haveSize
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldNot
import io.kotlintest.specs.WordSpec
import io.ontola.activitystreams.ASObject
import io.ontola.activitystreams.Object
import io.ontola.rdfUtils.createIRI
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.rio.RDFFormat
import java.io.File
import java.nio.file.Files

class PaginatedFileSystemCollectionTest : WordSpec({
    "empty collection" should {
        val col = getCollection()

        "have no items" {
            col.items shouldHaveSize 0
        }

        "have no pages" {
            col.findPages(RDFFormat.NQUADS) shouldBe emptyArray()
        }

        "save successfully" {
            col.save()

            col.location should beNonEmptyDirectory()

            col.collectionFile(RDFFormat.NQUADS) should exist()
            col.collectionFile(RDFFormat.NQUADS) shouldHaveName "testColl.nq"

            col.page(1).toFile() shouldNot exist()
        }
    }

    "single-page collection" should {
        val factory = SimpleValueFactory.getInstance()
        val col = getCollection()
        col.add(Object(factory.createBNode()))
        col.add(Object(factory.createBNode()))

        "have items" {
            col.items shouldHaveSize 2
        }

        "have no pages before save" {
            col.findPages(RDFFormat.NQUADS).toList() should haveSize(0)
        }

        "save successfully" {
            col.save()

            col.location should beNonEmptyDirectory()

            col.collectionFile(RDFFormat.NQUADS) should exist()
            col.collectionFile(RDFFormat.NQUADS) shouldHaveName "testColl.nq"

            col.page(1).toFile() shouldNot exist()
        }

        "have a single page after save" {
            col.findPages(RDFFormat.NQUADS).toList() should haveSize(1)
        }

        "be loaded correctly" {
            val newCol = getCollection(col.location)

            newCol.items shouldHaveSize 0
            newCol.loadWithType()
            newCol.items shouldHaveSize 2
            newCol.items.map { i -> i.id }.toSet() shouldHaveSize 2
        }
    }
})

fun testDir(): File {
    return Files.createTempDirectory("PaginatedFileSystemCollectionTest").toFile()
}

fun getCollection(location: File = testDir()): PaginatedFilesystemCollection<ASObject> {
    val tFactory = { _: Int -> Object() }

    val items = MutableList<ASObject>(0, tFactory)
    return PaginatedFilesystemCollection(
        tFactory,
        { createIRI("http://example.com/") },
        "testColl",
        location,
        items = items
    )
}
