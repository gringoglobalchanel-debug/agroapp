package com.agroapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroapp.model.*
import com.agroapp.network.RetrofitClient
import com.agroapp.network.SessionManager
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {

    private val api = RetrofitClient.instance

    // Dashboard
    private val _dashboardStats = MutableLiveData<AdminDashboardStats?>()
    val dashboardStats: LiveData<AdminDashboardStats?> = _dashboardStats

    // Products
    private val _products = MutableLiveData<List<ProductWithInventory>>()
    val products: LiveData<List<ProductWithInventory>> = _products

    // Drivers
    private val _drivers = MutableLiveData<List<User>>()
    val drivers: LiveData<List<User>> = _drivers

    // Driver Payments
    private val _driverPayments = MutableLiveData<List<DriverPayment>>()
    val driverPayments: LiveData<List<DriverPayment>> = _driverPayments

    // Inventory logs
    private val _inventoryLogs = MutableLiveData<List<InventoryLog>>()
    val inventoryLogs: LiveData<List<InventoryLog>> = _inventoryLogs

    // Categories
    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    // ==================== DASHBOARD ====================

    fun loadDashboardStats() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.getAdminDashboardStats(SessionManager.getToken())
                if (response.isSuccessful) _dashboardStats.value = response.body()
                else _message.value = "Error al cargar estadísticas: ${response.code()}"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
            _loading.value = false
        }
    }

    // ==================== PRODUCTOS ====================

    fun loadProducts(categoryId: Int? = null, search: String? = null, lowStock: Boolean? = null) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.getAllProducts(SessionManager.getToken(), categoryId, search, lowStock)
                if (response.isSuccessful) _products.value = response.body()
                else _message.value = "Error al cargar productos: ${response.code()}"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
            _loading.value = false
        }
    }

    fun createProduct(request: CreateProductRequest) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.createProduct(SessionManager.getToken(), request)
                if (response.isSuccessful) {
                    _message.value = "✅ Producto creado exitosamente"
                    loadProducts()
                } else {
                    _message.value = "❌ Error al crear producto"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
            _loading.value = false
        }
    }

    fun updateProduct(productId: Int, request: UpdateProductRequest) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.updateProduct(SessionManager.getToken(), productId, request)
                if (response.isSuccessful) {
                    _message.value = "✅ Producto actualizado"
                    loadProducts()
                } else {
                    _message.value = "❌ Error al actualizar"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
            _loading.value = false
        }
    }

    fun updateStock(productId: Int, quantity: Double, changeType: String, notes: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.updateProductStock(
                    SessionManager.getToken(),
                    productId,
                    UpdateStockRequest(productId, quantity, changeType, notes)
                )
                if (response.isSuccessful) {
                    _message.value = "✅ Stock actualizado"
                    loadProducts()
                } else {
                    _message.value = "❌ Error al actualizar stock"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
            _loading.value = false
        }
    }

    fun deleteProduct(productId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.deleteProduct(SessionManager.getToken(), productId)
                if (response.isSuccessful) {
                    _message.value = "✅ Producto eliminado"
                    loadProducts()
                } else {
                    _message.value = "❌ Error al eliminar"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
            _loading.value = false
        }
    }

    // ==================== DRIVERS Y PAGOS ====================

    fun loadDrivers() {
        viewModelScope.launch {
            try {
                val response = api.getDriversList(SessionManager.getToken())
                if (response.isSuccessful) _drivers.value = response.body()
            } catch (e: Exception) { }
        }
    }

    fun loadDriverPayments(status: String? = null, driverId: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.getDriverPayments(SessionManager.getToken(), status, driverId)
                if (response.isSuccessful) _driverPayments.value = response.body()
                else _message.value = "Error al cargar pagos"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
            _loading.value = false
        }
    }

    fun calculateDriverPayment(driverId: String, weekStart: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.calculateDriverPayment(SessionManager.getToken(), driverId, weekStart)
                if (response.isSuccessful) {
                    _message.value = "✅ Pago calculado correctamente"
                    loadDriverPayments()
                } else {
                    _message.value = "❌ Error al calcular pago: ${response.code()}"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
            _loading.value = false
        }
    }

    fun processDriverPayment(paymentId: String, status: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.processDriverPayment(SessionManager.getToken(), ProcessPaymentRequest(paymentId, status))
                if (response.isSuccessful) {
                    _message.value = "✅ Pago procesado"
                    loadDriverPayments()
                } else {
                    _message.value = "❌ Error al procesar"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
            _loading.value = false
        }
    }

    // ==================== INVENTARIO ====================

    fun loadInventoryLogs(productId: Int? = null) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.getInventoryLogs(SessionManager.getToken(), productId)
                if (response.isSuccessful) _inventoryLogs.value = response.body()
                else _message.value = "Error al cargar logs"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
            _loading.value = false
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val response = api.getCategories(SessionManager.getToken())
                if (response.isSuccessful) _categories.value = response.body()
            } catch (e: Exception) { }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}