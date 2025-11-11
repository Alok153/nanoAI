package com.vjaykrsna.nanoai.core.coverage.tasks

import com.google.common.truth.Truth.assertThat
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
import org.junit.Test

class VerifyCoverageThresholdsTaskParserTest {

  @Test
  fun `parses jacoco report referencing external dtd without local copy`() {
    val report = createTempFile(prefix = "jacoco-", suffix = ".xml")
    report.writeText(SAMPLE_REPORT)
    report.toFile().deleteOnExit()

    val document = VerifyCoverageThresholdsTask.newDocumentBuilderForTest().parse(report.toFile())

    assertThat(document.documentElement.nodeName).isEqualTo("report")
  }

  private companion object {
    private val SAMPLE_REPORT =
      """
      <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
      <!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">
      <report name="sample">
        <package name="com/example">
          <class name="com/example/Sample">
            <counter type="LINE" missed="0" covered="5"/>
          </class>
        </package>
      </report>
      """
        .trimIndent()
  }
}
