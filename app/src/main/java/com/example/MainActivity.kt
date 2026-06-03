package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.provider.OrderProvider
import com.example.data.provider.OrderProviderFactory
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val orderProvider: OrderProvider = viewModel(
                    factory = OrderProviderFactory(applicationContext)
                )

                NavHost(
                    navController = navController,
                    startDestination = "login",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("login") {
                        LoginScreen(
                            onNavigateToDashboard = {
                                navController.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            }
                        )
                    }

                    composable("dashboard") {
                        DashboardScreen(
                            orderProvider = orderProvider,
                            onLogout = {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onNavigateToProductDetail = { product ->
                                orderProvider.loadSubItemsForProduct(product)
                                navController.navigate("detail")
                            }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            orderProvider = orderProvider,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("detail") {
                        DetailScreen(
                            orderProvider = orderProvider,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onLogout = {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
