package com.vjaykrsna.nanoai.core.data.library

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.security.MessageDigest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModelArtifactStoreTest {

  private lateinit var context: Context
  private lateinit var store: ModelArtifactStore

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    store = ModelArtifactStore(context)
  }

  @After
  fun tearDown() {
    store.directory().deleteRecursively()
  }

  @Test
  fun `modelFile resides under models directory`() {
    val file = store.modelFile("sample")

    assertThat(file.parentFile).isEqualTo(store.directory())
    assertThat(file.name).isEqualTo("sample.bin")
  }

  @Test
  fun `deleteArtifacts removes matching prefixes`() {
    val keep = File(store.directory(), "other.bin").apply { writeText("keep") }
    val dropBin = store.modelFile("modelA").apply { writeText("drop") }
    val dropPartial = File(store.directory(), "modelA.partial").apply { writeText("drop") }

    store.deleteArtifacts("modelA")

    assertThat(keep.exists()).isTrue()
    assertThat(dropBin.exists()).isFalse()
    assertThat(dropPartial.exists()).isFalse()
  }

  @Test
  fun `checksumForModel returns sha256 when file exists`() {
    val target = store.modelFile("checksum").apply { writeText("nanoai") }

    val checksum = store.checksumForModel("checksum")

    assertThat(checksum).isEqualTo(expectedSha256("nanoai"))
    assertThat(store.checksumForModel("missing")).isNull()
    target.delete()
  }

  @Test
  fun `checksumForFile matches expected digest`() {
    val file = File(store.directory(), "raw.bin").apply { writeText("content") }

    val checksum = store.checksumForFile(file)

    assertThat(checksum).isEqualTo(expectedSha256("content"))
  }

  private fun expectedSha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
  }
}
