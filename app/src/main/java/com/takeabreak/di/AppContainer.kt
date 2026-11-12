package com.takeabreak.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.room.Room
import com.takeabreak.BuildConfig
import com.takeabreak.TakeABreakApp
import com.takeabreak.data.local.AppDatabase
import com.takeabreak.data.local.KeystoreTokenCipher
import com.takeabreak.data.local.SessionStore
import com.takeabreak.data.local.SettingsDataStore
import com.takeabreak.data.local.dataStore
import com.takeabreak.data.remote.AuthInterceptor
import com.takeabreak.data.remote.TakeABreakApi
import com.takeabreak.data.repository.AuthRepositoryImpl
import com.takeabreak.data.repository.BreakRepositoryImpl
import com.takeabreak.data.repository.PointsRepositoryImpl
import com.takeabreak.data.repository.RedemptionRepositoryImpl
import com.takeabreak.data.repository.RewardRepositoryImpl
import com.takeabreak.domain.repository.AuthRepository
import com.takeabreak.domain.repository.BreakRepository
import com.takeabreak.domain.repository.GoogleIdTokenProvider
import com.takeabreak.domain.repository.NetworkMonitor
import com.takeabreak.domain.repository.PermissionChecker
import com.takeabreak.domain.repository.PointsRepository
import com.takeabreak.domain.repository.RedemptionRepository
import com.takeabreak.domain.repository.RewardRepository
import com.takeabreak.domain.repository.SettingsRepository
import com.takeabreak.domain.rules.BreakRules
import com.takeabreak.domain.usecase.ConfirmDelivery
import com.takeabreak.domain.usecase.GetAccount
import com.takeabreak.domain.usecase.GetBreakHistory
import com.takeabreak.domain.usecase.GetHomeSummary
import com.takeabreak.domain.usecase.GetProfile
import com.takeabreak.domain.usecase.GetRedemptions
import com.takeabreak.domain.usecase.GetRewards
import com.takeabreak.domain.usecase.ObserveSession
import com.takeabreak.domain.usecase.RedeemReward
import com.takeabreak.domain.usecase.SetGoal
import com.takeabreak.domain.usecase.SetNotificationsEnabled
import com.takeabreak.domain.usecase.SignInWithEmail
import com.takeabreak.domain.usecase.SignInWithGoogle
import com.takeabreak.domain.usecase.SignOut
import com.takeabreak.domain.usecase.SyncBreaks
import com.takeabreak.domain.usecase.SyncPoints
import com.takeabreak.platform.auth.CredentialManagerGoogleProvider
import com.takeabreak.platform.location.FusedLocationProvider
import com.takeabreak.platform.location.LocationProvider
import com.takeabreak.platform.network.AndroidNetworkMonitor
import com.takeabreak.platform.notifications.AndroidNotifier
import com.takeabreak.platform.notifications.Notifier
import com.takeabreak.platform.notifications.PreferenceGatedNotifier
import com.takeabreak.platform.permissions.AndroidPermissionChecker
import com.takeabreak.platform.usage.AndroidUsageEventsReader
import com.takeabreak.platform.usage.UsageEventsReader
import com.takeabreak.platform.workers.BreakSync
import com.takeabreak.platform.workers.WorkScheduler
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

// Inyección manual: cada dependencia se crea una sola vez, cuando se necesita
// baseUrl y database quedan a mano para que los tests instrumentados usen un servidor falso
class AppContainer(context: Context, private val baseUrl: String = BuildConfig.BASE_URL) {

    private val appContext = context.applicationContext

    internal val database by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "takeabreak.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    private val sessionStore by lazy { SessionStore(appContext.dataStore, KeystoreTokenCipher()) }

    private val api: TakeABreakApi by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(sessionStore)).build()
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create()
    }

    // Repositorios
    val settings: SettingsRepository by lazy { SettingsDataStore(appContext) }
    private val breakRepository: BreakRepository by lazy { BreakRepositoryImpl(database.breakDao()) }
    private val rewardRepository: RewardRepository by lazy { RewardRepositoryImpl(api, database.catalogDao(), settings) }
    private val redemptionRepository: RedemptionRepository by lazy {
        RedemptionRepositoryImpl(api, database.redemptionDao(), ::schedulePointsSync)
    }
    private val pointsRepository: PointsRepository by lazy { PointsRepositoryImpl(api, sessionStore) }

    private val authRepository: AuthRepository by lazy { AuthRepositoryImpl(api, sessionStore) }

    // Plataforma
    val networkMonitor: NetworkMonitor by lazy { AndroidNetworkMonitor(appContext) }
    val usageEventsReader: UsageEventsReader by lazy { AndroidUsageEventsReader(appContext) }
    // El interruptor de Perfil envuelve al notificador real
    val notifier: Notifier by lazy { PreferenceGatedNotifier(AndroidNotifier(appContext), settings) }
    val locationProvider: LocationProvider by lazy { FusedLocationProvider(appContext) }
    val permissionChecker: PermissionChecker by lazy { AndroidPermissionChecker(appContext) }

    // Sin client ID en local.properties el botón de Google queda deshabilitado
    val isGoogleSignInEnabled: Boolean = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    // No se guarda: necesita la Activity actual
    fun googleIdTokenProvider(activityContext: Context): GoogleIdTokenProvider =
        CredentialManagerGoogleProvider(activityContext, BuildConfig.GOOGLE_WEB_CLIENT_ID)

    // Casos de uso
    // Una sola configuración de reglas para toda la app
    val breakRules by lazy { BreakRules() }
    val syncBreaks by lazy { SyncBreaks(breakRepository, settings, breakRules) }
    val getHomeSummary by lazy { GetHomeSummary(breakRepository, redemptionRepository, rewardRepository, settings, pointsRepository) }
    val getRewards by lazy { GetRewards(rewardRepository) }
    val redeemReward by lazy { RedeemReward(redemptionRepository) }
    val setGoal by lazy { SetGoal(settings, breakRules) }
    val setNotificationsEnabled by lazy { SetNotificationsEnabled(settings) }
    val getProfile by lazy { GetProfile(authRepository, breakRepository, redemptionRepository, rewardRepository, settings) }
    val getBreakHistory by lazy { GetBreakHistory(breakRepository, settings) }
    val getRedemptions by lazy { GetRedemptions(redemptionRepository) }
    val signInWithGoogle by lazy { SignInWithGoogle(authRepository) }
    val signInWithEmail by lazy { SignInWithEmail(authRepository) }
    val signOut by lazy { SignOut(authRepository) }

    // Una sola instancia para todo el proceso: su Mutex es lo que evita dos sincronizaciones a la vez
    val breakSync by lazy { BreakSync(usageEventsReader, settings, syncBreaks, getHomeSummary, notifier, ::schedulePointsSync) }
    val syncPoints by lazy { SyncPoints(breakRepository, pointsRepository) }
    val confirmDelivery by lazy { ConfirmDelivery(redemptionRepository) }
    val observeSession by lazy { ObserveSession(authRepository) }

    // Queda en cola hasta que haya red, así los puntos de una tarde sin señal se informan solos
    fun schedulePointsSync() = WorkScheduler.syncPoints(appContext)

    val getAccount by lazy { GetAccount(authRepository) }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as TakeABreakApp).container

// Atajo para las factories de los ViewModels
val CreationExtras.appContainer: AppContainer
    get() = (this[APPLICATION_KEY] as TakeABreakApp).container
