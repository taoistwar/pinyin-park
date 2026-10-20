package com.pinyinpark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.pinyinpark.viewmodel.Cell
import com.pinyinpark.viewmodel.CellType
import com.pinyinpark.viewmodel.Direction
import com.pinyinpark.viewmodel.MazeAdventureViewModel
import com.pinyinpark.viewmodel.Position

// ============================================================
// 迷宫探险游戏页面
// 在迷宫里移动，收集拼音，走到终点通关
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MazeAdventureGameScreen(
    viewModel: MazeAdventureViewModel = remember { MazeAdventureViewModel() },
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🗺️ 迷宫探险") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "退出")
                    }
                },
                actions = {
                    Text(
                        "第 ${uiState.level} 关",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(end = 8.dp)
                    )
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
                    text = "用方向键走到 🏁，路上点到拼音会自动收集",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "已收集 ${uiState.collectedPinyins.size} 个 · 走了 ${uiState.moves} 步",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8D6E63)
                )

                Spacer(modifier = Modifier.height(12.dp))

                MazeBoard(
                    maze = uiState.maze,
                    playerPosition = uiState.playerPosition,
                    targetPosition = uiState.targetPosition,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 方向键
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DirectionButton("↑") { viewModel.move(Direction.UP) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DirectionButton("←") { viewModel.move(Direction.LEFT) }
                        DirectionButton("↓") { viewModel.move(Direction.DOWN) }
                        DirectionButton("→") { viewModel.move(Direction.RIGHT) }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (uiState.showCelebration) {
                    Text(
                        text = "🎉 过关啦！",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
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
// 迷宫棋盘
// ============================================================

@Composable
private fun MazeBoard(
    maze: List<List<Cell>>,
    playerPosition: Position,
    targetPosition: Position,
    modifier: Modifier = Modifier
) {
    if (maze.isEmpty()) return

    BoxWithConstraints(modifier = modifier) {
        val gap = 4.dp
        val columns = maze.first().size
        val cellSize = if (columns > 0) (maxWidth - gap * (columns - 1)) / columns else 0.dp

        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            maze.forEachIndexed { y, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEachIndexed { x, cell ->
                        MazeCellView(
                            cell = cell,
                            size = cellSize,
                            isPlayer = playerPosition.x == x && playerPosition.y == y,
                            isTarget = targetPosition.x == x && targetPosition.y == y
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MazeCellView(
    cell: Cell,
    size: androidx.compose.ui.unit.Dp,
    isPlayer: Boolean,
    isTarget: Boolean
) {
    val isWall = cell.type == CellType.WALL

    val background = when {
        isWall -> Color(0xFF5D4037)
        isPlayer -> Color(0xFFBBDEFB)
        isTarget -> Color(0xFFFFF9C4)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .border(
                width = if (isWall) 0.dp else 1.dp,
                color = if (isWall) Color.Transparent else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        val label = when {
            isPlayer -> "🐼"
            isTarget -> "🏁"
            cell.pinyin != null -> cell.pinyin.character
            else -> ""
        }

        if (label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = if (label.length > 1) 12.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isWall) Color.White else Color(0xFF3E2723),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DirectionButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Text(text = label, fontSize = 22.sp, color = Color(0xFF0D47A1))
    }
}
