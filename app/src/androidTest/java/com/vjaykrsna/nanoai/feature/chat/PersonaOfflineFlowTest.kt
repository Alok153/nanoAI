package com.vjaykrsna.nanoai.feature.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

/**
 * Compose UI instrumentation test for Persona and Offline features.
 * Tests persona toggle prompts, logging overlay, and offline banner behavior.
 *
 * TDD: This test is written BEFORE the UI is implemented.
 * Expected to FAIL with compilation errors until:
 * - ChatScreen composable is created
 * - ChatViewModel is defined
 * - PersonaSwitchDialog is implemented
 * - OfflineIndicator component exists
 */
@RunWith(AndroidJUnit4::class)
class PersonaOfflineFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: ChatViewModel
    private lateinit var uiState: MutableStateFlow<ChatUiState>

    @Before
    fun setup() {
        viewModel = mockk(relaxed = true)
        uiState = MutableStateFlow(
            ChatUiState(
                currentThread = null,
                messages = emptyList(),
                currentPersona = null,
                isOnline = true,
                hasLocalModelAvailable = false,
                isGenerating = false,
                errorMessage = null
            )
        )
        coEvery { viewModel.uiState } returns uiState
    }

    @Test
    fun personaSelector_shouldDisplayCurrentPersona() {
        // Arrange
        val persona = PersonaProfile(
            personaId = UUID.randomUUID(),
            name = "Creative Writer",
            description = "Creative writing assistant",
            systemPrompt = "You are a creative writer",
            defaultModelPreference = null,
            temperature = 0.9f,
            topP = 0.95f,
            defaultVoice = null,
            defaultImageStyle = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        uiState.value = uiState.value.copy(currentPersona = persona)

        // Act
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("Creative Writer").assertIsDisplayed()
    }

    @Test
    fun personaSelector_whenClicked_shouldShowPersonaList() {
        // Arrange
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule.onNodeWithContentDescription("Select persona")
            .performClick()

        // Assert
        composeTestRule.onNodeWithText("Choose Persona").assertIsDisplayed()
    }

    @Test
    fun personaSwitch_shouldShowConfirmationDialog() {
        // Arrange
        val currentPersona = PersonaProfile(
            personaId = UUID.randomUUID(),
            name = "Assistant",
            description = "General assistant",
            systemPrompt = "You are helpful",
            defaultModelPreference = null,
            temperature = 0.7f,
            topP = 0.9f,
            defaultVoice = null,
            defaultImageStyle = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        uiState.value = uiState.value.copy(
            currentPersona = currentPersona,
            currentThread = ChatThread(
                threadId = UUID.randomUUID(),
                title = "Existing Chat",
                personaId = currentPersona.personaId,
                activeModelId = "test-model",
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                isArchived = false
            )
        )

        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Act: Open persona selector and choose different persona
        composeTestRule.onNodeWithContentDescription("Select persona")
            .performClick()
        composeTestRule.onNodeWithText("Creative Writer")
            .performClick()

        // Assert
        composeTestRule.onNodeWithText("Switch Persona?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue in this thread").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start new thread").assertIsDisplayed()
    }

    @Test
    fun personaSwitchDialog_continueThread_shouldLogSwitch() {
        // Arrange
        val threadId = UUID.randomUUID()
        val currentPersonaId = UUID.randomUUID()
        val newPersonaId = UUID.randomUUID()

        uiState.value = uiState.value.copy(
            currentThread = ChatThread(
                threadId = threadId,
                title = "Test Chat",
                personaId = currentPersonaId,
                activeModelId = "test-model",
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                isArchived = false
            )
        )

        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule.onNodeWithContentDescription("Select persona")
            .performClick()
        composeTestRule.onNodeWithText("Creative Writer")
            .performClick()
        composeTestRule.onNodeWithText("Continue in this thread")
            .performClick()

        // Assert
        coVerify { 
            viewModel.switchPersona(
                threadId = threadId,
                newPersonaId = newPersonaId,
                action = PersonaSwitchAction.CONTINUE_THREAD
            ) 
        }
    }

    @Test
    fun personaSwitchDialog_startNewThread_shouldCreateThread() {
        // Arrange
        val oldThreadId = UUID.randomUUID()
        val newPersonaId = UUID.randomUUID()

        uiState.value = uiState.value.copy(
            currentThread = ChatThread(
                threadId = oldThreadId,
                title = "Old Chat",
                personaId = UUID.randomUUID(),
                activeModelId = "test-model",
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                isArchived = false
            )
        )

        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule.onNodeWithContentDescription("Select persona")
            .performClick()
        composeTestRule.onNodeWithText("Creative Writer")
            .performClick()
        composeTestRule.onNodeWithText("Start new thread")
            .performClick()

        // Assert
        coVerify { 
            viewModel.switchPersona(
                threadId = oldThreadId,
                newPersonaId = newPersonaId,
                action = PersonaSwitchAction.START_NEW_THREAD
            ) 
        }
    }

    @Test
    fun offlineBanner_shouldDisplayWhenOffline() {
        // Arrange
        uiState.value = uiState.value.copy(
            isOnline = false,
            hasLocalModelAvailable = true
        )

        // Act
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("Offline Mode").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Offline indicator")
            .assertIsDisplayed()
    }

    @Test
    fun offlineBanner_shouldShowLocalModelStatus() {
        // Arrange
        uiState.value = uiState.value.copy(
            isOnline = false,
            hasLocalModelAvailable = true
        )

        // Act
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("Using local model").assertIsDisplayed()
    }

    @Test
    fun offlineBanner_shouldWarnWhenNoLocalModel() {
        // Arrange
        uiState.value = uiState.value.copy(
            isOnline = false,
            hasLocalModelAvailable = false
        )

        // Act
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithText("No local model available").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download a model to use offline").assertIsDisplayed()
    }

    @Test
    fun messageSourceIndicator_shouldShowLocalBadge() {
        // Arrange
        val messages = listOf(
            Message(
                messageId = UUID.randomUUID(),
                threadId = UUID.randomUUID(),
                role = MessageRole.ASSISTANT,
                text = "This is a local response",
                audioUri = null,
                imageUri = null,
                source = MessageSource.LOCAL_MODEL,
                latencyMs = 1200,
                createdAt = Instant.now(),
                errorCode = null
            )
        )

        uiState.value = uiState.value.copy(messages = messages)

        // Act
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithTag("source-badge-local").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Message from local model")
            .assertIsDisplayed()
    }

    @Test
    fun messageSourceIndicator_shouldShowCloudBadge() {
        // Arrange
        val messages = listOf(
            Message(
                messageId = UUID.randomUUID(),
                threadId = UUID.randomUUID(),
                role = MessageRole.ASSISTANT,
                text = "This is a cloud API response",
                audioUri = null,
                imageUri = null,
                source = MessageSource.CLOUD_API,
                latencyMs = 3500,
                createdAt = Instant.now(),
                errorCode = null
            )
        )

        uiState.value = uiState.value.copy(messages = messages)

        // Act
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithTag("source-badge-cloud").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Message from cloud API")
            .assertIsDisplayed()
    }

    @Test
    fun personaSwitchLog_shouldDisplayOverlay() {
        // Arrange
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Act: Open persona switch history
        composeTestRule.onNodeWithContentDescription("View persona history")
            .performClick()

        // Assert
        composeTestRule.onNodeWithText("Persona Switch History").assertIsDisplayed()
    }

    @Test
    fun personaSwitchLog_shouldShowTimeline() {
        // Arrange
        val logs = listOf(
            PersonaSwitchLog(
                logId = UUID.randomUUID(),
                threadId = UUID.randomUUID(),
                previousPersonaId = null,
                newPersonaId = UUID.randomUUID(),
                actionTaken = PersonaSwitchAction.START_NEW_THREAD,
                createdAt = Instant.now().minusSeconds(3600)
            ),
            PersonaSwitchLog(
                logId = UUID.randomUUID(),
                threadId = UUID.randomUUID(),
                previousPersonaId = UUID.randomUUID(),
                newPersonaId = UUID.randomUUID(),
                actionTaken = PersonaSwitchAction.CONTINUE_THREAD,
                createdAt = Instant.now().minusSeconds(1800)
            )
        )

        coEvery { viewModel.getPersonaSwitchLogs() } returns logs

        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Act
        composeTestRule.onNodeWithContentDescription("View persona history")
            .performClick()

        // Assert: Timeline should show both switches
        composeTestRule.onNodeWithText("Started new thread").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continued in thread").assertIsDisplayed()
    }

    @Test
    fun talkbackLabels_shouldDescribePersonaSelector() {
        // Arrange
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Assert: Accessibility labels exist
        composeTestRule.onNodeWithContentDescription("Select persona")
            .assertExists()
    }

    @Test
    fun talkbackLabels_shouldDescribeOfflineStatus() {
        // Arrange
        uiState.value = uiState.value.copy(
            isOnline = false,
            hasLocalModelAvailable = true
        )

        // Act
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithContentDescription("Offline indicator")
            .assertExists()
        composeTestRule.onNodeWithContentDescription("Using local model for responses")
            .assertExists()
    }

    @Test
    fun talkbackLabels_shouldDescribeMessageSource() {
        // Arrange
        val messages = listOf(
            Message(
                messageId = UUID.randomUUID(),
                threadId = UUID.randomUUID(),
                role = MessageRole.ASSISTANT,
                text = "Response",
                audioUri = null,
                imageUri = null,
                source = MessageSource.LOCAL_MODEL,
                latencyMs = 1200,
                createdAt = Instant.now(),
                errorCode = null
            )
        )

        uiState.value = uiState.value.copy(messages = messages)

        // Act
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Assert
        composeTestRule.onNodeWithContentDescription("Message from local model")
            .assertExists()
    }

    @Test
    fun onlineStatusChange_shouldUpdateUIReactively() {
        // Arrange
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        // Act: Simulate going offline
        uiState.value = uiState.value.copy(isOnline = false, hasLocalModelAvailable = true)
        composeTestRule.waitForIdle()

        // Assert
        composeTestRule.onNodeWithText("Offline Mode").assertIsDisplayed()

        // Act: Come back online
        uiState.value = uiState.value.copy(isOnline = true)
        composeTestRule.waitForIdle()

        // Assert: Offline banner should disappear
        composeTestRule.onNodeWithText("Offline Mode").assertDoesNotExist()
    }
}
