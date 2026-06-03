package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var username by remember { mutableStateOf("montaj_uzmani") }
    var password by remember { mutableStateOf("saha2026") }
    var loginError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Settings Action Button at the top right
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 24.dp)
                .testTag("login_settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Ayarlar",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Enterprise Logo Emblem
            Box(
                modifier = Modifier
                    .size(85.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(BlueSecondary),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SM",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Application Branding
            Text(
                text = "Saha Montaj Portal",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Fotoğraf Onay ve Teslimat Sistemi",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Credentials Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "GİRİŞ YAP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlueSecondary,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Username input
                    OutlinedTextField(
                        value = username,
                        onValueChange = { 
                            username = it
                            loginError = null
                        },
                        label = { Text("Kullanıcı Adı", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BlueSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = BlueSecondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedLabelColor = BlueSecondary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            loginError = null
                        },
                        label = { Text("Şifre", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BlueSecondary) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = BlueSecondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedLabelColor = BlueSecondary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    loginError?.let {
                        Text(
                            text = it,
                            color = Color(0xFFDC2626), // Sharp red-600
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Action Button
                    Button(
                        onClick = {
                            if (username.trim().isEmpty() || password.trim().isEmpty()) {
                                loginError = "Lütfen tüm alanları doldurun."
                            } else {
                                onNavigateToDashboard()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Giriş Yap",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
