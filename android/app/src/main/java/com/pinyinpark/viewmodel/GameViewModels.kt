package com.pinyinpark.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinyinpark.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.random.Random

// ============================================================
// 拼音地鼠游戏 ViewModel
// ============================================================

/**
 * 拼音地鼠游戏状态
 * 拼音地鼠从洞中冒出，玩家需要点击正确的拼音
 */
data class WhackMoleUiState(
    val holes: List<MoleHole> = emptyList(),  // 9个洞
    val currentTarget: PinyinItem? = null,     // 当前要打的拼音
    val score: Int = 0,                        // 得分
    val timeLeft: Int = 60,                    // 剩余时间
    val correctHits: Int = 0,                  // 正确击中次数
    val wrongHits: Int = 0,                    // 错误击中次数
    val isGameOver: Boolean = false,
    val showHitEffect: Boolean = false,        // 显示击中特效
    val hitEffectType: HitEffectType = HitEffectType.NONE,
    val result: GameResult? = null
)

enum class HitEffectType { NONE, CORRECT, WRONG }

data class MoleHole(
    val id: Int,
    val currentPinyin: PinyinItem? = null,  // 当前洞里的拼音地鼠
    val isHitting: Boolean = false         // 是否正在被点击
)

class WhackMoleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WhackMoleUiState())
    val uiState: StateFlow<WhackMoleUiState> = _uiState.asStateFlow()

    // 所有可用的拼音数据
    private val allPinyin = PinyinData.shengMuList + PinyinData.yunMuList.take(10)
    private val moleHoleCount = 9
    private val moleShowDuration = 2000L  // 地鼠显示时间(ms)

    init {
        startGame()
    }

    private fun startGame() {
        // 初始化9个洞
        val holes = (0 until moleHoleCount).map { MoleHole(id = it) }
        _uiState.update {
            it.copy(
                holes = holes,
                score = 0,
                timeLeft = 60,
                correctHits = 0,
                wrongHits = 0,
                isGameOver = false,
                result = null
            )
        }

        // 开始计时
        startCountdown()
        // 开始冒出地鼠
        startMoleSpawning()
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

    private fun startMoleSpawning() {
        viewModelScope.launch {
            while (!_uiState.value.isGameOver) {
                // 随机等待1-2秒
                delay(Random.nextLong(1000, 2000))
                if (_uiState.value.isGameOver) break

                // 随机选择1-3个洞冒出地鼠
                val holesToShow = Random.nextInt(1, minOf(4, moleHoleCount + 1))
                val availableHoles = _uiState.value.holes
                    .filter { it.currentPinyin == null }
                    .shuffled()
                    .take(holesToShow)

                val newHoles = _uiState.value.holes.map { hole ->
                    if (hole.id in availableHoles.map { it.id }) {
                        // 随机选一个拼音
                        val randomPinyin = allPinyin.random()
                        hole.copy(currentPinyin = randomPinyin)
                    } else {
                        hole
                    }
                }

                // 目标拼音从本次冒出的地鼠里挑，保证场上一定有一个可击中的目标
                val targetPinyin = newHoles.mapNotNull { it.currentPinyin }.randomOrNull()
                _uiState.update {
                    it.copy(holes = newHoles, currentTarget = targetPinyin)
                }

                // 地鼠显示一段时间后消失
                delay(moleShowDuration)
                if (!_uiState.value.isGameOver) {
                    val clearedHoles = _uiState.value.holes.map { hole ->
                        if (hole.currentPinyin != null) {
                            hole.copy(currentPinyin = null)
                        } else {
                            hole
                        }
                    }
                    _uiState.update { it.copy(holes = clearedHoles) }
                }
            }
        }
    }

    fun onHoleClicked(holeId: Int) {
        val state = _uiState.value
        if (state.isGameOver) return

        val hole = state.holes.find { it.id == holeId } ?: return
        val molePinyin = hole.currentPinyin ?: return  // 洞里没地鼠

        // 检查是否击中正确的拼音
        val isCorrect = molePinyin.id == state.currentTarget?.id

        if (isCorrect) {
            // 正确击中！
            val newScore = state.score + 10
            val newCorrectHits = state.correctHits + 1

            // 显示击中特效
            _uiState.update {
                it.copy(
                    score = newScore,
                    correctHits = newCorrectHits,
                    showHitEffect = true,
                    hitEffectType = HitEffectType.CORRECT,
                    holes = it.holes.map { h ->
                        if (h.id == holeId) h.copy(currentPinyin = null) else h
                    }
                )
            }

            // 特效消失
            viewModelScope.launch {
                delay(500)
                _uiState.update { it.copy(showHitEffect = false, hitEffectType = HitEffectType.NONE) }
            }

        } else {
            // 错误击中
            val newWrongHits = state.wrongHits + 1
            _uiState.update {
                it.copy(
                    wrongHits = newWrongHits,
                    showHitEffect = true,
                    hitEffectType = HitEffectType.WRONG,
                    holes = it.holes.map { h ->
                        if (h.id == holeId) h.copy(isHitting = true) else h
                    }
                )
            }

            // 特效消失
            viewModelScope.launch {
                delay(500)
                _uiState.update {
                    it.copy(
                        showHitEffect = false,
                        hitEffectType = HitEffectType.NONE,
                        holes = it.holes.map { h -> h.copy(isHitting = false) }
                    )
                }
            }
        }
    }

    private fun endGame() {
        val state = _uiState.value
        // 60 秒内大约可完成 20 次正确击打，用作满分基准（此前用正确数×10，会导致得分永远等于满分）
        val maxPossibleScore = 20 * 10
        val starsEarned = when {
            state.correctHits >= 15 -> 3
            state.correctHits >= 10 -> 2
            state.correctHits >= 5 -> 1
            else -> 0
        }

        val result = GameResult(
            gameType = GameType.WHACK_MOLE,
            score = state.score,
            maxScore = maxPossibleScore,
            starsEarned = starsEarned,
            timeTaken = (60 - state.timeLeft) * 1000L
        )

        _uiState.update { it.copy(isGameOver = true, result = result) }
    }

    fun restartGame() = startGame()
}

