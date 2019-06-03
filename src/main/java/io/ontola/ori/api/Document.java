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

import com.github.jsonldjava.shaded.com.google.common.base.Splitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Properties;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.zeroturnaround.zip.ZipUtil;

/**
 * A resource in the ORI API.
 */
class Document {
  private Properties config;

  final File baseDir;
  final String iri;
  final String id;
  final Resource subject;
  final Model data;
  final String version;


  Document(String iri, Model data, String version, File baseDir, Properties config) {
    this.iri = iri;
    this.data = data;
    this.version = version;
    this.config = config;

    this.id = this.iri.substring(this.iri.lastIndexOf('/') + 1);
    this.subject = SimpleValueFactory.getInstance().createIRI(iri);
    this.baseDir = baseDir;
  }

  File dir() {
    return new File(String.format("%s/%s", baseDir.getAbsolutePath(), this.version));
  }


  void save() {
    RDFFormat[] formats = {
      RDFFormat.NTRIPLES,
      RDFFormat.N3,
      RDFFormat.NQUADS,
      RDFFormat.TURTLE,
      RDFFormat.JSONLD,
      RDFFormat.RDFJSON,
    };

    System.out.printf("Writing subject '%s' with version '%s'\n", this.subject, this.version);
    File filepath = this.dir();
    if (!filepath.exists() && !filepath.mkdirs()) {
      throw new Error(String.format("Couldn't create directory '%s'", filepath));
    }

    for (RDFFormat format : formats) {
      String filename = this.id + "." + format.getDefaultFileExtension();
      String file = filepath + "/" + filename;
      try {
        RDFWriter rdfWriter = Rio.createWriter(format, new FileOutputStream(file));
        this.handleNamespaces(rdfWriter);
        if (format == RDFFormat.JSONLD) {
          WriterConfig jsonldConfig = new WriterConfig();
          jsonldConfig.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);
          jsonldConfig.set(JSONLDSettings.USE_NATIVE_TYPES, true);
          jsonldConfig.set(JSONLDSettings.HIERARCHICAL_VIEW, true);
          rdfWriter.setWriterConfig(jsonldConfig);
        }
        rdfWriter.startRDF();
        for (Statement s : this.data.filter(this.subject, null, null)) {
          rdfWriter.handleStatement(s);
        }
        rdfWriter.endRDF();
      } catch (FileNotFoundException e) {
        System.out.printf("Couldn't create file '%s' because '%s' \n", file, e.toString());
      }
    }

    String archiveName = this.id + ".zip";
    File archive = new File(filepath + "/" + archiveName);
    if (archive.exists()) {
      archive.delete();
    }
    ZipUtil.pack(filepath, archive);
    if (ZipUtil.containsEntry(archive, archiveName)) {
      ZipUtil.removeEntry(archive, archiveName);
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
    Document document = (Document) o;

    return iri.equals(document.iri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(iri);
  }

  private void handleNamespaces(RDFHandler h) {
    // h.handleNamespace("@vocab", "http://schema.org/");
    h.handleNamespace("schema", "http://schema.org/");
  }
}
