package com.takeabreak.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.takeabreak.R
import com.takeabreak.platform.notifications.AndroidNotifier
import com.takeabreak.ui.screens.ActivityScreen
import com.takeabreak.ui.screens.ExploreScreen
import com.takeabreak.ui.screens.HomeScreen
import com.takeabreak.ui.screens.OnboardingScreen
import com.takeabreak.ui.screens.ProfileScreen
import com.takeabreak.ui.screens.RedeemScreen
import com.takeabreak.ui.screens.RedemptionsScreen
import com.takeabreak.ui.screens.RewardDetailScreen

private data class Tab(val route: Route, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Route.Home, R.string.tab_home, Icons.Filled.Home),
    Tab(Route.Explore, R.string.tab_explore, Icons.Filled.Place),
    Tab(Route.Activity, R.string.tab_activity, Icons.AutoMirrored.Filled.List),
)

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val showTabs = tabs.any { backStack?.destination?.hasRoute(it.route::class) == true }

    Scaffold(bottomBar = { if (showTabs) BottomBar(navController) }) { padding ->
        // TODO: saltear el primer uso si ya se completó (flag en DataStore)
        NavHost(navController, startDestination = Route.Onboarding, modifier = Modifier.padding(padding)) {
            composable<Route.Onboarding> {
                OnboardingScreen(onDone = {
                    navController.navigate(Route.Home) { popUpTo<Route.Onboarding> { inclusive = true } }
                })
            }
            composable<Route.Home> {
                HomeScreen(
                    onRewardClick = { navController.navigate(Route.RewardDetail(it)) },
                    onProfileClick = { navController.navigate(Route.Profile) },
                )
            }
            composable<Route.Explore> {
                ExploreScreen(onRewardClick = { navController.navigate(Route.RewardDetail(it)) })
            }
            composable<Route.Activity> { ActivityScreen() }
            composable<Route.RewardDetail>(
                deepLinks = listOf(navDeepLink<Route.RewardDetail>(basePath = AndroidNotifier.DEEP_LINK_REWARD)),
            ) {
                RewardDetailScreen(onRedeem = { navController.navigate(Route.Redeem(it)) })
            }
            composable<Route.Redeem> { RedeemScreen(onDone = { navController.popBackStack(Route.Home, inclusive = false) }) }
            composable<Route.Profile> { ProfileScreen(onRedemptionsClick = { navController.navigate(Route.Redemptions) }) }
            composable<Route.Redemptions> { RedemptionsScreen() }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = backStack?.destination?.hasRoute(tab.route::class) == true,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}
