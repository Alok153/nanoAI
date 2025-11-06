package com.vjaykrsna.nanoai.detektrules

import io.gitlab.arturbosch.detekt.test.compileAndLint
import kotlin.test.Test
import kotlin.test.assertTrue

class CleanArchitectureRuleTest {
  private val rule = CleanArchitectureRule(io.gitlab.arturbosch.detekt.api.Config.empty)

  @Test
  fun `reports when feature layer imports data layer`() {
    val findings =
      rule.compileAndLint(
        """
            package com.vjaykrsna.nanoai.feature.chat

            import com.vjaykrsna.nanoai.core.domain.repository.ChatRepository

            class ChatViewModel(private val repository: ChatRepository)
        """
          .trimIndent()
      )

    assertTrue(findings.size == 1)
    assertTrue(findings.first().message.contains("feature"))
  }

  @Test
  fun `reports when core layer imports feature package`() {
    val findings =
      rule.compileAndLint(
        """
            package com.vjaykrsna.nanoai.core.domain

            import com.vjaykrsna.nanoai.feature.chat.ChatCoordinator

            object DomainGateway {
                fun attach() = ChatCoordinator()
            }
        """
          .trimIndent()
      )

    assertTrue(findings.size == 1)
    assertTrue(findings.first().message.contains("Core layer"))
  }

  @Test
  fun `does not report allowed imports`() {
    val findings =
      rule.compileAndLint(
        """
            package com.vjaykrsna.nanoai.feature.chat

            import com.vjaykrsna.nanoai.core.domain.UseCase

            class ChatViewModel(private val useCase: UseCase)
        """
          .trimIndent()
      )

    assertTrue(findings.isEmpty())
  }
}
