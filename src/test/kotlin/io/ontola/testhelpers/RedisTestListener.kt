package io.ontola.testhelpers

import io.kotlintest.extensions.TestListener
import io.ontola.ori.api.initConfig
import redis.embedded.RedisServer

object RedisTestListener : TestListener {
  private val config = initConfig()
  private val server = RedisServer(
    Integer.parseInt(config.getProperty("ori.api.redis.port"))
  )

  override fun beforeProject() {
      server.start()
  }

  override fun afterProject() {
    server.stop()
  }
}
