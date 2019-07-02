package io.kotlintest.provided

import io.kotlintest.AbstractProjectConfig
import io.kotlintest.TestCaseOrder

object ProjectConfig : AbstractProjectConfig() {
    override fun parallelism(): Int = Runtime.getRuntime().availableProcessors()
    override fun testCaseOrder() = TestCaseOrder.Random
}
