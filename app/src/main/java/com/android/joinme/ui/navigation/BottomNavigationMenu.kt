package com.android.joinme.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.customColors

sealed class Tab(val name: String, val icon: ImageVector, val destination: Screen) {
  object Overview : Tab("Overview", Icons.Outlined.Menu, Screen.Overview)

  object Search : Tab("Search", Icons.Outlined.Search, Screen.Search)

  object Map : Tab("Map", Icons.Outlined.Place, Screen.Map())

  object Profile : Tab("Profile", Icons.Outlined.Person, Screen.Profile)
}

private val tabs = listOf(Tab.Overview, Tab.Search, Tab.Map, Tab.Profile)

@Composable
fun BottomNavigationMenu(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
  NavigationBar(
      modifier =
          modifier
              .fillMaxWidth()
              .height(Dimens.NavigationBar.height)
              .testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU),
      containerColor = MaterialTheme.customColors.containerColor,
      content = {
        tabs.forEach { tab ->
          NavigationBarItem(
              selected = tab == selectedTab,
              onClick = { onTabSelected(tab) },
              icon = {
                Icon(
                    tab.icon,
                    contentDescription = null,
                )
              },
              colors =
                  NavigationBarItemColors(
                      selectedIconColor = MaterialTheme.customColors.selectedIconColor,
                      selectedTextColor = MaterialTheme.customColors.selectedTextColor,
                      selectedIndicatorColor = MaterialTheme.customColors.selectedIndicatorColor,
                      unselectedIconColor = MaterialTheme.customColors.unselectedIconColor,
                      unselectedTextColor = MaterialTheme.customColors.unselectedTextColor,
                      disabledIconColor = MaterialTheme.customColors.disabledIconColor,
                      disabledTextColor = MaterialTheme.customColors.disabledTextColor,
                  ),
              modifier = modifier.testTag(NavigationTestTags.tabTag(tab.name)))
        }
      },
  )
}
