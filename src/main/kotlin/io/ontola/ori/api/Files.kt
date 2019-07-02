/*
 * ORI API
 * Copyright (C), Argu BV
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.ontola.ori.api

import org.eclipse.rdf4j.rio.RDFFormat
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.PosixFilePermissions

internal fun ensureDirectoryTree(filePath: File) {
    if (!filePath.exists()) {
        val dirPerms = PosixFilePermissions.fromString("rwxr-xr-x")
        try {
            Files.createDirectories(
                filePath.toPath(),
                PosixFilePermissions.asFileAttribute(dirPerms)
            )
        } catch (e: IOException) {
            throw Exception("Couldn't create hash directory tree '$filePath'", e)
        }
    }
}

/** Retrieve a dot-prefixed file extension for an RDFFormat if available */
internal fun formatExtension(format: RDFFormat?): String {
    if (format == null || format.defaultFileExtension == null) {
        return ""
    }

    return ".${format.defaultFileExtension}"
}

internal fun recreateSymbolicLink(
    link: Path,
    target: Path,
    vararg attrs: FileAttribute<*>?
) {
    val from = link.toFile()
    if (from.exists()) {
        from.delete() || throw Exception("Couldn't delete file $from")
    }
    Files.createSymbolicLink(link, target, *attrs)
}
