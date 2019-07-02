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
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.redisson.Redisson
import org.redisson.api.RLock
import java.io.File
import java.util.*

private val rLockMap: WeakHashMap<String, Mutex> = WeakHashMap()

private val redis = Redisson.create(ORIContext.getCtx().redis)

class ResourceLock(
    val name: String,
    private val lock: Mutex,
    private val distLock: RLock
) : Mutex by lock {
    override suspend fun lock(owner: Any?) {
        lock.lock(owner)
        distLock.lock()
    }

    override fun unlock(owner: Any?) {
        lock.unlock(owner)
        distLock.unlock()
    }
}

fun createLock(file: File): Mutex {
    val path = file.toPath().toString()

    val mutex = rLockMap.computeIfAbsent(path) {
        Mutex()
    }

    return ResourceLock(
        path,
        mutex,
        redis.getLock(path)
    )
}

suspend inline fun <T> Mutex.withLock(crossinline action: () -> T): T = withContext(Dispatchers.IO) {
    async { lock() }.join()
    try {
        return@withContext action()
    } finally {
        unlock()
    }
}
