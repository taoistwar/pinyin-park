package com.pinyinpark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinyinpark.data.rememberUserProgressRepository
import com.pinyinpark.model.PinyinItem
import com.pinyinpark.viewmodel.ChainGameViewModel

// ============================================================
// 音节接龙游戏页面
// 下一个拼音的韵母要等于上一个拼音的韵母
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChainGameScreen(
    viewModel: ChainGameViewModel = remember { ChainGameViewModel() },
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val repository = rememberUserProgressRepository()
    LaunchedEffect(uiState.isGameOver) {
        if (uiState.isGameOver) {
            uiState.result?.let { result ->
                repository.recordGame(result.starsEarned, result.isPerfect)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔄 音节接龙") },
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
                // 当前拼音
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("当前音节", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(
                            text = uiState.currentPinyin,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D47A1)
                        )
                        Text(
                            text = uiState.currentExample,
                            fontSize = 16.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "接一个韵母是「${uiState.currentFinal}」的拼音",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 接龙历史
                if (uiState.historyPinyins.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.historyPinyins.chunked(8).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowItems.forEach { pinyin ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(pinyin, fontSize = 12.sp, color = Color(0xFF2E7D32))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "接龙 ${uiState.chainLength} 个",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 错误提示
                if (uiState.showWrong) {
                    Text(
                        text = "❌ 这个不接得上，再看看",
                        color = Color(0xFFF44336),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 候选拼音
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    uiState.candidates.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowItems.forEach { item ->
                                CandidateButton(
                                    item = item,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.selectPinyin(item) }
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                if (uiState.gameOverReason.isNotBlank()) {
                    Text(
                        text = uiState.gameOverReason,
                        color = Color(0xFF5D4037),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                GameResultPanel(
                    result = uiState.result,
                    onRestart = { viewModel.restartGame() },
                    onExit = onBack
                )
            }
        }
    }
}

@Composable
private fun CandidateButton(
    item: PinyinItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFCE4EC))
    ) {
        Text(
            text = "${item.character} ${item.exampleWord}",
            color = Color(0xFF3E2723),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
