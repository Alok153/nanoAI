package com.vjaykrsna.nanoai.contracts

import kotlin.test.Test
import kotlin.test.fail

class ModelVerificationContractTest {
    @Test
    fun `checksum mismatch returns retryable error envelope`() {
        fail("Pending implementation: assert POST /catalog/models/{modelId}/verify returns RETRY with ErrorEnvelope")
    }
}
