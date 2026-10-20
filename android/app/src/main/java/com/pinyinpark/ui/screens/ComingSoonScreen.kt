package com.pinyinpark.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinyinpark.model.GameType

// ============================================================
// 未实现的游戏占位页
// 避免点进去只看到空白页面
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonScreen(
    gameType: GameType,
    onBack: () -> Unit = {}
) {
    val (emoji, title) = when (gameType) {
        GameType.MATCHING -> "🔗" to "拼音配对"
        GameType.WHACK_MOLE -> "🐹" to "拼音地鼠"
        GameType.SPEAK_REPEAT -> "🎤" to "跟我读"
        GameType.BLOCK_PUZZLE -> "🧱" to "拼音积木"
        GameType.CHAIN_GAME -> "🔄" to "音节接龙"
        GameType.MAZE_ADVENTURE -> "🗺️" to "迷宫探险"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$emoji $title") },
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
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 72.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3E2723)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "这个游戏正在开发中\n先去玩玩「拼音配对」和「拼音地鼠」吧～",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("返回首页")
            }
        }
    }
}