// ============================================================
// 跟我读游戏 ViewModel
// ============================================================

/**
 * 跟我读游戏状态
 * 播放拼音，用户跟读，系统判断发音是否正确
 */
data class SpeakRepeatUiState(
    val currentItem: PinyinItem? = null,
    val currentIndex: Int = 0,
    val totalItems: Int = 10,
    val isPlayingAudio: Boolean = false,
    val isListening: Boolean = false,
    val speechResult: SpeechResult? = null,
    val score: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val isGameOver: Boolean = false,
    val result: GameResult? = null
)

data class SpeechResult(
    val recognizedText: String,
    val isCorrect: Boolean,
    val confidence: Float  // 置信度 0-1
)

class SpeakRepeatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SpeakRepeatUiState())
    val uiState: StateFlow<SpeakRepeatUiState> = _uiState.asStateFlow()

    // 游戏使用的拼音数据
    private val gameItems = (PinyinData.shengMuList + PinyinData.yunMuList)
        .shuffled()
        .take(10)

    init {
        startGame()
    }

    private fun startGame() {
        if (gameItems.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    currentItem = gameItems[0],
                    currentIndex = 0,
                    totalItems = gameItems.size,
                    score = 0,
                    correctCount = 0,
                    wrongCount = 0,
                    isGameOver = false,
                    speechResult = null,
                    result = null
                )
            }
        }
    }

    fun playCurrentAudio() {
        _uiState.update { it.copy(isPlayingAudio = true) }
        // TODO: 调用 AudioPlayerManager 播放音频
        viewModelScope.launch {
            delay(1500)
            _uiState.update { it.copy(isPlayingAudio = false) }
        }
    }

    fun startListening() {
        _uiState.update { it.copy(isListening = true, speechResult = null) }
        // TODO: 调用语音识别 API
        // 这里模拟识别结果
        viewModelScope.launch {
            delay(2000)
            val item = _uiState.value.currentItem
            val isCorrect = Random.nextBoolean()  // 模拟识别结果
            val result = SpeechResult(
                recognizedText = if (isCorrect) item?.character ?: "" else "识别失败",
                isCorrect = isCorrect,
                confidence = if (isCorrect) 0.9f else 0.3f
            )

            val state = _uiState.value
            val newScore = if (isCorrect) state.score + 10 else state.score
            val newCorrectCount = if (isCorrect) state.correctCount + 1 else state.correctCount
            val newWrongCount = if (!isCorrect) state.wrongCount + 1 else state.wrongCount

            _uiState.update {
                it.copy(
                    isListening = false,
                    speechResult = result,
                    score = newScore,
                    correctCount = newCorrectCount,
                    wrongCount = newWrongCount
                )
            }
        }
    }

    fun nextItem() {
        val nextIndex = _uiState.value.currentIndex + 1
        if (nextIndex < gameItems.size) {
            _uiState.update {
                it.copy(
                    currentItem = gameItems[nextIndex],
                    currentIndex = nextIndex,
                    speechResult = null
                )
            }
        } else {
            endGame()
        }
    }

    private fun endGame() {
        val state = _uiState.value
        val maxScore = state.totalItems * 10
        val starsEarned = when {
            state.correctCount >= 9 -> 3
            state.correctCount >= 7 -> 2
            state.correctCount >= 5 -> 1
            else -> 0
        }

        val result = GameResult(
            gameType = GameType.SPEAK_REPEAT,
            score = state.score,
            maxScore = maxScore,
            starsEarned = starsEarned,
            timeTaken = 0
        )

        _uiState.update { it.copy(isGameOver = true, result = result) }
    }

    fun restartGame() = startGame()
}

