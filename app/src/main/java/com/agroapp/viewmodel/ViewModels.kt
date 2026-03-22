package com.agroapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroapp.model.*
import com.agroapp.network.RetrofitClient
import com.agroapp.network.SessionManager
import kotlinx.coroutines.launch

// ─── AUTH ───────────────────────────────────────────────────────────────────

class AuthViewModel : ViewModel() {

    private val api = RetrofitClient.instance

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.postValue(AuthState.Loading)
            try {
                val response = api.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        SessionManager.saveSession(
                            data.token,
                            data.userId,
                            data.name,
                            email,
                            data.role,
                            data.address,
                            data.userType
                        )
                        _authState.postValue(AuthState.Success(data.name, data.role))
                    } else {
                        _authState.postValue(AuthState.Error("Respuesta vacía del servidor"))
                    }
                } else {
                    _authState.postValue(AuthState.Error("Email o contraseña incorrectos"))
                }
            } catch (e: Exception) {
                _authState.postValue(AuthState.Error("Error de conexión: ${e.message}"))
            }
        }
    }

    fun register(email: String, password: String, fullName: String, phone: String, address: String, userType: String = "cliente") {
        viewModelScope.launch {
            _authState.postValue(AuthState.Loading)
            try {
                val response = api.register(RegisterRequest(email, password, fullName, phone, address, userType))
                if (response.isSuccessful) {
                    _authState.postValue(AuthState.Registered)
                } else {
                    _authState.postValue(AuthState.Error("Error al registrarse, intenta con otro email"))
                }
            } catch (e: Exception) {
                _authState.postValue(AuthState.Error("Error de conexión: ${e.message}"))
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Registered : AuthState()
    data class Success(val name: String, val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

// ─── CARRITO COMPARTIDO ───────────────────────────────────────────────────────

object CartRepository {
    val cart = MutableLiveData<MutableMap<Product, Double>>(mutableMapOf())

    fun add(product: Product, quantity: Double) {
        val current = cart.value ?: mutableMapOf()
        current[product] = (current[product] ?: 0.0) + quantity
        cart.value = current
    }

    fun remove(product: Product) {
        val current = cart.value ?: return
        current.remove(product)
        cart.value = current
    }

    fun update(product: Product, quantity: Double) {
        val current = cart.value ?: return
        if (quantity <= 0) current.remove(product)
        else current[product] = quantity
        cart.value = current
    }

    fun clear() { cart.value = mutableMapOf() }

    fun getItems(): Map<Product, Double> = cart.value ?: emptyMap()

    fun getTotal(): Double =
        cart.value?.entries?.sumOf { it.key.price * it.value } ?: 0.0
}

// ─── PRODUCTS & CART ─────────────────────────────────────────────────────────

class ProductViewModel : ViewModel() {

    private val api = RetrofitClient.instance

    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    val cart: LiveData<MutableMap<Product, Double>> = CartRepository.cart

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun loadProducts() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.getProducts()
                if (response.isSuccessful) _products.value = response.body()
            } catch (e: Exception) { }
            _loading.value = false
        }
    }

    fun addToCart(product: Product, quantity: Double) = CartRepository.add(product, quantity)
    fun removeFromCart(product: Product) = CartRepository.remove(product)
    fun updateQuantity(product: Product, quantity: Double) = CartRepository.update(product, quantity)
    fun clearCart() = CartRepository.clear()
    fun getCartItemsMap(): Map<Product, Double> = CartRepository.getItems()
    fun getCartTotal(): Double = CartRepository.getTotal()
    fun getCartItemCount(): Int = CartRepository.getItems().size
}

// ─── ORDERS ──────────────────────────────────────────────────────────────────

class OrderViewModel : ViewModel() {

    private val api = RetrofitClient.instance

    private val _orderState = MutableLiveData<OrderState>()
    val orderState: LiveData<OrderState> = _orderState

    private val _myOrders = MutableLiveData<List<Order>>()
    val myOrders: LiveData<List<Order>> = _myOrders

    fun createOrder(
        cartItems: Map<Product, Double>,
        paymentMethod: String,
        deliveryAddress: String,
        notes: String?
    ) {
        // ========== VALIDACIÓN DE HORARIO DESACTIVADA PARA PRUEBAS ==========
        // val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        // if (hour < 8 || hour >= 12) {
        //     _orderState.value = OrderState.OutOfTime
        //     return
        // }

        viewModelScope.launch {
            _orderState.postValue(OrderState.Loading)
            try {
                val items = cartItems.map { (product, qty) -> CartItem(product.id, qty) }
                val response = api.createOrder(
                    SessionManager.getToken(),
                    CreateOrderRequest(items, paymentMethod, deliveryAddress, notes)
                )
                if (response.isSuccessful) {
                    _orderState.postValue(OrderState.Success(response.body()!!))
                } else {
                    _orderState.postValue(OrderState.Error("Error al crear el pedido"))
                }
            } catch (e: Exception) {
                _orderState.postValue(OrderState.Error("Error de conexión"))
            }
        }
    }

    fun loadMyOrders() {
        viewModelScope.launch {
            try {
                val response = api.getMyOrders(SessionManager.getToken())
                if (response.isSuccessful) _myOrders.postValue(response.body())
            } catch (e: Exception) { }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            try {
                val response = api.cancelOrder(SessionManager.getToken(), orderId)
                if (response.isSuccessful) loadMyOrders()
            } catch (e: Exception) { }
        }
    }

    fun createPaymentIntent(total: Double, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val amountInCents = (total * 100).toInt()
                val response = api.createPaymentIntent(
                    SessionManager.getToken(),
                    PaymentIntentRequest(amountInCents, "usd")
                )
                if (response.isSuccessful) {
                    val clientSecret = response.body()?.clientSecret
                    callback(true, clientSecret)
                } else {
                    callback(false, null)
                }
            } catch (e: Exception) {
                callback(false, null)
            }
        }
    }

    // ==================== MÉTODOS PARA YAPPI ====================

    fun createPendingYappiOrder(
        cartItems: Map<Product, Double>,
        deliveryAddress: String,
        callback: (Boolean, String?, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val items = cartItems.map { (product, qty) -> CartItem(product.id, qty) }
                val response = api.createPendingYappiOrder(
                    SessionManager.getToken(),
                    com.agroapp.model.CreatePendingOrderRequest(items, deliveryAddress)
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        callback(true, body.orderId, body.referenceCode)
                    } else {
                        callback(false, null, null)
                    }
                } else {
                    callback(false, null, null)
                }
            } catch (e: Exception) {
                callback(false, null, null)
            }
        }
    }

    fun confirmYappiPayment(
        orderId: String,
        referenceCode: String,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = api.confirmYappiPayment(
                    SessionManager.getToken(),
                    orderId,
                    com.agroapp.model.ConfirmPaymentRequest(referenceCode)
                )
                if (response.isSuccessful) {
                    val message = response.body()?.message ?: "Pedido confirmado"
                    callback(true, message)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error al confirmar pago"
                    callback(false, errorMsg)
                }
            } catch (e: Exception) {
                callback(false, "Error de conexión: ${e.message}")
            }
        }
    }
}

