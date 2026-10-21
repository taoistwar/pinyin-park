package com.pinyinpark.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.pinyinpark.data.rememberUserProgressRepository
import com.pinyinpark.model.*
import com.pinyinpark.util.AudioPlayerManager
import com.pinyinpark.viewmodel.*
import com.pinyinpark.ui.components.*

// ============================================================
// 学习页面 - 单个拼音学习
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    chapter: LessonChapter,
    onBack: () -> Unit = {},
    onComplete: () -> Unit = {}
) {
    val context = LocalContext.current

    // 音频播放器与页面同生命周期，负责发音（无本地音频时走系统 TTS）
    val audioPlayer = remember(chapter.id) { AudioPlayerManager(context) }
    DisposableEffect(audioPlayer) {
        onDispose { audioPlayer.release() }
    }
    val repository = rememberUserProgressRepository()
    val viewModel = remember(chapter.id) { LearnViewModel(chapter, audioPlayer, repository) }
    val uiState by viewModel.uiState.collectAsState()

    // 设备缺少中文语音引擎时提示一次
    LaunchedEffect(uiState.audioUnavailable) {
        if (uiState.audioUnavailable) {
            Toast.makeText(
                context,
                "未找到中文语音引擎，请在「设置 → 文字转语音」中安装或启用中文语音包",
                Toast.LENGTH_LONG
            ).show()
            viewModel.consumeAudioUnavailable()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chapter.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE3F2FD)
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
            // 进度指示器
            LinearProgressIndicator(
                progress = {
                    if (uiState.totalItems > 0)
                        (uiState.currentIndex + 1).toFloat() / uiState.totalItems
                    else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFFE0E0E0)
            )
            Text(
                "${uiState.currentIndex + 1} / ${uiState.totalItems}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            uiState.currentItem?.let { item ->
                // 中间内容可滚动，避免展开口型动画后在小屏设备上被裁掉
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 主拼音卡片
                    PinyinCard(item = item, isPlaying = uiState.isPlayingAudio)

                    Spacer(modifier = Modifier.height(28.dp))

                    // 播放音频按钮
                    PlayAudioButton(
                        onClick = { viewModel.playAudio() },
                        isPlaying = uiState.isPlayingAudio
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 口型动画开关
                    MouthShapeToggle(
                        showMouthShape = uiState.showMouthShape,
                        onToggle = { viewModel.toggleMouthShape() }
                    )

                    // 口型动画展示区
                    AnimatedVisibility(
                        visible = uiState.showMouthShape,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        MouthShapeIndicator(
                            pinyin = uiState.currentItem?.character.orEmpty(),
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 底部导航按钮
                NavigationButtons(
                    canGoPrev = uiState.currentIndex > 0,
                    canGoNext = !uiState.isCompleted,
                    onPrev = { viewModel.prevItem() },
                    onNext = { viewModel.nextItem() }
                )
            }
        }
    }

    // 完成弹窗
    if (uiState.isCompleted) {
        CompletionDialog(
            starsEarned = uiState.starsEarned,
            onContinue = onComplete
        )
    }
}

// ============================================================
// 拼音卡片
// ============================================================

@Composable
fun PinyinCard(item: PinyinItem, isPlaying: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.1f else 1f,
        animationSpec = tween(300),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .size(200.dp)
            .scale(scale),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.character,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (item.examplePinyin.isNotBlank()) {
                    Text(
                        text = item.examplePinyin,
                        fontSize = 22.sp,
                        maxLines = 1,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Text(
                    text = item.exampleWord,
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            // 播放动画
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "播放中",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

// ============================================================
// 播放音频按钮
// ============================================================

@Composable
fun PlayAudioButton(onClick: () -> Unit, isPlaying: Boolean) {
    Button(
        onClick = onClick,
        enabled = !isPlaying,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
        modifier = Modifier.height(56.dp)
    ) {
        Icon(
            if (isPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isPlaying) "播放中..." else "点击听发音", fontSize = 18.sp)
    }
}

// ============================================================
// 口型动画开关
// ============================================================

@Composable
fun MouthShapeToggle(showMouthShape: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onToggle)
    ) {
        Switch(
            checked = showMouthShape,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CAF50))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("显示口型动画", style = MaterialTheme.typography.bodyMedium)
        Text("👄", fontSize = 20.sp)
    }
}

// ============================================================
// 导航按钮
// ============================================================

@Composable
fun NavigationButtons(
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedButton(
            onClick = onPrev,
            enabled = canGoPrev,
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("上一个")
        }

        Button(
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier.height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("下一个")
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

// ============================================================
// 完成弹窗
// ============================================================

@Composable
fun CompletionDialog(starsEarned: Int, onContinue: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            Button(onClick = onContinue) {
                Text("继续学习")
            }
        },
        icon = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 48.sp)
                Row {
                    repeat(3) { index ->
                        Text(
                            if (index < starsEarned) "⭐" else "☆",
                            fontSize = 32.sp
                        )
                    }
                }
            }
        },
        title = { Text("太棒了！", style = MaterialTheme.typography.headlineSmall) },
        text = { Text("你完成了本章节的学习，获得了 $starsEarned 颗星星！") }
    )
}

