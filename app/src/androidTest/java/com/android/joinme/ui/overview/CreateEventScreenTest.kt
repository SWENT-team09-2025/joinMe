package com.android.joinme.ui.overview

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class CreateEventScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** --- BASIC RENDERING --- */
    @Test
    fun allFieldsAndButtonAreDisplayed() {
        composeTestRule.setContent { CreateEventScreen(onDone = {}) }

        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT)
            .assertIsDisplayed()
    }

    /** --- INPUT BEHAVIOR --- */
    @Test
    fun enteringValidDataEnablesSaveButton() {
        composeTestRule.setContent { CreateEventScreen(onDone = {}) }

        // Type (dropdown)
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performClick()
        composeTestRule.onNodeWithText("SPORTS").performClick()

        // Normal text fields
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
            .performTextInput("Football Match")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
            .performTextInput("Friendly game")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION)
            .performTextInput("EPFL Field")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE)
            .performTextInput("25/12/2023 10:00")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
            .performTextInput("10")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION)
            .performTextInput("90")

        // Visibility (dropdown)
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
            .performClick()
        composeTestRule.onNodeWithText("PUBLIC").performClick()

        // Wait for Compose state updates
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT)
                .fetchSemanticsNodes()
                .firstOrNull()
                ?.config
                ?.getOrNull(SemanticsProperties.Disabled) == null
        }

        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT).assertIsEnabled()
    }

    @Test
    fun invalidMaxParticipants_showsErrorAndDisablesButton() {
        composeTestRule.setContent { CreateEventScreen(onDone = {}) }

        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
            .performTextInput("0")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT)
            .assertIsNotEnabled()
    }

    @Test
    fun invalidDuration_showsErrorAndDisablesButton() {
        composeTestRule.setContent { CreateEventScreen(onDone = {}) }

        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION)
            .performTextInput("-5")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT)
            .assertIsNotEnabled()
    }

    @Test
    fun invalidDateFormat_showsError() {
        composeTestRule.setContent { CreateEventScreen(onDone = {}) }

        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE)
            .performTextInput("12-25-2023")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /** --- EDGE CASES --- */
    @Test
    fun emptyTitle_disablesSaveButton() {
        composeTestRule.setContent { CreateEventScreen(onDone = {}) }

        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
            .performTextInput("")
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT)
            .assertIsNotEnabled()
    }

    @Test
    fun whitespaceInputsShouldBeTreatedAsEmpty() {
        composeTestRule.setContent { CreateEventScreen(onDone = {}) }

        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
            .performTextInput("   ")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
            .performTextInput("   ")
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT)
            .assertIsNotEnabled()
    }

    @Test
    fun switchingTypeRetainsOtherFields() {
        composeTestRule.setContent { CreateEventScreen(onDone = {}) }

        val title = "Morning Run"
        val desc = "Light jog near EPFL"
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
            .performTextInput(title)
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
            .performTextInput(desc)
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE)
            .performTextInput("SPORTS")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE)
            .performTextInput("SOCIAL")

        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
            .assertTextContains(title)
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
            .assertTextContains(desc)
    }

    @Test
    fun saveButtonRemainsDisabledUntilAllMandatoryFieldsAreFilled() {
        composeTestRule.setContent { CreateEventScreen(onDone = {}) }

        // Fill only some fields
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE)
            .performTextInput("SPORTS")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
            .performTextInput("Game")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE)
            .performTextInput("25/12/2023 10:00")
        // Missing others -> must be disabled
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT)
            .assertIsNotEnabled()
    }

    /** --- SAVE LOGIC --- */
    @Test
    fun clickingSaveAfterAllValidInputs_callsOnDone() {
        var saveCalled = false

        composeTestRule.setContent { CreateEventScreen(onDone = { saveCalled = true }) }

        // Select Type (dropdown)
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performClick()
        composeTestRule.onNodeWithText("SPORTS").performClick()

        // Fill the other fields
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
            .performTextInput("Basketball")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
            .performTextInput("Friendly 3v3")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION)
            .performTextInput("EPFL Gym")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE)
            .performTextInput("24/12/2023 15:30")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
            .performTextInput("6")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION)
            .performTextInput("60")

        // Select Visibility (dropdown)
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
            .performClick()
        composeTestRule.onNodeWithText("PUBLIC").performClick()

        // Wait until button becomes enabled (validation complete)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT)
                .fetchSemanticsNodes()
                .firstOrNull()
                ?.config
                ?.getOrNull(SemanticsProperties.Disabled) == null
        }

        // Click save
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT).performClick()

        // Assert callback called
        assert(saveCalled)
    }

    @Test
    fun switchingVisibilityUpdatesStateCorrectly() {
        composeTestRule.setContent { CreateEventScreen(onDone = {}) }

        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
            .performClick()
        composeTestRule.onNodeWithText("PRIVATE").performClick()
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
            .assertTextContains("PRIVATE")

        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
            .performClick()
        composeTestRule.onNodeWithText("PUBLIC").performClick()
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
            .assertTextContains("PUBLIC")
    }

    @Test
    fun correctingInvalidDuration_removesErrorAndEnablesButton() {
        composeTestRule.setContent { CreateEventScreen(onDone = {}) }

        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION)
            .performTextInput("-10")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION)
            .performTextClearance()
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION)
            .performTextInput("60")

        composeTestRule.onNodeWithTag(
            CreateEventScreenTestTags.ERROR_MESSAGE,
            useUnmergedTree = true
        )
            .assertDoesNotExist()
    }

    @Test
    fun maxParticipantsEqualToOne_isValidAndEnablesButtonWhenFormComplete() {
        composeTestRule.setContent { CreateEventScreen(onDone = {}) }

        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performClick()
        composeTestRule.onNodeWithText("SPORTS").performClick()

        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
            .performTextInput("Solo Run")
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
            .performTextInput("Morning jog")
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION)
            .performTextInput("EPFL Track")
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE)
            .performTextInput("01/01/2024 07:00")
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
            .performTextInput("1")
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION)
            .performTextInput("45")

        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY)
            .performClick()
        composeTestRule.onNodeWithText("PUBLIC").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT)
                .fetchSemanticsNodes()
                .firstOrNull()
                ?.config
                ?.getOrNull(SemanticsProperties.Disabled) == null
        }

        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT).assertIsEnabled()
    }

    @Test
    fun invalidThenValidDate_updatesErrorStateProperly() {
        composeTestRule.setContent { CreateEventScreen(onDone = {}) }

        val dateNode = composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE)
        dateNode.performTextInput("2023-12-25")
        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
            .assertIsDisplayed()

        dateNode.performTextClearance()
        dateNode.performTextInput("25/12/2023 18:00")

        composeTestRule
            .onNodeWithTag(CreateEventScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun saveButtonTransitionsFromDisabledToEnabled_whenAllFieldsFilled() {
        composeTestRule.setContent { CreateEventScreen(onDone = {}) }

        val saveButton = composeTestRule.onNodeWithTag(
            CreateEventScreenTestTags.BUTTON_SAVE_EVENT
        )
        saveButton.assertIsNotEnabled()

        // Fill everything except the last field (duration) to prove it's still disabled
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TYPE).performClick()
        composeTestRule.onNodeWithText("SPORTS").performClick()

        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_TITLE)
            .performTextInput("Run")
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DESCRIPTION)
            .performTextInput("Morning jog")
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_LOCATION)
            .performTextInput("EPFL Track")
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DATE)
            .performTextInput("25/12/2023 10:00")
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_MAX_PARTICIPANTS)
            .performTextInput("5")

        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_VISIBILITY).performClick()
        composeTestRule.onNodeWithText("PUBLIC").performClick()

        // Still disabled because duration is missing
        composeTestRule.waitForIdle()
        saveButton.assertIsNotEnabled()

        // Now fill the last required field
        composeTestRule.onNodeWithTag(CreateEventScreenTestTags.INPUT_EVENT_DURATION)
            .performTextInput("45")

        // Wait for recomposition/validation to complete
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag(CreateEventScreenTestTags.BUTTON_SAVE_EVENT)
                .fetchSemanticsNodes()
                .firstOrNull()
                ?.config
                ?.getOrNull(SemanticsProperties.Disabled) == null
        }

        saveButton.assertIsEnabled()
    }
}
