package com.henryai.aviationwatch.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.henryai.aviationwatch.ui.airport.AirportInfoScreen
import com.henryai.aviationwatch.ui.altimeter.AltimeterScreen
import com.henryai.aviationwatch.ui.clock.UtcClockScreen
import com.henryai.aviationwatch.ui.common.PlaceholderScreen
import com.henryai.aviationwatch.ui.directto.DirectToScreen
import com.henryai.aviationwatch.ui.engine.EngineTimeScreen
import com.henryai.aviationwatch.ui.engine.FlightLogScreen
import com.henryai.aviationwatch.ui.engine.SettingsScreen
import com.henryai.aviationwatch.ui.engine.SignInScreen
import com.henryai.aviationwatch.ui.home.HomeScreen
import com.henryai.aviationwatch.ui.nearest.NearestScreen
import com.henryai.aviationwatch.ui.search.SearchScreen
import com.henryai.aviationwatch.ui.theme.AviationWatchTheme
import com.henryai.aviationwatch.ui.weather.WeatherDetailScreen
import com.henryai.aviationwatch.ui.weather.WeatherScreen

/** Top-level destinations. Order here is the order on the home menu. */
enum class Destination(
    val route: String,
    val title: String,
    val subtitle: String,
    val phase: Int,
) {
    NEAREST("nearest", "Nearest", "Closest airports", 2),
    ENGINE_TIME("engine_time", "Engine Time", "Motor an · Start · Motor aus", 4),
    DIRECT_TO("direct_to", "Direct-To", "Bearing, distance, ETE, CDI", 2),
    SEARCH("search", "Airports", "Search ICAO, IATA, name", 2),
    WEATHER("weather", "Weather", "METAR / TAF nearby", 3),
    ALTIMETER("altimeter", "Altimeter", "Baro altitude, QNH", 1),
    UTC("utc", "Zulu Time", "UTC and local", 1),
    FLIGHT_LOG("flight_log", "Flight Log", "Engine times · Enginetime sync", 4),
    PULSE_OX("pulse_ox", "Pulse / SpO₂", "Heart rate, oxygen", 5),
    SETTINGS("settings", "Settings", "Registration, Enginetime account", 4),
    ;

    val implemented: Boolean get() = phase <= 4
}

private const val HOME_ROUTE = "home"
private const val AIRPORT_ROUTE = "airport/{ident}"
private const val WEATHER_ROUTE = "weather/{ident}"
private const val SIGN_IN_ROUTE = "sign_in"

private fun airportRoute(ident: String) = "airport/" + Uri.encode(ident)
private fun weatherRoute(ident: String) = "weather/" + Uri.encode(ident)

@Composable
fun AviationWatchApp() {
    AviationWatchTheme {
        AppScaffold {
            val navController = rememberSwipeDismissableNavController()
            SwipeDismissableNavHost(navController = navController, startDestination = HOME_ROUTE) {
                composable(HOME_ROUTE) {
                    HomeScreen(onOpen = { navController.navigate(it.route) })
                }
                composable(Destination.NEAREST.route) {
                    NearestScreen(onDirectTo = {
                        // Swiping back from Direct-To returns to the home menu, not to Nearest.
                        navController.navigate(Destination.DIRECT_TO.route) {
                            popUpTo(Destination.NEAREST.route) { inclusive = true }
                        }
                    })
                }
                composable(Destination.DIRECT_TO.route) {
                    DirectToScreen(
                        onOpenNearest = {
                            navController.navigate(Destination.NEAREST.route) {
                                popUpTo(Destination.DIRECT_TO.route) { inclusive = true }
                            }
                        },
                        onOpenAirport = { navController.navigate(airportRoute(it)) },
                    )
                }
                composable(Destination.SEARCH.route) {
                    SearchScreen(onOpenAirport = { navController.navigate(airportRoute(it)) })
                }
                composable(AIRPORT_ROUTE) { entry ->
                    AirportInfoScreen(
                        ident = entry.arguments?.getString("ident").orEmpty(),
                        onDirectTo = {
                            // Back from Direct-To goes to the home menu.
                            navController.navigate(Destination.DIRECT_TO.route) {
                                popUpTo(HOME_ROUTE)
                            }
                        },
                        onWeather = { navController.navigate(weatherRoute(it)) },
                    )
                }
                composable(Destination.WEATHER.route) {
                    WeatherScreen(onOpenStation = { navController.navigate(weatherRoute(it)) })
                }
                composable(WEATHER_ROUTE) { entry ->
                    WeatherDetailScreen(ident = entry.arguments?.getString("ident").orEmpty())
                }
                composable(Destination.ENGINE_TIME.route) {
                    EngineTimeScreen(
                        onOpenLog = { navController.navigate(Destination.FLIGHT_LOG.route) },
                        onOpenSettings = { navController.navigate(Destination.SETTINGS.route) },
                    )
                }
                composable(Destination.FLIGHT_LOG.route) { FlightLogScreen() }
                composable(Destination.SETTINGS.route) {
                    SettingsScreen(onSignIn = { navController.navigate(SIGN_IN_ROUTE) })
                }
                composable(SIGN_IN_ROUTE) { SignInScreen() }
                composable(Destination.UTC.route) { UtcClockScreen() }
                composable(Destination.ALTIMETER.route) { AltimeterScreen() }
                Destination.entries.filterNot { it.implemented }.forEach { destination ->
                    composable(destination.route) { PlaceholderScreen(destination) }
                }
            }
        }
    }
}