// ============================================================
// 拼音积木游戏 ViewModel
// ============================================================

/**
 * 拼音积木游戏状态
 * 将拼音的声母、韵母、声调拼成完整的音节
 */
data class BlockPuzzleUiState(
    val targetPinyin: String = "",           // 目标拼音
    val targetExample: String = "",            // 目标例词
    val targetChar: String = "",               // 目标汉字
    val placedBlocks: List<PlacedBlock> = emptyList(),  // 已放置的积木
    val availableBlocks: List<PuzzleBlock> = emptyList(), // 可用积木
    val isComplete: Boolean = false,           // 是否完成
    val showHint: Boolean = false,            // 显示提示
    val currentHint: String = "",             // 当前提示
    val score: Int = 0,
    val completedCount: Int = 0,
    val totalRounds: Int = 10,
    val isGameOver: Boolean = false,
    val result: GameResult? = null
)

data class PlacedBlock(
    val position: Int,      // 位置 0, 1, 2 (声母、韵母、声调)
    val block: PuzzleBlock?
)

data class PuzzleBlock(
    val id: String,
    val text: String,
    val type: BlockType
)

enum class BlockType { INITIAL, FINAL, TONE }

class BlockPuzzleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BlockPuzzleUiState())
    val uiState: StateFlow<BlockPuzzleUiState> = _uiState.asStateFlow()

    // 拼读数据
    private val puzzleData = PinyinData.combinedList.shuffled().take(10)

    init {
        startGame()
    }

    private fun startGame() {
        if (puzzleData.isNotEmpty()) {
            loadNextPuzzle(0)
        }
    }

    private fun loadNextPuzzle(index: Int) {
        if (index >= puzzleData.size) {
            endGame()
            return
        }

        val item = puzzleData[index]
        // 分解拼音为声母、韵母、声调
        val (initial, final, tone) = parsePinyin(item.character)

        // 生成干扰项
        val allInitials = PinyinData.shengMuList.map { it.character }
        val allFinals = PinyinData.yunMuList.map { it.character }

        val blocks = mutableListOf<PuzzleBlock>()

        // 添加正确答案
        if (initial.isNotEmpty()) blocks.add(PuzzleBlock("i_$initial", initial, BlockType.INITIAL))
        if (final.isNotEmpty()) blocks.add(PuzzleBlock("f_$final", final, BlockType.FINAL))
        blocks.add(PuzzleBlock("t_$tone", tone, BlockType.TONE))

        // 添加干扰项
        blocks.addAll(allInitials.filter { it != initial }.shuffled().take(2).map {
            PuzzleBlock("i_$it", it, BlockType.INITIAL)
        })
        blocks.addAll(allFinals.filter { it != final }.shuffled().take(2).map {
            PuzzleBlock("f_$it", it, BlockType.FINAL)
        })

        _uiState.update {
            it.copy(
                targetPinyin = item.character,
                targetExample = item.exampleWord,
                targetChar = item.character,
                placedBlocks = listOf(
                    PlacedBlock(0, null),
                    PlacedBlock(1, null),
                    PlacedBlock(2, null)
                ),
                availableBlocks = blocks.shuffled(),
                isComplete = false,
                showHint = false,
                currentHint = "",
                completedCount = index
            )
        }
    }

    private fun parsePinyin(pinyin: String): Triple<String, String, String> {
        // 简单的拼音解析，实际需要更复杂的逻辑
        val tone = when {
            pinyin.endsWith("1") || pinyin.contains("ā") -> "1"
            pinyin.endsWith("2") || pinyin.contains("á") -> "2"
            pinyin.endsWith("3") || pinyin.contains("ǎ") -> "3"
            pinyin.endsWith("4") || pinyin.contains("à") -> "4"
            else -> "1"
        }

        // 提取声母和韵母
        val shengMus = listOf("b", "p", "m", "f", "d", "t", "n", "l", "g", "k", "h",
            "j", "q", "x", "zh", "ch", "sh", "r", "z", "c", "s", "y", "w")

        var initial = ""
        var final = pinyin

        for (sm in shengMus.sortedByDescending { it.length }) {
            if (pinyin.startsWith(sm)) {
                initial = sm
                final = pinyin.removePrefix(sm)
                break
            }
        }

        // 去掉声调数字
        final = final.replace(Regex("[1-4]"), "")

        return Triple(initial, final, tone)
    }

    fun placeBlock(position: Int, block: PuzzleBlock) {
        val state = _uiState.value

        // 检查这个位置是否已经有这个类型的积木
        val type = when (position) {
            0 -> BlockType.INITIAL
            1 -> BlockType.FINAL
            2 -> BlockType.TONE
            else -> return
        }

        if (block.type != type) return  // 类型不匹配

        // 移除这个积木从可用列表
        val newAvailable = state.availableBlocks.filter { it.id != block.id }

        // 放到指定位置
        val newPlaced = state.placedBlocks.map {
            if (it.position == position) {
                it.copy(block = block)
            } else {
                it
            }
        }

        _uiState.update {
            it.copy(
                placedBlocks = newPlaced,
                availableBlocks = newAvailable
            )
        }

        checkCompletion()
    }

    fun removeBlock(position: Int) {
        val state = _uiState.value
        val placed = state.placedBlocks.find { it.position == position } ?: return
        val block = placed.block ?: return

        // 放回可用列表
        val newAvailable = state.availableBlocks + block
        val newPlaced = state.placedBlocks.map {
            if (it.position == position) {
                it.copy(block = null)
            } else {
                it
            }
        }

        _uiState.update {
            it.copy(
                placedBlocks = newPlaced,
                availableBlocks = newAvailable
            )
        }
    }

    fun showHint() {
        val state = _uiState.value
        val target = state.targetPinyin
        val hint = when {
            state.placedBlocks[0].block == null -> "提示：第一个是声母"
            state.placedBlocks[1].block == null -> "提示：第二个是韵母"
            state.placedBlocks[2].block == null -> "提示：第三个是声调"
            else -> ""
        }
        _uiState.update { it.copy(showHint = true, currentHint = hint) }
    }

    private fun checkCompletion() {
        val state = _uiState.value
        val placed0 = state.placedBlocks.find { it.position == 0 }?.block?.text ?: ""
        val placed1 = state.placedBlocks.find { it.position == 1 }?.block?.text ?: ""
        val placed2 = state.placedBlocks.find { it.position == 2 }?.block?.text ?: ""

        val currentAnswer = placed0 + placed1 + placed2
        val targetAnswer = state.targetPinyin.replace(Regex("[āáǎàēéěèīíǐìōóǒòūúǔùüǖǘǚǜ]")) {
            when (it.value) {
                "ā", "á", "ǎ", "à" -> "a"
                "ē", "é", "ě", "è" -> "e"
                "ī", "í", "ǐ", "ì" -> "i"
                "ō", "ó", "ǒ", "ò" -> "o"
                "ū", "ú", "ǔ", "ù" -> "u"
                "ü", "ǖ", "ǘ", "ǚ", "ǜ" -> "ü"
                else -> ""
            }
        }

        if (placed0.isNotEmpty() && placed1.isNotEmpty() && placed2.isNotEmpty()) {
            val isCorrect = currentAnswer == targetAnswer ||
                (currentAnswer == state.targetPinyin)

            if (isCorrect) {
                val newScore = state.score + 20
                _uiState.update {
                    it.copy(
                        isComplete = true,
                        score = newScore
                    )
                }
            } else {
                // 清空重新尝试
                val returnedBlocks = state.placedBlocks.mapNotNull { it.block }
                _uiState.update {
                    it.copy(
                        placedBlocks = it.placedBlocks.map { p -> p.copy(block = null) },
                        availableBlocks = it.availableBlocks + returnedBlocks
                    )
                }
            }
        }
    }

    fun nextPuzzle() {
        val nextIndex = _uiState.value.completedCount + 1
        loadNextPuzzle(nextIndex)
    }

    private fun endGame() {
        val state = _uiState.value
        val starsEarned = when {
            state.score >= 180 -> 3
            state.score >= 120 -> 2
            state.score >= 60 -> 1
            else -> 0
        }

        val result = GameResult(
            gameType = GameType.BLOCK_PUZZLE,
            score = state.score,
            maxScore = state.totalRounds * 20,
            starsEarned = starsEarned,
            timeTaken = 0
        )

        _uiState.update { it.copy(isGameOver = true, result = result) }
    }

    fun restartGame() = startGame()
}

