package com.android.joinme.ui.overview

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.android.joinme.model.group.Group
import com.android.joinme.ui.groups.GroupListScreen
import com.android.joinme.ui.groups.GroupListScreenTestTags
import com.android.joinme.viewmodel.GroupListUIState
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class GroupListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_isDisplayed() {
        composeTestRule.setContent {
            GroupListScreen(uiState = GroupListUIState(groups = emptyList()))
        }

        composeTestRule.onNodeWithTag(GroupListScreenTestTags.EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(GroupListScreenTestTags.ADD_NEW_GROUP).assertIsDisplayed()
    }

    @Test
    fun groupCards_areDisplayed() {
        val groups = listOf(
            Group(id = "1", name = "Football"),
            Group(id = "2", name = "Hiking")
        )

        composeTestRule.setContent {
            GroupListScreen(uiState = GroupListUIState(groups = groups))
        }

        composeTestRule.onNodeWithTag(GroupListScreenTestTags.EMPTY).assertDoesNotExist()
        composeTestRule.onNodeWithTag(GroupListScreenTestTags.LIST).assertIsDisplayed()
        composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("2")).assertIsDisplayed()
    }

    @Test
    fun cardClick_callsOnGroup() {
        val groups = listOf(
            Group(id = "1", name = "Football"),
            Group(id = "2", name = "Hiking")
        )
        var clickedName: String? = null

        composeTestRule.setContent {
            GroupListScreen(
                uiState = GroupListUIState(groups = groups),
                onGroup = { g -> clickedName = g.name }
            )
        }

        composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag("1")).performClick()

        assertEquals("Football", clickedName)
    }

    @Test
    fun moreMenuClick_callsOnMoreOptionMenu() {
        val groups = listOf(
            Group(id = "1", name = "Football"),
            Group(id = "2", name = "Hiking")
        )
        var moreClickedId: String? = null

        composeTestRule.setContent {
            GroupListScreen(
                uiState = GroupListUIState(groups = groups),
                onMoreOptionMenu = { g -> moreClickedId = g.id }
            )
        }
        composeTestRule.onNodeWithTag(GroupListScreenTestTags.moreTag("2")).performClick()
        assertEquals("2", moreClickedId)
    }

    @Test
    fun list_isScrollable_and_reaches_last_item() {
        val groups = (1..50).map { i -> Group(id = "$i", name = "Group $i") }
        val lastId = "50"

        composeTestRule.setContent {
            GroupListScreen(
                uiState = GroupListUIState(groups = groups)
            )
        }

        composeTestRule.onNodeWithTag(GroupListScreenTestTags.LIST)
            .performScrollToNode(hasTestTag(GroupListScreenTestTags.cardTag(lastId)))

        composeTestRule.onNodeWithTag(GroupListScreenTestTags.cardTag(lastId)).assertIsDisplayed()
    }
}