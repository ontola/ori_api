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

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.apache.kafka.common.serialization.StringDeserializer;

class DeltaProcessor implements Runnable {
  private Properties config;
  private static final String deserializerClass = "org.apache.kafka.common.serialization.StringDeserializer";

  DeltaProcessor(Properties config) {
    this.config = config;
  }

  public void run() {
    this.printlnWithThread("Started thread");
    KafkaConsumer<String, String> consumer = oriDeltaSubscriber();
    String baseDocument = config.getProperty("ori.api.baseIRI");

    Thread t = Thread.currentThread();

    while (!t.isInterrupted()) {
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
      for (ConsumerRecord<String, String> record : records) {
        RDFParser rdfParser = Rio.createParser(RDFFormat.NQUADS);
        Model deltaEvent = new LinkedHashModel();
        rdfParser.setRDFHandler(new StatementCollector(deltaEvent));

        try (StringReader streamString = new StringReader(record.value())) {
          this.printlnWithThread("[start][%s] Processing message", record.timestamp());
          rdfParser.parse(streamString, baseDocument);
          partitionDelta(deltaEvent)
            .forEach(DeltaEvent::process);
        } catch (Exception e) {
          this.printlnWithThread("Exception while parsing delta event: '%s'\n", e.toString());
          e.printStackTrace();
        }
        this.printlnWithThread("[end][%s] Done with message\n", record.timestamp());
      }
    }
  }

  private void printlnWithThread(String message, Object... opts) {
    System.out.printf("[%s] %s\n", Thread.currentThread().getName(), String.format(message, opts));
  }

  private KafkaConsumer<String, String> oriDeltaSubscriber() {
    Properties kafkaOpts = new Properties();
    kafkaOpts.setProperty("bootstrap.servers", config.getProperty("ori.api.kafka.address"));
    kafkaOpts.setProperty("group.id", config.getProperty("ori.api.kafka.group_id"));
    kafkaOpts.setProperty("enable.auto.commit", "true");
    kafkaOpts.setProperty("auto.commit.interval.ms", "1000");
    kafkaOpts.setProperty("key.deserializer", DeltaProcessor.deserializerClass);
    kafkaOpts.setProperty("value.deserializer", DeltaProcessor.deserializerClass);
    kafkaOpts.setProperty("request.timeout.ms", "20000");
    kafkaOpts.setProperty("retry.backoff.ms", "500");

    // Force inclusion of the deserializer into the uber jar
    if (StringDeserializer.class == null) {
      System.out.println("StringDeserializer class not found");
    }

    String clusterApiKey = config.getProperty("ori.api.kafka.clusterApiKey");
    String clusterApiSecret = config.getProperty("ori.api.kafka.clusterApiSecret");
    if (clusterApiKey == null || "".equals(clusterApiKey) || clusterApiSecret == null || "".equals(clusterApiSecret)) {
      System.out.println("Either cluster API key or secret was left blank, skipping SASL authentication");
    } else {
      kafkaOpts.setProperty("ssl.endpoint.identification.algorithm", "https");
      kafkaOpts.setProperty("sasl.mechanism", "PLAIN");
      String jaasConfig = String.format(
          "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
          clusterApiKey,
          clusterApiSecret
      );
      kafkaOpts.setProperty("sasl.jaas.config", jaasConfig);
      kafkaOpts.setProperty("security.protocol", "SASL_SSL");
    }

    String topic = config.getProperty("io.ontola.ori.api.kafka.topic", "ori-delta");

    this.printlnWithThread("Connecting to kafka on '%s' with group '%s' and topic '%s' \n",
        kafkaOpts.get("bootstrap.servers"),
        kafkaOpts.get("group.id"),
        topic);
    KafkaConsumer<String, String> consumer;
    try {
      consumer = new KafkaConsumer<>(kafkaOpts);
      consumer.subscribe(Arrays.asList(topic));

      return consumer;
    } catch (KafkaException e) {
      Throwable c = e.getCause();
      String message = c == null ? e.getMessage() : c.getMessage();
      this.printlnWithThread(String.format("[FATAL] Error while creating subscriber: %s", message));
      Thread.currentThread().interrupt();
    }

    return null;
  }

  /**
   * Partitions a delta into separate models for processing.
   */
  private List<DeltaEvent> partitionDelta(Model deltaEvent) {
    List<DeltaEvent> parts = new ArrayList<>();
    // TODO: implement an RDFHandler which does this while parsing
    for (Statement s : deltaEvent) {
      if (!s.getContext().toString().equals(config.getProperty("ori.api.supplantIRI"))) {
        this.printlnWithThread("Expected supplant statement, got %s", s.getContext());
        continue;
      }
      DeltaEvent target = new DeltaEvent(s.getSubject().toString(), config);
      if (!parts.contains(target)) {
        parts.add(target);
      }
      int index = parts.indexOf(target);
      if (index == -1) {
        throw new Error(String.format("Index '%s' out of bounds for '%s' with '%s' items (%s)",
          index,
          s.getSubject().toString(),
          parts.size(),
          parts.get(0).iri));
      }
      parts.get(index).deltaAdd(s.getSubject(), s.getPredicate(), s.getObject());
    }

    return parts;
  }

  private void processDocuments(List<Document> models) {
    models.forEach((Document doc) -> {
      File filePath = doc.dir();
      if (!filePath.exists()) {
        filePath.mkdirs();
      }
      // TODO: create activity log for each incoming resource
      File streamsFile = new File(filePath + ".activity.json");
      if (!streamsFile.exists()) {
        // Create empty streamfile
        this.printlnWithThread("Resource has no activitystream");
      }
      // Append create or update action to streamfile
      // Process model
      doc.save();
    });
  }
}
