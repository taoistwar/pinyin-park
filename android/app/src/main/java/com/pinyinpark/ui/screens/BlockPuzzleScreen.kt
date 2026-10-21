package com.pinyinpark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.pinyinpark.viewmodel.BlockPuzzleViewModel
import com.pinyinpark.viewmodel.BlockType
import com.pinyinpark.viewmodel.PlacedBlock
import com.pinyinpark.viewmodel.PuzzleBlock

// ============================================================
// 拼音积木游戏页面
// 把声母 / 韵母 / 声调三块积木放到对应位置，拼出目标拼音
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockPuzzleGameScreen(
    viewModel: BlockPuzzleViewModel = remember { BlockPuzzleViewModel() },
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
                title = { Text("🧱 拼音积木") },
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
                    "第 ${uiState.completedCount + 1} / ${uiState.totalRounds} 题",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 目标
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("拼出 ", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = uiState.targetPinyin,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D47A1)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.targetExample,
                            fontSize = 20.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 三个槽位
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.placedBlocks.sortedBy { it.position }.forEach { placed ->
                        SlotView(
                            placed = placed,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (placed.block != null) viewModel.removeBlock(placed.position)
                            }
                        )
                    }
                }

                Text(
                    text = "声母　　韵母　　声调",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 提示
                TextButton(onClick = { viewModel.showHint() }) {
                    Text("💡 提示")
                }
                if (uiState.showHint && uiState.currentHint.isNotBlank()) {
                    Text(
                        text = uiState.currentHint,
                        color = Color(0xFFFF9800),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 积木池
                Text(
                    text = "点一点积木，把它放进对应的位置",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.availableBlocks.chunked(4).forEach { rowBlocks ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowBlocks.forEach { block ->
                                BlockView(
                                    block = block,
                                    enabled = !uiState.isComplete,
                                    onClick = {
                                        val position = when (block.type) {
                                            BlockType.INITIAL -> 0
                                            BlockType.FINAL -> 1
                                            BlockType.TONE -> 2
                                        }
                                        viewModel.placeBlock(position, block)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (uiState.isComplete) {
                    Text(
                        text = "✅ 拼对啦！",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.nextPuzzle() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("下一题")
                    }
                } else {
                    Text(
                        text = "放错了会自动退回，别着急～",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
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

// ============================================================
// 槽位
// ============================================================

@Composable
private fun SlotView(
    placed: PlacedBlock,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val block = placed.block
    val slotColor = when (placed.position) {
        0 -> Color(0xFF2196F3)
        1 -> Color(0xFF4CAF50)
        else -> Color(0xFFFF9800)
    }

    Box(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (block == null) Color(0xFFF5F5F5) else slotColor.copy(alpha = 0.15f))
            .border(
                width = 2.dp,
                color = if (block == null) Color(0xFFBDBDBD) else slotColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = block != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = block?.let { blockText(it) } ?: "?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (block == null) Color(0xFFBDBDBD) else slotColor
        )
    }
}

// ============================================================
// 积木
// ============================================================

@Composable
private fun BlockView(
    block: PuzzleBlock,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val color = when (block.type) {
        BlockType.INITIAL -> Color(0xFF2196F3)
        BlockType.FINAL -> Color(0xFF4CAF50)
        BlockType.TONE -> Color(0xFFFF9800)
    }

    Box(
        modifier = Modifier
            .size(width = 68.dp, height = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = blockText(block),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}

/** 声调积木显示成带调号的说明，其他积木直接显示拼音 */
private fun blockText(block: PuzzleBlock): String = when (block.type) {
    BlockType.TONE -> when (block.text) {
        "1" -> "ˉ"
        "2" -> "ˊ"
        "3" -> "ˇ"
        "4" -> "ˋ"
        else -> block.text
    }
    else -> block.text
}
