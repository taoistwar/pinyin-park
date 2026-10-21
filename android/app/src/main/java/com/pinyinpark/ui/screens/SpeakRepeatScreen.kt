package com.pinyinpark.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinyinpark.data.rememberUserProgressRepository
import com.pinyinpark.util.AudioPlayerManager
import com.pinyinpark.viewmodel.SpeakRepeatViewModel
import kotlinx.coroutines.launch

// ============================================================
// 跟我读游戏页面
// 听标准发音 → 用系统语音识别跟读 → 判定是否读对
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakRepeatGameScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current

    val audioPlayer = remember { AudioPlayerManager(context) }
    DisposableEffect(audioPlayer) {
        onDispose { audioPlayer.release() }
    }
    val viewModel = remember { SpeakRepeatViewModel(audioPlayer) }
    val uiState by viewModel.uiState.collectAsState()

    val repository = rememberUserProgressRepository()
    val scope = rememberCoroutineScope()

    // 没有语音识别 / 识别被取消时，改为家长自评
    var showSelfCheck by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isGameOver) {
        if (uiState.isGameOver) {
            uiState.result?.let { result ->
                repository.recordGame(result.starsEarned, result.isPerfect)
            }
        }
    }

    // 系统语音识别（RecognizerIntent 由系统/第三方识别应用负责录音权限）
    val recognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            viewModel.onSpeechResult(spoken)
            if (!spoken.isNullOrBlank()) {
                scope.launch { repository.markSpoken() }
            }
        } else {
            viewModel.onSpeechError()
            showSelfCheck = true
        }
    }

    fun startRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请跟着读一遍")
        }
        viewModel.startListening()
        showSelfCheck = false
        try {
            recognizerLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            viewModel.onSpeechError()
            showSelfCheck = true
            Toast.makeText(
                context,
                "设备没有语音识别服务，可以用下面的按钮自己判断",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎤 跟我读") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "退出")
                    }
                },
                actions = {
                    Text(
                        "⭐ ${uiState.score}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFCE4EC)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.isGameOver) {
                Text(
                    "${uiState.currentIndex + 1} / ${uiState.totalItems}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 拼音卡片
                Card(
                    modifier = Modifier.size(200.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.currentItem?.character ?: "",
                            fontSize = 68.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = uiState.currentItem?.exampleWord ?: "",
                            fontSize = 22.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 听一听
                Button(
                    onClick = { viewModel.playCurrentAudio() },
                    enabled = !uiState.isPlayingAudio,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isPlayingAudio) "播放中..." else "听一听")
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 跟读
                Button(
                    onClick = { startRecognition() },
                    enabled = !uiState.isListening,
                    modifier = Modifier.height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isListening) "聆听中..." else "开始跟读",
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 判定结果
                uiState.speechResult?.let { result ->
                    val text = when {
                        result.isCorrect -> "✅ 读得真好！"
                        result.recognizedText.isBlank() -> "🤔 没听清，再试一次吧"
                        result.recognizedText.startsWith("自评") -> result.recognizedText
                        else -> "❌ 听到的是「${result.recognizedText}」，再试一次"
                    }
                    val color = if (result.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                    Text(
                        text = text,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // 兜底：没法语音识别时由家长自评
                if (showSelfCheck) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "听不清？让爸爸妈妈帮忙判断一下",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8D6E63)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.markSelfResult(true)
                                        showSelfCheck = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    )
                                ) {
                                    Text("读对了")
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.markSelfResult(false)
                                        showSelfCheck = false
                                    }
                                ) {
                                    Text("再读一次")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = { viewModel.nextItem() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("下一个")
                }
            } else {
                GameResultPanel(
                    result = uiState.result,
                    onRestart = { viewModel.restartGame() },
                    onExit = onBack
                )
            }
        }
    }
}