// ============================================================
// 音节接龙游戏 ViewModel
// ============================================================

/**
 * 音节接龙游戏状态
 * 下一个拼音的首字母要等于上一个拼音的韵母
 */
data class ChainGameUiState(
    val currentPinyin: String = "",
    val currentExample: String = "",
    val currentChar: String = "",
    val historyPinyins: List<String> = emptyList(),
    val score: Int = 0,
    val chainLength: Int = 0,
    val maxChainLength: Int = 0,
    val isGameOver: Boolean = false,
    val gameOverReason: String = "",
    val showWrong: Boolean = false,
    val result: GameResult? = null
)

class ChainGameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChainGameUiState())
    val uiState: StateFlow<ChainGameUiState> = _uiState.asStateFlow()

    // 声母到韵母的映射（用于判断是否可以接龙）
    private val initialToFinals = mapOf(
        "b" to listOf("a", "o", "ei", "i", "u", "ian", "iang", "in", "ing", "iao", "ou", "ang"),
        "p" to listOf("a", "o", "i", "u", "ian", "iang", "in", "ing", "iao", "ou", "ang"),
        "m" to listOf("a", "o", "i", "u", "ian", "iang", "in", "ing", "iao", "ei", "ou", "ang", "ian"),
        "f" to listOf("a", "o", "i", "u", "en", "eng", "ei"),
        "d" to listOf("a", "e", "i", "u", "ai", "ei", "ao", "ou", "an", "en", "ang", "eng", "ia", "ie", "iao", "ian", "iang", "iu", "ong"),
        "t" to listOf("a", "e", "i", "u", "ai", "ei", "ao", "ou", "an", "en", "ang", "eng", "ia", "ie", "iao", "ian", "iang", "ong"),
        "n" to listOf("a", "e", "i", "u", "ai", "ei", "ao", "an", "en", "ang", "eng", "iang", "iao", "ian", "in", "iong", "ou"),
        "l" to listOf("a", "e", "i", "u", "ai", "ei", "ao", "ou", "an", "en", "ang", "eng", "ia", "ie", "iao", "ian", "iang", "in", "iu", "ong"),
        "g" to listOf("a", "e", "i", "u", "ai", "ei", "ao", "ou", "an", "en", "ang", "eng", "ua", "uo", "uai", "ui", "uan", "un", "uang"),
        "k" to listOf("a", "e", "i", "u", "ai", "ei", "ao", "ou", "an", "en", "ang", "eng", "ua", "uo", "uai", "ui", "uan", "un", "uang"),
        "h" to listOf("a", "e", "i", "u", "ai", "ei", "ao", "an", "ang", "eng", "ua", "uo", "uai", "ui", "uan", "un", "uang"),
        "j" to listOf("i", "ia", "ie", "iao", "iu", "ian", "iang", "in", "iong"),
        "q" to listOf("i", "ia", "ie", "iao", "iu", "ian", "iang", "in", "iong"),
        "x" to listOf("i", "ia", "ie", "iao", "iu", "ian", "iang", "in", "iong"),
        "zh" to listOf("i", "a", "e", "ai", "ei", "ao", "ou", "an", "en", "ang", "eng", "ong", "ia", "ua", "uo", "uai", "ui", "uan", "un", "uang"),
        "ch" to listOf("i", "a", "e", "ai", "ei", "ao", "ou", "an", "en", "ang", "eng", "ong", "ua", "uo", "uai", "ui", "uan", "un", "uang"),
        "sh" to listOf("i", "a", "e", "ai", "ei", "ao", "ou", "an", "en", "ang", "eng", "ong", "ua", "uo", "uai", "ui", "uan", "un", "uang"),
        "r" to listOf("i", "a", "e", "ao", "ou", "an", "en", "ang", "eng", "ong", "ua", "uo", "ui", "uan", "un"),
        "z" to listOf("i", "a", "e", "ai", "ei", "ao", "ou", "an", "en", "ang", "eng", "ong", "ua", "uo", "ui", "uan", "un", "uang"),
        "c" to listOf("i", "a", "e", "ai", "ei", "ao", "ou", "an", "en", "ang", "eng", "ong", "ua", "uo", "ui", "uan", "un", "uang"),
        "s" to listOf("i", "a", "e", "ai", "ei", "ao", "ou", "an", "en", "ang", "eng", "ong", "ua", "uo", "ui", "uan", "un", "uang"),
        "y" to listOf("a", "e", "i", "ao", "ou", "an", "en", "ang", "eng", "ong", "in"),
        "w" to listOf("a", "e", "i", "o", "ai", "ei", "an", "en", "ang", "eng")
    )

    // 韵母到声母的映射（用于找下一个拼音）
    private val finalToInitials = mutableMapOf<String, MutableList<String>>()
    init {
        initialToFinals.forEach { (initial, finals) ->
            finals.forEach { final ->
                finalToInitials.getOrPut(final) { mutableListOf() }.add(initial)
            }
        }
    }

    private val usedPinyins = mutableSetOf<String>()
    private var lastFinal = ""  // 上一个拼音的韵母

    init {
        startGame()
    }

    private fun startGame() {
        usedPinyins.clear()

        // 随机选一个开始
        val startItem = PinyinData.combinedList.random()
        usedPinyins.add(startItem.character)

        val (_, final) = parsePinyin(startItem.character)
        lastFinal = final

        _uiState.update {
            it.copy(
                currentPinyin = startItem.character,
                currentExample = startItem.exampleWord,
                currentChar = startItem.character,
                historyPinyins = listOf(startItem.character),
                score = 0,
                chainLength = 1,
                maxChainLength = 1,
                isGameOver = false,
                gameOverReason = "",
                result = null
            )
        }
    }

    private fun parsePinyin(pinyin: String): Pair<String, String> {
        val shengMus = listOf("zh", "ch", "sh", "b", "p", "m", "f", "d", "t", "n", "l",
            "g", "k", "h", "j", "q", "x", "r", "z", "c", "s", "y", "w")

        var initial = ""
        var final = pinyin

        for (sm in shengMus.sortedByDescending { it.length }) {
            if (pinyin.startsWith(sm)) {
                initial = sm
                final = pinyin.removePrefix(sm)
                break
            }
        }

        // 标准化韵母
        final = normalizeFinal(final)

        return Pair(initial, final)
    }

    private fun normalizeFinal(final: String): String {
        return when {
            final.startsWith("iang") -> "iang"
            final.startsWith("iong") -> "iong"
            final.startsWith("uang") -> "uang"
            final.startsWith("iang") -> "iang"
            final.startsWith("eng") -> "eng"
            final.startsWith("ang") -> "ang"
            final.startsWith("ian") -> "ian"
            final.startsWith("iao") -> "iao"
            final.startsWith("ian") -> "ian"
            final.startsWith("uai") -> "uai"
            final.startsWith("uan") -> "uan"
            final.startsWith("ong") -> "ong"
            final.startsWith("ing") -> "ing"
            final.startsWith("ia") -> "ia"
            final.startsWith("ie") -> "ie"
            final.startsWith("iu") -> "iu"
            final.startsWith("ai") -> "ai"
            final.startsWith("ei") -> "ei"
            final.startsWith("ao") -> "ao"
            final.startsWith("ou") -> "ou"
            final.startsWith("an") -> "an"
            final.startsWith("en") -> "en"
            final.startsWith("ia") -> "ia"
            final == "ü" || final == "u:" -> "v"
            final.startsWith("u") -> "u"
            final.startsWith("i") -> "i"
            final.startsWith("v") -> "v"
            final == "a" -> "a"
            final == "o" -> "o"
            final == "e" -> "e"
            else -> final
        }
    }

    fun selectPinyin(item: PinyinItem): Boolean {
        val state = _uiState.value
        if (state.isGameOver) return false

        // 检查是否已使用
        if (usedPinyins.contains(item.character)) {
            _uiState.update { it.copy(showWrong = true) }
            viewModelScope.launch {
                delay(500)
                _uiState.update { it.copy(showWrong = false) }
            }
            return false
        }

        val (initial, final) = parsePinyin(item.character)

        // 检查韵母是否匹配
        if (final != lastFinal) {
            _uiState.update { it.copy(showWrong = true) }
            viewModelScope.launch {
                delay(500)
                _uiState.update { it.copy(showWrong = false) }
            }
            // 错误，游戏结束
            endGame("韵母不匹配！${lastFinal} 应该接的韵母开头")
            return false
        }

        // 正确
        usedPinyins.add(item.character)
        lastFinal = final
        val newChain = state.chainLength + 1
        val newScore = state.score + newChain * 5  // 连续越长分数越高

        _uiState.update {
            it.copy(
                currentPinyin = item.character,
                currentExample = item.exampleWord,
                currentChar = item.character,
                historyPinyins = it.historyPinyins + item.character,
                score = newScore,
                chainLength = newChain,
                maxChainLength = maxOf(it.maxChainLength, newChain),
                showWrong = false
            )
        }

        // 检查是否无法继续
        checkIfStuck()
        return true
    }

    private fun checkIfStuck() {
        // 查找可以接龙的下一个拼音
        val availableItems = PinyinData.combinedList.filter { item ->
            !usedPinyins.contains(item.character) &&
                item.character.startsWith(lastFinal) ||
                parsePinyin(item.character).second == lastFinal
        }

        if (availableItems.isEmpty()) {
            endGame("太棒了！接龙了 ${_uiState.value.chainLength} 个拼音！")
        }
    }

    private fun endGame(reason: String) {
        val state = _uiState.value
        val starsEarned = when {
            state.chainLength >= 20 -> 3
            state.chainLength >= 15 -> 2
            state.chainLength >= 10 -> 1
            else -> 0
        }

        val result = GameResult(
            gameType = GameType.CHAIN_GAME,
            score = state.score,
            maxScore = state.chainLength * 5,
            starsEarned = starsEarned,
            timeTaken = 0
        )

        _uiState.update {
            it.copy(
                isGameOver = true,
                gameOverReason = reason,
                result = result
            )
        }
    }

    fun restartGame() = startGame()
}

