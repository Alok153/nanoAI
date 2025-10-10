package com.vjaykrsna.nanoai.model.huggingface

import com.vjaykrsna.nanoai.model.huggingface.network.dto.HuggingFaceLfsDto
import com.vjaykrsna.nanoai.model.huggingface.network.dto.HuggingFacePathInfoDto
import com.vjaykrsna.nanoai.model.huggingface.network.dto.HuggingFaceSiblingDto
import com.vjaykrsna.nanoai.model.huggingface.network.dto.HuggingFaceTreeEntryDto
import java.util.Locale

internal const val SHA256_LENGTH = 64

internal fun HuggingFacePathInfoDto.bestSha256(): String? {
  return sha256 ?: lfs?.bestSha256() ?: gitOid?.takeIf { it.isSha256() }
}

internal fun HuggingFacePathInfoDto.bestSize(): Long? = lfs?.sizeBytes ?: sizeBytes

internal fun HuggingFaceSiblingDto.bestSha256(): String? {
  return sha256 ?: lfs?.bestSha256() ?: gitOid?.takeIf { it.isSha256() }
}

internal fun HuggingFaceSiblingDto.bestSize(): Long? = lfs?.sizeBytes ?: sizeBytes

internal fun HuggingFaceTreeEntryDto.bestSha256(): String? {
  return lfs?.oid?.takeIf { it.isSha256() } ?: gitOid?.takeIf { it.isSha256() }
}

internal fun HuggingFaceTreeEntryDto.bestSize(): Long? = lfs?.sizeBytes ?: sizeBytes

internal fun HuggingFaceLfsDto.bestSha256(): String? {
  return when {
    !sha256.isNullOrBlank() -> sha256
    oid.startsWith("sha256:", ignoreCase = true) -> oid.substringAfter(':').lowercase(Locale.US)
    oid.isSha256() -> oid
    else -> null
  }
}

internal fun String.isSha256(): Boolean = length == SHA256_LENGTH && all { it.isHexDigit() }

private fun Char.isHexDigit(): Boolean =
  (this in '0'..'9') || (this in 'a'..'f') || (this in 'A'..'F')
