package com.vjaykrsna.nanoai.core.coverage.tasks

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.coverage.model.TestLayer
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [VerifyCoverageThresholdsTask].
 *
 * Tests parsing of JaCoCo XML reports, threshold verification, and markdown rendering.
 */
class VerifyCoverageThresholdsTaskTest {

  private lateinit var tempDir: Path
  private lateinit var reportXml: Path
  private lateinit var layerMap: Path
  private lateinit var metadataJson: Path

  @BeforeEach
  fun setUp() {
    tempDir = createTempDirectory("coverage-task-test")
    reportXml = tempDir.resolve("report.xml")
    layerMap = tempDir.resolve("layer-map.json")
    metadataJson = tempDir.resolve("coverage-metadata.json")

    writeDefaultLayerMap()
    writeDefaultMetadata()
  }

  @AfterEach
  fun tearDown() {
    tempDir.toFile().deleteRecursively()
  }

  private fun writeDefaultLayerMap() {
    layerMap.writeText(
      """
      {
        "_default": "DATA",
        "VIEW_MODEL": ["^com/example/viewmodel/.*$"],
        "UI": ["^com/example/ui/.*$"],
        "DATA": ["^com/example/data/.*$", "^com/example/repository/.*$"]
      }
      """
        .trimIndent()
    )
  }

  private fun writeDefaultMetadata() {
    metadataJson.writeText(
      """
      {
        "metrics": [
          {"layer": "VIEW_MODEL", "minimumPercent": 75},
          {"layer": "UI", "minimumPercent": 65},
          {"layer": "DATA", "minimumPercent": 70}
        ]
      }
      """
        .trimIndent()
    )
  }

  @Nested
  inner class ThresholdLoading {

    @Test
    fun `loads custom thresholds from metadata`() {
      metadataJson.writeText(
        """
        {
          "metrics": [
            {"layer": "VIEW_MODEL", "minimumPercent": 80},
            {"layer": "UI", "minimumPercent": 70},
            {"layer": "DATA", "minimumPercent": 75}
          ]
        }
        """
          .trimIndent()
      )

      val overrides = VerifyCoverageThresholdsTask.loadThresholdOverrides(metadataJson)

      assertThat(overrides[TestLayer.VIEW_MODEL]).isEqualTo(80.0)
      assertThat(overrides[TestLayer.UI]).isEqualTo(70.0)
      assertThat(overrides[TestLayer.DATA]).isEqualTo(75.0)
    }

    @Test
    fun `ignores unknown layer names in metadata`() {
      metadataJson.writeText(
        """
        {
          "metrics": [
            {"layer": "UNKNOWN_LAYER", "minimumPercent": 99},
            {"layer": "VIEW_MODEL", "minimumPercent": 80}
          ]
        }
        """
          .trimIndent()
      )

      val overrides = VerifyCoverageThresholdsTask.loadThresholdOverrides(metadataJson)

      assertThat(overrides[TestLayer.VIEW_MODEL]).isEqualTo(80.0)
      // Unknown layer should be ignored, DATA stays at default
      assertThat(overrides[TestLayer.DATA]).isEqualTo(70.0)
    }

    @Test
    fun `ignores metrics without minimumPercent`() {
      metadataJson.writeText(
        """
        {
          "metrics": [
            {"layer": "VIEW_MODEL"},
            {"layer": "UI", "someOtherField": 99}
          ]
        }
        """
          .trimIndent()
      )

      val overrides = VerifyCoverageThresholdsTask.loadThresholdOverrides(metadataJson)

      // Both should use defaults since no minimumPercent was specified
      assertThat(overrides[TestLayer.VIEW_MODEL]).isEqualTo(75.0)
      assertThat(overrides[TestLayer.UI]).isEqualTo(65.0)
    }

    @Test
    fun `handles empty metrics array`() {
      metadataJson.writeText(
        """
        {
          "metrics": []
        }
        """
          .trimIndent()
      )

      val overrides = VerifyCoverageThresholdsTask.loadThresholdOverrides(metadataJson)

      assertThat(overrides[TestLayer.VIEW_MODEL]).isEqualTo(75.0)
      assertThat(overrides[TestLayer.UI]).isEqualTo(65.0)
      assertThat(overrides[TestLayer.DATA]).isEqualTo(70.0)
    }

    @Test
    fun `handles missing metrics key`() {
      metadataJson.writeText(
        """
        {
          "someOtherKey": "value"
        }
        """
          .trimIndent()
      )

      val overrides = VerifyCoverageThresholdsTask.loadThresholdOverrides(metadataJson)

      assertThat(overrides[TestLayer.VIEW_MODEL]).isEqualTo(75.0)
      assertThat(overrides[TestLayer.UI]).isEqualTo(65.0)
      assertThat(overrides[TestLayer.DATA]).isEqualTo(70.0)
    }

    @Test
    fun `handles lowercase layer names`() {
      metadataJson.writeText(
        """
        {
          "metrics": [
            {"layer": "view_model", "minimumPercent": 80}
          ]
        }
        """
          .trimIndent()
      )

      val overrides = VerifyCoverageThresholdsTask.loadThresholdOverrides(metadataJson)

      assertThat(overrides[TestLayer.VIEW_MODEL]).isEqualTo(80.0)
    }

    @Test
    fun `handles mixed case layer names`() {
      metadataJson.writeText(
        """
        {
          "metrics": [
            {"layer": "View_Model", "minimumPercent": 82}
          ]
        }
        """
          .trimIndent()
      )

      val overrides = VerifyCoverageThresholdsTask.loadThresholdOverrides(metadataJson)

      assertThat(overrides[TestLayer.VIEW_MODEL]).isEqualTo(82.0)
    }

    @Test
    fun `handles malformed JSON gracefully`() {
      metadataJson.writeText("not valid json {{{")

      val overrides = VerifyCoverageThresholdsTask.loadThresholdOverrides(metadataJson)

      // Should fallback to defaults
      assertThat(overrides[TestLayer.VIEW_MODEL]).isEqualTo(75.0)
      assertThat(overrides[TestLayer.UI]).isEqualTo(65.0)
      assertThat(overrides[TestLayer.DATA]).isEqualTo(70.0)
    }

    @Test
    fun `handles non-numeric minimumPercent`() {
      metadataJson.writeText(
        """
        {
          "metrics": [
            {"layer": "VIEW_MODEL", "minimumPercent": "not-a-number"}
          ]
        }
        """
          .trimIndent()
      )

      val overrides = VerifyCoverageThresholdsTask.loadThresholdOverrides(metadataJson)

      // Should use default since minimumPercent couldn't be parsed
      assertThat(overrides[TestLayer.VIEW_MODEL]).isEqualTo(75.0)
    }

    @Test
    fun `handles metric without layer key`() {
      metadataJson.writeText(
        """
        {
          "metrics": [
            {"minimumPercent": 80},
            {"layer": "UI", "minimumPercent": 70}
          ]
        }
        """
          .trimIndent()
      )

      val overrides = VerifyCoverageThresholdsTask.loadThresholdOverrides(metadataJson)

      // Only the UI entry with proper layer key should be applied
      assertThat(overrides[TestLayer.UI]).isEqualTo(70.0)
    }
  }

