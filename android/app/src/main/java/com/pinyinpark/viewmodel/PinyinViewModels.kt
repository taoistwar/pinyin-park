package com.pinyinpark.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinyinpark.data.UserProgressRepository
import com.pinyinpark.model.*
import com.pinyinpark.util.AudioPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

// ============================================================
// 主页 ViewModel
// ============================================================

data class HomeUiState(
    val userProgress: UserProgress = UserProgress(),
    val chapters: List<LessonChapter> = emptyList(),
    val todayBadges: List<Badge> = emptyList(),
    val showCelebration: Boolean = false,
    val celebrationMessage: String = "",
    // 新增：奖杯和宠物数据
    val totalStars: Int = 0,
    val unlockedBadgesCount: Int = 0,
    val totalBadgesCount: Int = PinyinData.allBadges.size,
    val unlockedTrophiesCount: Int = 0,
    val totalTrophiesCount: Int = 10,
    val petLevel: Int = 1,
    val petName: String = "小拼",
    val petMood: PetMood = PetMood.HAPPY,
    val petHunger: Int = 100,
    val expProgress: Float = 0f
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // 构建章节数据
            val chapters = buildChapters()
            val progress = loadUserProgress()

            // 统计已解锁的徽章
            val unlockedBadges = progress.unlockedBadges.size

            _uiState.update {
                it.copy(
                    userProgress = progress,
                    chapters = chapters,
                    totalStars = progress.totalStars,
                    unlockedBadgesCount = unlockedBadges,
                    petLevel = progress.petLevel,
                    petHunger = progress.petHunger
                )
            }
        }
    }

    private fun buildChapters(): List<LessonChapter> = listOf(
        LessonChapter(
            id = 1,
            title = "声母家族",
            description = "认识21个声母小伙伴",
            iconEmoji = "🅱️",
            pinyinItems = PinyinData.shengMuList,
            isUnlocked = true  // 默认解锁
        ),
        LessonChapter(
            id = 2,
            title = "韵母乐园",
            description = "学会${PinyinData.yunMuList.size}个韵母",
            iconEmoji = "🎵",
            pinyinItems = PinyinData.yunMuList,
            isUnlocked = true  // 默认解锁
        ),
        LessonChapter(
            id = 3,
            title = "四声宫殿",
            description = "掌握音调变化",
            iconEmoji = "🎶",
            pinyinItems = PinyinData.shengDiaoList,
            isUnlocked = true  // 默认解锁
        ),
        LessonChapter(
            id = 4,
            title = "拼读城堡",
            description = "声母+韵母组合练习",
            iconEmoji = "🏰",
            pinyinItems = PinyinData.combinedList.take(20),  // 取前20个作为城堡入门
            isUnlocked = true  // 默认解锁
        )
    )

    private fun loadUserProgress(): UserProgress {
        // TODO: 从本地数据库加载，这里先返回默认值
        return UserProgress(totalStars = 12, streakDays = 3)
    }

    fun onChapterSelected(chapter: LessonChapter) {
        // 导航到章节详情（由导航层处理）
    }



    fun dismissCelebration() {
        _uiState.update { it.copy(showCelebration = false) }
    }
}

// ============================================================
// 学习详情 ViewModel
// ============================================================

data class LearnUiState(
    val currentItem: PinyinItem? = null,
    val currentIndex: Int = 0,
    val totalItems: Int = 0,
    val isPlayingAudio: Boolean = false,
    val showMouthShape: Boolean = false,
    val starsEarned: Int = 0,
    val isCompleted: Boolean = false,
    val audioUnavailable: Boolean = false  // 设备缺少中文语音引擎时提示一次
)

