/**
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

package io.ontola.ori_api;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;

/**
 * Listens to kafka streams and processes delta streams into the file system
 *
 * TODO: Fix killing the app gracefully (currently hangs indefinitely in the while loop)
 * TODO: Add activity streams to allow viewing which resources have changed
 * TODO: Add error handling service
 */
public class OriAPI
{
    /** The directory data will be stored in. Should generally end with `/id` */
    private static final File BASE_DIRECTORY = new File(System.getProperty("io.ontola.ori_api.root_data_dir", String.format("%s/id", System.getProperty("java.io.tmpdir"))));
    /** The graph IRI used to replace resources entirely */
    private static final String SUPPLANT_GRAPH = System.getProperty("io.ontola.ori_api.supplant_iri", "http://purl.org/link-lib/supplant");

    public static void main( String[] args ) throws NoSuchAlgorithmException
    {
        if (!BASE_DIRECTORY.exists()) {
            BASE_DIRECTORY.mkdirs();
        }

        KafkaConsumer<String, String> consumer = oriDeltaSubscriber();
        System.out.println("Listening for messages?");
        String baseDocument = System.getProperty("io.ontola.ori_api.base_iri", "https://id.openraadsinformatie.nl");

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                RDFParser rdfParser = Rio.createParser(RDFFormat.NQUADS);
                Model deltaEvent = new LinkedHashModel();
                rdfParser.setRDFHandler(new StatementCollector(deltaEvent));
                StringReader streamString = new StringReader(record.value());

                try {
                    System.out.printf("[start][%s] Processing message\n", record.timestamp());
                    rdfParser.parse(streamString, baseDocument);
                    List<Document> documents = partitionDelta(deltaEvent);
                    processDocuments(documents);
                } catch(IOException | RDFParseException | RDFHandlerException e) {
                    System.out.printf("Exception while parsing delta event: '%s'\n", e.toString());
                } finally {
                    streamString.close();
                }
                System.out.printf("[end][%s] Done with message\n", record.timestamp());
            }
        }
    }

    private static KafkaConsumer<String, String> oriDeltaSubscriber() {
        String topic = System.getProperty("io.ontola.ori_api.kafka.topic", "ori-delta");

        String hostname = System.getProperty("io.ontola.ori_api.kafka.hostname", "localhost");
        String port = System.getProperty("io.ontola.ori_api.kafka.port", "9092");
        String address = System.getProperty("io.ontola.ori_api.kafka.address");
        if (address.isEmpty()) {
            address = hostname + ":" + port;
        }

        Properties props = new Properties();
        props.setProperty("bootstrap.servers", address);
        props.setProperty("group.id", System.getProperty("io.ontola.ori_api.kafka.group_id", "ori_api"));
        props.setProperty("enable.auto.commit", "true");
        props.setProperty("auto.commit.interval.ms", "1000");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        System.out.printf("Connecting to kafka on '%s' with group '%s' and topic '%s' \n",
                props.get("bootstrap.servers"),
                props.get("group.id"),
                topic);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(topic));

        return consumer;
    }

    /**
     * Partitions a delta into separate models for processing
     */
    private static List<Document> partitionDelta(Model deltaEvent) throws NoSuchAlgorithmException {
        List<Document> parts = new ArrayList<>();
        // TODO: implement an RDFHandler which does this while parsing
        for (Statement s : deltaEvent) {
            if (!s.getContext().toString().equals(SUPPLANT_GRAPH)) {
                System.out.printf("Expected supplant statement, got %s", s.getContext());
                continue;
            }
            Document target = new Document(s.getSubject().toString(), new LinkedHashModel());
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
            parts.get(index).data.add(s.getSubject(), s.getPredicate(), s.getObject());
        }

        return parts;
    }

    private static void processDocuments(List<Document> models) {
        models.forEach((Document doc) -> {
            File filepath = doc.dir();
            if (!filepath.exists()) {
                filepath.mkdirs();
            }
            // TODO: create activity log for each incoming resource
            // Append create or update action to streamfile
            // Process model
            storeResource(doc);
        });
    }

    private static void storeResource(Document doc) {
        RDFFormat[] formats = {
                RDFFormat.NTRIPLES,
                RDFFormat.N3,
                RDFFormat.NQUADS,
                RDFFormat.TURTLE,
                RDFFormat.JSONLD,
                RDFFormat.RDFJSON,
        };

        System.out.println("Processing " + doc.subject);
        File filepath = doc.dir();
        if (!filepath.exists() && !filepath.mkdirs()) {
            throw new Error(String.format("Couldn't create directory '%s'", filepath));
        }

        for (RDFFormat format : formats) {
            String filename = doc.id + "." + format.getDefaultFileExtension();
            String file = filepath + "/" + filename;
            try {
                RDFWriter rdfWriter = Rio.createWriter(format, new FileOutputStream(file));
                handleNamespaces(rdfWriter);
                if (format == RDFFormat.JSONLD) {
                    WriterConfig jsonldConfig = new WriterConfig();
                    jsonldConfig.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);
                    jsonldConfig.set(JSONLDSettings.USE_NATIVE_TYPES, true);
                    jsonldConfig.set(JSONLDSettings.HIERARCHICAL_VIEW, true);
                    rdfWriter.setWriterConfig(jsonldConfig);
                }
                rdfWriter.startRDF();
                for (Statement s : doc.data.filter(doc.subject, null, null)) {
                    rdfWriter.handleStatement(s);
                }
                rdfWriter.endRDF();
            } catch (FileNotFoundException e) {
                System.out.printf("Couldn't create file '%s' because '%s' \n", file, e.toString());
            }
        }

        String archiveName = doc.id + ".zip";
        File archive = new File(filepath + "/" + archiveName);
        if (archive.exists()) {
            archive.delete();
        }
        ZipUtil.pack(filepath, archive);
        if (ZipUtil.containsEntry(archive, archiveName)) {
            ZipUtil.removeEntry(archive, archiveName);
        }
    }

    private static void handleNamespaces(RDFHandler h) {
//        h.handleNamespace("@vocab", "http://schema.org/");
        h.handleNamespace("schema", "http://schema.org/");
    }
}
