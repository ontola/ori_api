/*
 * ORI API
 * Copyright (C) 2019, Argu BV
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

package io.ontola.ori.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Listens to kafka streams and processes delta streams into the file system.
 *
 * TODO: Add activity streams to allow viewing which resources have changed
 * TODO: Add error handling service
 */
public class OriApi {

  /** Starts listening to kafka and process incoming messages. */
  public static void main(String[] args) {
    System.out.println("Starting ORI API");
    Properties config = initConfig();

    ensureOutputFolder(config);

    int threadCount = Integer.parseInt(System.getProperty("io.ontola.ori.api.threads", "1"));
    ExecutorService s = Executors.newFixedThreadPool(threadCount);
    List<Future> tasks = new ArrayList<>();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      s.shutdown();
      tasks.forEach((t) -> t.cancel(true));
    }));

    DeltaProcessor p = new DeltaProcessor(config);
    tasks.add(s.submit(p));
  }

  private static void ensureOutputFolder(Properties settings) {
    File baseDirectory = new File(settings.getProperty("ori.api.dataDir"));
    if (!baseDirectory.exists()) {
      baseDirectory.mkdirs();
    }
  }

  private static Properties initConfig() {
    Properties config = new Properties();

    config.setProperty(
        "ori.api.dataDir",
        System.getProperty(
            "io.ontola.ori.api.dataDir",
            String.format("%s/id", System.getProperty("java.io.tmpdir"))
        )
    );
    config.setProperty(
        "ori.api.baseIRI",
        System.getProperty("io.ontola.ori.api.baseIRI", "https://id.openraadsinformatie.nl")
    );
    config.setProperty(
        "ori.api.supplantIRI",
        System.getProperty("io.ontola.ori.api.supplantIRI", "http://purl.org/link-lib/supplant")
    );
    config.setProperty(
        "ori.api.kafka.group_id",
        System.getProperty("io.ontola.ori.api.kafka.group_id", "ori_api")
    );
    config.setProperty(
        "ori.api.kafka.hostname",
        System.getProperty("io.ontola.ori.api.kafka.hostname", "localhost")
    );
    config.setProperty(
        "ori.api.kafka.port",
        System.getProperty("io.ontola.ori.api.kafka.port", "9092")
    );
    config.setProperty(
        "ori.api.kafka.hostname",
        System.getProperty("io.ontola.ori.api.kafka.topic", "ori-delta")
    );
    String hostname = config.getProperty("ori.api.kafka.hostname");
    String port = config.getProperty("ori.api.kafka.port");
    String address = config.getProperty("ori.api.kafka.address");
    if (address == null || address.isEmpty()) {
      address = hostname + ":" + port;
    }
    config.setProperty("ori.api.kafka.address", address);

    return config;
  }
}