class LearnViewModel(
    private val chapter: LessonChapter,
    private val audioPlayer: AudioPlayerManager? = null,
    private val repository: UserProgressRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnUiState())
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    private val items = chapter.pinyinItems

    init {
        if (items.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    currentItem = items[0],
                    currentIndex = 0,
                    totalItems = items.size
                )
            }
        }
        // 监听音频播放状态
        audioPlayer?.let { player ->
            viewModelScope.launch {
                player.isPlaying.collect { isPlaying ->
                    _uiState.update { it.copy(isPlayingAudio = isPlaying) }
                }
            }
        }
    }

    fun playAudio() {
        val item = _uiState.value.currentItem ?: return
        val player = audioPlayer
        if (player == null) {
            _uiState.update { it.copy(isPlayingAudio = false, audioUnavailable = true) }
            return
        }

        _uiState.update { it.copy(isPlayingAudio = true, audioUnavailable = false) }
        // TTS 用示例汉字朗读最稳定（汉字能保证按中文读；直接读 "mā" 可能被逐字母拼读）
        val started = player.playPinyin(
            pinyinChar = item.character,
            exampleWord = item.exampleWord
        )

        if (!started) {
            _uiState.update { it.copy(isPlayingAudio = false, audioUnavailable = true) }
            return
        }

        // 兜底：引擎没真正出声（例如未安装中文语音包）时恢复按钮状态
        viewModelScope.launch {
            kotlinx.coroutines.delay(2500)
            if (!player.isCurrentlyPlaying()) {
                _uiState.update {
                    it.copy(
                        isPlayingAudio = false,
                        audioUnavailable = !player.isTtsAvailable()
                    )
                }
            }
        }
    }

    /** 提示只弹一次 */
    fun consumeAudioUnavailable() {
        _uiState.update { it.copy(audioUnavailable = false) }
    }

    fun playExampleWord() {
        val item = _uiState.value.currentItem ?: return
        audioPlayer?.playExampleWord(item.exampleWord)
    }

    fun toggleMouthShape() {
        _uiState.update { it.copy(showMouthShape = !it.showMouthShape) }
    }

    fun nextItem() {
        val nextIndex = _uiState.value.currentIndex + 1
        if (nextIndex < items.size) {
            _uiState.update {
                it.copy(
                    currentItem = items[nextIndex],
                    currentIndex = nextIndex,
                    isPlayingAudio = false,
                    showMouthShape = false
                )
            }
        } else {
            // 章节完成：写入本地存档（星星、已学知识点、连续天数）
            val stars = 3
            _uiState.update { it.copy(isCompleted = true, starsEarned = stars) }
            repository?.let { repo ->
                viewModelScope.launch {
                    repo.completeChapter(chapter, stars)
                }
            }
        }
    }

    fun prevItem() {
        val prevIndex = _uiState.value.currentIndex - 1
        if (prevIndex >= 0) {
            _uiState.update {
                it.copy(
                    currentItem = items[prevIndex],
                    currentIndex = prevIndex
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer?.release()
    }
}

// ============================================================
// 游戏 ViewModel（以配对游戏为例）
// ============================================================

data class MatchGameUiState(
    val leftItems: List<PinyinItem> = emptyList(),  // 声母
    val rightItems: List<PinyinItem> = emptyList(), // 韵母/汉字
    val selectedLeft: String? = null,
    val selectedRight: String? = null,
    val matchedPairs: Set<String> = emptySet(),
    val wrongPair: Pair<String, String>? = null,
    val score: Int = 0,
    val timeLeft: Int = 60,
    val isGameOver: Boolean = false,
    val result: GameResult? = null
)

class MatchGameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MatchGameUiState())
    val uiState: StateFlow<MatchGameUiState> = _uiState.asStateFlow()

    init {
        startGame()
    }

    private fun startGame() {
        // 随机取5对声母供配对
        val shengMu = PinyinData.shengMuList.shuffled().take(5)
        val shuffledRight = shengMu.shuffled() // 右边打乱顺序

        _uiState.update {
            it.copy(
                leftItems = shengMu,
                rightItems = shuffledRight,
                score = 0,
                timeLeft = 60,
                isGameOver = false,
                matchedPairs = emptySet()
            )
        }
        startCountdown()
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (_uiState.value.timeLeft > 0 && !_uiState.value.isGameOver) {
                delay(1000L)
                _uiState.update { it.copy(timeLeft = it.timeLeft - 1) }
            }
            if (_uiState.value.timeLeft <= 0) {
                endGame()
            }
        }
    }

    fun selectLeft(itemId: String) {
        _uiState.update { it.copy(selectedLeft = itemId, wrongPair = null) }
        checkMatch()
    }

    fun selectRight(itemId: String) {
        _uiState.update { it.copy(selectedRight = itemId, wrongPair = null) }
        checkMatch()
    }

    private fun checkMatch() {
        val state = _uiState.value
        val leftId = state.selectedLeft ?: return
        val rightId = state.selectedRight ?: return

        if (leftId == rightId) {
            // 匹配成功！
            val newMatched = state.matchedPairs + leftId
            val newScore = state.score + 10
            _uiState.update {
                it.copy(
                    matchedPairs = newMatched,
                    score = newScore,
                    selectedLeft = null,
                    selectedRight = null
                )
            }
            if (newMatched.size == state.leftItems.size) {
                endGame()
            }
        } else {
            // 匹配失败
            _uiState.update {
                it.copy(
                    wrongPair = Pair(leftId, rightId),
                    selectedLeft = null,
                    selectedRight = null
                )
            }
        }
    }

    private fun endGame() {
        val state = _uiState.value
        val maxScore = state.leftItems.size * 10
        val starsEarned = when {
            state.score >= maxScore -> 3
            state.score >= maxScore * 0.6 -> 2
            state.score > 0 -> 1
            else -> 0
        }
        val result = GameResult(
            gameType = GameType.MATCHING,
            score = state.score,
            maxScore = maxScore,
            starsEarned = starsEarned,
            timeTaken = (60 - state.timeLeft) * 1000L
        )
        _uiState.update { it.copy(isGameOver = true, result = result) }
    }

    fun restartGame() = startGame()
}
