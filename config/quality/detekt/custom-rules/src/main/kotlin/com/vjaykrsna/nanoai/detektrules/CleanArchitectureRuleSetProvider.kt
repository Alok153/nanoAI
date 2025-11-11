package com.vjaykrsna.nanoai.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class CleanArchitectureRuleSetProvider : RuleSetProvider {
  override val ruleSetId: String = "nanoai-clean-architecture"

  override fun instance(config: Config): RuleSet =
    RuleSet(ruleSetId, listOf(CleanArchitectureRule(config), ViewModelStateRule(config)))
}
