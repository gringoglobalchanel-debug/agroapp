package com.agroapp.model

import com.google.gson.annotations.SerializedName

// ==================== AUTH ====================

data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("full_name") val fullName: String,
    val phone: String,
    val address: String,
    @SerializedName("user_type") val userType: String = "cliente"
)

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val token: String,
    val userId: String,
    val name: String,
    val role: String,
    val address: String?,
    @SerializedName("user_type") val userType: String = "cliente"
)

data class MessageResponse(val message: String)

data class User(
    val id: String,
    val email: String,
    @SerializedName("full_name") val fullName: String,
    val phone: String?,
    val address: String?,
    val role: String
)

// ==================== PRODUCTOS ====================

data class Product(
    val id: Int,
    @SerializedName("category_id") val categoryId: Int,
    val name: String,
    val description: String?,
    val price: Double,
    val unit: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("is_available") val isAvailable: Boolean,
    val categories: Category?
)

data class Category(
    val id: Int,
    val name: String,
    val icon: String?
)

// ==================== PEDIDOS ====================

data class Order(
    val id: String,
    @SerializedName("user_id") val userId: String,
    val status: String,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("payment_status") val paymentStatus: String,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("tip_amount") val tipAmount: Double = 0.0,
    @SerializedName("delivery_address") val deliveryAddress: String?,
    @SerializedName("delivery_latitude") val deliveryLatitude: Double? = null,
    @SerializedName("delivery_longitude") val deliveryLongitude: Double? = null,
    @SerializedName("delivery_date") val deliveryDate: String,
    val notes: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("order_items") val items: List<OrderItem>?,
    @SerializedName("driver_id") val driverId: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("customer_phone") val customerPhone: String? = null
) {
    val displayTotal: Double get() = if (totalAmount > 0) totalAmount else items?.sumOf { it.subtotal } ?: 0.0
}

data class OrderItem(
    val id: Int,
    @SerializedName("order_id") val orderId: String,
    @SerializedName("product_id") val productId: Int,
    val quantity: Double,
    @SerializedName("unit_price") val unitPrice: Double,
    val products: ProductSimple?
) {
    val subtotal: Double get() = unitPrice * quantity
}

data class ProductSimple(val name: String, val unit: String)

data class CreateOrderRequest(
    val items: List<CartItem>,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("delivery_address") val deliveryAddress: String,
    @SerializedName("delivery_latitude") val deliveryLatitude: Double? = null,
    @SerializedName("delivery_longitude") val deliveryLongitude: Double? = null,
    val notes: String?,
    @SerializedName("tip_amount") val tipAmount: Double = 0.0
)

data class CartItem(
    @SerializedName("product_id") val productId: Int,
    val quantity: Double
)

data class UpdateStatusRequest(val status: String)

// ==================== VENDEDOR ====================

data class OrderByClient(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("full_name") val fullName: String,
    val phone: String?,
    @SerializedName("delivery_address") val deliveryAddress: String?,
    @SerializedName("delivery_date") val deliveryDate: String,
    val status: String,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("total_amount") val totalAmount: Double,
    val items: List<OrderItemSummary>
)

data class OrderItemSummary(
    val product: String,
    val quantity: Double,
    val unit: String,
    val subtotal: Double
)

data class OrderByProduct(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String,
    val unit: String,
    val category: String,
    @SerializedName("total_quantity") val totalQuantity: Double,
    @SerializedName("total_value") val totalValue: Double,
    @SerializedName("delivery_date") val deliveryDate: String
)

// ==================== STRIPE PAYMENTS ====================

data class PaymentIntentRequest(
    val amount: Int,
    val currency: String = "usd"
)

data class PaymentIntentResponse(
    @SerializedName("clientSecret") val clientSecret: String
)

// ==================== PERFIL ====================

data class UserProfile(
    val id: String,
    @SerializedName("full_name") val fullName: String,
    val email: String,
    val phone: String?,
    val address: String?,
    val role: String
)

data class UpdateProfileRequest(
    @SerializedName("full_name") val fullName: String,
    val phone: String,
    val address: String
)

data class UpdateProfileResponse(
    val message: String,
    val name: String,
    val phone: String?,
    val address: String?
)

data class ChangePasswordRequest(
    @SerializedName("currentPassword") val currentPassword: String,
    @SerializedName("newPassword") val newPassword: String
)

// ==================== REPARTIDORES (BLOQUES - VIEJO) ====================

data class DeliveryBlock(
    val id: String,
    val block_date: String,
    val start_time: String,
    val end_time: String,
    val zone: String?,
    val total_orders: Int,
    val available_orders: Int,
    val base_payment: Double,
    val platform_fee: Double,
    val driver_payment: Double,
    val status: String
)

data class DriverBlock(
    val id: String,
    val block_id: String,
    val driver_id: String,
    val status: String,
    val assigned_at: String,
    val started_at: String?,
    val completed_at: String?,
    val total_earned: Double,
    val block: DeliveryBlock?
)

data class DriverEarnings(
    val total_blocks: Int,
    val total_deliveries: Int,
    val total_amount: Double,
    val platform_commission: Double,
    val driver_net_amount: Double,
    val next_payment_date: String
)

data class TakeBlockRequest(
    val block_id: String
)

// ==================== PAQUETES DINÁMICOS (NUEVO) ====================

data class DynamicPackage(
    val id: String,
    val current_size: Int,
    val max_size: Int,
    val status: String,
    val taken_by: String?,
    val taken_at: String?,
    val created_at: String,
    val updated_at: String,
    val orders: List<DynamicPackageOrder>? = null
)

data class DynamicPackageOrder(
    val order_id: String,
    val user_id: String,
    val delivery_address: String?,
    val delivery_latitude: Double? = null,
    val delivery_longitude: Double? = null,
    val total_amount: Double,
    val tip_amount: Double = 0.0,
    val payment_method: String,
    val created_at: String,
    val customer_name: String = "Cliente",
    val customer_phone: String = "No disponible"
)

data class TakePackageRequest(
    val package_id: String
)

data class TakePackageResponse(
    val message: String,
    @SerializedName("package") val packageData: DynamicPackage,
    val driver_payment: Double,
    val total_tips: Double = 0.0,
    val platform_fee: Double,
    val total_orders: Int
)

data class DriverPackageEarnings(
    val total_packages: Int,
    val total_orders: Int,
    val total_amount: Double,
    val total_tips: Double = 0.0,
    val platform_commission: Double,
    val driver_net_amount: Double,
    val next_payment_date: String
)

// ==================== YAPPI ====================

data class CreatePendingOrderRequest(
    val items: List<CartItem>,
    val deliveryAddress: String
)

data class CreatePendingOrderResponse(
    val orderId: String,
    val referenceCode: String,
    val totalAmount: Double,
    val deliveryDate: String
)

data class ConfirmPaymentRequest(
    val referenceCode: String
)

data class ConfirmPaymentResponse(
    val success: Boolean,
    val message: String
)