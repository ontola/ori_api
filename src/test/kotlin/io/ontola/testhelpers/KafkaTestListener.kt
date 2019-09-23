package io.ontola.testhelpers

import io.kotlintest.extensions.TestListener
import io.ontola.ori.api.initConfig

object KafkaTestListener : TestListener {
  private val config = initConfig()
  private val server = KafkaEmbedded(
    Integer.parseInt(config.getProperty("ori.api.kafka.port")),
    config.getProperty("ori.api.kafka.hostname")
  )

  override fun beforeProject() {
      server.open()
  }

  override fun afterProject() {
    server.close()
  }
}
