package com.ares.ewe.presentation.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.ares.ewe.core.crash.TrackNavDestination
import com.ares.ewe.di.OrderRepositoryEntryPoint
import com.ares.ewe.di.SessionEventBusEntryPoint
import com.ares.ewe.di.CartRepositoryEntryPoint
import com.ares.ewe.domain.cart.CartShopSwitchPolicy
import com.ares.ewe.domain.model.FeaturedPlace
import com.ares.ewe.push.OrderPushNavigation
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import com.ares.ewe.presentation.ui.main.profile.OrderHistoryScreen
import com.ares.ewe.presentation.ui.main.home.ProductScreen
import com.ares.ewe.presentation.ui.main.home.ServiceDetailScreen
import com.ares.ewe.presentation.ui.main.home.BestSellersScreen
import com.ares.ewe.presentation.ui.main.home.FeaturedPlacesScreen
import com.ares.ewe.presentation.ui.main.home.ShopDetailScreen
import com.ares.ewe.presentation.ui.components.ShopSwitchConfirmDialog
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
    TrackNavDestination(navController)
    val context = LocalContext.current
    val cartRepository = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            CartRepositoryEntryPoint::class.java,
        ).cartRepository()
    }
    val cartItems by cartRepository.items.collectAsState(initial = emptyList())
    var pendingShopPlace by remember { mutableStateOf<FeaturedPlace?>(null) }

    fun navigateToShop(place: FeaturedPlace) {
        navController.navigate(
            DobbyScreens.shopDetail(place.id, place.name, place.latitude, place.longitude),
        )
    }

    fun handlePlaceClick(place: FeaturedPlace) {
        if (place.isService) {
            navController.navigate(DobbyScreens.serviceDetail(place.id))
            return
        }
        if (CartShopSwitchPolicy.needsConfirmation(cartItems, place.id)) {
            pendingShopPlace = place
        } else {
            navigateToShop(place)
        }
    }

    pendingShopPlace?.let {
        ShopSwitchConfirmDialog(
            onConfirm = {
                cartRepository.clear()
                navigateToShop(it)
                pendingShopPlace = null
            },
            onDismiss = { pendingShopPlace = null },
        )
    }

    val sessionEventBus = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SessionEventBusEntryPoint::class.java
        ).sessionEventBus()
    }
    LaunchedEffect(sessionEventBus) {
        sessionEventBus.sessionExpired.collect {
            navController.navigate(DobbyScreens.Home) {
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
        val orderRepository = EntryPointAccessors.fromApplication(
            context.applicationContext,
            OrderRepositoryEntryPoint::class.java,
        ).orderRepository()
        val openTracking = withContext(Dispatchers.IO) {
            orderRepository.getOrderTracking(orderId).getOrNull()?.let { tracking ->
                tracking != null && OrderPushNavigation.canOpenTracking(tracking.status)
            } == true
        }
        if (openTracking) {
            navController.navigate(DobbyScreens.orderTracking(orderId)) {
                launchSingleTop = true
            }
        } else {
            navController.popBackStack(DobbyScreens.Home, false)
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
                },
            )
        }
        composable(DobbyScreens.Phone) {
            PhoneScreen(
                onCodeSent = { phone, userExists ->
                    navController.navigate(DobbyScreens.otp(phone, userExists))
                },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(DobbyScreens.Home) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
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
                        popUpTo(DobbyScreens.Home) { inclusive = true }
                        launchSingleTop = true
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
                        popUpTo(DobbyScreens.Home) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = {
                    navController.navigate(DobbyScreens.Phone) {
                        popUpTo(DobbyScreens.AddUserInfo) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(DobbyScreens.Home) {
            HomeScreen(
                onLogout = {
                    navController.navigate(DobbyScreens.Home) {
                        popUpTo(DobbyScreens.Home) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRequireLogin = {
                    navController.navigate(DobbyScreens.Phone)
                },
                onPlaceClick = ::handlePlaceClick,
                onAdClick = { adId ->
                    navController.navigate(DobbyScreens.adDetail(adId))
                },
                onAddressLabelClick = {
                    navController.navigate(DobbyScreens.DeliveryAddress)
                },
                onProductClick = { productId, shopId, shopAvailable ->
                    navController.navigate(
                        DobbyScreens.productDetail(productId, null, null, shopId, shopAvailable),
                    )
                },
                onCartClick = { navController.navigate(DobbyScreens.Cart) },
                onTrackOrderClick = { orderId ->
                    navController.navigate(DobbyScreens.orderTracking(orderId))
                },
                onActiveOrdersClick = {
                    navController.navigate(DobbyScreens.ActiveOrders)
                },
                onOrderHistoryClick = {
                    navController.navigate(DobbyScreens.OrderHistory)
                },
                onBestSellersClick = {
                    navController.navigate(DobbyScreens.BestSellers)
                },
                onFeaturedPlacesClick = {
                    navController.navigate(DobbyScreens.FeaturedPlaces)
                },
            )
        }
        composable(DobbyScreens.FeaturedPlaces) {
            FeaturedPlacesScreen(
                onBack = { navController.popBackStack() },
                onPlaceClick = ::handlePlaceClick,
            )
        }
        composable(DobbyScreens.BestSellers) {
            BestSellersScreen(
                onBack = { navController.popBackStack() },
                onProductClick = { productId, pickupLat, pickupLng, shopId, shopAvailable ->
                    navController.navigate(
                        DobbyScreens.productDetail(productId, pickupLat, pickupLng, shopId, shopAvailable),
                    )
                },
                onCartClick = { navController.navigate(DobbyScreens.Cart) },
            )
        }
        composable(DobbyScreens.OrderHistory) {
            OrderHistoryScreen(
                onBack = { navController.popBackStack() },
                onOrderClick = { orderId ->
                    navController.navigate(DobbyScreens.orderTracking(orderId, returnToHistory = true))
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
            val homeTabViewModel: HomeTabViewModel = hiltViewModel(
                navController.getBackStackEntry(DobbyScreens.Home),
            )
            AddressScreen(
                onBack = {
                    homeTabViewModel.loadAddresses()
                    navController.popBackStack()
                },
                onCurrentLocationClick = { navController.navigate(DobbyScreens.CurrentLocationMap) },
                onNavigateToMapWithLocation = { lat, lng, address ->
                    navController.navigate(DobbyScreens.currentLocationMapWithLocation(lat, lng, address))
                },
                onRequireLogin = {
                    navController.popBackStack()
                    navController.navigate(DobbyScreens.Phone)
                },
            )
        }
        composable(DobbyScreens.CurrentLocationMap) {
            val homeTabViewModel: HomeTabViewModel = hiltViewModel(
                navController.getBackStackEntry(DobbyScreens.Home),
            )
            MapLocationScreen(
                onBack = { navController.popBackStack() },
                onSaveSuccess = {
                    homeTabViewModel.loadAddresses()
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
            val homeTabViewModel: HomeTabViewModel = hiltViewModel(
                navController.getBackStackEntry(DobbyScreens.Home),
            )
            MapLocationScreen(
                onBack = { navController.popBackStack() },
                onSaveSuccess = {
                    homeTabViewModel.loadAddresses()
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
                    homeTabViewModel.refreshActiveOrderAfterCheckout()
                    navController.popBackStack(DobbyScreens.Home, inclusive = false)
                },
                onRequireLogin = {
                    navController.navigate(DobbyScreens.Phone)
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
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType },
                navArgument("returnToHistory") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) {
            val returnToHistory = it.arguments?.getBoolean("returnToHistory") == true
            val homeTabViewModel: HomeTabViewModel = hiltViewModel(
                navController.getBackStackEntry(DobbyScreens.Home),
            )
            OrderTrackingScreen(
                onBack = { navController.popBackStack() },
                onFinish = {
                    if (returnToHistory) {
                        navController.popBackStack()
                    } else {
                        navController.popBackStack(DobbyScreens.Home, inclusive = false)
                        homeTabViewModel.loadActiveOrder()
                    }
                },
            )
        }
    }
}
