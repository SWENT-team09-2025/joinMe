package com.android.joinme.ui.navigation

//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries

import android.graphics.drawable.Icon
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

sealed class Tab(val name: String, val icon: ImageVector, val destination: Screen) {
  object Overview : Tab("Overview", Icons.Default.Menu, Screen.Overview)

  object Search : Tab("Search", Icons.Default.Search, Screen.Search)

  object Map : Tab("Map", Icons.Outlined.Place, Screen.Map)

  object Profile : Tab("Profile", Icons.Default.Person, Screen.Profile)
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
          modifier.fillMaxWidth().height(60.dp).testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU),
      content = {
        tabs.forEach { tab ->
          NavigationBarItem(
              selected = tab == selectedTab,
              onClick = { onTabSelected(tab) },
              icon = { Icon(tab.icon, contentDescription = null) },
              modifier = modifier.testTag(NavigationTestTags.tabTag(tab.name)))
        }
      },
  )
}
