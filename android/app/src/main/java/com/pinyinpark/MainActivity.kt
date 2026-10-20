package com.pinyinpark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.pinyinpark.model.*
import com.pinyinpark.ui.screens.*
import com.pinyinpark.ui.theme.PinyinParkTheme

// ============================================================
// 拼音乐园 - 主入口
// ============================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PinyinParkTheme {
                PinyinParkApp()
            }
        }
    }
}

// ============================================================
// 应用导航状态
// ============================================================

sealed class AppScreen {
    object Home : AppScreen()
    data class Learn(val chapter: LessonChapter) : AppScreen()
    data class Game(val gameType: GameType) : AppScreen()
    object Badges : AppScreen()
    object Profile : AppScreen()
}

@Composable
fun PinyinParkApp() {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
            is AppScreen.Home -> {
                HomeScreen(
                    onChapterClick = { chapter ->
                        currentScreen = AppScreen.Learn(chapter)
                    },
                    onGameClick = { gameType ->
                        currentScreen = AppScreen.Game(gameType)
                    }
                )
            }

            is AppScreen.Learn -> {
                LearnScreen(
                    chapter = screen.chapter,
                    onBack = { currentScreen = AppScreen.Home },
                    onComplete = { currentScreen = AppScreen.Home }
                )
            }

            is AppScreen.Game -> {
                when (screen.gameType) {
                    GameType.MATCHING -> {
                        MatchGameScreen(
                            onBack = { currentScreen = AppScreen.Home },
                            onComplete = { currentScreen = AppScreen.Home }
                        )
                    }

                    GameType.WHACK_MOLE -> {
                        WhackMoleGameScreen(
                            onBack = { currentScreen = AppScreen.Home }
                        )
                    }

                    // 尚未实现的游戏：给出占位页，避免空白
                    else -> {
                        ComingSoonScreen(
                            gameType = screen.gameType,
                            onBack = { currentScreen = AppScreen.Home }
                        )
                    }
                }
            }

            is AppScreen.Badges -> {
                BadgesScreen(onBack = { currentScreen = AppScreen.Home })
            }

            is AppScreen.Profile -> {
                ProfileScreen(onBack = { currentScreen = AppScreen.Home })
            }
        }
    }
}

// ============================================================
// 徽章墙页面
// ============================================================

@Composable
fun BadgesScreen(onBack: () -> Unit) {
    // TODO: 实现徽章展示
}

// ============================================================
// 个人中心页面
// ============================================================

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    // TODO: 实现个人中心
}