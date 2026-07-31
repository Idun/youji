package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screen.HomeScreen
import com.example.ui.screen.InspirationEditScreen
import com.example.ui.screen.InspirationListScreen
import com.example.ui.screen.InspirationMergePreviewScreen
import com.example.ui.screen.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.InspirationViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 获取手动注入的 Repository 容器
        val appContainer = (application as YouJiApplication).container
        
        // 利用 Factory 创建 ViewModel
        val sharedPreferences = getSharedPreferences("youji_prefs", android.content.Context.MODE_PRIVATE)
        val viewModel: InspirationViewModel by viewModels {
            InspirationViewModel.Factory(appContainer.inspirationRepository, sharedPreferences)
        }

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "splash",
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 启动欢迎页
                    composable("splash") {
                        SplashScreen(
                            brandColor = Color(0xFF1B7679),
                            onSplashFinished = {
                                navController.navigate("home") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 新的首页 (Dashboard)
                    composable("home") {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToList = {
                                navController.navigate("list")
                            },
                            onNavigateToEdit = { id ->
                                navController.navigate("edit/$id")
                            },
                            onNavigateToMergePreview = { ids ->
                                navController.navigate("merge_preview/$ids")
                            }
                        )
                    }

                    // 灵感列表主页
                    composable("list") {
                        InspirationListScreen(
                            viewModel = viewModel,
                            onNavigateToEdit = { id ->
                                navController.navigate("edit/$id")
                            },
                            onNavigateToMergePreview = { ids ->
                                navController.navigate("merge_preview/$ids")
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                    
                    // 灵感编辑 & Markdown 预览页面
                    composable(
                        route = "edit/{id}",
                        arguments = listOf(
                            navArgument("id") { type = NavType.IntType }
                        )
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getInt("id") ?: 0
                        InspirationEditScreen(
                            inspirationId = id,
                            viewModel = viewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToList = {
                                navController.navigate("list") {
                                    popUpTo("home") { inclusive = false }
                                }
                            },
                            onNavigateToEdit = { nextId ->
                                navController.navigate("edit/$nextId")
                            }
                        )
                    }

                    // 合并预览页面
                    composable(
                        route = "merge_preview/{ids}",
                        arguments = listOf(
                            navArgument("ids") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val ids = backStackEntry.arguments?.getString("ids") ?: ""
                        InspirationMergePreviewScreen(
                            ids = ids,
                            viewModel = viewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
