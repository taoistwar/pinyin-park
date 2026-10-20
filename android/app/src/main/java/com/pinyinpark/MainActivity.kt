package com.pinyinpark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
                    },
                    onBadgesClick = { currentScreen = AppScreen.Badges },
                    onProfileClick = { currentScreen = AppScreen.Profile }
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
                val backToHome = { currentScreen = AppScreen.Home }
                when (screen.gameType) {
                    GameType.MATCHING -> MatchGameScreen(
                        onBack = backToHome,
                        onComplete = { backToHome() }
                    )

                    GameType.WHACK_MOLE -> WhackMoleGameScreen(onBack = backToHome)

                    GameType.SPEAK_REPEAT -> SpeakRepeatGameScreen(onBack = backToHome)

                    GameType.BLOCK_PUZZLE -> BlockPuzzleGameScreen(onBack = backToHome)

                    GameType.CHAIN_GAME -> ChainGameScreen(onBack = backToHome)

                    GameType.MAZE_ADVENTURE -> MazeAdventureGameScreen(onBack = backToHome)
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
