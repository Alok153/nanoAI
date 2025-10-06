package com.vjaykrsna.nanoai.core.domain.model.uiux

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

fun Flow<UserProfileRecord?>.mapToUserProfile(): Flow<UserProfile?> = map { record ->
  record?.toDomain()
}

fun Flow<UserProfileRecord?>.mergeWithPreferences(
  preferences: Flow<UiPreferencesSnapshot>,
  fallback: (UiPreferencesSnapshot) -> UserProfile? = { null },
): Flow<UserProfile?> {
  return combine(this, preferences) { record, prefs ->
    val normalized = prefs.normalized()
    val base = record?.toDomain() ?: fallback(normalized)
    base?.withPreferences(normalized)
  }
}

fun Flow<UserProfileRecord?>.requireUserProfile(
  preferences: Flow<UiPreferencesSnapshot>,
  fallback: (UiPreferencesSnapshot) -> UserProfile,
): Flow<UserProfile> {
  return mergeWithPreferences(preferences, fallback).mapNotNull { it }
}