// ============================================================
// 迷宫探险游戏 ViewModel
// ============================================================

/**
 * 迷宫探险游戏状态
 * 在迷宫中找到正确的拼音路径
 */
data class MazeAdventureUiState(
    val maze: List<List<Cell>> = emptyList(),
    val playerPosition: Position = Position(0, 0),
    val targetPosition: Position = Position(0, 0),
    val currentTargetPinyin: String = "",
    val currentTargetExample: String = "",
    val collectedPinyins: List<String> = emptyList(),
    val score: Int = 0,
    val level: Int = 1,
    val moves: Int = 0,
    val isLevelComplete: Boolean = false,
    val isGameOver: Boolean = false,
    val showCelebration: Boolean = false,
    val result: GameResult? = null
)

data class Position(val x: Int, val y: Int)

enum class CellType { WALL, PATH, START, TARGET, COLLECTIBLE }

data class Cell(
    val type: CellType,
    val pinyin: PinyinItem? = null  // 路径上的拼音
)

class MazeAdventureViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MazeAdventureUiState())
    val uiState: StateFlow<MazeAdventureUiState> = _uiState.asStateFlow()

    private val mazeSize = 7
    private val allPinyinItems = PinyinData.shengMuList.shuffled().take(20)

    init {
        startGame()
    }

    private fun startGame() {
        generateMaze()
    }

    private fun generateMaze() {
        // 简单迷宫生成算法
        val maze = MutableList(mazeSize) { y ->
            MutableList(mazeSize) { x ->
                // 边缘是墙
                if (x == 0 || y == 0 || x == mazeSize - 1 || y == mazeSize - 1) {
                    Cell(CellType.WALL)
                } else {
                    // 随机生成墙
                    if (Random.nextFloat() < 0.3f) {
                        Cell(CellType.WALL)
                    } else {
                        Cell(CellType.PATH)
                    }
                }
            }
        }

        // 设置起点和终点
        maze[1][1] = Cell(CellType.START)
        maze[mazeSize - 2][mazeSize - 2] = Cell(CellType.TARGET)

        // 在路径上放置拼音
        val pinyinItems = allPinyinItems.shuffled().toMutableList()
        maze.forEachIndexed { y, row ->
            row.forEachIndexed { x, cell ->
                if (cell.type == CellType.PATH && pinyinItems.isNotEmpty() &&
                    !(x == 1 && y == 1) && !(x == mazeSize - 2 && y == mazeSize - 2)) {
                    maze[y][x] = Cell(CellType.PATH, pinyinItems.removeAt(0))
                }
            }
        }

        val targetItem = pinyinItems.firstOrNull() ?: PinyinData.shengMuList.first()

        _uiState.update {
            it.copy(
                maze = maze,
                playerPosition = Position(1, 1),
                targetPosition = Position(mazeSize - 2, mazeSize - 2),
                currentTargetPinyin = targetItem.character,
                currentTargetExample = targetItem.exampleWord,
                collectedPinyins = emptyList(),
                isLevelComplete = false,
                showCelebration = false
            )
        }
    }

    fun move(direction: Direction) {
        val state = _uiState.value
        if (state.isGameOver || state.isLevelComplete) return

        val newPos = when (direction) {
            Direction.UP -> Position(state.playerPosition.x, state.playerPosition.y - 1)
            Direction.DOWN -> Position(state.playerPosition.x, state.playerPosition.y + 1)
            Direction.LEFT -> Position(state.playerPosition.x - 1, state.playerPosition.y)
            Direction.RIGHT -> Position(state.playerPosition.x + 1, state.playerPosition.y)
        }

        // 检查边界
        if (newPos.x < 0 || newPos.x >= mazeSize || newPos.y < 0 || newPos.y >= mazeSize) {
            return
        }

        // 检查是否为墙
        val cell = state.maze[newPos.y][newPos.x]
        if (cell.type == CellType.WALL) {
            return
        }

        // 移动
        var newScore = state.score
        val newCollected = state.collectedPinyins.toMutableList()

        // 如果路径上有拼音，自动收集
        if (cell.pinyin != null) {
            newCollected.add(cell.pinyin!!.character)
            newScore += 5

            // 清除已收集的拼音显示
            val newMaze = state.maze.mapIndexed { y, row ->
                row.mapIndexed { x, c ->
                    if (y == newPos.y && x == newPos.x) {
                        Cell(CellType.PATH, null)
                    } else {
                        c
                    }
                }
            }
            _uiState.update {
                it.copy(
                    maze = newMaze,
                    playerPosition = newPos,
                    collectedPinyins = newCollected,
                    score = newScore,
                    moves = it.moves + 1
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    playerPosition = newPos,
                    moves = it.moves + 1
                )
            }
        }

        // 检查是否到达终点
        if (newPos == state.targetPosition) {
            completeLevel()
        }
    }

    private fun completeLevel() {
        val state = _uiState.value
        val bonusPoints = maxOf(0, 50 - state.moves)  // 步数越少奖励越多
        val levelScore = state.collectedPinyins.size * 5 + bonusPoints

        _uiState.update {
            it.copy(
                isLevelComplete = true,
                showCelebration = true,
                score = it.score + levelScore,
                level = it.level + 1
            )
        }

        // 显示庆祝动画后进入下一关
        viewModelScope.launch {
            delay(2000)
            if (_uiState.value.level <= 5) {
                _uiState.update { it.copy(isLevelComplete = false, showCelebration = false) }
                generateMaze()
            } else {
                endGame()
            }
        }
    }

    private fun endGame() {
        val state = _uiState.value
        val starsEarned = when {
            state.score >= 200 -> 3
            state.score >= 150 -> 2
            state.score >= 100 -> 1
            else -> 0
        }

        val result = GameResult(
            gameType = GameType.MAZE_ADVENTURE,
            score = state.score,
            maxScore = 300,
            starsEarned = starsEarned,
            timeTaken = 0
        )

        _uiState.update { it.copy(isGameOver = true, result = result) }
    }

    fun nextLevel() {
        if (_uiState.value.level <= 5) {
            _uiState.update { it.copy(isLevelComplete = false, showCelebration = false) }
            generateMaze()
        }
    }

    fun restartGame() {
        _uiState.update { it.copy(level = 1, score = 0, moves = 0) }
        generateMaze()
    }
}

enum class Direction { UP, DOWN, LEFT, RIGHT }
