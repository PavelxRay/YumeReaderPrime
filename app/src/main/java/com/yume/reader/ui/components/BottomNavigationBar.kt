package com.yume.reader.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.yume.reader.R

@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentDestination: NavDestination?
) {
    val items = listOf(
        BottomNavItem(
            route = "library",
            label = "Библиотека",
            icon = Icons.Outlined.MenuBook  // Используем Material Icons
        ),
        BottomNavItem(
            route = "reading_now",
            label = "Читаю сейчас",
            icon = Icons.Outlined.PlayArrow
        ),
        BottomNavItem(
            route = "favorites",
            label = "Избранное",
            icon = Icons.Outlined.Favorite
        ),
        BottomNavItem(
            route = "profile",
            label = "Профиль",
            icon = Icons.Outlined.Person
        )
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                selected = currentDestination?.route == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector  // Изменили тип
)