package com.agroapp.network

import com.agroapp.model.*
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<MessageResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @GET("auth/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<UserProfile>

    @PATCH("auth/profile")
    suspend fun updateProfile(@Header("Authorization") token: String, @Body body: UpdateProfileRequest): Response<UpdateProfileResponse>

    @PATCH("auth/password")
    suspend fun changePassword(@Header("Authorization") token: String, @Body body: ChangePasswordRequest): Response<MessageResponse>

    @POST("auth/avatar")
    suspend fun uploadAvatar(@Header("Authorization") token: String, @Body body: Map<String, String>): Response<Map<String, Any>>

    @POST("admin/products/upload-image")
    suspend fun uploadProductImage(@Header("Authorization") token: String, @Body body: Map<String, String>): Response<Map<String, Any>>

    @GET("products")
    suspend fun getProducts(): Response<List<Product>>

    @GET("products/category/{id}")
    suspend fun getProductsByCategory(@Path("id") categoryId: Int): Response<List<Product>>

    @POST("orders")
    suspend fun createOrder(@Header("Authorization") token: String, @Body body: CreateOrderRequest): Response<Order>

    @GET("orders/my")
    suspend fun getMyOrders(@Header("Authorization") token: String): Response<List<Order>>

    @GET("orders/active")
    suspend fun getActiveOrder(@Header("Authorization") token: String): Response<ActiveOrderResponse>

    @PATCH("orders/{id}/cancel")
    suspend fun cancelOrder(@Header("Authorization") token: String, @Path("id") orderId: String): Response<Order>

    @GET("vendor/orders/by-client")
    suspend fun getOrdersByClient(@Header("Authorization") token: String, @Query("date") date: String? = null): Response<List<OrderByClient>>

    @GET("vendor/orders/by-product")
    suspend fun getOrdersByProduct(@Header("Authorization") token: String, @Query("date") date: String? = null): Response<List<OrderByProduct>>

    @PATCH("vendor/orders/{id}/status")
    suspend fun updateOrderStatus(@Header("Authorization") token: String, @Path("id") orderId: String, @Body body: UpdateStatusRequest): Response<Order>

    @POST("orders/pending-yappi")
    suspend fun createPendingYappiOrder(@Header("Authorization") token: String, @Body body: CreatePendingOrderRequest): Response<CreatePendingOrderResponse>

    @POST("orders/{id}/confirm-yappi")
    suspend fun confirmYappiPayment(@Header("Authorization") token: String, @Path("id") orderId: String, @Body body: ConfirmPaymentRequest): Response<ConfirmPaymentResponse>

    @POST("payments/create-intent")
    suspend fun createPaymentIntent(@Header("Authorization") token: String, @Body body: PaymentIntentRequest): Response<PaymentIntentResponse>

    @GET("driver/blocks/available")
    suspend fun getAvailableBlocks(@Header("Authorization") token: String): Response<List<DeliveryBlock>>

    @GET("driver/blocks/my")
    suspend fun getMyBlocks(@Header("Authorization") token: String): Response<List<DriverBlock>>

    @GET("driver/earnings")
    suspend fun getDriverEarnings(@Header("Authorization") token: String): Response<DriverEarnings>

    @POST("driver/blocks/take")
    suspend fun takeBlock(@Header("Authorization") token: String, @Body body: TakeBlockRequest): Response<MessageResponse>

    @GET("driver/packages/available")
    suspend fun getAvailablePackages(@Header("Authorization") token: String): Response<List<DynamicPackage>>

    @POST("driver/packages/take")
    suspend fun takePackage(@Header("Authorization") token: String, @Body body: TakePackageRequest): Response<TakePackageResponse>

    @GET("driver/packages/my")
    suspend fun getMyPackages(@Header("Authorization") token: String): Response<List<DynamicPackage>>

    @GET("driver/earnings/packages")
    suspend fun getDriverPackageEarnings(@Header("Authorization") token: String): Response<DriverPackageEarnings>

    @PATCH("driver/orders/{id}/status")
    suspend fun updateDeliveryOrderStatus(@Header("Authorization") token: String, @Path("id") orderId: String, @Body body: UpdateStatusRequest): Response<Order>

    @POST("driver/orders/{orderId}/start-trip")
    suspend fun startTrip(@Header("Authorization") token: String, @Path("orderId") orderId: String): Response<MessageResponse>

    @POST("driver/orders/{orderId}/cancel")
    suspend fun cancelDriverOrder(@Header("Authorization") token: String, @Path("orderId") orderId: String): Response<MessageResponse>

    @POST("driver/location")
    suspend fun updateDriverLocation(@Header("Authorization") token: String, @Body body: DriverLocationRequest): Response<MessageResponse>

    @GET("driver/location/{orderId}")
    suspend fun getDriverLocation(@Header("Authorization") token: String, @Path("orderId") orderId: String): Response<DriverLocationResponse>

    @GET("driver/location/by-driver/{driverId}")
    suspend fun getDriverLocationByDriver(@Header("Authorization") token: String, @Path("driverId") driverId: String): Response<DriverLocationResponse>

    @GET("admin/dashboard/stats")
    suspend fun getAdminDashboardStats(@Header("Authorization") token: String): Response<AdminDashboardStats>

    @GET("admin/products")
    suspend fun getAllProducts(@Header("Authorization") token: String, @Query("category") categoryId: Int? = null, @Query("search") search: String? = null, @Query("low_stock") lowStock: Boolean? = null): Response<List<ProductWithInventory>>

    @POST("admin/products")
    suspend fun createProduct(@Header("Authorization") token: String, @Body body: CreateProductRequest): Response<Product>

    @PATCH("admin/products/{id}")
    suspend fun updateProduct(@Header("Authorization") token: String, @Path("id") productId: Int, @Body body: UpdateProductRequest): Response<Product>

    @PATCH("admin/products/{id}/stock")
    suspend fun updateProductStock(@Header("Authorization") token: String, @Path("id") productId: Int, @Body body: UpdateStockRequest): Response<MessageResponse>

    @DELETE("admin/products/{id}")
    suspend fun deleteProduct(@Header("Authorization") token: String, @Path("id") productId: Int): Response<MessageResponse>

    @GET("admin/drivers/payments")
    suspend fun getDriverPayments(@Header("Authorization") token: String, @Query("status") status: String? = null, @Query("driver_id") driverId: String? = null, @Query("week_start") weekStart: String? = null): Response<List<DriverPayment>>

    @POST("admin/drivers/payments/process")
    suspend fun processDriverPayment(@Header("Authorization") token: String, @Body body: ProcessPaymentRequest): Response<MessageResponse>

    @POST("admin/drivers/payments/calculate")
    suspend fun calculateDriverPayment(@Header("Authorization") token: String, @Query("driver_id") driverId: String, @Query("week_start") weekStart: String): Response<MessageResponse>

    @GET("admin/inventory/logs")
    suspend fun getInventoryLogs(@Header("Authorization") token: String, @Query("product_id") productId: Int? = null, @Query("limit") limit: Int = 100): Response<List<InventoryLog>>

    @GET("admin/categories")
    suspend fun getCategories(@Header("Authorization") token: String): Response<List<Category>>

    @GET("admin/drivers/list")
    suspend fun getDriversList(@Header("Authorization") token: String): Response<List<User>>

    @GET("admin/yappi/pending")
    suspend fun getYappiPendingOrders(@Header("Authorization") token: String): Response<List<YappiPendingOrder>>

    @POST("admin/yappi/{orderId}/approve")
    suspend fun approveYappiPayment(@Header("Authorization") token: String, @Path("orderId") orderId: String): Response<MessageResponse>

    @POST("admin/yappi/{orderId}/reject")
    suspend fun rejectYappiPayment(@Header("Authorization") token: String, @Path("orderId") orderId: String, @Body body: RejectYappiRequest): Response<MessageResponse>

    @GET("admin/orders/pending")
    suspend fun getAdminPendingOrders(@Header("Authorization") token: String): Response<List<com.agroapp.model.AdminPendingOrder>>

    @POST("admin/orders/{orderId}/assign-driver")
    suspend fun assignDriverToOrder(@Header("Authorization") token: String, @Path("orderId") orderId: String, @Body body: com.agroapp.model.AssignDriverRequest): Response<MessageResponse>

    @GET("users/{userId}/avatar")
    suspend fun getUserAvatar(@Header("Authorization") token: String, @Path("userId") userId: String): Response<UserAvatarResponse>

    @POST("auth/fcm-token")
    suspend fun registerFcmToken(@Header("Authorization") token: String, @Body body: Map<String, String>): Response<MessageResponse>

    @GET("banners")
    suspend fun getBanners(): Response<List<AppBanner>>

    @GET("admin/banners")
    suspend fun getAdminBanners(@Header("Authorization") token: String): Response<List<AppBanner>>

    @PATCH("admin/banners/{id}")
    suspend fun updateBanner(@Header("Authorization") token: String, @Path("id") bannerId: Int, @Body body: Map<String, String>): Response<MessageResponse>
}

data class DriverLocationRequest(val orderId: String, val latitude: Double, val longitude: Double)

data class DriverLocationResponse(
    val latitude: Double?, val longitude: Double?, val updated_at: String?,
    @SerializedName("driver_name") val driverName: String? = null,
    @SerializedName("driver_avatar") val driverAvatar: String? = null
)

data class YappiPendingOrder(
    val id: String, val total_amount: Double,
    @SerializedName("tip_amount") val tipAmount: Double = 0.0,
    val reference_code: String, val created_at: String,
    val payment_confirmed_at: String?, val delivery_address: String?,
    val customer_name: String, val customer_phone: String, val customer_email: String
)

data class RejectYappiRequest(val reason: String)

data class UserAvatarResponse(
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("full_name") val fullName: String?
)

data class AppBanner(
    val id: Int,
    val slot: Int,
    val title: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("is_active") val isActive: Boolean = true,
    val price: Double? = null,
    @SerializedName("product_id") val productId: Int? = null,
    @SerializedName("link_url") val linkUrl: String? = null
)