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

import java.util.*

/**
 * Global store for configuration used across the application. Prevents passing around the same object everywhere.
 */
data class ORIContext(
    internal val config: Properties,
    internal val kafkaOpts: Properties
) {
    companion object {
        private val context: ORIContext

        init {
            val config = initConfig()
            context = ORIContext(config, initKafkaConfig(config))
        }

        fun getCtx(): ORIContext {
            return context
        }
    }
}
