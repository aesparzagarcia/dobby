package com.ares.ewe.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ares.ewe.di.SessionEventBusEntryPoint
import com.ares.ewe.domain.model.FeaturedPlace
import dagger.hilt.android.EntryPointAccessors
import com.ares.ewe.presentation.ui.auth.register.AddUserInfoScreen
import com.ares.ewe.presentation.ui.auth.otp.OtpScreen
import com.ares.ewe.presentation.ui.auth.phone.PhoneScreen
import com.ares.ewe.presentation.ui.main.HomeScreen
import com.ares.ewe.presentation.ui.main.home.AdDetailScreen
import com.ares.ewe.presentation.ui.main.home.AddressScreen
import com.ares.ewe.presentation.ui.main.home.CartScreen
import com.ares.ewe.presentation.ui.main.home.MapLocationScreen
import com.ares.ewe.presentation.ui.main.home.OrderTrackingScreen
import com.ares.ewe.presentation.ui.main.home.ProductScreen
import com.ares.ewe.presentation.ui.main.home.ServiceDetailScreen
import com.ares.ewe.presentation.ui.main.home.ShopDetailScreen
import com.ares.ewe.presentation.ui.splash.SplashScreen

@Composable
fun DobbyNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionEventBus = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SessionEventBusEntryPoint::class.java
        ).sessionEventBus()
    }
    LaunchedEffect(sessionEventBus) {
        sessionEventBus.sessionExpired.collect {
            navController.navigate(DobbyScreens.Phone) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    NavHost(
        navController = navController,
        startDestination = DobbyScreens.Splash
    ) {
        composable(DobbyScreens.Splash) {
            SplashScreen(
                onOpenAuth = {
                    navController.navigate(DobbyScreens.Phone) {
                        popUpTo(DobbyScreens.Splash) { inclusive = true }
                    }
                },
                onOpenHome = {
                    navController.navigate(DobbyScreens.Home) {
                        popUpTo(DobbyScreens.Splash) { inclusive = true }
                    }
                }
            )
        }
        composable(DobbyScreens.Phone) {
            PhoneScreen(
                onCodeSent = { phone, userExists ->
                    navController.navigate(DobbyScreens.otp(phone, userExists))
                }
            )
        }
        composable(
            route = DobbyScreens.Otp,
            arguments = listOf(
                navArgument("phone") { type = NavType.StringType },
                navArgument("userExists") { type = NavType.BoolType }
            )
        ) {
            OtpScreen(
                onLoggedIn = {
                    navController.navigate(DobbyScreens.Home) {
                        popUpTo(DobbyScreens.Phone) { inclusive = true }
                    }
                },
                onRequiresRegistration = { phone ->
                    navController.navigate(DobbyScreens.addUserInfo(phone)) {
                        popUpTo(DobbyScreens.Phone) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = DobbyScreens.AddUserInfo,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) {
            AddUserInfoScreen(
                onComplete = {
                    navController.navigate(DobbyScreens.Home) {
                        popUpTo(DobbyScreens.Phone) { inclusive = true }
                    }
                }
            )
        }
        composable(DobbyScreens.Home) {
            HomeScreen(
                onLogout = {
                    navController.navigate(DobbyScreens.Phone) {
                        popUpTo(DobbyScreens.Home) { inclusive = true }
                    }
                },
                onPlaceClick = { place ->
                    if (place.isService) {
                        navController.navigate(DobbyScreens.serviceDetail(place.id))
                    } else {
                        navController.navigate(DobbyScreens.shopDetail(place.id, place.name))
                    }
                },
                onAdClick = { adId ->
                    navController.navigate(DobbyScreens.adDetail(adId))
                },
                onAddressLabelClick = {
                    navController.navigate(DobbyScreens.DeliveryAddress)
                },
                onProductClick = { productId ->
                    navController.navigate(DobbyScreens.productDetail(productId))
                },
                onCartClick = { navController.navigate(DobbyScreens.Cart) },
                onTrackOrderClick = { orderId ->
                    navController.navigate(DobbyScreens.orderTracking(orderId))
                }
            )
        }
        composable(DobbyScreens.DeliveryAddress) {
            AddressScreen(
                onBack = { navController.popBackStack() },
                onCurrentLocationClick = { navController.navigate(DobbyScreens.CurrentLocationMap) },
                onNavigateToMapWithLocation = { lat, lng, address ->
                    navController.navigate(DobbyScreens.currentLocationMapWithLocation(lat, lng, address))
                }
            )
        }
        composable(DobbyScreens.CurrentLocationMap) {
            MapLocationScreen(
                onBack = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack(DobbyScreens.Home, false)
                }
            )
        }
        composable(
            route = DobbyScreens.CurrentLocationMapWithLocation,
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lng") { type = NavType.StringType },
                navArgument("address") { type = NavType.StringType; defaultValue = "" }
            )
        ) {
            MapLocationScreen(
                onBack = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack(DobbyScreens.Home, false)
                }
            )
        }
        composable(
            route = DobbyScreens.AdDetail,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            AdDetailScreen(
                onBack = { navController.popBackStack() },
                onCartClick = { navController.navigate(DobbyScreens.Cart) }
            )
        }
        composable(
            route = DobbyScreens.ShopDetail,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) {
            ShopDetailScreen(
                onBack = { navController.popBackStack() },
                onProductClick = { productId ->
                    navController.navigate(DobbyScreens.productDetail(productId))
                },
                onCartClick = { navController.navigate(DobbyScreens.Cart) }
            )
        }
        composable(
            route = DobbyScreens.ProductDetail,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            ProductScreen(
                onBack = { navController.popBackStack() },
                onAddToCartClick = { navController.navigate(DobbyScreens.Cart) },
                onCartClick = { navController.navigate(DobbyScreens.Cart) }
            )
        }
        composable(DobbyScreens.Cart) {
            CartScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = DobbyScreens.ServiceDetail,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            ServiceDetailScreen(
                onBack = { navController.popBackStack() },
                onCartClick = { navController.navigate(DobbyScreens.Cart) }
            )
        }
        composable(
            route = DobbyScreens.OrderTracking,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) {
            OrderTrackingScreen(onBack = { navController.popBackStack() })
        }
    }
}
