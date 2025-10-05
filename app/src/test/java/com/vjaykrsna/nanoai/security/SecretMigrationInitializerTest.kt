package com.vjaykrsna.nanoai.security

import org.junit.Test

class SecretMigrationInitializerTest {
  @Test
  fun `ensureMigration completes without side effects`() {
    SecretMigrationInitializer().ensureMigration()
  }
}
