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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.takeabreak.R
import com.takeabreak.ui.screens.AccessScreen
import com.takeabreak.ui.screens.ActivityScreen
import com.takeabreak.ui.screens.ExploreScreen
import com.takeabreak.ui.screens.HomeScreen
import com.takeabreak.ui.screens.OnboardingScreen
import com.takeabreak.ui.screens.ProfileScreen
import com.takeabreak.ui.screens.RedeemScreen
import com.takeabreak.ui.screens.RedemptionsScreen
import com.takeabreak.ui.screens.RewardDetailScreen
import kotlinx.coroutines.flow.Flow

private data class Tab(val route: Route, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Route.Home, R.string.tab_home, Icons.Filled.Home),
    Tab(Route.Explore, R.string.tab_explore, Icons.Filled.Place),
    Tab(Route.Activity, R.string.tab_activity, Icons.AutoMirrored.Filled.List),
)

@Composable
fun AppNavHost(signedIn: Flow<Boolean>, navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val showTabs = tabs.any { backStack?.destination?.hasRoute(it.route::class) == true }
    // null mientras se lee el token guardado
    val isSignedIn by signedIn.collectAsStateWithLifecycle(initialValue = null)

    // Al entrar sale del acceso; al cerrar sesión o recibir un 401 vuelve a él sin historial
    LaunchedEffect(isSignedIn, backStack?.destination) {
        val destination = backStack?.destination ?: return@LaunchedEffect
        val onAccess = destination.hasRoute<Route.Access>()
        val onOnboarding = destination.hasRoute<Route.Onboarding>()
        when {
            isSignedIn == true && onAccess ->
                navController.navigate(Route.Home) { popUpTo<Route.Access> { inclusive = true } }
            isSignedIn == false && !onAccess && !onOnboarding ->
                navController.navigate(Route.Access) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    Scaffold(bottomBar = { if (showTabs) BottomBar(navController) }) { padding ->
        // TODO: saltear el primer uso si ya se completó (flag en DataStore)
        NavHost(navController, startDestination = Route.Onboarding, modifier = Modifier.padding(padding)) {
            composable<Route.Onboarding> {
                OnboardingScreen(onDone = {
                    // Con sesión guardada se saltea el acceso
                    val next = if (isSignedIn == true) Route.Home else Route.Access
                    navController.navigate(next) { popUpTo<Route.Onboarding> { inclusive = true } }
                })
            }
            composable<Route.Access> { AccessScreen() }
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
                deepLinks = listOf(navDeepLink<Route.RewardDetail>(basePath = DeepLinks.REWARD)),
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
