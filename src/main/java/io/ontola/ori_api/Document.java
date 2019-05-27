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

import com.github.jsonldjava.shaded.com.google.common.base.Splitter;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * A resource in the ORI API
 */
public class Document {
    private static final File BASE_DIRECTORY = new File(System.getProperty("java.io.tmpdir") + "/id");

    public final String iri;
    public final String id;
    public final String hashedId;
    public final Resource subject;
    public final Iterable<String> hashKeys;
    public final Model data;

    Document(String iri, Model data) throws NoSuchAlgorithmException {
        this.iri = iri;
        this.data = data;

        this.id = this.iri.substring(this.iri.lastIndexOf('/') + 1);
        this.subject = SimpleValueFactory.getInstance().createIRI(iri);
        byte[] md5sum = MessageDigest.getInstance("MD5").digest(id.getBytes());
        this.hashedId = String.format("%032x", new BigInteger(1, md5sum));
        this.hashKeys = Splitter.fixedLength(2).split(this.hashedId);
    }

    public File dir() {
        return new File(BASE_DIRECTORY + "/" + String.join("/", this.hashKeys));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return iri.equals(document.iri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iri);
    }
}
