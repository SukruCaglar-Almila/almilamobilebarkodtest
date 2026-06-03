package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.provider.OrderProvider
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    orderProvider: OrderProvider,
    onNavigateBack: () -> Unit
) {
    val currentUrl by orderProvider.apiUrl.collectAsState()
    val isDemoActive by orderProvider.demoMode.collectAsState()

    var urlInput by remember { mutableStateOf(currentUrl) }
    var tempDemoActive by remember { mutableStateOf(isDemoActive) }
    var saveFeedbackMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Sistem Bağlantı Ayarları",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "BAĞLANTI YAPILANDIRMASI",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlueSecondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // API URL TextField
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { 
                            urlInput = it
                            saveFeedbackMessage = null
                        },
                        label = { Text("REST API Base URL", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, tint = BlueSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_api_url_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = BlueSecondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedLabelColor = BlueSecondary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        placeholder = { Text("https://your-api.com/") }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Spacer(modifier = Modifier.height(20.dp))

                    // Demo Mode Switch Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_demo_mode_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Demo Modu",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sunucu bağlantısı olmadan statik veriler ile uygulamayı simüle et.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        
                        Switch(
                            checked = tempDemoActive,
                            onCheckedChange = { 
                                tempDemoActive = it 
                                saveFeedbackMessage = null
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BlueSecondary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("settings_demo_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    saveFeedbackMessage?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BlueSecondary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, BlueSecondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = BlueSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = it,
                                color = BlueSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Save Button
                    Button(
                        onClick = {
                            var cleanUrl = urlInput.trim()
                            if (cleanUrl.isNotEmpty() && !cleanUrl.startsWith("http")) {
                                cleanUrl = "https://$cleanUrl"
                            }
                            urlInput = cleanUrl
                            orderProvider.updateApiSettings(cleanUrl, tempDemoActive)
                            saveFeedbackMessage = "Bağlantı ayarları başarıyla kaydedildi."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_save_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Ayarları Kaydet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tech Alert Banner
            val isDark = isSystemInDarkTheme()
            val alertBg = if (isDark) Color(0xFF3F2E0F) else Color(0xFFFEF3C7)
            val alertIconTint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
            val alertText = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = alertBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = alertIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Demo modu aktifken, internet veya API adresi geçersiz olsa dahi sevk sorgulaması ve tüm montaj akışları çevrimdışı çalışacaktır.",
                        fontSize = 12.sp,
                        color = alertText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
