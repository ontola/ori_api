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

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Path

private const val walkDepth = 8

suspend fun cleanOldVersionsAsync() = GlobalScope.async {
    val ctx = ORIContext.getCtx()
    val dataDir = File(ctx.config.getProperty("ori.api.dataDir"))

    dataDir
        .listFiles()
        .map { launch { processDir(it.toPath()) } }
        .forEach { job -> job.join() }
}

fun processDir(dir: Path) {
    val isVersionDir = DocumentSet.versionStringMatcher

    dir
        .toFile()
        .walk()
        .maxDepth(walkDepth)
        .filter { file -> file.list().any { fileName -> isVersionDir.matches(fileName) } }
        .flatMap { file ->
            val versions = file.listFiles()
            val latest =
                versions.find { v -> v.name == "latest" } ?: throw Exception("File ${file.name} has no 'latest' entry")
            val latestVersion = latest.canonicalFile

            sequenceOf<File>(*versions).filter { f -> f != latest && f != latestVersion }
        }
        .forEach { notLatest -> notLatest.deleteRecursively() }
}
