package com.example.data.provider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AltParcaModel
import com.example.data.model.SevkEmirModel
import com.example.data.model.UrunModel
import com.example.data.service.DioService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

class OrderProvider(private val context: Context) : ViewModel() {

    private val dioService = DioService(context)

    // UI States
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _scannedOrderId = MutableStateFlow<String?>(null)
    val scannedOrderId: StateFlow<String?> = _scannedOrderId.asStateFlow()

    private val _currentOrder = MutableStateFlow<SevkEmirModel?>(null)
    val currentOrder: StateFlow<SevkEmirModel?> = _currentOrder.asStateFlow()

    private val _products = MutableStateFlow<List<UrunModel>>(emptyList())
    val products: StateFlow<List<UrunModel>> = _products.asStateFlow()

    private val _selectedProduct = MutableStateFlow<UrunModel?>(null)
    val selectedProduct: StateFlow<UrunModel?> = _selectedProduct.asStateFlow()

    private val _subParts = MutableStateFlow<List<AltParcaModel>>(emptyList())
    val subParts: StateFlow<List<AltParcaModel>> = _subParts.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    // Preferences properties exposed to Compose State
    private val _demoMode = MutableStateFlow(dioService.isDemoMode())
    val demoMode: StateFlow<Boolean> = _demoMode.asStateFlow()

    private val _apiUrl = MutableStateFlow(dioService.getBaseUrl())
    val apiUrl: StateFlow<String> = _apiUrl.asStateFlow()

    sealed interface UploadState {
        object Idle : UploadState
        object Uploading : UploadState
        object Success : UploadState
        data class Error(val message: String) : UploadState
    }

    // Update settings in Real-time
    fun updateApiSettings(url: String, isDemo: Boolean) {
        dioService.setBaseUrl(url)
        dioService.setDemoMode(isDemo)
        _apiUrl.value = dioService.getBaseUrl()
        _demoMode.value = dioService.isDemoMode()
    }

