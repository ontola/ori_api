package io.ontola.ori.api;

import com.google.common.base.Splitter;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;

class DeltaEvent {
  private static MessageDigest digester = getDigester();

  final String iri;

  private final Properties config;
  private final String id;
  private final Iterable<String> hashKeys;
  private Model delta;

  DeltaEvent(String iri, Properties config) {
    this.iri = iri;
    this.config = config;

    this.id = this.iri.substring(this.iri.lastIndexOf('/') + 1);
    this.delta = new LinkedHashModel();

    byte[] md5sum = digester.digest(id.getBytes());
    String hashedId = String.format("%032x", new BigInteger(1, md5sum));
    this.hashKeys = Splitter.fixedLength(2).split(hashedId);
  }

  private File baseDir() {
    String path = String.format(
        "%s/%s",
        config.getProperty("ori.api.dataDir"),
        String.join("/", this.hashKeys)
    );

    return new File(path);
  }

  boolean deltaAdd(Resource s, IRI p, Value o) {
    return this.delta.add(s, p, o);
  }

  String findLatestDocument() {
    String[] documents = this
      .baseDir()
      .list((File dir, String name) -> dir.isDirectory() && name.matches("[0-9]{6}"));
    Arrays.sort(documents);

    return documents[0];
  }

  void process() {
    System.out.printf("Processing deltaevent, %s\n", this.iri);
    File filePath = this.baseDir();
    if (!filePath.exists()) {
      filePath.mkdirs();
    }
//    // TODO: create activity log for each incoming resource
//    File streamsFile = new File(filePath + ".activity.json");
//    if (!streamsFile.exists()) {
//      // Create empty streamfile
//      System.out.println("Resource has no activitystream");
//    }
    // Append create or update action to streamfile
    // Process model
    String versionStamp = new SimpleDateFormat("yyyyMMdd'T'hhmm").format(new Date());
    Document newestVersion = new Document(
        this.iri,
        this.delta,
        versionStamp,
        this.baseDir(),
        this.config
    );
    newestVersion.save();

    try {
      Files.createSymbolicLink(
          new File(String.format("%s/%s", this.baseDir(), "latest")).toPath(),
          newestVersion.dir().toPath()
      );
    } catch (IOException e) {
      System.out.printf(
          "Error while marking '%s' as latest for resource '%s'; %s\n",
          newestVersion.version,
          this.iri,
          e.getMessage()
      );
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DeltaEvent document = (DeltaEvent) o;

    return iri.equals(document.iri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(iri);
  }

  /**
   * Hard check for an MD5 digester.
   * No fallback is used since that could cause inconsistent results when multiple hash methods are mixed.
   */
  private static MessageDigest getDigester() {
    MessageDigest digester = null;
    try {
      digester = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      System.out.println("[FATAL] No MD5 MessageDigest algorithm support, exiting");
      System.exit(1);
    }

    return digester;
  }
}
