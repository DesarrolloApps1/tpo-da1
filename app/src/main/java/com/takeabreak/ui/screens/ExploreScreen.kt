package com.takeabreak.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.takeabreak.BuildConfig
import com.takeabreak.R
import com.takeabreak.domain.model.Coordinates
import com.takeabreak.domain.usecase.RewardListItem
import com.takeabreak.presentation.explore.ExploreListing
import com.takeabreak.presentation.explore.ExploreViewModel
import com.takeabreak.ui.components.StateContent
import java.time.Duration
import java.time.Instant
import java.util.Locale

@Composable
private fun updatedAgo(synced: Instant): String {
    val minutes = Duration.between(synced, Instant.now()).toMinutes()
    return when {
        minutes < 1 -> stringResource(R.string.explore_updated_now)
        minutes < 60 -> stringResource(R.string.explore_updated_ago, stringResource(R.string.minutes, minutes.toInt()))
        else -> stringResource(R.string.explore_updated_ago, stringResource(R.string.hours, (minutes / 60).toInt()))
    }
}

// Frames 13 a 16: el mapa orienta y la lista, al alcance del pulgar, es la que se toca.
// Sin la API key de Maps (o si no cargan los tiles) queda solo la lista
@Composable
fun ExploreScreen(
    onRewardClick: (String) -> Unit,
    viewModel: ExploreViewModel = viewModel(factory = ExploreViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listing by viewModel.listing.collectAsStateWithLifecycle()
    val lastSync by viewModel.lastSync.collectAsStateWithLifecycle()

    // Se pide una sola vez y bajo demanda; si lo rechaza, la lista sigue de la A a la Z sin insistir
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) viewModel.locate()
    }

    StateContent(state, emptyText = stringResource(R.string.explore_empty), onRetry = viewModel::refresh) {
        Column(Modifier.fillMaxSize()) {
            if (BuildConfig.MAPS_API_KEY.isNotBlank()) RewardsMap(listing, Modifier.fillMaxWidth().height(MapHeight))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = listing.onlyAffordable,
                    onClick = viewModel::toggleAffordable,
                    label = { Text(stringResource(R.string.explore_only_affordable)) },
                )
                if (listing.here == null) {
                    TextButton(onClick = {
                        askLocation.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                    }) { Text(stringResource(R.string.explore_use_location)) }
                }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                lastSync?.let { synced ->
                    item { Text(updatedAgo(synced), Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge) }
                }
                if (listing.items.isEmpty() && listing.onlyAffordable) {
                    item { Text(stringResource(R.string.explore_none_affordable), Modifier.padding(16.dp)) }
                }
                items(listing.items, key = { it.reward.id }) { item -> RewardRow(item, onRewardClick) }
            }
        }
    }
}

@Composable
private fun RewardRow(item: RewardListItem, onRewardClick: (String) -> Unit) {
    val business = item.reward.business.name
    ListItem(
        headlineContent = { Text(item.reward.title) },
        supportingContent = {
            Text(item.distanceMeters?.let { stringResource(R.string.explore_business_distance, business, distanceLabel(it)) } ?: business)
        },
        trailingContent = {
            Text(
                stringResource(R.string.points, item.reward.points),
                color = if (item.affordable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.fillMaxWidth().clickable { onRewardClick(item.reward.id) },
    )
}

// Un marcador por comercio. Se centra en el usuario si hay ubicación; si no, en el primer comercio
@Composable
private fun RewardsMap(listing: ExploreListing, modifier: Modifier) {
    val businesses = listing.items.map { it.reward.business }.distinctBy { it.id }
    val center = listing.here ?: businesses.firstOrNull()?.location ?: BuenosAires
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(center.toLatLng(), 13f) }
    LaunchedEffect(listing.here) {
        listing.here?.let { camera.position = CameraPosition.fromLatLngZoom(it.toLatLng(), 14f) }
    }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        properties = MapProperties(isMyLocationEnabled = listing.here != null),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
    ) {
        businesses.forEach { business ->
            Marker(state = MarkerState(business.location.toLatLng()), title = business.name, snippet = business.address)
        }
    }
}

@Composable
private fun distanceLabel(meters: Int): String =
    if (meters < 1000) stringResource(R.string.explore_distance_m, meters)
    else stringResource(R.string.explore_distance_km, String.format(Locale.forLanguageTag("es"), "%.1f", meters / 1000.0))

private fun Coordinates.toLatLng() = LatLng(lat, lng)

private val MapHeight = 240.dp
private val BuenosAires = Coordinates(-34.6037, -58.3816)
