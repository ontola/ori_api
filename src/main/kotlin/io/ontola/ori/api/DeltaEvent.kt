package io.ontola.ori.api

import com.google.common.base.Splitter

import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.Files
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.impl.LinkedHashModel

class DeltaEvent(val iri: String,
                 private val config: Properties) {

  companion object {
    private val digester: MessageDigest = getDigester()
  }

  private val id: String = iri.substring(iri.lastIndexOf('/') + 1)
  private val hashKeys: Iterable<String>
  private val delta: Model

  init {
    delta = LinkedHashModel()

    val md5sum = digester.digest(id.toByteArray())
    val hashedId = String.format("%032x", BigInteger(1, md5sum))
    hashKeys = Splitter.fixedLength(2).split(hashedId)
  }

  private fun baseDir(): File {
    return File("${config.getProperty("ori.api.dataDir")}/${hashKeys.joinToString("/")}")
  }

  fun deltaAdd(s: Resource, p: IRI, o: Value): Boolean {
    return delta.add(s, p, o)
  }

  fun findLatestDocument(): String {
    val timestampMatcher = Regex("[0-9]{6}")

    return baseDir()
      .list { dir: File, name: String -> dir.isDirectory() && name.matches(timestampMatcher) }
      .sortedArray()
      .first()
  }

  fun process() {
    System.out.printf("Processing deltaevent, %s\n", this.iri)
    ensureDirectoryTree()
//    // TODO: create activity log for each incoming resource
//    File streamsFile = new File(filePath + ".activity.json")
//    if (!streamsFile.exists()) {
//      // Create empty streamfile
//      System.out.println("Resource has no activitystream")
//    }
    // Append create or update action to streamfile
    // Process model
    val versionStamp = SimpleDateFormat("yyyyMMdd'T'hhmm").format(Date())
    val newestVersion = Document(
        this.iri,
        this.delta,
        versionStamp,
        this.baseDir()
    )
    newestVersion.save()

    try {
      val latestDir = File(String.format("%s/%s", this.baseDir(), "latest"))
      Files.deleteIfExists(latestDir.toPath())
      // The link needs to be relative to work across volume mounts
      Files.createSymbolicLink(
          latestDir.toPath(),
          newestVersion.dir().relativeTo(this.baseDir()).toPath()
      )
    } catch (e: IOException) {
      System.out.printf(
          "Error while marking '%s' as latest for resource '%s'; %s\n",
          newestVersion.version,
          this.iri,
          e.message
      )
    }
  }

  private fun ensureDirectoryTree() {
    val filePath = this.baseDir()
    if (!filePath.exists()) {
      val dirPerms = PosixFilePermissions.fromString("rwxr-xr-x")
      try {
        Files.createDirectories(
                filePath.toPath(),
                PosixFilePermissions.asFileAttribute(dirPerms)
        )
      } catch (e: IOException) {
        throw Exception(String.format("Couldn't create hash directory tree '%s'", filePath), e)
      }
    }
  }

  override operator fun equals(other: Any?): Boolean {
    if (other == null || this.javaClass != other.javaClass) {
      return false
    }

    return iri == (other as DeltaEvent).iri
  }
}
