package com.vjaykrsna.nanoai.core.common

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@RunWith(AndroidJUnit4::class)
class NotificationHelperTest {

  private lateinit var context: Context
  private lateinit var application: Application
  private lateinit var notificationHelper: NotificationHelper

  @Before
  fun setUp() {
    application = ApplicationProvider.getApplicationContext()
    context = application
    notificationHelper = NotificationHelper(context)
  }

  @Test
  fun buildProgressNotification_createsNotificationWithCorrectDetails() {
    val modelName = "Test Model"
    val progress = 50
    val taskId = "task-123"
    val modelId = "model-456"

    val notification =
      notificationHelper.buildProgressNotification(modelName, progress, taskId, modelId)

    assertNotNull(notification)
    assertEquals("Downloading $modelName", notification.extras.getString(Notification.EXTRA_TITLE))
    assertEquals("$progress% complete", notification.extras.getString(Notification.EXTRA_TEXT))
    @Suppress("DEPRECATION") val priority = notification.priority
    assertEquals(NotificationCompat.PRIORITY_DEFAULT, priority)
    assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
  }

  @Test
  fun buildCompletionNotification_createsNotificationWithCorrectDetails() {
    val modelName = "Test Model"

    val notification = notificationHelper.buildCompletionNotification(modelName)

    assertNotNull(notification)
    assertEquals("Download Complete", notification.extras.getString(Notification.EXTRA_TITLE))
    assertEquals(
      "$modelName downloaded successfully",
      notification.extras.getString(Notification.EXTRA_TEXT),
    )
    @Suppress("DEPRECATION") val priority = notification.priority
    assertEquals(NotificationCompat.PRIORITY_DEFAULT, priority)
    assertTrue(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
  }

  @Test
  fun buildFailureNotification_createsNotificationWithCorrectDetails() {
    val modelName = "Test Model"
    val errorMessage = "Network error"

    val notification = notificationHelper.buildFailureNotification(modelName, errorMessage)

    assertNotNull(notification)
    assertEquals("Download Failed", notification.extras.getString(Notification.EXTRA_TITLE))
    assertEquals(
      "$modelName: $errorMessage",
      notification.extras.getString(Notification.EXTRA_TEXT),
    )
    @Suppress("DEPRECATION") val priority = notification.priority
    assertEquals(NotificationCompat.PRIORITY_DEFAULT, priority)
    assertTrue(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
  fun notifyProgress_skipsWhenPermissionDenied() {
    val shadowApplication: ShadowApplication = Shadows.shadowOf(application)
    shadowApplication.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
    val notification = notificationHelper.buildProgressNotification("Model", 5, "task", "id")

    notificationHelper.notifyProgress(notification)

    val manager = context.getSystemService(NotificationManager::class.java)
    val shadowManager = Shadows.shadowOf(manager)
    assertThat(shadowManager.allNotifications).isEmpty()
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
  fun notifyCompletion_postsWhenPermissionGranted() {
    val shadowApplication: ShadowApplication = Shadows.shadowOf(application)
    shadowApplication.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    val notification = notificationHelper.buildCompletionNotification("Model")

    notificationHelper.notifyCompletion(notification)

    val manager = context.getSystemService(NotificationManager::class.java)
    val shadowManager = Shadows.shadowOf(manager)
    assertThat(shadowManager.allNotifications).hasSize(1)
  }
}
