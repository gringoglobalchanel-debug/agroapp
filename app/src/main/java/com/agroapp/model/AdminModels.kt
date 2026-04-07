package com.agroapp.model

import com.google.gson.annotations.SerializedName

// ==================== DASHBOARD ====================

data class AdminDashboardStats(
    @SerializedName("totalProducts") val totalProducts: Int,
    @SerializedName("lowStockProducts") val lowStockProducts: Int,
    @SerializedName("outOfStockProducts") val outOfStockProducts: Int,
    @SerializedName("totalOrdersToday") val totalOrdersToday: Int,
    @SerializedName("totalRevenueToday") val totalRevenueToday: Double,
    @SerializedName("totalOrdersWeek") val totalOrdersWeek: Int,
    @SerializedName("totalRevenueWeek") val totalRevenueWeek: Double,
    @SerializedName("totalDrivers") val totalDrivers: Int,
    @SerializedName("activeDrivers") val activeDrivers: Int,
    @SerializedName("pendingPayments") val pendingPayments: Double,
    @SerializedName("pendingYappiApprovals") val pendingYappiApprovals: Int? = 0
)

// ==================== PRODUCTOS CON INVENTARIO ====================

data class ProductWithInventory(
    val id: Int,
    val name: String,
    val description: String?,
    val price: Double,
    val unit: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("category_id") val categoryId: Int,
    val category: String?,
    val stock: Double?,
    @SerializedName("min_stock") val minStock: Double?,
    @SerializedName("is_available") val isAvailable: Boolean
)

data class CreateProductRequest(
    val name: String,
    val description: String?,
    val price: Double,
    val unit: String,
    @SerializedName("category_id") val categoryId: Int,
    val stock: Double = 0.0,
    @SerializedName("min_stock") val minStock: Double = 0.0,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val unit: String? = null,
    @SerializedName("category_id") val categoryId: Int? = null,
    val stock: Double? = null,
    @SerializedName("min_stock") val minStock: Double? = null,
    @SerializedName("is_available") val isAvailable: Boolean? = null,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class UpdateStockRequest(
    @SerializedName("product_id") val productId: Int,
    val quantity: Double,
    @SerializedName("change_type") val changeType: String,
    val notes: String? = null
)

// ==================== PAGOS A DRIVERS ====================

data class DriverPayment(
    val id: String,
    @SerializedName("driver_id") val driverId: String,
    @SerializedName("driver_name") val driverName: String?,
    @SerializedName("week_start") val weekStart: String,
    @SerializedName("week_end") val weekEnd: String,
    @SerializedName("total_orders") val totalOrders: Int,
    @SerializedName("total_base_payment") val totalBasePayment: Double,
    @SerializedName("total_tips") val totalTips: Double,
    @SerializedName("platform_commission") val platformCommission: Double,
    @SerializedName("net_amount") val netAmount: Double,
    @SerializedName("payment_status") val paymentStatus: String,
    @SerializedName("paid_at") val paidAt: String?
)

data class ProcessPaymentRequest(
    @SerializedName("payment_id") val paymentId: String,
    @SerializedName("payment_status") val paymentStatus: String
)

// ==================== INVENTARIO LOGS ====================

data class InventoryLog(
    val id: String,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String,
    @SerializedName("previous_quantity") val previousQuantity: Double,
    @SerializedName("new_quantity") val newQuantity: Double,
    @SerializedName("change_type") val changeType: String,
    @SerializedName("order_id") val orderId: String?,
    val notes: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("created_by") val createdBy: String?,
    @SerializedName("created_by_name") val createdByName: String?
)

// NOTA: Category ya existe en Models.kt, no la dupliques aquí