sealed class OrderState {
    object Loading : OrderState()
    object OutOfTime : OrderState()
    data class Success(val order: Order) : OrderState()
    data class Error(val message: String) : OrderState()
}

// ─── VENDOR ──────────────────────────────────────────────────────────────────

class VendorViewModel : ViewModel() {

    private val api = RetrofitClient.instance

    private val _ordersByClient = MutableLiveData<List<OrderByClient>>()
    val ordersByClient: LiveData<List<OrderByClient>> = _ordersByClient

    private val _ordersByProduct = MutableLiveData<List<OrderByProduct>>()
    val ordersByProduct: LiveData<List<OrderByProduct>> = _ordersByProduct

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun loadOrdersByClient(date: String? = null) {
        viewModelScope.launch {
            _loading.postValue(true)
            try {
                val response = api.getOrdersByClient(SessionManager.getToken(), date)
                if (response.isSuccessful) _ordersByClient.postValue(response.body())
            } catch (e: Exception) { }
            _loading.postValue(false)
        }
    }

    fun loadOrdersByProduct(date: String? = null) {
        viewModelScope.launch {
            _loading.postValue(true)
            try {
                val response = api.getOrdersByProduct(SessionManager.getToken(), date)
                if (response.isSuccessful) _ordersByProduct.postValue(response.body())
            } catch (e: Exception) { }
            _loading.postValue(false)
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            try {
                api.updateOrderStatus(
                    SessionManager.getToken(), orderId, UpdateStatusRequest(status)
                )
                loadOrdersByClient()
            } catch (e: Exception) { }
        }
    }
}

