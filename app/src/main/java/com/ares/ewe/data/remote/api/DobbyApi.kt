package com.ares.ewe.data.remote.api

import com.ares.ewe.data.remote.model.AddressDto
import com.ares.ewe.data.remote.model.AdDto
import com.ares.ewe.data.remote.model.ActiveOrderDto
import com.ares.ewe.data.remote.model.CreateOrderRequest
import com.ares.ewe.data.remote.model.CreateOrderResponse
import com.ares.ewe.data.remote.model.OrderTrackingDto
import com.ares.ewe.data.remote.model.RateDeliveryRequest
import com.ares.ewe.data.remote.model.RateDeliveryResponse
import com.ares.ewe.data.remote.model.CompleteRegistrationRequest
import com.ares.ewe.data.remote.model.CreateAddressRequest
import com.ares.ewe.data.remote.model.CompleteRegistrationResponse
import com.ares.ewe.data.remote.model.HomeResponse
import com.ares.ewe.data.remote.model.PlacesResponse
import com.ares.ewe.data.remote.model.RequestOtpRequest
import com.ares.ewe.data.remote.model.RequestOtpResponse
import com.ares.ewe.data.remote.model.VerifyOtpRequest
import com.ares.ewe.data.remote.model.VerifyOtpResponse
import com.ares.ewe.data.remote.model.ServiceDetailDto
import com.ares.ewe.data.remote.model.ProductDetailDto
import com.ares.ewe.data.remote.model.GamificationDto
import com.ares.ewe.data.remote.model.PromotionProductDto
import com.ares.ewe.data.remote.model.ShopProductDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.Response

interface DobbyApi {

    @POST("auth/request-otp")
    suspend fun requestOtp(@Body request: RequestOtpRequest): RequestOtpResponse

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): VerifyOtpResponse

    @POST("auth/complete-registration")
    suspend fun completeRegistration(@Body request: CompleteRegistrationRequest): CompleteRegistrationResponse

    @GET("app/places")
    suspend fun getPlaces(): PlacesResponse

    @GET("app/home")
    suspend fun getHome(): HomeResponse

    @GET("app/me/gamification")
    suspend fun getGamification(): GamificationDto

    @GET("app/promotions")
    suspend fun getPromotions(): List<PromotionProductDto>

    @GET("app/shops/{id}/products")
    suspend fun getShopProducts(@Path("id") shopId: String): List<ShopProductDto>

    @GET("app/products/{id}")
    suspend fun getProduct(@Path("id") productId: String): ProductDetailDto

    @GET("app/services/{id}")
    suspend fun getService(@Path("id") serviceId: String): ServiceDetailDto

    @GET("app/ads")
    suspend fun getAds(): List<AdDto>

    @GET("app/ads/{id}")
    suspend fun getAd(@Path("id") id: String): AdDto

    @GET("addresses")
    suspend fun getAddresses(): List<AddressDto>

    @POST("addresses")
    suspend fun createAddress(@Body body: CreateAddressRequest): AddressDto

    @PATCH("addresses/{id}/default")
    suspend fun setDefaultAddress(@Path("id") id: String): Unit

    @GET("orders/active")
    suspend fun getActiveOrder(): Response<ActiveOrderDto>

    @GET("orders/{id}/tracking")
    suspend fun getOrderTracking(@Path("id") orderId: String): Response<OrderTrackingDto>

    @POST("orders/{id}/rate-delivery")
    suspend fun rateDelivery(
        @Path("id") orderId: String,
        @Body body: RateDeliveryRequest,
    ): Response<RateDeliveryResponse>

    @POST("orders")
    suspend fun createOrder(@Body body: CreateOrderRequest): CreateOrderResponse

    @DELETE("addresses/{id}")
    suspend fun deleteAddress(@Path("id") id: String): Unit
}