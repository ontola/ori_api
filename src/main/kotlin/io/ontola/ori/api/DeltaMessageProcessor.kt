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

import io.ontola.ori.api.context.ResourceCtx

class DeltaMessageProcessor(private val docCtx: ResourceCtx<*>) {
    private val record = docCtx.ctx.record

    fun process() {
        try {
            printlnWithThread("[at:${record?.timestamp()}][start] Processing message")
            val event = Event.parseRecord(docCtx)
            if (event == null || event.type != EventType.DELTA || event.data == null) {
                EventBus.getBus().publishError(docCtx, InvalidEventException("Received invalid event on delta bus"))
                return
            }
            event.process()

            printlnWithThread("[at:%s][end] Done with message\n", record?.timestamp())
        } catch (e: Exception) {
            EventBus.getBus().publishError(docCtx, e)
            printlnWithThread("Exception while processing delta event: '%s'\n", e.toString())
            e.printStackTrace()
        }
    }

    private fun printlnWithThread(message: String, vararg opts: Any?) {
        val msg = String.format(message, *opts)
        val separator = if (msg.startsWith("[")) "" else " "
        val template = "[%s]$separator%s\n"

        System.out.printf(template, Thread.currentThread().name, msg)
    }
}