    // Query shipment code (Sevkiyat Sorgulama)
    fun queryShipmentOrder(orderNumber: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _scannedOrderId.value = orderNumber
            _selectedProduct.value = null
            _subParts.value = emptyList()

            try {
                if (dioService.isDemoMode()) {
                    delay(800) // Simulating network lag
                    val cleanNumber = orderNumber.trim().uppercase()
                    
                    // Mock shipment header info
                    _currentOrder.value = SevkEmirModel(
                        id = cleanNumber,
                        orderNumber = cleanNumber,
                        customerName = when {
                            cleanNumber.contains("TUPRAS") || cleanNumber.contains("1") -> "Tüpraş Rafineri A.Ş."
                            cleanNumber.contains("ASELSAN") || cleanNumber.contains("2") -> "Aselsan Savunma Teknolojileri"
                            else -> "General Enerji & Montaj Sanayi"
                        },
                        date = "22.05.2026",
                        status = "Montaj Aşamasında"
                    )

                    // Mock Level 1 items
                    _products.value = when {
                        cleanNumber.contains("ASELSAN") || cleanNumber.contains("2") -> listOf(
                            UrunModel("UR-201", "Gözetleme Kulesi Güç Kutusu", "GDK-900", 1, "Bekliyor"),
                            UrunModel("UR-202", "UPS Akü Grubu 30Ah", "AKU-30", 4, "Bekliyor")
                        )
                        else -> listOf(
                            UrunModel("UR-101", "Ana Kumanda Kabini (Montaj)", "KAB-101", 1, "Bekliyor"),
                            UrunModel("UR-102", "Reaktör Basınç Sensör Bloğu", "SEN-202", 2, "Bekliyor")
                        )
                    }
                } else {
                    // Real REST API query
                    val api = dioService.getApiService()
                    val result = api.getOrderItems(orderNumber)
                    _products.value = result
                    
                    // Construct a mock order header based on response metadata
                    _currentOrder.value = SevkEmirModel(
                        id = orderNumber,
                        orderNumber = orderNumber,
                        customerName = "REST API Müşterisi",
                        date = "Bugün",
                        status = "Aktif Sevkiyat"
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = "Sorgulama hatası: ${e.localizedMessage ?: "Sunucu bağlantısı kurulamadı"}"
                _products.value = emptyList()
                _currentOrder.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Fetch Level 2 sub-items for specified product
    fun loadSubItemsForProduct(product: UrunModel) {
        viewModelScope.launch {
            _selectedProduct.value = product
            _isLoading.value = true
            _errorMessage.value = null
            _uploadState.value = UploadState.Idle

            try {
                if (dioService.isDemoMode()) {
                    delay(600) // simulated delay
                    
                    val mockList = when (product.id) {
                        "UR-101" -> listOf(
                            AltParcaModel("SUB-101-1", product.id, "PLC İşlemci Modülü", "PLC-CPU-X", false),
                            AltParcaModel("SUB-101-2", product.id, "Klemens Grubu & Raylar", "KLE-77", false),
                            AltParcaModel("SUB-101-3", product.id, "Sigorta Sigorta Kutusu", "SIG-M3", false),
                            AltParcaModel("SUB-101-4", product.id, "Havalandırma Fan Filtreleri", "FAN-90F", false)
                        )
                        "UR-102" -> listOf(
                            AltParcaModel("SUB-102-1", product.id, "Kalibrasyon Göstergesi", "MAN-PRESSURE", false),
                            AltParcaModel("SUB-102-2", product.id, "Sızdırmazlık Conta Takımı", "CONT-2", false)
                        )
                        "UR-201" -> listOf(
                            AltParcaModel("SUB-201-1", product.id, "24V Güç Kaynağı Ray Tipi", "PSU-24V", false),
                            AltParcaModel("SUB-201-2", product.id, "Topraklama Barası", "BAR-GND", false)
                        )
                        else -> listOf(
                            AltParcaModel("SUB-GEN-1", product.id, "Destek Braketi", "BR-GEN", false),
                            AltParcaModel("SUB-GEN-2", product.id, "Sabitleme Vidaları Seti", "SC-SET", false)
                        )
                    }

                    // Dynamically append the "Kurulu Ürünün Tam Fotoğrafı" special line
                    val finalParts = mockList + AltParcaModel(
                        id = "FULL_PRODUCT_PHOTO",
                        productId = product.id,
                        partName = "Kurulu Ürünün Tam Fotoğrafı",
                        partCode = "TAM_FOTO_SPE",
                        isCompleted = false
                    )
                    _subParts.value = finalParts

                } else {
                    // API Call
                    val api = dioService.getApiService()
                    val rawList = api.getSubItems(product.id)
                    
                    // If special final row is not returned by backend, append it manually as required
                    val hasSpecialRow = rawList.any { it.id == "FULL_PRODUCT_PHOTO" }
                    val finalParts = if (!hasSpecialRow) {
                        rawList + AltParcaModel(
                            id = "FULL_PRODUCT_PHOTO",
                            productId = product.id,
                            partName = "Kurulu Ürünün Tam Fotoğrafı",
                            partCode = "TAM_FOTO_SPE",
                            isCompleted = false
                        )
                    } else {
                        rawList
                    }
                    _subParts.value = finalParts
                }
            } catch (e: Exception) {
                _errorMessage.value = "Parçalar alınamadı: ${e.localizedMessage ?: "Hata oluştu"}"
                _subParts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Handle photo pick / camera result, compute base64 automatically and update subpart state
    fun addPhotoToPart(partId: String, imagePath: String, bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Compute base64 in background
                val base64 = convertBitmapToBase64(bitmap)
                
                // Update specific subpart item and hold base64/path in model state
                val updatedList = _subParts.value.map { part ->
                    if (part.id == partId) {
                        part.copy(
                            imagePath = imagePath,
                            imageBase64 = base64,
                            isCompleted = true
                        )
                    } else {
                        part
                    }
                }
                _subParts.value = updatedList
            } catch (e: Exception) {
                _errorMessage.value = "Fotoğraf işleme hatası: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Helper utility to compress and transform bitmap to web-safe base64 string
    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress to reliable quality JPEG
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    // Clear uploaded status
    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }

    // Upload all captured photos of sub-items
    fun uploadAllPhotos() {
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading
            
            // Collect parts which have captured images
            val partsWithPhotos = _subParts.value.filter { it.isCompleted && !it.imageBase64.isNullOrEmpty() }
            
            if (partsWithPhotos.isEmpty()) {
                _uploadState.value = UploadState.Error("Yüklenecek fotoğraf bulunmuyor. Lütfen önce fotoğrafları çekin.")
                return@launch
            }

            try {
                if (dioService.isDemoMode()) {
                    delay(1500) // Simulate upload lag
                    
                    // Since it's demo mode, update product status of selectedProduct to completed!
                    val selected = _selectedProduct.value
                    if (selected != null) {
                        _products.value = _products.value.map { prod ->
                            if (prod.id == selected.id) {
                                prod.copy(status = "Tamamlandı")
                            } else {
                                prod
                            }
                        }
                    }
                    _uploadState.value = UploadState.Success
                } else {
                    val api = dioService.getApiService()
                    api.uploadPhotos(partsWithPhotos)
                    
                    val selected = _selectedProduct.value
                    if (selected != null) {
                        _products.value = _products.value.map { prod ->
                            if (prod.id == selected.id) {
                                prod.copy(status = "Tamamlandı")
                            } else {
                                prod
                            }
                        }
                    }
                    _uploadState.value = UploadState.Success
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error("Fotoğraf yükleme hatası: ${e.localizedMessage ?: "Sunucu hatası"}")
            }
        }
    }
}

// Factory for OrderProvider ViewModel injecting context
class OrderProviderFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrderProvider::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrderProvider(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
