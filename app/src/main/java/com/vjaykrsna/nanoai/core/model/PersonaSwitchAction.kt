package com.vjaykrsna.nanoai.core.model

/**
 * Action taken when switching personas in a chat thread.
 */
enum class PersonaSwitchAction {
    /**
     * Continue the current thread with the new persona.
     * Previous context is maintained.
     */
    CONTINUE_THREAD,

    /**
     * Start a new thread with the new persona.
     * Previous context is not carried over.
     */
    START_NEW_THREAD
}