  @Nested
  inner class DocumentBuilding {

    @Test
    fun `creates document builder that can parse jacoco reports`() {
      val builder = VerifyCoverageThresholdsTask.newDocumentBuilderForTest()

      reportXml.writeText(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <report name="test">
          <package name="com/example">
            <class name="com/example/Test">
              <counter type="LINE" missed="5" covered="10"/>
            </class>
          </package>
        </report>
        """
          .trimIndent()
      )

      val document = builder.parse(reportXml.toFile())

      assertThat(document.documentElement.nodeName).isEqualTo("report")
      assertThat(document.documentElement.getAttribute("name")).isEqualTo("test")
    }

    @Test
    fun `handles reports with DTD declaration`() {
      val builder = VerifyCoverageThresholdsTask.newDocumentBuilderForTest()

      reportXml.writeText(
        """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">
        <report name="with-dtd">
          <package name="com/example">
            <class name="com/example/Foo">
              <counter type="LINE" missed="2" covered="8"/>
            </class>
          </package>
        </report>
        """
          .trimIndent()
      )

      val document = builder.parse(reportXml.toFile())

      assertThat(document.documentElement.getAttribute("name")).isEqualTo("with-dtd")
    }

    @Test
    fun `parses multiple classes in report`() {
      val builder = VerifyCoverageThresholdsTask.newDocumentBuilderForTest()

      reportXml.writeText(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <report name="multi-class">
          <package name="com/example">
            <class name="com/example/ClassA">
              <counter type="LINE" missed="0" covered="10"/>
            </class>
            <class name="com/example/ClassB">
              <counter type="LINE" missed="5" covered="5"/>
            </class>
          </package>
        </report>
        """
          .trimIndent()
      )

      val document = builder.parse(reportXml.toFile())
      val classNodes = document.getElementsByTagName("class")

      assertThat(classNodes.length).isEqualTo(2)
    }

    @Test
    fun `parses multiple packages in report`() {
      val builder = VerifyCoverageThresholdsTask.newDocumentBuilderForTest()

      reportXml.writeText(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <report name="multi-package">
          <package name="com/example/data">
            <class name="com/example/data/Repository">
              <counter type="LINE" missed="10" covered="90"/>
            </class>
          </package>
          <package name="com/example/ui">
            <class name="com/example/ui/Screen">
              <counter type="LINE" missed="20" covered="80"/>
            </class>
          </package>
        </report>
        """
          .trimIndent()
      )

      val document = builder.parse(reportXml.toFile())
      val packageNodes = document.getElementsByTagName("package")

      assertThat(packageNodes.length).isEqualTo(2)
    }

    @Test
    fun `handles nested class names`() {
      val builder = VerifyCoverageThresholdsTask.newDocumentBuilderForTest()

      reportXml.writeText(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <report name="nested">
          <package name="com/example">
            <class name="com/example/Outer">
              <counter type="LINE" missed="1" covered="9"/>
            </class>
            <class name="com/example/Outer${'$'}Inner">
              <counter type="LINE" missed="2" covered="8"/>
            </class>
            <class name="com/example/Outer${'$'}Inner${'$'}DeepNested">
              <counter type="LINE" missed="3" covered="7"/>
            </class>
          </package>
        </report>
        """
          .trimIndent()
      )

      val document = builder.parse(reportXml.toFile())
      val classNodes = document.getElementsByTagName("class")

      assertThat(classNodes.length).isEqualTo(3)
    }

    @Test
    fun `handles class with multiple counter types`() {
      val builder = VerifyCoverageThresholdsTask.newDocumentBuilderForTest()

      reportXml.writeText(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <report name="multi-counter">
          <package name="com/example">
            <class name="com/example/Test">
              <counter type="INSTRUCTION" missed="100" covered="200"/>
              <counter type="BRANCH" missed="5" covered="15"/>
              <counter type="LINE" missed="10" covered="40"/>
              <counter type="COMPLEXITY" missed="3" covered="12"/>
              <counter type="METHOD" missed="1" covered="9"/>
              <counter type="CLASS" missed="0" covered="1"/>
            </class>
          </package>
        </report>
        """
          .trimIndent()
      )

      val document = builder.parse(reportXml.toFile())
      val classNode = document.getElementsByTagName("class").item(0)
      val counters = classNode.childNodes

      var lineCounter: org.w3c.dom.Node? = null
      for (i in 0 until counters.length) {
        val node = counters.item(i)
        if (
          node is org.w3c.dom.Element &&
            node.tagName == "counter" &&
            node.getAttribute("type") == "LINE"
        ) {
          lineCounter = node
          break
        }
      }

      assertThat(lineCounter).isNotNull()
      assertThat((lineCounter as org.w3c.dom.Element).getAttribute("missed")).isEqualTo("10")
      assertThat(lineCounter.getAttribute("covered")).isEqualTo("40")
    }
  }

  @Nested
  inner class EdgeCases {

    @Test
    fun `handles empty report`() {
      val builder = VerifyCoverageThresholdsTask.newDocumentBuilderForTest()

      reportXml.writeText(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <report name="empty"/>
        """
          .trimIndent()
      )

      val document = builder.parse(reportXml.toFile())

      assertThat(document.documentElement.nodeName).isEqualTo("report")
      assertThat(document.getElementsByTagName("class").length).isEqualTo(0)
    }

    @Test
    fun `handles class with zero coverage`() {
      val builder = VerifyCoverageThresholdsTask.newDocumentBuilderForTest()

      reportXml.writeText(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <report name="zero-coverage">
          <package name="com/example">
            <class name="com/example/Uncovered">
              <counter type="LINE" missed="100" covered="0"/>
            </class>
          </package>
        </report>
        """
          .trimIndent()
      )

      val document = builder.parse(reportXml.toFile())
      val classNodes = document.getElementsByTagName("class")

      assertThat(classNodes.length).isEqualTo(1)
    }

    @Test
    fun `handles class with full coverage`() {
      val builder = VerifyCoverageThresholdsTask.newDocumentBuilderForTest()

      reportXml.writeText(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <report name="full-coverage">
          <package name="com/example">
            <class name="com/example/FullyCovered">
              <counter type="LINE" missed="0" covered="100"/>
            </class>
          </package>
        </report>
        """
          .trimIndent()
      )

      val document = builder.parse(reportXml.toFile())
      val classNodes = document.getElementsByTagName("class")

      assertThat(classNodes.length).isEqualTo(1)
    }

    @Test
    fun `handles class without counter elements`() {
      val builder = VerifyCoverageThresholdsTask.newDocumentBuilderForTest()

      reportXml.writeText(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <report name="no-counters">
          <package name="com/example">
            <class name="com/example/NoCounters"/>
          </package>
        </report>
        """
          .trimIndent()
      )

      val document = builder.parse(reportXml.toFile())
      val classNode = document.getElementsByTagName("class").item(0)

      assertThat(classNode).isNotNull()
    }

    @Test
    fun `handles special characters in class names`() {
      val builder = VerifyCoverageThresholdsTask.newDocumentBuilderForTest()

      reportXml.writeText(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <report name="special-chars">
          <package name="com/example">
            <class name="com/example/Lambda${'$'}1">
              <counter type="LINE" missed="1" covered="9"/>
            </class>
            <class name="com/example/Class${'$'}Companion">
              <counter type="LINE" missed="2" covered="8"/>
            </class>
          </package>
        </report>
        """
          .trimIndent()
      )

      val document = builder.parse(reportXml.toFile())
      val classNodes = document.getElementsByTagName("class")

      assertThat(classNodes.length).isEqualTo(2)
    }
  }
}
