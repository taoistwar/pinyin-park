package com.pinyinpark.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinyinpark.model.PinyinItem
import com.pinyinpark.viewmodel.AntiAddictionManager
import com.pinyinpark.viewmodel.HitEffectType
import com.pinyinpark.viewmodel.MoleHole
import com.pinyinpark.viewmodel.WhackMoleViewModel
import com.pinyinpark.ui.components.GameTimeIndicator

// ============================================================
// 拼音地鼠游戏页面
// 地鼠从 9 个洞里冒出，点掉与提示一致的拼音即可得分
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhackMoleGameScreen(
    viewModel: WhackMoleViewModel = remember { WhackMoleViewModel() },
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // 防沉迷：与其他游戏保持一致
    val antiAddictionManager = remember { AntiAddictionManager() }
    val antiAddictionState by antiAddictionManager.state.collectAsState()
    val challenge by antiAddictionManager.challenge.collectAsState()

    LaunchedEffect(Unit) {
        antiAddictionManager.startSession()
    }
    LaunchedEffect(uiState.isGameOver) {
        if (uiState.isGameOver) {
            antiAddictionManager.onGameCompleted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🐹 拼音地鼠") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "退出")
                    }
                },
                actions = {
                    GameTimeIndicator(remainingMinutes = antiAddictionManager.getRemainingTime())
                    Text(
                        "⏱ ${uiState.timeLeft}s",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    Text(
                        "⭐ ${uiState.score}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 8.dp, end = 12.dp)
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
                TargetBanner(currentTarget = uiState.currentTarget)

                Spacer(modifier = Modifier.height(8.dp))

                HitEffectText(
                    visible = uiState.showHitEffect,
                    type = uiState.hitEffectType
                )

                Spacer(modifier = Modifier.height(8.dp))

                MoleGrid(
                    holes = uiState.holes,
                    onHoleClick = viewModel::onHoleClicked,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "点掉和上方提示一样的拼音地鼠",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                GameResultPanel(
                    result = uiState.result,
                    onRestart = { viewModel.restartGame() },
                    onExit = onBack
                )
            }
        }
    }

    AntiAddictionDialogHandler(
        state = antiAddictionState,
        challenge = challenge,
        antiAddictionManager = antiAddictionManager,
        currentGameMinutes = antiAddictionManager.getCurrentGameTime(),
        onExit = onBack
    )
}

// ============================================================
// 目标拼音提示条
// ============================================================

@Composable
private fun TargetBanner(currentTarget: PinyinItem?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("打这个 →", color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = currentTarget?.character ?: "…",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentTarget?.exampleWord ?: "",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text("🐹", fontSize = 22.sp)
        }
    }
}

// ============================================================
// 击中提示（固定高度，避免布局跳动）
// ============================================================

@Composable
private fun HitEffectText(visible: Boolean, type: HitEffectType) {
    Box(
        modifier = Modifier.height(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            val (text, color) = when (type) {
                HitEffectType.CORRECT -> "✅ 打中了！+10" to Color(0xFF4CAF50)
                HitEffectType.WRONG -> "❌ 打错了，再找找" to Color(0xFFF44336)
                HitEffectType.NONE -> "" to Color.Transparent
            }
            Text(
                text = text,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

// ============================================================
// 3 x 3 地洞网格
// ============================================================

@Composable
private fun MoleGrid(
    holes: List<MoleHole>,
    onHoleClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        holes.chunked(3).forEach { rowHoles ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowHoles.forEach { hole ->
                    MoleHoleView(
                        hole = hole,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onHoleClick(hole.id) }
                    )
                }
                // 不足 3 个时补位，保证格子大小一致
                repeat(3 - rowHoles.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MoleHoleView(
    hole: MoleHole,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val mole = hole.currentPinyin
    val holeColor = when {
        hole.isHitting -> Color(0xFFE53935)
        mole != null -> Color(0xFF8D6E63)
        else -> Color(0xFF6D4C41)
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(CircleShape)
            .background(holeColor)
            .border(3.dp, Color(0xFF4E342E), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (mole != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🐹", fontSize = 22.sp)
                Text(
                    text = mole.character,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // 空洞口
            Box(
                modifier = Modifier
                    .fillMaxSize(0.5f)
                    .clip(CircleShape)
                    .background(Color(0xFF3E2723))
            )
        }
    }
}
