package com.vjaykrsna.nanoai.feature.chat.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val CHAT_SCREEN_LOG_TAG = "ChatScreen"

@Composable
internal fun rememberChatImagePicker(onImageSelect: (Bitmap) -> Unit): () -> Unit {
  val context = LocalContext.current
  val launcher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
      ->
      if (uri != null) {
        try {
          loadBitmapFromUri(context, uri)?.let(onImageSelect)
        } catch (error: Throwable) {
          logImageSelectionFailure(error)
        }
      }
    }
  return remember(launcher) { { launcher.launch("image/*") } }
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
  } else {
    context.contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream)
    }
  }
}

private fun logImageSelectionFailure(throwable: Throwable) {
  Log.e(CHAT_SCREEN_LOG_TAG, "Failed to load image", throwable)
}
