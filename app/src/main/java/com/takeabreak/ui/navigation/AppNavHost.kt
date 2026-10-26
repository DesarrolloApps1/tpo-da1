package com.takeabreak.ui.navigation

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.navOptions
import com.takeabreak.R
import com.takeabreak.domain.model.Account
import com.takeabreak.presentation.app.AppViewModel
import com.takeabreak.ui.components.InitialAvatar
import com.takeabreak.ui.components.OfflineBanner
import com.takeabreak.ui.screens.AccessScreen
import com.takeabreak.ui.screens.ActivityScreen
import com.takeabreak.ui.screens.ExploreScreen
import com.takeabreak.ui.screens.HomeScreen
import com.takeabreak.ui.screens.OnboardingScreen
import com.takeabreak.ui.screens.PermissionsScreen
import com.takeabreak.ui.screens.ProfileScreen
import com.takeabreak.ui.screens.RedeemScreen
import com.takeabreak.ui.screens.RedemptionsScreen
import com.takeabreak.ui.screens.RewardDetailScreen
import com.takeabreak.ui.theme.Palette

private data class Tab(val route: Route, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Route.Home, R.string.tab_home, Icons.Filled.Home),
    Tab(Route.Explore, R.string.tab_explore, Icons.Filled.Place),
    Tab(Route.Activity, R.string.tab_activity, Icons.AutoMirrored.Filled.List),
)

// Antes de entrar: ahí no hay barra, ni avatar, ni aviso offline
private fun NavDestination.isBeforeSignIn() =
    hasRoute<Route.Onboarding>() || hasRoute<Route.Access>() || hasRoute<Route.Permissions>()

// Flujo de Figma: onboarding → acceso → permisos → pestañas (Inicio, Explorar, Actividad) + Perfil
@Composable
fun AppNavHost(
    viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory),
    navController: NavHostController = rememberNavController(),
) {
    val app by viewModel.state.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination

    // Splash mientras se lee el token guardado: el destino inicial depende de la sesión
    val signedIn = app.isSignedIn
    if (signedIn == null) {
        Box(Modifier.fillMaxSize().background(Palette.Forest))
        return
    }

    // Después de entrar se pasa por Permisos solo si falta el acceso a uso
    fun afterSignIn(): Route = if (viewModel.needsPermissions()) Route.Permissions else Route.Home

    // Con sesión arranca en Inicio, así el atrás de un deep link vuelve a Inicio y nunca al acceso
    val start = remember { if (signedIn) afterSignIn() else Route.Onboarding }

    // Al entrar sale del acceso; al cerrar sesión o recibir un 401 vuelve a él sin historial
    LaunchedEffect(signedIn, destination) {
        val current = destination ?: return@LaunchedEffect
        val onAccess = current.hasRoute<Route.Access>()
        when {
            signedIn && onAccess ->
                navController.navigate(afterSignIn()) { popUpTo<Route.Access> { inclusive = true } }
            !signedIn && !onAccess && !current.hasRoute<Route.Onboarding>() ->
                navController.navigate(Route.Access) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    NewIntentDeepLinks(navController, enabled = signedIn)

    val showTabs = tabs.any { destination?.hasRoute(it.route::class) == true }
    val onHome = destination?.hasRoute<Route.Home>() == true

    Scaffold(
        topBar = { if (onHome) HomeTopBar(app.account, onAvatarClick = { navController.navigate(Route.Profile) { launchSingleTop = true } }) },
        bottomBar = { if (showTabs) BottomBar(navController) },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (!app.isOnline && destination?.isBeforeSignIn() == false) OfflineBanner()
            NavHost(navController, startDestination = start, modifier = Modifier.weight(1f)) {
                // TODO(US-05): saltear el primer uso si ya se completó (flag en DataStore)
                composable<Route.Onboarding> {
                    OnboardingScreen(onDone = {
                        val next = if (signedIn) afterSignIn() else Route.Access
                        navController.navigate(next) { popUpTo<Route.Onboarding> { inclusive = true } }
                    })
                }
                composable<Route.Access> { AccessScreen() }
                composable<Route.Permissions> {
                    PermissionsScreen(onDone = {
                        navController.navigate(Route.Home) { popUpTo<Route.Permissions> { inclusive = true } }
                    })
                }
                composable<Route.Home>(deepLinks = listOf(navDeepLink { uriPattern = DeepLinks.HOME })) {
                    HomeScreen(onRewardClick = { navController.navigate(Route.RewardDetail(it)) })
                }
                composable<Route.Explore> {
                    ExploreScreen(onRewardClick = { navController.navigate(Route.RewardDetail(it)) })
                }
                composable<Route.Activity> { ActivityScreen() }
                composable<Route.RewardDetail>(deepLinks = listOf(navDeepLink<Route.RewardDetail>(basePath = DeepLinks.REWARD))) {
                    RewardDetailScreen(onRedeem = { navController.navigate(Route.Redeem(it)) })
                }
                composable<Route.Redeem> { RedeemScreen(onDone = { navController.popBackStack(Route.Home, inclusive = false) }) }
                composable<Route.Profile> {
                    ProfileScreen(
                        onBack = navController::navigateUp,
                        onRedemptionsClick = { navController.navigate(Route.Redemptions) { launchSingleTop = true } },
                    )
                }
                composable<Route.Redemptions>(deepLinks = listOf(navDeepLink { uriPattern = DeepLinks.REDEMPTIONS })) {
                    RedemptionsScreen()
                }
            }
        }
    }
}

// Con la app abierta o en segundo plano la notificación llega por onNewIntent (MainActivity es singleTop).
// En frío lo resuelve NavHost solo, con Inicio debajo. Acá se apila sobre Inicio sin rearmar la tarea
@Composable
private fun NewIntentDeepLinks(navController: NavHostController, enabled: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    DisposableEffect(activity, navController, enabled) {
        val listener = Consumer<Intent> { intent ->
            val uri = intent.data
            if (enabled && uri != null && navController.graph.hasDeepLink(uri)) {
                navController.navigate(uri, navOptions {
                    popUpTo<Route.Home>()
                    launchSingleTop = true
                })
            }
        }
        activity.addOnNewIntentListener(listener)
        onDispose { activity.removeOnNewIntentListener(listener) }
    }
}

// Lo que se usa poco va arriba (Fitts): el avatar abre Perfil (frame 08)
@Composable
private fun HomeTopBar(account: Account?, onAvatarClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.access_logo),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Palette.Forest,
        )
        Spacer(Modifier.weight(1f))
        val profileLabel = stringResource(R.string.profile_open)
        // TalkBack lee "Abrir Perfil" en vez de la inicial
        InitialAvatar(
            account = account,
            size = 48.dp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onAvatarClick)
                .clearAndSetSemantics { contentDescription = profileLabel },
        )
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
                    // Inicio es la raíz después de entrar: cada pestaña guarda y recupera su propia pila
                    navController.navigate(tab.route) {
                        popUpTo<Route.Home> { saveState = true }
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
