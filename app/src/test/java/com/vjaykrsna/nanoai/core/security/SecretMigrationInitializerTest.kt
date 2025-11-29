package com.vjaykrsna.nanoai.core.security

import org.junit.jupiter.api.Test

class SecretMigrationInitializerTest {
  @Test
  fun `ensureMigration completes without side effects`() {
    SecretMigrationInitializer().ensureMigration()
  }
}
