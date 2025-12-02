package com.vjaykrsna.nanoai.core.common.annotations

/**
 * Marks APIs that continuously emit updates via Flow and must be collected.
 *
 * Use on repository/use case functions returning `Flow<T>`. Callers must collect the flow to
 * receive updates. See [REACTIVE_DATA_CONTRACT.md] for patterns.
 *
 * Example:
 * ```kotlin
 * @ReactiveStream("Emits conversation list updates")
 * fun observeConversations(): Flow<List<Conversation>>
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReactiveStream(val description: String = "")

/**
 * Marks single-shot suspend APIs that return a [NanoAIResult].
 *
 * Use on repository/use case functions that perform a one-time operation (fetch, create, update,
 * delete). Returns immediately with success or error wrapped in [NanoAIResult].
 *
 * Example:
 * ```kotlin
 * @OneShot("Creates a new conversation")
 * suspend fun createConversation(title: String): NanoAIResult<Conversation>
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OneShot(val description: String = "")
