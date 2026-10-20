package com.pinyinpark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================
// 个人中心
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👤 我的") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA5D6A7)),
                contentAlignment = Alignment.Center
            ) {
                Text("🐼", fontSize = 44.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "小小拼音家",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3E2723)
            )
            Text(
                text = "今天也要开心学拼音～",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("⭐", "12", "星星", Modifier.weight(1f))
                StatCard("🔥", "3", "连续天数", Modifier.weight(1f))
                StatCard("🎖️", "1", "徽章", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            InfoCard(
                title = "🐰 我的宠物",
                lines = listOf("小拼 Lv.1", "在首页点「喂食」可以喂它吃东西")
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoCard(
                title = "🛡️ 防沉迷",
                lines = listOf(
                    "每日游戏时长上限 30 分钟",
                    "单次连续游戏 15 分钟会提醒休息",
                    "每 3 局游戏进行一次汉字验证"
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoCard(
                title = "ℹ️ 关于",
                lines = listOf(
                    "拼音乐园 v$versionName",
                    "面向 3-8 岁幼儿的拼音启蒙应用",
                    "数据仅保存在本机，不做上传"
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3E2723)
            )
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
private fun InfoCard(title: String, lines: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3E2723)
            )
            Spacer(modifier = Modifier.height(8.dp))
            lines.forEach { line ->
                Text(
                    text = "· $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
