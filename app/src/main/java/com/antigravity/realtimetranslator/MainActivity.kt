package com.antigravity.realtimetranslator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.antigravity.realtimetranslator.ui.ModelDownloadScreen
import com.antigravity.realtimetranslator.ui.TranslatorScreen
import com.antigravity.realtimetranslator.viewmodel.TranslatorViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {

    private val viewModel: TranslatorViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0D1117),
                    surface    = Color(0xFF161B22),
                    primary    = Color(0xFF58A6FF)
                )
            ) {
                val uiState by viewModel.uiState.collectAsState()

                // 화면 전환 상태
                // "translator" (메인) | "download" (모델 다운로드)
                var currentScreen by remember { mutableStateOf("translator") }

                // 최초 실행: 모델이 없으면 다운로드 화면으로
                LaunchedEffect(Unit) {
                    if (!uiState.isModelLoaded && uiState.downloadedModels.isEmpty()) {
                        currentScreen = "download"
                    }
                }

                // 마이크 권한
                val micPermission = rememberPermissionState(
                    android.Manifest.permission.RECORD_AUDIO
                )
                LaunchedEffect(Unit) {
                    if (!micPermission.status.isGranted) {
                        micPermission.launchPermissionRequest()
                    }
                }

                when (currentScreen) {
                    "download" -> {
                        ModelDownloadScreen(
                            hfToken = uiState.hfToken,
                            onHfTokenChange = { viewModel.setHfToken(it) },
                            downloadProgress = uiState.downloadProgress,
                            downloadedModels = uiState.downloadedModels,
                            activeModel = uiState.activeModel,
                            onDownload = { model -> viewModel.startModelDownload(model) },
                            onCancelDownload = { model -> viewModel.cancelModelDownload(model) },
                            onUseModel = { model ->
                                viewModel.useDownloadedModel(model)
                                currentScreen = "translator"
                            },
                            onBack = { currentScreen = "translator" }
                        )
                    }

                    else -> { // "translator"
                        if (!micPermission.status.isGranted) {
                            // 권한 없음 안내
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("마이크 권한이 필요합니다.", color = Color(0xFFE6EDF3))
                                    Button(onClick = { micPermission.launchPermissionRequest() }) {
                                        Text("권한 허용")
                                    }
                                }
                            }
                        } else {
                            TranslatorScreen(
                                uiState = uiState,
                                onStartListening = { viewModel.startListening() },
                                onStopListening  = { viewModel.stopListening() },
                                onSwapLanguages  = { viewModel.swapLanguages() },
                                onSourceLangChange = { viewModel.setSourceLang(it) },
                                onTargetLangChange = { viewModel.setTargetLang(it) },
                                onNavigateToSetup  = { currentScreen = "download" },
                                onClearError       = { viewModel.clearError() }
                            )
                        }
                    }
                }
            }
        }
    }
}
