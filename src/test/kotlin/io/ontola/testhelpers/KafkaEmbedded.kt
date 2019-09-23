package io.ontola.testhelpers

import org.apache.kafka.common.utils.Time
import java.io.Closeable
import java.nio.file.Files
import java.util.*
import kafka.admin.AdminUtils
import kafka.admin.RackAwareMode
import kafka.server.KafkaConfig
import kafka.server.KafkaServer
import kafka.utils.TestUtils
import kafka.utils.`ZKStringSerializer$`
import kafka.utils.ZkUtils
import kafka.zk.EmbeddedZookeeper
import org.I0Itec.zkclient.ZkClient
import scala.Option
import scala.collection.JavaConversions

/**
 * From https://gist.github.com/asmaier/6465468#gistcomment-2730120
 */
class KafkaEmbedded(port: Int, private val topic: String) : Closeable {

    private val server: KafkaServer
    private lateinit var zkClient: ZkClient
    private val zkServer: EmbeddedZookeeper = EmbeddedZookeeper()
    private val zkConnect = "127.0.0.1:${zkServer.port()}"

    init {

        val props = Properties()
        props.setProperty("zookeeper.connect", zkConnect)
        props.setProperty("broker.id", "0")
        props.setProperty("log.dirs", Files.createTempDirectory("kafka-").toAbsolutePath().toString())
        props.setProperty("listeners", "PLAINTEXT://127.0.0.1:$port")
        props.setProperty("offsets.topic.replication.factor", "1")

        server = KafkaServer(KafkaConfig(props), Time.SYSTEM, Option.apply("kafka-broker"), JavaConversions.asScalaBuffer(emptyList()))
    }

    fun open() {
        server.startup()

        zkClient = ZkClient(zkConnect, 30000, 30000, `ZKStringSerializer$`.`MODULE$`)
        val zkUtils = ZkUtils.apply(zkClient, false)
        AdminUtils.createTopic(zkUtils, topic, 1, 1, Properties(), RackAwareMode.`Disabled$`.`MODULE$`)

        TestUtils.waitUntilMetadataIsPropagated(scala.collection.JavaConversions.asScalaBuffer(listOf(server)), topic, 0, 5000);
    }

    override fun close() {
        server.shutdown()
        server.awaitShutdown()
        zkClient.close()
        zkServer.shutdown()
    }
}