// ============================================================
// 游戏页面 - 配对游戏（集成防沉迷系统）
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchGameScreen(
    viewModel: MatchGameViewModel = remember { MatchGameViewModel() },
    onBack: () -> Unit = {},
    onComplete: (GameResult) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // 防沉迷管理器
    val antiAddictionManager = remember { AntiAddictionManager() }
    val repository = rememberUserProgressRepository()
    val antiAddictionState by antiAddictionManager.state.collectAsState()
    val challenge by antiAddictionManager.challenge.collectAsState()

    // 启动游戏会话
    LaunchedEffect(Unit) {
        antiAddictionManager.startSession()
    }

    // 游戏完成时通知防沉迷系统，并把结果写入本地存档
    LaunchedEffect(uiState.isGameOver) {
        if (uiState.isGameOver) {
            antiAddictionManager.onGameCompleted()
            uiState.result?.let { result ->
                repository.recordGame(result.starsEarned, result.isPerfect)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔗 拼音配对") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "退出")
                    }
                },
                actions = {
                    // 防沉迷时间指示器
                    GameTimeIndicator(
                        remainingMinutes = antiAddictionManager.getRemainingTime()
                    )
                    Text(
                        "得分: ${uiState.score}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
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
            Text(
                "把相同的拼音连在一起！",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!uiState.isGameOver) {
                // 配对网格
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 左边：声母
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        uiState.leftItems.forEach { item ->
                            MatchItem(
                                item = item,
                                isMatched = uiState.matchedPairs.contains(item.id),
                                isWrong = uiState.wrongPair?.first == item.id,
                                isSelected = uiState.selectedLeft == item.id,
                                onClick = { viewModel.selectLeft(item.id) }
                            )
                        }
                    }

                    Text("🔗", fontSize = 40.sp, modifier = Modifier.align(Alignment.CenterVertically))

                    // 右边：打乱顺序
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        uiState.rightItems.forEach { item ->
                            MatchItem(
                                item = item,
                                isMatched = uiState.matchedPairs.contains(item.id),
                                isWrong = uiState.wrongPair?.second == item.id,
                                isSelected = uiState.selectedRight == item.id,
                                onClick = { viewModel.selectRight(item.id) }
                            )
                        }
                    }
                }
            } else {
                // 游戏结束 - 显示结果
                GameResultPanel(
                    result = uiState.result,
                    onRestart = { viewModel.restartGame() },
                    onExit = onBack
                )
            }
        }
    }

    // 防沉迷弹窗
    AntiAddictionDialogHandler(
        state = antiAddictionState,
        challenge = challenge,
        antiAddictionManager = antiAddictionManager,
        currentGameMinutes = antiAddictionManager.getCurrentGameTime(),
        onExit = onBack
    )
}

// ============================================================
// 配对项
// ============================================================

@Composable
fun MatchItem(
    item: PinyinItem,
    isMatched: Boolean,
    isWrong: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isMatched -> Color(0xFF4CAF50)
        isWrong -> Color(0xFFF44336)
        isSelected -> Color(0xFFFF9800)
        else -> Color.White
    }

    val borderColor = when {
        isMatched -> Color(0xFF388E3C)
        isWrong -> Color(0xFFD32F2F)
        isSelected -> Color(0xFFE65100)
        else -> Color(0xFFBDBDBD)
    }

    Card(
        modifier = Modifier
            .size(80.dp)
            .clickable(enabled = !isMatched, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(3.dp, borderColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isMatched -> Text("✓", fontSize = 40.sp, color = Color.White)
                item.id.length <= 2 -> Text(item.character, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                else -> Text(item.exampleWord, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================================
// 游戏结果面板
// ============================================================

@Composable
fun GameResultPanel(
    result: GameResult?,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎮 游戏结束", fontSize = 28.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            result?.let {
                Text("得分: ${it.score} / ${it.maxScore}", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    repeat(3) { index ->
                        Text(
                            if (index < it.starsEarned) "⭐" else "☆",
                            fontSize = 40.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = onExit) {
                    Text("返回")
                }
                Button(
                    onClick = onRestart,
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("再玩一次")
                }
            }
        }
    }
}

// ============================================================
// 防沉迷验证状态处理
// ============================================================

/**
 * 防沉迷弹窗状态处理
 */
@Composable
fun AntiAddictionDialogHandler(
    state: AntiAddictionState,
    challenge: VerificationChallenge?,
    antiAddictionManager: AntiAddictionManager,
    currentGameMinutes: Int,
    onExit: () -> Unit
) {
    when (state) {
        is AntiAddictionState.ShowVerification -> {
            challenge?.let {
                VerificationDialog(
                    challenge = it,
                    onVerified = {
                        antiAddictionManager.onVerificationSuccess()
                    },
                    onFailed = { attempts ->
                        antiAddictionManager.onVerificationFailed(attempts)
                    },
                    onTimeout = {
                        antiAddictionManager.onVerificationTimeout()
                    }
                )
            }
        }

        is AntiAddictionState.ShowReminder -> {
            GameTimeReminderDialog(
                currentMinutes = currentGameMinutes,
                onContinue = {
                    antiAddictionManager.onContinueGame()
                },
                onTakeBreak = {
                    antiAddictionManager.onTakeBreak()
                    onExit()
                }
            )
        }

        is AntiAddictionState.TimeLimitReached -> {
            GameTimeLimitDialog(
                onExit = onExit
            )
        }

        is AntiAddictionState.VerificationFailed -> {
            VerificationFailedDialog(
                attempts = state.attempts,
                onRetry = {
                    // 重新开始验证
                    antiAddictionManager.startSession()
                },
                onExit = onExit
            )
        }

        else -> { /* Normal 状态不显示弹窗 */ }
    }
}