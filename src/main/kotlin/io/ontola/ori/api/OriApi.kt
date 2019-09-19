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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.ontola.ori.api

import io.ontola.ori.api.context.CtxProps
import io.ontola.ori.api.context.DocumentCtx
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.system.exitProcess

/**
 * Listens to kafka streams and processes delta streams into the file system.
 *
 * TODO: Add error handling service
 */
@ExperimentalCoroutinesApi
fun main(args: Array<String>) = runBlocking {
    val ctx = ORIContext.getCtx()

    printInitMessage(ctx.config)

    ensureOutputFolder(ctx.config)

    var primaryFlag = ""
    if (args.isNotEmpty()) {
        primaryFlag = args[0]
    }

    when (primaryFlag) {
        "--clean-old-versions" -> {
            cleanOldVersionsAsync().await()
            exitProcess(0)
        }
        else -> {
            val cmd = arrayListOf("processDeltas", primaryFlag).joinToString(" ")
            val deltas = launch(coroutineContext) {
                processDeltas(DocumentCtx(CtxProps(cmd)), primaryFlag == "--from-beginning")
            }

            val updates = processUpdates()

            joinAll(deltas, updates)
        }
    }

    joinAll()
}

fun ensureOutputFolder(settings: Properties) {
    val baseDirectory = File(settings.getProperty("ori.api.dataDir"))
    if (!baseDirectory.exists()) {
        baseDirectory.mkdirs()
    }
}

fun printInitMessage(p: Properties) {
    println("================================================")
    println("Starting ORI API\n")
    val keys = p.keys()
    while (keys.hasMoreElements()) {
        val key = keys.nextElement() as String
        val value = p.get(key)
        println(key.substring("ori.api.".length) + ": " + value)
    }
    println("================================================")
}

/**
 * Hard check for an MD5 digester.
 * No fallback is used since that could cause inconsistent results when multiple hash methods are mixed.
 */
fun getDigester(): MessageDigest {
    var digester: MessageDigest? = null
    try {
        digester = MessageDigest.getInstance("MD5")
    } catch (e: NoSuchAlgorithmException) {
        println("[FATAL] No MD5 MessageDigest algorithm support, exiting")
        ORIContext.notify(e)
        System.exit(1)
    }

    return digester as MessageDigest
}
