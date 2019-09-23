package io.kotlintest.provided

import io.kotlintest.AbstractProjectConfig
import io.kotlintest.TestCaseOrder
import io.kotlintest.extensions.TestListener
import io.ontola.testhelpers.KafkaTestListener
import io.ontola.testhelpers.RedisTestListener

object ProjectConfig : AbstractProjectConfig() {
    override fun parallelism(): Int = Runtime.getRuntime().availableProcessors()
    override fun testCaseOrder() = TestCaseOrder.Random

    override fun listeners(): List<TestListener>{
      return listOf(KafkaTestListener, RedisTestListener)
    }
}
