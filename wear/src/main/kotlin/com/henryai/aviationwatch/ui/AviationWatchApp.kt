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
import com.henryai.aviationwatch.ui.home.HomeScreen
import com.henryai.aviationwatch.ui.nearest.NearestScreen
import com.henryai.aviationwatch.ui.search.SearchScreen
import com.henryai.aviationwatch.ui.theme.AviationWatchTheme

/** Top-level destinations. Order here is the order on the home menu. */
enum class Destination(
    val route: String,
    val title: String,
    val subtitle: String,
    val phase: Int,
) {
    NEAREST("nearest", "Nearest", "Closest airports", 2),
    DIRECT_TO("direct_to", "Direct-To", "Bearing, distance, ETE, CDI", 2),
    SEARCH("search", "Airports", "Search ICAO, IATA, name", 2),
    WEATHER("weather", "Weather", "METAR / TAF", 3),
    ALTIMETER("altimeter", "Altimeter", "Baro altitude, QNH", 1),
    UTC("utc", "Zulu Time", "UTC and local", 1),
    TIMERS("timers", "Timers", "Flight / countdown", 4),
    FLIGHT_LOG("flight_log", "Flight Log", "Auto block & air time", 4),
    PULSE_OX("pulse_ox", "Pulse / SpO₂", "Heart rate, oxygen", 5),
    ;

    val implemented: Boolean get() = phase <= 2
}

private const val HOME_ROUTE = "home"
private const val AIRPORT_ROUTE = "airport/{ident}"

private fun airportRoute(ident: String) = "airport/" + Uri.encode(ident)

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
                    )
                }
                composable(Destination.UTC.route) { UtcClockScreen() }
                composable(Destination.ALTIMETER.route) { AltimeterScreen() }
                Destination.entries.filterNot { it.implemented }.forEach { destination ->
                    composable(destination.route) { PlaceholderScreen(destination) }
                }
            }
        }
    }
}
