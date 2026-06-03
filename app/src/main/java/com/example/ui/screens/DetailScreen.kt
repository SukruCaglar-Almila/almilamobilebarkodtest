package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AltParcaModel
import com.example.data.provider.OrderProvider
import com.example.ui.theme.*
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    orderProvider: OrderProvider,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val selectedProduct by orderProvider.selectedProduct.collectAsState()
    val subParts by orderProvider.subParts.collectAsState()
    val isLoading by orderProvider.isLoading.collectAsState()
    val uploadState by orderProvider.uploadState.collectAsState()
    val errorMessage by orderProvider.errorMessage.collectAsState()

    // Local dictionary to preserve actual captured Bitmaps for active high-performance rendering.
    var capturedBitmaps by remember { mutableStateOf(mapOf<String, Bitmap>()) }
    var activeTargetPartId by remember { mutableStateOf<String?>(null) }
    var showSourceSelector by remember { mutableStateOf(false) }

    // Built-in device Camera launcher (System TakePicturePreview contract)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        val partId = activeTargetPartId
        if (bitmap != null && partId != null) {
            capturedBitmaps = capturedBitmaps + (partId to bitmap)
            orderProvider.addPhotoToPart(partId, "uri_mock_camera_$partId.jpg", bitmap)
        }
        activeTargetPartId = null
    }

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Kamera izni verilmediği için cihaz kamerası açılamadı. Lütfen cihaz ayarlarından izin verin.", Toast.LENGTH_LONG).show()
        }
    }

    // Effect: trigger Load of sub-items whenever screen opens for active product
    LaunchedEffect(selectedProduct) {
        selectedProduct?.let {
            orderProvider.loadSubItemsForProduct(it)
        }
    }

    val product = selectedProduct
    if (product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BluePrimary)
        }
        return
    }

    // Checking if the upload button should be active (at least one sub-item must have a photo)
    val hasCapturedAny = subParts.any { it.isCompleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Montaj Detayı",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "MODEL: ${product.productCode}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .testTag("detail_back_button")
                            .padding(start = 8.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = BlueSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .testTag("detail_logout_button")
                            .padding(end = 8.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Çıkış Yap",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            val completedCount = subParts.count { it.isCompleted }
            val total = subParts.size
            
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Header Summary Card (High Density Solid Blue Card)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("detail_header_summary"),
                        colors = CardDefaults.cardColors(containerColor = BlueSecondary),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ANA ÜRÜN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f),
                                    letterSpacing = 1.sp
                                )
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "AKTİF",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = product.productName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            Spacer(modifier = Modifier.height(2.dp))
                            
                            Text(
                                text = "Seri No: SN-882109${product.id}",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    val ratioString = if (total > 0) "$completedCount/$total Tamamlandı" else "0/0 Tamamlandı"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alt Parçalar ve Kontrol ($total)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = ratioString,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueSecondary
                        )
                    }

                    if (isLoading && subParts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BluePrimary, modifier = Modifier.testTag("detail_loading_spinner"))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("subparts_list_view"),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(subParts) { part ->
                                val bitmap = capturedBitmaps[part.id]
                                SubPartItemCard(
                                    part = part,
                                    imageBitmap = bitmap,
                                    onCaptureClick = {
                                        activeTargetPartId = part.id
                                        showSourceSelector = true
                                    }
                                )
                            }
                        }
                    }
                }

                // High Density Bottom Action Bar (always visible)
                val pendingPhotosCount = total - completedCount
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { orderProvider.uploadAllPhotos() },
                            enabled = hasCapturedAny,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BlueSecondary,
                                disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("upload_all_photos_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = null,
                                tint = if (hasCapturedAny) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "TÜMÜNÜ SİSTEME YÜKLE",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (hasCapturedAny) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (pendingPhotosCount > 0) "Kalan: $pendingPhotosCount Fotoğraf Bekleniyor" else "Tüm Fotoğraflar Alındı",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingPhotosCount > 0) MaterialTheme.colorScheme.onSurfaceVariant else SuccessGreen,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Media Resource Picker/Selector dialog
            if (showSourceSelector) {
                AlertDialog(
                    onDismissRequest = {
                        showSourceSelector = false
                        activeTargetPartId = null
                    },
                    title = {
                        Text(
                            text = "Fotoğraf Kaynağı Seçin",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        Text(
                            text = "Hangi yöntemle fotoğraf yüklemek istersiniz?",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    confirmButton = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Simulated Mock Capturing (Fast feedback, highly recommended for testing)
                            Button(
                                onClick = {
                                    val partId = activeTargetPartId
                                    if (partId != null) {
                                        // Generate simulated corporate schematic mock photo containing label
                                        val placeholderBitmap = generateMockBitmapForPart(partId)
                                        capturedBitmaps = capturedBitmaps + (partId to placeholderBitmap)
                                        orderProvider.addPhotoToPart(partId, "simulated_$partId.jpg", placeholderBitmap)
                                    }
                                    showSourceSelector = false
                                    activeTargetPartId = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dialog_mock_camera_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fotoğraf Simüle Et (Hızlı Test)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            // 2. Real System Camera intent launcher
                            Button(
                                onClick = {
                                    showSourceSelector = false
                                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    
                                    if (hasCameraPermission) {
                                        cameraLauncher.launch()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dialog_real_camera_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cihaz Kamerasını Aç (Gerçek)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showSourceSelector = false
                                activeTargetPartId = null
                            },
                            modifier = Modifier.testTag("dialog_select_dismiss_button")
                        ) {
                            Text("Vazgeç", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            // Upload Status Overlays (Uploading / Success / Error)
            when (uploadState) {
                is OrderProvider.UploadState.Uploading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.width(260.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = BluePrimary, modifier = Modifier.testTag("upload_loading_spinner"))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Sisteme Yükleniyor...",
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "base64 verileri sunucuya aktarılıyor, lütfen bekleyin.",
                                    fontSize = 11.sp,
                                    color = TextDarkSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
                is OrderProvider.UploadState.Success -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .width(280.dp)
                                .testTag("upload_success_modal")
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "İşlem Başarılı!",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextDarkPrimary,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Fotoğraflar base64 formatına çevrilip sunucuya başarıyla yüklendi ve iş emri güncellendi.",
                                    fontSize = 12.sp,
                                    color = TextDarkSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        orderProvider.resetUploadState()
                                        onNavigateBack() // automatically exit details and reveal complete status!
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("success_confirm_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Ana Menüye Dön", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                is OrderProvider.UploadState.Error -> {
                    val errorMsg = (uploadState as OrderProvider.UploadState.Error).message
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .width(280.dp)
                                .testTag("upload_error_modal")
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEE2E2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Yükleme Hatası",
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = errorMsg,
                                    fontSize = 12.sp,
                                    color = TextDarkSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { orderProvider.resetUploadState() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .testTag("error_dismiss_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Kapat", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

// Custom Nested Subpart Row Item Component (Level 2)
@Composable
fun SubPartItemCard(
    part: AltParcaModel,
    imageBitmap: Bitmap?,
    onCaptureClick: () -> Unit
) {
    val isSpecialCustomRow = part.id == "FULL_PRODUCT_PHOTO"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subpart_item_${part.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSpecialCustomRow) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isSpecialCustomRow) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: High Density Thumbnail Container (56dp square)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (part.isCompleted) Color(0xFFD1FAE5)
                        else if (isSpecialCustomRow) BlueSecondary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (part.isCompleted) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap.asImageBitmap(),
                            contentDescription = "Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, SuccessGreen, RoundedCornerShape(8.dp))
                        )
                        // Green Done overlay tick
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SuccessGreen.copy(alpha = 0.15f))
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else {
                        // Safe fallback icon
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Tamamlandı",
                            tint = SuccessGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    // Empty or Pending state visual indicators
                    Icon(
                        imageVector = if (isSpecialCustomRow) Icons.Filled.Wallpaper else Icons.Filled.AddAPhoto,
                        contentDescription = null,
                        tint = if (isSpecialCustomRow) BlueSecondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Middle Column: Text metadata details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = part.partName,
                    fontWeight = if (isSpecialCustomRow) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isSpecialCustomRow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                if (part.isCompleted) {
                    Text(
                        text = "Fotoğraf Alındı",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                } else {
                    if (isSpecialCustomRow) {
                        Text(
                            text = "Gözlemsel Onay Görseli",
                            fontSize = 10.sp,
                            color = BlueSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Fotoğraf Bekleniyor",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFF97316) // orange-500
                        )
                    }
                }
                
                Text(
                    text = "Satır Kodu: ${part.partCode}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Column: Camera Icon button
            IconButton(
                onClick = onCaptureClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (part.isCompleted) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else if (isSpecialCustomRow) {
                            BlueSecondary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        }
                    )
                    .testTag("camera_button_${part.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Fotoğraf Çek",
                    tint = if (part.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else if (isSpecialCustomRow) {
                        Color.White
                    } else {
                        BlueSecondary
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Custom Helper to generate simulated high-quality Bitmap visual containing labels,
// gridlines & schematic representation. This resolves camera emulator limitations beautifully.
fun generateMockBitmapForPart(partId: String): Bitmap {
    val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val paintBg = Paint().apply {
        color = 0xFF0F172A.toInt() // Slate 900 dark bg
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, 400f, 400f, paintBg)

    // Draw some architectural grid lines for schematic look
    val paintGrid = Paint().apply {
        color = 0xFF1E293B.toInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    for (i in 0..400 step 40) {
        canvas.drawLine(i.toFloat(), 0f, i.toFloat(), 400f, paintGrid)
        canvas.drawLine(0f, i.toFloat(), 400f, i.toFloat(), paintGrid)
    }

    // Draw a mock lens circular boundary
    val paintLens = Paint().apply {
        color = 0xFF3B82F6.toInt() // Blue Primary accent
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    canvas.drawCircle(200f, 200f, 120f, paintLens)

    // Draw green pass tag indicator
    val paintPass = Paint().apply {
        color = 0xFF10B981.toInt() // Emerald Green
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawRoundRect(RectF(130f, 185f, 270f, 225f), 10f, 10f, paintPass)

    // Write metadata label inside
    val paintText = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 15f
        textAlign = Paint.Align.CENTER
        color = 0xFFFFFFFF.toInt()
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText("MONTAJ OK", 200f, 210f, paintText)

    // Draw target sight reticles
    val paintSight = Paint().apply {
        color = 0xFF60A5FA.toInt()
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    canvas.drawLine(200f, 50f, 200f, 80f, paintSight)
    canvas.drawLine(200f, 320f, 200f, 350f, paintSight)
    canvas.drawLine(50f, 200f, 80f, 200f, paintSight)
    canvas.drawLine(320f, 200f, 350f, 200f, paintSight)

    // Write Part details tag text
    val paintPartText = Paint().apply {
        color = 0xFF94A3B8.toInt()
        textSize = 12f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("PARCA ID: $partId", 200f, 370f, paintPartText)
    canvas.drawText("SAHA KONTROL METRIGI", 200f, 30f, paintPartText)

    return bitmap
}
