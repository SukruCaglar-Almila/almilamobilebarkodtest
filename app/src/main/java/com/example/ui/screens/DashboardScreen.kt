package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UrunModel
import com.example.data.provider.OrderProvider
import com.example.ui.theme.*
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    orderProvider: OrderProvider,
    onLogout: () -> Unit,
    onNavigateToProductDetail: (UrunModel) -> Unit
) {
    val isLoading by orderProvider.isLoading.collectAsState()
    val errorMessage by orderProvider.errorMessage.collectAsState()
    val currentOrder by orderProvider.currentOrder.collectAsState()
    val products by orderProvider.products.collectAsState()
    val isDemoMode by orderProvider.demoMode.collectAsState()

    var manualCodeInput by remember { mutableStateOf("") }
    var showScannerDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val hasCameraPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission.value = isGranted
        showScannerDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Saha Montaj Portal",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isDemoMode) Color(0xFFFBBF24) else Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDemoMode) "DEMO MODU (ÇEVRİMDIŞI)" else "ÇEVRİMİÇİ SUNUCU MODU",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDemoMode) Color(0xFFB45309) else Color(0xFF047857),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .testTag("dashboard_logout_button")
                            .padding(end = 6.dp)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Barcode scanner trigger bar
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sevkiyat Sorgulama",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BlueSecondary,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Text(
                            text = "İş emrini başlatmak için sevk barkodunu okutun.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(top = 2.dp, bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Manual Input Field
                            OutlinedTextField(
                                value = manualCodeInput,
                                onValueChange = { manualCodeInput = it },
                                placeholder = { Text("Sevk Emir No (örn: SEV-001)", fontWeight = FontWeight.Normal) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("shipment_manual_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = BlueSecondary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Query search button
                            IconButton(
                                onClick = { 
                                    if (manualCodeInput.isNotBlank()) {
                                        orderProvider.queryShipmentOrder(manualCodeInput)
                                    }
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BlueSecondary)
                                    .testTag("shipment_query_run_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Sorgula",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // QR SCAN button trigger
                        Button(
                            onClick = {
                                val currentPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                hasCameraPermission.value = currentPermission
                                
                                if (currentPermission) {
                                    showScannerDialog = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("trigger_scanner_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Barkod / QR Tarayıcıyı Aç",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Info banner details
                errorMessage?.let { err ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .background(Color(0xFFFEE2E2), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = err, color = Color(0xFF991B1B), fontSize = 13.sp)
                        }
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BlueSecondary, modifier = Modifier.testTag("loading_spinner"))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Sevk emir bilgileri alınıyor...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (currentOrder == null) {
                    // Empty state (First run)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = BlueSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Bekleyen İş Gösterilemedi",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Yukarıdan bir Sevk Emir Numarası girin ya da barkod okutarak montaj detaylarını görüntüleyin.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Fast demo tags
                            Text(
                                text = "HIZLI TEST ŞABLONLARI (DEMO)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Card(
                                    modifier = Modifier
                                        .clickable {
                                            manualCodeInput = "SEV-001"
                                            orderProvider.queryShipmentOrder("SEV-001")
                                        }
                                        .padding(4.dp),
                                    colors = CardDefaults.cardColors(containerColor = BlueSecondary.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "SEV-001\n(Tüpraş)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BlueSecondary,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Card(
                                    modifier = Modifier
                                        .clickable {
                                            manualCodeInput = "SEV-002"
                                            orderProvider.queryShipmentOrder("SEV-002")
                                        }
                                        .padding(4.dp),
                                    colors = CardDefaults.cardColors(containerColor = BlueSecondary.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "SEV-002\n(Aselsan)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BlueSecondary,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Result state! Display shipment metadata + nested products list
                    val order = currentOrder!!
                    
                    // Header Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = BlueSecondary),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FİŞ NO: ${order.orderNumber}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = order.status,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Business,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = order.customerName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarToday,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sorgulama Tarihi: ${order.date}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    // Level 1 Products Title
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = BlueSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sevk Kapsamındaki Ürünler (${products.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Level 1 Products List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("products_list_view"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(products) { item ->
                            ProductCard(
                                product = item,
                                onClickDetails = { onNavigateToProductDetail(item) }
                            )
                        }
                    }
                }
            }

            // QR / Barcode Scanner Simulator dialog overlay
            if (showScannerDialog) {
                BarcodeScannerSimulatorDialog(
                    hasCameraPermission = hasCameraPermission.value,
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onDismiss = { showScannerDialog = false },
                    onBarcodeScanned = { code ->
                        manualCodeInput = code
                        orderProvider.queryShipmentOrder(code)
                        showScannerDialog = false
                    }
                )
            }
        }
    }
}

// Custom Level 1 Item Component
@Composable
fun ProductCard(
    product: UrunModel,
    onClickDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.productName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Kod: ${product.productCode}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }

                // Completion Badge Status
                val isDone = product.status.equals("Tamamlandı", ignoreCase = true)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDone) Color(0xFFD1FAE5) else Color(0xFFFFF7ED)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, if (isDone) Color(0xFF34D399) else Color(0xFFFED7AA))
                ) {
                    Text(
                        text = if (isDone) "TAMAMLANDI" else "ONAY BEKLİYOR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) Color(0xFF047857) else Color(0xFFC2410C),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sevk Miktarı: ${product.quantity} Adet",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Detay/Parçalar button (Trigger details)
                Button(
                    onClick = onClickDetails,
                    colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("product_details_button_${product.id}")
                ) {
                    Text(
                        text = "Detay/Parçalar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}

// Barcode Scanning dialog Mock Simulator
@Composable
fun BarcodeScannerSimulatorDialog(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    // Holographic retro scanner infinite laser line animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_offset"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Mobil Barkod Entegrasyonu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BluePrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (hasCameraPermission) 
                        "Lütfen sevk emri etiketindeki barkodu vizör içinde hizalayın."
                    else 
                        "İzniniz olmadığı için simulasyon listesini kullanabilir veya kamera iznini etkinleştirebilirsiniz.",
                    fontSize = 12.sp,
                    color = TextDarkSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Outer Scanner Boundary
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .border(3.dp, BlueSecondary, RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Transparent guidelines inside
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        if (hasCameraPermission) {
                            CameraPreview(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                onBarcodeScanned = onBarcodeScanned
                            )
                        } else {
                            // Simulated QR matrix block visual
                            Icon(
                                imageVector = Icons.Filled.QrCode,
                                contentDescription = null,
                                tint = Color.Black.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .size(120.dp)
                                    .align(Alignment.Center)
                            )
                        }

                        // Animated Laser horizontal swipe line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .offset(y = (200 * laserOffset).dp)
                                .background(Color.Red)
                        )
                    }
                }

                if (!hasCameraPermission) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Gerçek kamera ile taratmak için izin vermelisiniz.",
                        fontSize = 11.sp,
                        color = LogoPink,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Kamera İznini Etkinleştir", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // High-performance quick mock selection buttons representing QR scanner read successes
                Text(
                    text = "TARAMA SİMÜLATÖR SEÇENEKLERİ:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BlueSecondary,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onBarcodeScanned("SEV-001") },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scanner_mock_sev1"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SEV-001", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onBarcodeScanned("SEV-002") },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scanner_mock_sev2"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SEV-002", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("scanner_dismiss_button")
            ) {
                Text("Vazgeç", color = TextDarkSecondary)
            }
        },
        shape = RoundedCornerShape(12.dp),
        containerColor = SurfaceWhite
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
class BarcodeAnalyzer(private val onBarcodeScanned: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val options = com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
        .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS)
        .build()
    private val scanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient(options)
    private var isScanned = false

    override fun analyze(imageProxy: ImageProxy) {
        if (isScanned) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue ?: barcode.displayValue
                        if (rawValue != null && rawValue.isNotBlank()) {
                            isScanned = true
                            onBarcodeScanned(rawValue)
                            break
                        }
                    }
                }
                .addOnFailureListener {
                    // Ignore errors during continuous analysis
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                cameraProviderRef = cameraProvider

                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor, BarcodeAnalyzer { code ->
                    previewView.post {
                        onBarcodeScanned(code)
                    }
                })

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderRef?.unbindAll()
            cameraExecutor.shutdown()
        }
    }
}
