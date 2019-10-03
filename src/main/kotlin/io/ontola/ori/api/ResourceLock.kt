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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.redisson.Redisson
import org.redisson.api.RLock
import java.io.File

private val redis = Redisson.create(ORIContext.getCtx().redis)

class ResourceLock(
    val name: String,
    private val distLock: RLock
) {
    fun lock(owner: Any? = null) : Boolean {
        distLock.lock()

        return true
    }

    fun unlock(owner: Any? = null) : Boolean {
        distLock.unlock()

        return true
    }
}

fun createLock(file: File): ResourceLock {
    val path = file.toPath().toString()
    val rlock = redis.getLock(path)

    return ResourceLock(
        path,
        rlock
    )
}

suspend inline fun <T> ResourceLock.withLock(crossinline action: () -> T): T = withContext(Dispatchers.IO) {
    lock()
    try {
        return@withContext action()
    } finally {
        unlock()
    }
}