// ─── PAYMENT ─────────────────────────────────────────────────────────────────

class PaymentViewModel : ViewModel() {

    private val api = RetrofitClient.instance

    private val _paymentState = MutableLiveData<PaymentState?>()
    val paymentState: LiveData<PaymentState?> = _paymentState

    fun createPaymentIntent(amount: Double) {
        viewModelScope.launch {
            _paymentState.postValue(PaymentState.Loading)
            try {
                val amountInCents = (amount * 100).toInt()
                val response = api.createPaymentIntent(
                    SessionManager.getToken(),
                    PaymentIntentRequest(amountInCents, "usd")
                )
                if (response.isSuccessful) {
                    response.body()?.let {
                        _paymentState.postValue(PaymentState.Ready(it.clientSecret))
                    } ?: run {
                        _paymentState.postValue(PaymentState.Error("Respuesta vacía del servidor"))
                    }
                } else {
                    _paymentState.postValue(PaymentState.Error("Error al iniciar el pago: ${response.code()}"))
                }
            } catch (e: Exception) {
                _paymentState.postValue(PaymentState.Error("Error de conexión: ${e.message}"))
            }
        }
    }

    fun resetState() {
        _paymentState.value = null
    }
}

sealed class PaymentState {
    object Loading : PaymentState()
    data class Ready(val clientSecret: String) : PaymentState()
    data class Error(val message: String) : PaymentState()
}

// ─── PROFILE ─────────────────────────────────────────────────────────────────

class ProfileViewModel : ViewModel() {

    private val api = RetrofitClient.instance

    private val _profile = MutableLiveData<UserProfile?>()
    val profile: LiveData<UserProfile?> = _profile

    private val _updateState = MutableLiveData<ProfileState>()
    val updateState: LiveData<ProfileState> = _updateState

    fun loadProfile() {
        viewModelScope.launch {
            try {
                val response = api.getProfile(SessionManager.getToken())
                if (response.isSuccessful) _profile.postValue(response.body())
            } catch (e: Exception) { }
        }
    }

    fun updateProfile(name: String, phone: String, address: String) {
        viewModelScope.launch {
            try {
                val response = api.updateProfile(
                    SessionManager.getToken(),
                    UpdateProfileRequest(name, phone, address)
                )
                if (response.isSuccessful) {
                    response.body()?.let {
                        SessionManager.updateProfile(it.name, it.phone ?: "", it.address ?: "")
                    }
                    _updateState.postValue(ProfileState.Success("Perfil actualizado correctamente"))
                } else {
                    _updateState.postValue(ProfileState.Error("Error al actualizar el perfil"))
                }
            } catch (e: Exception) {
                _updateState.postValue(ProfileState.Error("Error de conexión"))
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            try {
                val response = api.changePassword(
                    SessionManager.getToken(),
                    ChangePasswordRequest(currentPassword, newPassword)
                )
                if (response.isSuccessful) {
                    _updateState.postValue(ProfileState.Success("Contraseña cambiada correctamente"))
                } else {
                    val error = response.errorBody()?.string()
                    val message = when {
                        error?.contains("incorrecta") == true -> "Contraseña actual incorrecta"
                        else -> "Error al cambiar la contraseña"
                    }
                    _updateState.postValue(ProfileState.Error(message))
                }
            } catch (e: Exception) {
                _updateState.postValue(ProfileState.Error("Error de conexión"))
            }
        }
    }
}

sealed class ProfileState {
    data class Success(val message: String) : ProfileState()
    data class Error(val message: String) : ProfileState()
}