package com.vjaykrsna.nanoai.core.data.library

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Centralizes access to on-device model artifacts so workers and managers use the same paths. */
@Singleton
class ModelArtifactStore @Inject constructor(@ApplicationContext context: Context) {
  private val modelsDirectory: File = File(context.filesDir, MODEL_DIR_NAME).apply { mkdirs() }

  /** Returns the directory that stores all model artifacts. */
  fun directory(): File = modelsDirectory

  /** Resolve the canonical file for the given [modelId]. */
  fun modelFile(modelId: String): File = File(modelsDirectory, "$modelId$MODEL_FILE_SUFFIX")

  /** Delete all artifacts for [modelId] (partial downloads, variants, etc.). */
  fun deleteArtifacts(modelId: String) {
    modelsDirectory
      .listFiles { file -> file.name.startsWith(modelId) }
      ?.forEach { file -> file.delete() }
  }

  /** Compute the SHA-256 checksum for [modelId]'s artifact if it exists. */
  fun checksumForModel(modelId: String): String? {
    val file = modelFile(modelId)
    if (!file.exists()) return null
    return checksumForFile(file)
  }

  /** Compute the SHA-256 checksum for a concrete artifact [file]. */
  fun checksumForFile(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(file).use { input ->
      val buffer = ByteArray(CHECKSUM_BUFFER_SIZE)
      var read: Int
      while (input.read(buffer).also { read = it } != -1) {
        digest.update(buffer, 0, read)
      }
    }
    return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
  }

  companion object {
    private const val MODEL_DIR_NAME = "models"
    private const val MODEL_FILE_SUFFIX = ".bin"
    private const val CHECKSUM_BUFFER_SIZE = 8_192
  }
}
