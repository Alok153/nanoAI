package com.vjaykrsna.nanoai.core.data.library.impl

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.vjaykrsna.nanoai.core.data.library.ModelArtifactStore
import com.vjaykrsna.nanoai.core.data.library.daos.DownloadTaskDao
import com.vjaykrsna.nanoai.core.data.library.entities.DownloadTaskEntity
import com.vjaykrsna.nanoai.core.domain.model.library.DownloadStatus
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class DownloadManagerImplTest {

  @MockK(relaxed = true) private lateinit var downloadTaskDao: DownloadTaskDao
  @MockK(relaxed = true) private lateinit var workManager: WorkManager

  private lateinit var context: Context
  private lateinit var filesDir: File
  private lateinit var artifactStore: ModelArtifactStore
  private lateinit var subject: DownloadManagerImpl

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this)
    filesDir = createTempDirectory(prefix = "download-manager-test").toFile()
    context = mockk(relaxed = true)
    every { context.filesDir } returns filesDir
    artifactStore = ModelArtifactStore(context)
    subject = DownloadManagerImpl(downloadTaskDao, workManager, artifactStore)
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    filesDir.deleteRecursively()
  }

  @Test
  fun startDownload_enqueuesWorkAndPersistsTask() = runTest {
    val entitySlot = slot<DownloadTaskEntity>()
    val uniqueSlot = slot<String>()
    val policySlot = slot<ExistingWorkPolicy>()
    val requestSlot = slot<OneTimeWorkRequest>()

    coEvery { downloadTaskDao.insert(capture(entitySlot)) } returns Unit
    every {
      workManager.enqueueUniqueWork(capture(uniqueSlot), capture(policySlot), capture(requestSlot))
    } returns mockk(relaxed = true)

    val taskId = subject.startDownload("modelA")

    assertEquals(taskId.toString(), entitySlot.captured.taskId)
    assertEquals("modelA", entitySlot.captured.modelId)
    assertEquals(DownloadStatus.QUEUED, entitySlot.captured.status)
    assertEquals(0f, entitySlot.captured.progress)
    assertNotNull(entitySlot.captured.startedAt)
    assertNull(entitySlot.captured.errorMessage)

    assertEquals("download_$taskId", uniqueSlot.captured)
    assertEquals(ExistingWorkPolicy.KEEP, policySlot.captured)
    val input = requestSlot.captured.workSpec.input
    assertEquals(taskId.toString(), input.getString("TASK_ID"))
    assertEquals("modelA", input.getString("MODEL_ID"))
    assertTrue(requestSlot.captured.tags.contains("download_$taskId"))
  }

  @Test
  fun queueDownload_insertsQueuedTask() = runTest {
    val entitySlot = slot<DownloadTaskEntity>()
    coEvery { downloadTaskDao.insert(capture(entitySlot)) } returns Unit

    val taskId = subject.queueDownload("modelB")

    assertEquals(taskId.toString(), entitySlot.captured.taskId)
    assertEquals("modelB", entitySlot.captured.modelId)
    assertEquals(DownloadStatus.QUEUED, entitySlot.captured.status)
    assertEquals(0f, entitySlot.captured.progress)
    assertNotNull(entitySlot.captured.startedAt)
    assertNull(entitySlot.captured.errorMessage)
  }

  @Test
  fun pauseDownload_cancelsWorkAndUpdatesStatus() = runTest {
    val taskId = UUID.randomUUID()

    subject.pauseDownload(taskId)

    verify { workManager.cancelAllWorkByTag("download_$taskId") }
    coVerify { downloadTaskDao.updateStatus(taskId.toString(), DownloadStatus.PAUSED) }
  }

  @Test
  fun resumeDownload_updatesStatusAndRequeuesWhenTaskExists() = runTest {
    val taskId = UUID.randomUUID()
    val entity = sampleEntity(taskId = taskId, modelId = "modelC", status = DownloadStatus.PAUSED)
    coEvery { downloadTaskDao.getById(taskId.toString()) } returns entity

    val uniqueSlot = slot<String>()
    val policySlot = slot<ExistingWorkPolicy>()
    every {
      workManager.enqueueUniqueWork(
        capture(uniqueSlot),
        capture(policySlot),
        any<OneTimeWorkRequest>(),
      )
    } returns mockk(relaxed = true)

    subject.resumeDownload(taskId)

    coVerify { downloadTaskDao.updateStatus(taskId.toString(), DownloadStatus.DOWNLOADING) }
    assertEquals("download_$taskId", uniqueSlot.captured)
    assertEquals(ExistingWorkPolicy.REPLACE, policySlot.captured)
  }

  @Test
  fun resumeDownload_withMissingTaskDoesNothing() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadTaskDao.getById(taskId.toString()) } returns null

    subject.resumeDownload(taskId)

    verify(exactly = 0) {
      workManager.enqueueUniqueWork(
        any<String>(),
        any<ExistingWorkPolicy>(),
        any<OneTimeWorkRequest>(),
      )
    }
    coVerify(exactly = 0) {
      downloadTaskDao.updateStatus(taskId.toString(), DownloadStatus.DOWNLOADING)
    }
  }

  @Test
  fun cancelDownload_cancelsWorkAndMarksCancelled() = runTest {
    val taskId = UUID.randomUUID()

    subject.cancelDownload(taskId)

    verify { workManager.cancelAllWorkByTag("download_$taskId") }
    coVerify { downloadTaskDao.updateStatus(taskId.toString(), DownloadStatus.CANCELLED) }
  }

  @Test
  fun retryDownload_resetsTaskAndRequeues() = runTest {
    val taskId = UUID.randomUUID()
    val entity =
      sampleEntity(
        taskId = taskId,
        status = DownloadStatus.FAILED,
        progress = 0.6f,
        bytesDownloaded = 1_024L,
        errorMessage = "boom",
      )
    coEvery { downloadTaskDao.getById(taskId.toString()) } returnsMany listOf(entity, entity)
    val updatedSlot = slot<DownloadTaskEntity>()
    coEvery { downloadTaskDao.update(capture(updatedSlot)) } returns Unit
    val uniqueSlot = slot<String>()
    val policySlot = slot<ExistingWorkPolicy>()
    every {
      workManager.enqueueUniqueWork(
        capture(uniqueSlot),
        capture(policySlot),
        any<OneTimeWorkRequest>(),
      )
    } returns mockk(relaxed = true)

    subject.retryDownload(taskId)

    coVerify { downloadTaskDao.update(any<DownloadTaskEntity>()) }
    assertTrue(updatedSlot.isCaptured)
    val updated = updatedSlot.captured
    assertEquals(DownloadStatus.QUEUED, updated.status)
    assertEquals(0f, updated.progress)
    assertEquals(0L, updated.bytesDownloaded)
    assertNull(updated.errorMessage)
    assertNotNull(updated.startedAt)
    assertNull(updated.finishedAt)
    assertEquals("download_$taskId", uniqueSlot.captured)
    assertEquals(ExistingWorkPolicy.REPLACE, policySlot.captured)
  }

  @Test
  fun retryDownload_withMissingTaskDoesNothing() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadTaskDao.getById(taskId.toString()) } returns null

    subject.retryDownload(taskId)

    verify(exactly = 0) {
      workManager.enqueueUniqueWork(
        any<String>(),
        any<ExistingWorkPolicy>(),
        any<OneTimeWorkRequest>(),
      )
    }
    coVerify(exactly = 0) { downloadTaskDao.update(any<DownloadTaskEntity>()) }
  }

  @Test
  fun resetTask_withMissingTaskDoesNothing() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadTaskDao.getById(taskId.toString()) } returns null

    subject.resetTask(taskId)

    coVerify(exactly = 0) { downloadTaskDao.update(any<DownloadTaskEntity>()) }
  }

  @Test
  fun getDownloadStatus_returnsDomainModel() = runTest {
    val taskId = UUID.randomUUID()
    val entity = sampleEntity(taskId = taskId, progress = 0.3f)
    coEvery { downloadTaskDao.getById(taskId.toString()) } returns entity

    val result = subject.getDownloadStatus(taskId)

    assertNotNull(result)
    assertEquals(taskId, result?.taskId)
    assertEquals(entity.modelId, result?.modelId)
    assertEquals(entity.status, result?.status)
    assertEquals(entity.progress, result?.progress)
  }

  @Test
  fun getDownloadStatus_whenMissingReturnsNull() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadTaskDao.getById(taskId.toString()) } returns null

    val result = subject.getDownloadStatus(taskId)

    assertNull(result)
  }

  @Test
  fun getTaskById_emitsMappedFlow() = runTest {
    val taskId = UUID.randomUUID()
    val entity = sampleEntity(taskId = taskId)
    every { downloadTaskDao.observeById(taskId.toString()) } returns flowOf(entity)

    val flow = subject.getTaskById(taskId)

    val value = flow.first()
    assertNotNull(value)
    assertEquals(taskId, value?.taskId)
  }

  @Test
  fun getActiveDownloads_emitsMappedList() = runTest {
    val taskId = UUID.randomUUID()
    val entity = sampleEntity(taskId = taskId)
    every { downloadTaskDao.observeActiveDownloads() } returns flowOf(listOf(entity))

    val flow = subject.getActiveDownloads()

    val items = flow.first()
    assertEquals(1, items.size)
    assertEquals(taskId, items.first().taskId)
  }

  @Test
  fun getQueuedDownloads_emitsMappedList() = runTest {
    val taskId = UUID.randomUUID()
    val entity = sampleEntity(taskId = taskId, status = DownloadStatus.QUEUED)
    every { downloadTaskDao.observeQueuedDownloads() } returns flowOf(listOf(entity))

    val items = subject.getQueuedDownloads().first()

    assertEquals(1, items.size)
    assertEquals(taskId, items.first().taskId)
  }

  @Test
  fun observeProgress_emitsStoredProgress() = runTest {
    val taskId = UUID.randomUUID()
    val entity = sampleEntity(taskId = taskId, progress = 0.42f)
    every { downloadTaskDao.observeById(taskId.toString()) } returns flowOf(entity)

    val value = subject.observeProgress(taskId).first()

    assertEquals(0.42f, value)
  }

  @Test
  fun observeProgress_withMissingTaskEmitsZero() = runTest {
    val taskId = UUID.randomUUID()
    every { downloadTaskDao.observeById(taskId.toString()) } returns flowOf(null)

    val value = subject.observeProgress(taskId).first()

    assertEquals(0f, value)
  }

  @Test
  fun getMaxConcurrentDownloads_returnsDefault() = runTest {
    assertEquals(1, subject.getMaxConcurrentDownloads())
  }

  @Test
  fun updateTaskStatus_delegatesToDao() = runTest {
    val taskId = UUID.randomUUID()

    subject.updateTaskStatus(taskId, DownloadStatus.DOWNLOADING)

    coVerify { downloadTaskDao.updateStatus(taskId.toString(), DownloadStatus.DOWNLOADING) }
  }

  @Test
  fun getModelIdForTask_returnsDaoValue() = runTest {
    val taskId = UUID.randomUUID()
    coEvery { downloadTaskDao.getModelIdForTask(taskId.toString()) } returns "modelZ"

    val result = subject.getModelIdForTask(taskId)

    assertEquals("modelZ", result)
  }

  @Test
  fun getDownloadedChecksum_returnsSha256() = runTest {
    val modelId = "modelChecksum"
    val modelsDir = File(filesDir, "models").apply { mkdirs() }
    val file = File(modelsDir, "$modelId.bin")
    file.writeText("nanoai")
    val expected =
      MessageDigest.getInstance("SHA-256").digest("nanoai".toByteArray()).joinToString(
        separator = ""
      ) { byte ->
        "%02x".format(byte)
      }

    val checksum = subject.getDownloadedChecksum(modelId)

    assertEquals(expected, checksum)
  }

  @Test
  fun getDownloadedChecksum_missingFileReturnsNull() = runTest {
    val checksum = subject.getDownloadedChecksum("missing")

    assertNull(checksum)
  }

  @Test
  fun deletePartialFiles_removesFilePrefixes() = runTest {
    val modelId = "modelPartial"
    val modelsDir = File(filesDir, "models").apply { mkdirs() }
    val keepFile = File(modelsDir, "other.bin").apply { writeText("keep") }
    val deleteOne = File(modelsDir, "$modelId.bin").apply { writeText("drop") }
    val deleteTwo = File(modelsDir, "$modelId.partial").apply { writeText("drop") }

    subject.deletePartialFiles(modelId)

    assertTrue(keepFile.exists())
    assertFalse(deleteOne.exists())
    assertFalse(deleteTwo.exists())
  }

  private fun sampleEntity(
    taskId: UUID = UUID.randomUUID(),
    modelId: String = "model-id",
    status: DownloadStatus = DownloadStatus.DOWNLOADING,
    progress: Float = 0.5f,
    bytesDownloaded: Long = 512L,
    errorMessage: String? = null,
  ): DownloadTaskEntity =
    DownloadTaskEntity(
      taskId = taskId.toString(),
      modelId = modelId,
      status = status,
      progress = progress,
      bytesDownloaded = bytesDownloaded,
      startedAt = Instant.fromEpochMilliseconds(0),
      finishedAt = null,
      errorMessage = errorMessage,
    )
}
