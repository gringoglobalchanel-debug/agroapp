package com.agroapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroapp.model.DeliveryBlock
import com.agroapp.model.DriverBlock
import com.agroapp.model.DriverEarnings
import com.agroapp.model.DynamicPackage
import com.agroapp.model.DriverPackageEarnings
import com.agroapp.model.TakeBlockRequest
import com.agroapp.model.TakePackageRequest
import com.agroapp.model.UpdateStatusRequest
import com.agroapp.network.DriverLocationRequest
import com.agroapp.network.RetrofitClient
import com.agroapp.network.SessionManager
import kotlinx.coroutines.launch

class DriverViewModel : ViewModel() {

    private val api = RetrofitClient.instance

    private val _availableBlocks = MutableLiveData<List<DeliveryBlock>>()
    val availableBlocks: LiveData<List<DeliveryBlock>> = _availableBlocks

    private val _myBlocks = MutableLiveData<List<DriverBlock>>()
    val myBlocks: LiveData<List<DriverBlock>> = _myBlocks

    private val _earnings = MutableLiveData<DriverEarnings?>()
    val earnings: LiveData<DriverEarnings?> = _earnings

    private val _availablePackages = MutableLiveData<List<DynamicPackage>>()
    val availablePackages: LiveData<List<DynamicPackage>> = _availablePackages

    private val _myPackages = MutableLiveData<List<DynamicPackage>>()
    val myPackages: LiveData<List<DynamicPackage>> = _myPackages

    private val _packageEarnings = MutableLiveData<DriverPackageEarnings?>()
    val packageEarnings: LiveData<DriverPackageEarnings?> = _packageEarnings

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _takeBlockState = MutableLiveData<TakeBlockState?>()
    val takeBlockState: LiveData<TakeBlockState?> = _takeBlockState

    private val _takePackageState = MutableLiveData<TakePackageState?>()
    val takePackageState: LiveData<TakePackageState?> = _takePackageState

    init {
        loadAvailableBlocks()
        loadMyBlocks()
        loadEarnings()
        loadAvailablePackages()
        loadMyPackages()
        loadPackageEarnings()
    }

    fun loadAvailableBlocks() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.getAvailableBlocks(SessionManager.getToken())
                if (response.isSuccessful) _availableBlocks.value = response.body()
            } catch (e: Exception) { }
            _loading.value = false
        }
    }

    fun loadMyBlocks() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.getMyBlocks(SessionManager.getToken())
                if (response.isSuccessful) _myBlocks.value = response.body()
            } catch (e: Exception) { }
            _loading.value = false
        }
    }

    fun loadEarnings() {
        viewModelScope.launch {
            try {
                val response = api.getDriverEarnings(SessionManager.getToken())
                if (response.isSuccessful) _earnings.value = response.body()
            } catch (e: Exception) { }
        }
    }

    fun takeBlock(blockId: String) {
        viewModelScope.launch {
            _takeBlockState.value = TakeBlockState.Loading
            try {
                val response = api.takeBlock(SessionManager.getToken(), TakeBlockRequest(blockId))
                if (response.isSuccessful) {
                    _takeBlockState.value = TakeBlockState.Success("Bloque tomado exitosamente")
                    loadAvailableBlocks()
                    loadMyBlocks()
                } else {
                    _takeBlockState.value = TakeBlockState.Error("Error al tomar el bloque")
                }
            } catch (e: Exception) {
                _takeBlockState.value = TakeBlockState.Error("Error de conexión")
            }
        }
    }

    fun resetTakeBlockState() { _takeBlockState.value = null }

    fun loadAvailablePackages() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.getAvailablePackages(SessionManager.getToken())
                if (response.isSuccessful) _availablePackages.value = response.body()
            } catch (e: Exception) { }
            _loading.value = false
        }
    }

    fun loadMyPackages() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.getMyPackages(SessionManager.getToken())
                if (response.isSuccessful) _myPackages.value = response.body()
            } catch (e: Exception) { }
            _loading.value = false
        }
    }

    fun loadPackageEarnings() {
        viewModelScope.launch {
            try {
                val response = api.getDriverPackageEarnings(SessionManager.getToken())
                if (response.isSuccessful) _packageEarnings.value = response.body()
            } catch (e: Exception) { }
        }
    }

    fun takePackage(packageId: String) {
        viewModelScope.launch {
            _takePackageState.value = TakePackageState.Loading
            try {
                val response = api.takePackage(SessionManager.getToken(), TakePackageRequest(packageId))
                if (response.isSuccessful) {
                    val data = response.body()
                    _takePackageState.value = TakePackageState.Success(
                        data?.message ?: "Paquete tomado exitosamente",
                        data?.total_orders ?: 0
                    )
                    loadAvailablePackages()
                    loadMyPackages()
                    loadPackageEarnings()
                } else {
                    _takePackageState.value = TakePackageState.Error("Error al tomar el paquete")
                }
            } catch (e: Exception) {
                _takePackageState.value = TakePackageState.Error("Error de conexión")
            }
        }
    }

    fun resetTakePackageState() { _takePackageState.value = null }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                val response = api.updateDeliveryOrderStatus(
                    SessionManager.getToken(),
                    orderId,
                    UpdateStatusRequest(newStatus)
                )
                if (response.isSuccessful) {
                    android.util.Log.d("DriverViewModel", "✅ Pedido $orderId actualizado a $newStatus")
                    loadMyPackages()
                } else {
                    android.util.Log.e("DriverViewModel", "❌ Error al actualizar pedido")
                }
            } catch (e: Exception) {
                android.util.Log.e("DriverViewModel", "❌ Error de conexión: ${e.message}")
            }
        }
    }

    fun updateDriverLocation(orderId: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken()
                val response = api.updateDriverLocation(
                    token,
                    DriverLocationRequest(orderId, latitude, longitude)
                )
                if (response.isSuccessful) {
                    android.util.Log.d("DriverViewModel", "📍 Ubicación actualizada")
                }
            } catch (e: Exception) {
                android.util.Log.e("DriverViewModel", "❌ Error actualizando ubicación: ${e.message}")
            }
        }
    }
}

sealed class TakeBlockState {
    object Loading : TakeBlockState()
    data class Success(val message: String) : TakeBlockState()
    data class Error(val message: String) : TakeBlockState()
}

sealed class TakePackageState {
    object Loading : TakePackageState()
    data class Success(val message: String, val totalOrders: Int) : TakePackageState()
    data class Error(val message: String) : TakePackageState()
}