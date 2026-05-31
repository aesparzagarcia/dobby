package com.ares.ewe.presentation.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
import com.ares.ewe.di.SessionEventBusEntryPoint
import com.ares.ewe.domain.model.FeaturedPlace
import dagger.hilt.android.EntryPointAccessors
import com.ares.ewe.presentation.ui.auth.register.AddUserInfoScreen
import com.ares.ewe.presentation.ui.auth.otp.OtpScreen
import com.ares.ewe.presentation.ui.auth.phone.PhoneScreen
import com.ares.ewe.presentation.ui.main.HomeScreen
import com.ares.ewe.presentation.ui.main.home.ActiveOrdersScreen
import com.ares.ewe.presentation.ui.main.home.AdDetailScreen
import com.ares.ewe.presentation.ui.main.home.AddressScreen
import com.ares.ewe.presentation.ui.main.home.CartScreen
import com.ares.ewe.presentation.ui.main.home.MapLocationScreen
import com.ares.ewe.presentation.ui.main.home.OrderTrackingScreen
import com.ares.ewe.presentation.ui.main.home.ProductScreen
import com.ares.ewe.presentation.ui.main.home.ServiceDetailScreen
import com.ares.ewe.presentation.ui.main.home.ShopDetailScreen
import com.ares.ewe.presentation.ui.splash.SplashScreen
import com.ares.ewe.presentation.viewmodel.main.home.HomeTabViewModel

@Composable
fun DobbyNavigation(
    pendingOrderTrackingId: String? = null,
    onPendingOrderTrackingNavigated: () -> Unit = {},
    pendingProductId: String? = null,
    pendingProductShopId: String? = null,
    onPendingProductNavigated: () -> Unit = {},
) {
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

    LaunchedEffect(pendingOrderTrackingId) {
        val orderId = pendingOrderTrackingId ?: return@LaunchedEffect
        snapshotFlow { navController.currentBackStackEntry?.destination?.route }
            .filter { route ->
                route != null &&
                    route != DobbyScreens.Splash &&
                    route != DobbyScreens.Phone &&
                    !route.startsWith("otp")
            }
            .first()
        navController.navigate(DobbyScreens.orderTracking(orderId)) {
            launchSingleTop = true
        }
        onPendingOrderTrackingNavigated()
    }

    LaunchedEffect(pendingProductId, pendingProductShopId) {
        val productId = pendingProductId ?: return@LaunchedEffect
        snapshotFlow { navController.currentBackStackEntry?.destination?.route }
            .filter { route ->
                route != null &&
                    route != DobbyScreens.Splash &&
                    route != DobbyScreens.Phone &&
                    !route.startsWith("otp")
            }
            .first()
        val shopId = pendingProductShopId?.takeIf { it.isNotEmpty() }
        navController.navigate(DobbyScreens.productDetail(productId, null, null, shopId)) {
            launchSingleTop = true
        }
        onPendingProductNavigated()
    }

    NavHost(
        modifier = Modifier.fillMaxSize(),
        navController = navController,
        startDestination = DobbyScreens.Splash,
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
                        navController.navigate(
                            DobbyScreens.shopDetail(place.id, place.name, place.latitude, place.longitude)
                        )
                    }
                },
                onAdClick = { adId ->
                    navController.navigate(DobbyScreens.adDetail(adId))
                },
                onAddressLabelClick = {
                    navController.navigate(DobbyScreens.DeliveryAddress)
                },
                onProductClick = { productId, shopId ->
                    navController.navigate(DobbyScreens.productDetail(productId, null, null, shopId))
                },
                onCartClick = { navController.navigate(DobbyScreens.Cart) },
                onTrackOrderClick = { orderId ->
                    navController.navigate(DobbyScreens.orderTracking(orderId))
                },
                onActiveOrdersClick = {
                    navController.navigate(DobbyScreens.ActiveOrders)
                },
            )
        }
        composable(DobbyScreens.ActiveOrders) {
            val homeTabViewModel: HomeTabViewModel = hiltViewModel(
                navController.getBackStackEntry(DobbyScreens.Home),
            )
            val homeState by homeTabViewModel.uiState.collectAsState()
            ActiveOrdersScreen(
                activeOrders = homeState.activeOrders,
                onBack = { navController.popBackStack() },
                onTrackOrderClick = { orderId ->
                    navController.navigate(DobbyScreens.orderTracking(orderId))
                },
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
                navArgument("name") { type = NavType.StringType },
                navArgument("pickupLat") { type = NavType.StringType; defaultValue = "none" },
                navArgument("pickupLng") { type = NavType.StringType; defaultValue = "none" },
            )
        ) {
            ShopDetailScreen(
                onBack = { navController.popBackStack() },
                onProductClick = { productId, pickupLat, pickupLng, shopId, shopAvailable ->
                    navController.navigate(
                        DobbyScreens.productDetail(productId, pickupLat, pickupLng, shopId, shopAvailable),
                    )
                },
                onCartClick = { navController.navigate(DobbyScreens.Cart) }
            )
        }
        composable(
            route = DobbyScreens.ProductDetail,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("pickupLat") { type = NavType.StringType; defaultValue = "none" },
                navArgument("pickupLng") { type = NavType.StringType; defaultValue = "none" },
                navArgument("shopId") { type = NavType.StringType; defaultValue = "none" },
                navArgument("shopAvailable") { type = NavType.BoolType; defaultValue = true },
            )
        ) {
            ProductScreen(
                onBack = { navController.popBackStack() },
                onAddToCartClick = { navController.navigate(DobbyScreens.Cart) },
                onCartClick = { navController.navigate(DobbyScreens.Cart) },
            )
        }
        composable(DobbyScreens.Cart) {
            val homeTabViewModel: HomeTabViewModel = hiltViewModel(
                navController.getBackStackEntry(DobbyScreens.Home)
            )
            CartScreen(
                onBack = { navController.popBackStack() },
                onCheckoutComplete = {
                    homeTabViewModel.refreshActiveOrder()
                    navController.popBackStack(DobbyScreens.Home, inclusive = false)
                },
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
            val homeTabViewModel: HomeTabViewModel = hiltViewModel(
                navController.getBackStackEntry(DobbyScreens.Home),
            )
            OrderTrackingScreen(
                onBack = { navController.popBackStack() },
                onFinish = {
                    navController.popBackStack(DobbyScreens.Home, inclusive = false)
                    homeTabViewModel.loadActiveOrder()
                },
            )
        }
    }
}
