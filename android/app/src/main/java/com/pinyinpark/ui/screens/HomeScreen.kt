package com.pinyinpark.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.pinyinpark.model.*
import com.pinyinpark.viewmodel.*

// ============================================================
// 主页面 - 拼音乐园首页
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = remember { HomeViewModel() },
    onChapterClick: (LessonChapter) -> Unit = {},
    onGameClick: (GameType) -> Unit = {},
    onBadgesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🐼 拼音乐园", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onBadgesClick) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "勋章墙")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = "我的")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFE5B4)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 用户进度卡片
            UserProgressCard(uiState.userProgress)

            // 学习章节区域
            SectionTitle("📚 学习章节")
            LazyColumn(
                modifier = Modifier.height(280.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.chapters) { chapter ->
                    ChapterCard(
                        chapter = chapter,
                        onClick = { onChapterClick(chapter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 游戏区域
            SectionTitle("🎮 趣味游戏")
            LazyHorizontalScroller(
                items = GameType.entries.toList(),
                itemContent = { gameType ->
                    GameCard(gameType = gameType, onClick = { onGameClick(gameType) })
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 宠物养成区
            SectionTitle("🐰 我的小宠物")
            PetCard(
                petLevel = uiState.userProgress.petLevel,
                hunger = uiState.petHunger,
                onFeed = { viewModel.feedPet() }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // 庆祝弹窗
    if (uiState.showCelebration) {
        CelebrationDialog(
            message = uiState.celebrationMessage,
            onDismiss = { viewModel.dismissCelebration() }
        )
    }
}

// ============================================================
// 用户进度卡片
// ============================================================

@Composable
fun UserProgressCard(progress: UserProgress) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("⭐ ${progress.totalStars} 星星", style = MaterialTheme.typography.titleLarge)
                Text("🔥 连续 ${progress.streakDays} 天", color = Color.Gray)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BadgeIcon("🏆", "完成 ${progress.completedItems.size} 个")
                BadgeIcon("🎖️", "获得 ${progress.unlockedBadges.size} 枚徽章")
            }
        }
    }
}

@Composable
fun BadgeIcon(emoji: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

// ============================================================
// 章节卡片
// ============================================================

@Composable
fun ChapterCard(chapter: LessonChapter, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = chapter.isUnlocked, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (chapter.isUnlocked) Color(0xFFE3F2FD) else Color(0xFFE0E0E0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (chapter.isUnlocked) Color(0xFF2196F3) else Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(chapter.iconEmoji, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(chapter.title, style = MaterialTheme.typography.titleMedium)
                Text(chapter.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                // 进度条
                LinearProgressIndicator(
                    progress = { chapter.progressPercent },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFFE0E0E0)
                )
                Text(
                    "${chapter.completedCount}/${chapter.totalCount} 已学",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Text(if (chapter.isUnlocked) "▶️" else "🔒", fontSize = 24.sp)
        }
    }
}

// ============================================================
// 游戏卡片
// ============================================================

@Composable
fun GameCard(gameType: GameType, onClick: () -> Unit) {
    val (emoji, title, desc) = when (gameType) {
        GameType.MATCHING -> Triple("🔗", "拼音配对", "连线匹配")
        GameType.WHACK_MOLE -> Triple("🐹", "拼音地鼠", "快速点击")
        GameType.SPEAK_REPEAT -> Triple("🎤", "跟我读", "语音练习")
        GameType.BLOCK_PUZZLE -> Triple("🧱", "拼音积木", "拖拽组合")
        GameType.CHAIN_GAME -> Triple("🔄", "音节接龙", "同韵接龙")
        GameType.MAZE_ADVENTURE -> Triple("🗺️", "迷宫探险", "拼音寻路")
    }

    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE4EC))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 40.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

// ============================================================
// 宠物卡片
// ============================================================

@Composable
fun PetCard(petLevel: Int, hunger: Int = 100, onFeed: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 宠物动画占位
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA5D6A7)),
                contentAlignment = Alignment.Center
            ) {
                Text("🐰", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("小兔子", style = MaterialTheme.typography.titleMedium)
                Text("等级 Lv.$petLevel", color = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) { index ->
                        Text(
                            if (index < petLevel) "❤️" else "🖤",
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { hunger / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFFF9800),
                    trackColor = Color(0xFFE0E0E0)
                )
                Text(
                    "饱食度 $hunger%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Button(onClick = onFeed) {
                Text("🍎 喂食")
            }
        }
    }
}

// ============================================================
// 章节标题
// ============================================================

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ============================================================
// 庆祝弹窗
// ============================================================

@Composable
fun CelebrationDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("太棒了！🎉") } },
        icon = { Text("🎊", fontSize = 48.sp) },
        title = { Text("恭喜你！") },
        text = { Text(message) }
    )
}

// ============================================================
// 横向滚动列表辅助
// ============================================================

@Composable
fun <T> LazyHorizontalScroller(
    items: List<T>,
    itemContent: @Composable (T) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item -> itemContent(item) }
    }
}