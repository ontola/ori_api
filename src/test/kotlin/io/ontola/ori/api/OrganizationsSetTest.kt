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

import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.file.shouldBeADirectory
import io.kotlintest.matchers.file.shouldContainFile
import io.kotlintest.matchers.file.shouldExist
import io.kotlintest.matchers.file.shouldNotExist
import io.kotlintest.specs.StringSpec
import io.ontola.activitystreams.Object
import io.ontola.activitystreams.vocabulary.AS
import io.ontola.rdf4j.iri
import java.io.File

class OrganizationsSetTest : StringSpec({
    fun transientTempDir(): File {
        val dir = createTempDir()

        return File("${dir.toPath()}/coll")
    }

    "should load empty" {
        val orgSet = OrganizationsSet(transientTempDir())
        val dir = orgSet.collectionDir

        orgSet shouldHaveSize 0
        dir.shouldNotExist()
    }

    "should add resources" {
        val orgSet = OrganizationsSet(transientTempDir())
        val dir = orgSet.collectionDir

        orgSet.add("http://id.openraadsinformatie.nl/1010".iri())
        orgSet.add("http://id.openraadsinformatie.nl/1011".iri())

        orgSet shouldHaveSize 2
        dir.shouldNotExist()

        orgSet.save()
        dir.shouldExist()
        dir.shouldBeADirectory()

        dir shouldContainFile "organizations.activity.json"
        dir shouldContainFile "organizations.nq"

        dir shouldContainFile "organizations;page=1.activity.json"
        dir shouldContainFile "organizations;page=1.nq"

        dir shouldContainFile "index.activity.json"
        dir shouldContainFile "index.nq"
        dir shouldContainFile "first.activity.json"
        dir shouldContainFile "first.nq"
        dir shouldContainFile "last.activity.json"
        dir shouldContainFile "last.nq"

        val parentDir = File("$dir/..").canonicalFile

        parentDir shouldContainFile "organizations.activity.json"
        parentDir shouldContainFile "organizations.nq"
    }

    "should load from disk" {
        val dir = transientTempDir()
        val orgSet = OrganizationsSet(dir)

        orgSet.add("http://id.openraadsinformatie.nl/1020".iri())
        orgSet.add("http://id.openraadsinformatie.nl/1021".iri())

        orgSet.save()

        val loadedSet = OrganizationsSet(dir)

        loadedSet shouldHaveSize 2
        loadedSet shouldContain Object(id = "http://id.openraadsinformatie.nl/1020".iri(), type = AS.DOCUMENT)
        loadedSet shouldContain Object(id = "http://id.openraadsinformatie.nl/1021".iri(), type = AS.DOCUMENT)
    }

    "should be updated from disk" {
        val dir = transientTempDir()
        val orgSet = OrganizationsSet(dir)

        orgSet.add("http://id.openraadsinformatie.nl/1030".iri())
        orgSet.add("http://id.openraadsinformatie.nl/1031".iri())

        orgSet.save()

        val loadedSet = OrganizationsSet(dir)

        loadedSet.add("http://id.openraadsinformatie.nl/1032".iri())
        loadedSet.add("http://id.openraadsinformatie.nl/1033".iri())
        loadedSet.add("http://id.openraadsinformatie.nl/1034".iri())

        loadedSet.save()

        val reloadedSet = OrganizationsSet(dir)

        reloadedSet shouldHaveSize 5
        loadedSet shouldContain Object(id = "http://id.openraadsinformatie.nl/1030".iri(), type = AS.DOCUMENT)
        loadedSet shouldContain Object(id = "http://id.openraadsinformatie.nl/1031".iri(), type = AS.DOCUMENT)
        loadedSet shouldContain Object(id = "http://id.openraadsinformatie.nl/1032".iri(), type = AS.DOCUMENT)
        loadedSet shouldContain Object(id = "http://id.openraadsinformatie.nl/1033".iri(), type = AS.DOCUMENT)
        loadedSet shouldContain Object(id = "http://id.openraadsinformatie.nl/1034".iri(), type = AS.DOCUMENT)
    }
})
