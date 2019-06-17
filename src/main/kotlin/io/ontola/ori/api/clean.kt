package io.ontola.ori.api

import kotlinx.coroutines.*
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
