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
import java.io.File
import java.nio.file.Path

private const val walkDepth = 8

fun cleanOldVersionsAsync() = GlobalScope.async {
    val ctx = ORIContext.getCtx()
    val dataDir = File(ctx.config.getProperty("ori.api.dataDir"))

    println("Starting cleaning old versions")
    dataDir
        .walk()
        .maxDepth(1)
        .forEach {
            try {
                if (it != dataDir) {
                    processDir(it.toPath())
                }
            } catch(e: Exception) {
                System.out.printf("$e\n")
                e.printStackTrace()
            }
        }

    println("Finished cleaning files")
}

fun processDir(dir: Path) {
    println("Cleaning $dir")
    val isVersionDir = DocumentSet.versionStringMatcher

    dir
        .toFile()
        .walk()
        .maxDepth(walkDepth)
        .filter { file -> file.list()?.any { fileName -> isVersionDir.matches(fileName) } ?: false }
        .flatMap { file ->
            val versions = file.listFiles()
            val latest = versions?.find { v -> v.name == "latest" }
            val latestVersion = latest?.canonicalFile

            sequenceOf<File>(*versions).filter { f -> f != latest && f != latestVersion }
        }
        .forEach { notLatest -> notLatest.deleteRecursively() }
}
