package com.pinyinpark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinyinpark.viewmodel.AchievementViewModel
import com.pinyinpark.viewmodel.BadgeWithStatus
import com.pinyinpark.viewmodel.TrophyWithStatus

// ============================================================
// 勋章墙
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(onBack: () -> Unit = {}) {
    val viewModel = remember { AchievementViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    // 目前还没有本地存档，用首页的示例进度初始化徽章解锁状态
    LaunchedEffect(Unit) {
        viewModel.updateLearningProgress(
            totalLearned = 12,
            streakDays = 3,
            perfectScores = 0
        )
    }

    val unlockedBadges = uiState.badges.count { it.isUnlocked }
    val unlockedTrophies = uiState.trophies.count { it.isUnlocked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏆 勋章墙") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "徽章 $unlockedBadges / ${uiState.badges.size}　奖杯 $unlockedTrophies / ${uiState.trophies.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8D6E63)
            )

            Spacer(modifier = Modifier.height(16.dp))

            BadgesSectionTitle("🎖️ 成就徽章")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.badges.chunked(2).forEach { rowBadges ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowBadges.forEach { badge ->
                            BadgeCard(badge, Modifier.weight(1f))
                        }
                        if (rowBadges.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            BadgesSectionTitle("🏆 奖杯收藏")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.trophies.forEach { trophy ->
                    TrophyRow(trophy)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BadgesSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF3E2723),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun BadgeCard(badge: BadgeWithStatus, modifier: Modifier = Modifier) {
    val unlocked = badge.isUnlocked
    Card(
        modifier = modifier.alpha(if (unlocked) 1f else 0.5f),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) Color(0xFFFFF3E0) else Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (unlocked) badge.badge.iconEmoji else "🔒",
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = badge.badge.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                text = badge.badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { badge.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

@Composable
private fun TrophyRow(trophy: TrophyWithStatus) {
    val unlocked = trophy.isUnlocked
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) Color(0xFFFFF8E1) else Color(0xFFF5F5F5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (unlocked) Color(0xFFFFE0B2) else Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (unlocked) trophy.trophy.emoji else "🔒", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trophy.trophy.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = trophy.trophy.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Text(
                text = if (unlocked) "已获得" else "${(trophy.progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = if (unlocked) Color(0xFF4CAF50) else Color.Gray
            )
        }
    }
}
