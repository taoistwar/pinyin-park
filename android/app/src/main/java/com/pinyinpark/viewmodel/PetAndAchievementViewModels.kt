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

// ============================================================
// 宠物系统 ViewModel
// ============================================================

/**
 * 宠物状态
 */
data class PetUiState(
    val pet: Pet = Pet(),                    // 当前宠物
    val isHungry: Boolean = false,           // 是否饿了
    val hungerLevel: Int = 100,              // 饱食度 0-100
    val mood: PetMood = PetMood.HAPPY,       // 心情
    val isPlaying: Boolean = false,          // 是否在玩耍
    val showEvolution: Boolean = false,      // 显示进化动画
    val evolutionMessage: String = "",        // 进化消息
    val feedingAnimation: Boolean = false,    // 喂食动画
    val playAnimation: Boolean = false      // 玩耍动画
)

enum class PetMood { HAPPY, NORMAL, SAD, SLEEPY }

/**
 * 宠物数据模型
 */
data class Pet(
    val name: String = "小拼",
    val level: Int = 1,
    val exp: Int = 0,
    val expToNextLevel: Int = 100,
    val stage: PetStage = PetStage.BABY,
    val appearance: PetAppearance = PetAppearance(),
    val totalFeeding: Int = 0,               // 累计喂食次数
    val totalPlay: Int = 0,                  // 累计玩耍次数
    val totalLearning: Int = 0               // 累计学习次数
)

enum class PetStage { BABY, CHILD, ADULT, LEGEND }

data class PetAppearance(
    val baseColor: String = "#FF9800",       // 基础颜色
    val eyeStyle: Int = 0,                    // 眼睛样式
    val accessoryStyle: Int = 0,             // 配饰样式
    val expression: String = "😊"             // 表情
)

class PetViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PetUiState())
    val uiState: StateFlow<PetUiState> = _uiState.asStateFlow()

    init {
        loadPet()
        startHungerTimer()
    }

    private fun loadPet() {
        // TODO: 从本地数据库加载宠物数据
        // 这里先用默认值
        _uiState.update {
            it.copy(
                pet = Pet(
                    name = "小拼",
                    level = 1,
                    exp = 30,
                    stage = PetStage.BABY
                ),
                hungerLevel = 80
            )
        }
    }

    private fun startHungerTimer() {
        // 饱食度每30秒降低1点
        viewModelScope.launch {
            while (true) {
                delay(30000)
                val state = _uiState.value
                val newHunger = maxOf(0, state.hungerLevel - 1)

                val newMood = when {
                    newHunger <= 20 -> PetMood.SAD
                    newHunger <= 50 -> PetMood.NORMAL
                    else -> PetMood.HAPPY
                }

                _uiState.update {
                    it.copy(
                        hungerLevel = newHunger,
                        mood = newMood,
                        isHungry = newHunger <= 30
                    )
                }
            }
        }
    }

    /**
     * 喂食宠物
     * 消耗积分，增加饱食度和经验
     */
    fun feedPet() {
        viewModelScope.launch {
            _uiState.update { it.copy(feedingAnimation = true) }

            delay(1000)

            val state = _uiState.value
            val newHunger = minOf(100, state.hungerLevel + 20)
            val newExp = state.pet.exp + 5

            // 检查升级
            val (newPet, canLevelUp) = checkLevelUp(state.pet.copy(
                exp = newExp,
                totalFeeding = state.pet.totalFeeding + 1
            ))

            _uiState.update {
                it.copy(
                    pet = newPet,
                    hungerLevel = newHunger,
                    feedingAnimation = false,
                    isHungry = newHunger <= 30
                )
            }
        }
    }

    /**
     * 和宠物玩耍
     * 增加经验和心情
     */
    fun playWithPet() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPlaying = true,
                    playAnimation = true
                )
            }

            delay(2000)

            val state = _uiState.value
            val newExp = state.pet.exp + 10
            val newMood = PetMood.HAPPY

            // 检查升级
            val (newPet, canLevelUp) = checkLevelUp(state.pet.copy(
                exp = newExp,
                totalPlay = state.pet.totalPlay + 1
            ))

            _uiState.update {
                it.copy(
                    pet = newPet,
                    mood = newMood,
                    isPlaying = false,
                    playAnimation = false
                )
            }
        }
    }

    /**
     * 学习完成后给宠物奖励
     */
    fun rewardFromLearning(starsEarned: Int) {
        val state = _uiState.value
        val expGained = starsEarned * 10
        val newExp = state.pet.exp + expGained

        // 检查升级
        val (newPet, canLevelUp) = checkLevelUp(state.pet.copy(
            exp = newExp,
            totalLearning = state.pet.totalLearning + 1
        ))

        if (canLevelUp) {
            // 显示进化/升级动画
            _uiState.update {
                it.copy(
                    pet = newPet,
                    showEvolution = true,
                    evolutionMessage = "恭喜升级到 Lv.${newPet.level}！"
                )
            }

            viewModelScope.launch {
                delay(3000)
                _uiState.update { it.copy(showEvolution = false, evolutionMessage = "") }
            }
        } else {
            _uiState.update { it.copy(pet = newPet) }
        }
    }

    /**
     * 游戏获胜后给宠物奖励
     */
    fun rewardFromGame(starsEarned: Int) {
        val state = _uiState.value
        val expGained = starsEarned * 15
        val newExp = state.pet.exp + expGained

        // 检查升级
        val (newPet, canLevelUp) = checkLevelUp(state.pet.copy(exp = newExp))

        if (canLevelUp) {
            _uiState.update {
                it.copy(
                    pet = newPet,
                    showEvolution = true,
                    evolutionMessage = "恭喜升级到 Lv.${newPet.level}！"
                )
            }

            viewModelScope.launch {
                delay(3000)
                _uiState.update { it.copy(showEvolution = false, evolutionMessage = "") }
            }
        } else {
            _uiState.update { it.copy(pet = newPet) }
        }
    }

    /**
     * 检查是否可以升级
     */
    private fun checkLevelUp(pet: Pet): Pair<Pet, Boolean> {
        var currentPet = pet
        var leveledUp = false

        while (currentPet.exp >= currentPet.expToNextLevel) {
            val newLevel = currentPet.level + 1
            currentPet = currentPet.copy(
                level = newLevel,
                exp = currentPet.exp - currentPet.expToNextLevel,
                expToNextLevel = calculateExpToNextLevel(newLevel),
                stage = calculateStage(newLevel),
                appearance = calculateAppearance(currentPet, newLevel)
            )
            leveledUp = true
        }

        return Pair(currentPet, leveledUp)
    }

    private fun calculateExpToNextLevel(level: Int): Int {
        return 100 + (level - 1) * 50
    }

    private fun calculateStage(level: Int): PetStage {
        return when {
            level >= 30 -> PetStage.LEGEND
            level >= 15 -> PetStage.ADULT
            level >= 5 -> PetStage.CHILD
            else -> PetStage.BABY
        }
    }

    private fun calculateAppearance(pet: Pet, newLevel: Int): PetAppearance {
        val stage = calculateStage(newLevel)
        val color = when (stage) {
            PetStage.BABY -> "#FF9800"      // 橙色
            PetStage.CHILD -> "#FFB74D"     // 浅橙
            PetStage.ADULT -> "#4CAF50"     // 绿色
            PetStage.LEGEND -> "#9C27B0"   // 紫色
        }

        val accessory = when (stage) {
            PetStage.BABY -> 0
            PetStage.CHILD -> 1
            PetStage.ADULT -> 2
            PetStage.LEGEND -> 3
        }

        return pet.appearance.copy(
            baseColor = color,
            accessoryStyle = accessory
        )
    }

    fun dismissEvolution() {
        _uiState.update { it.copy(showEvolution = false) }
    }

    fun getExpProgress(): Float {
        val pet = _uiState.value.pet
        return pet.exp.toFloat() / pet.expToNextLevel
    }
}

// ============================================================
// 奖杯/勋章系统 ViewModel
// ============================================================

/**
 * 成就系统状态
 */
data class AchievementUiState(
    val badges: List<BadgeWithStatus> = emptyList(),
    val trophies: List<TrophyWithStatus> = emptyList(),
    val recentUnlocks: List<UnlockedBadge> = emptyList(),
    val showUnlockAnimation: Boolean = false,
    val currentUnlock: UnlockedBadge? = null
)

data class BadgeWithStatus(
    val badge: Badge,
    val isUnlocked: Boolean = false,
    val progress: Float = 0f,        // 解锁进度 0-1
    val unlockedAt: Long? = null      // 解锁时间戳
)

data class TrophyWithStatus(
    val trophy: Trophy,
    val isUnlocked: Boolean = false,
    val progress: Float = 0f,
    val unlockedAt: Long? = null
)

data class UnlockedBadge(
    val name: String,
    val description: String,
    val emoji: String,
    val unlockTime: Long = System.currentTimeMillis()
)

/**
 * 奖杯数据模型
 */
data class Trophy(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val requiredScore: Int,
    val type: TrophyType = TrophyType.LEARNING
)

enum class TrophyType { LEARNING, GAME, SPECIAL }

/**
 * 全部奖杯定义（成就系统与勋章墙共用）
 */
val ALL_TROPHIES: List<Trophy> = listOf(
    // 学习类奖杯
    Trophy("trophy_learn_10", "拼音小助手", "学习10个拼音", "📚", 10, TrophyType.LEARNING),
    Trophy("trophy_learn_50", "拼音爱好者", "学习50个拼音", "📖", 50, TrophyType.LEARNING),
    Trophy("trophy_learn_100", "拼音达人", "学习100个拼音", "🎓", 100, TrophyType.LEARNING),
    Trophy("trophy_master_all", "拼音大师", "学会所有拼音", "👨‍🎓", 200, TrophyType.LEARNING),

    // 游戏类奖杯
    Trophy("trophy_game_win", "初战告捷", "游戏获胜1次", "🎮", 1, TrophyType.GAME),
    Trophy("trophy_game_master", "游戏王者", "游戏获胜50次", "🏆", 50, TrophyType.GAME),
    Trophy("trophy_perfect", "完美主义", "获得10次满分", "💯", 10, TrophyType.GAME),

    // 特殊奖杯
    Trophy("trophy_streak_3", "三天坚持", "连续学习3天", "🔥", 3, TrophyType.SPECIAL),
    Trophy("trophy_streak_7", "一周坚持", "连续学习7天", "🌟", 7, TrophyType.SPECIAL),
    Trophy("trophy_dedicated", "学习达人", "累计学习100次", "⭐", 100, TrophyType.SPECIAL)
)

class AchievementViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementUiState())
    val uiState: StateFlow<AchievementUiState> = _uiState.asStateFlow()

    init {
        loadAchievements()
    }

    private fun loadAchievements() {
        // 初始化徽章状态
        val badgeStatuses = PinyinData.allBadges.map { badge ->
            BadgeWithStatus(badge = badge, isUnlocked = false, progress = 0f)
        }

        // 初始化奖杯状态
        val trophyStatuses = getAllTrophies().map { trophy ->
            TrophyWithStatus(trophy = trophy, isUnlocked = false, progress = 0f)
        }

        _uiState.update {
            it.copy(
                badges = badgeStatuses,
                trophies = trophyStatuses
            )
        }
    }

    private fun getAllTrophies(): List<Trophy> = ALL_TROPHIES

    /**
     * 更新学习进度
     */
    fun updateLearningProgress(totalLearned: Int, streakDays: Int, perfectScores: Int) {
        val state = _uiState.value

        // 更新徽章进度
        val updatedBadges = state.badges.map { badgeStatus ->
            val progress = calculateBadgeProgress(badgeStatus.badge, totalLearned, streakDays, perfectScores)
            val isUnlocked = progress >= 1f

            if (isUnlocked && !badgeStatus.isUnlocked) {
                // 解锁新徽章
                showUnlockEffect(badgeStatus.badge.title, badgeStatus.badge.description, badgeStatus.badge.iconEmoji)
            }

            badgeStatus.copy(
                isUnlocked = isUnlocked || badgeStatus.isUnlocked,
                progress = progress,
                unlockedAt = if (isUnlocked && !badgeStatus.isUnlocked) System.currentTimeMillis() else badgeStatus.unlockedAt
            )
        }

        // 更新奖杯进度
        val updatedTrophies = state.trophies.map { trophyStatus ->
            val progress = calculateTrophyProgress(trophyStatus.trophy, totalLearned, streakDays, perfectScores)
            val isUnlocked = progress >= 1f

            if (isUnlocked && !trophyStatus.isUnlocked) {
                showUnlockEffect(trophyStatus.trophy.name, trophyStatus.trophy.description, trophyStatus.trophy.emoji)
            }

            trophyStatus.copy(
                isUnlocked = isUnlocked || trophyStatus.isUnlocked,
                progress = progress,
                unlockedAt = if (isUnlocked && !trophyStatus.isUnlocked) System.currentTimeMillis() else trophyStatus.unlockedAt
            )
        }

        _uiState.update {
            it.copy(
                badges = updatedBadges,
                trophies = updatedTrophies
            )
        }
    }

    /**
     * 更新游戏进度
     */
    fun updateGameProgress(wins: Int, perfectScores: Int) {
        val state = _uiState.value

        // 更新奖杯进度
        val updatedTrophies = state.trophies.map { trophyStatus ->
            val currentValue = when (trophyStatus.trophy.id) {
                "trophy_game_win" -> wins
                "trophy_game_master" -> wins
                "trophy_perfect" -> perfectScores
                else -> 0
            }

            val progress = if (trophyStatus.trophy.requiredScore > 0) {
                (currentValue.toFloat() / trophyStatus.trophy.requiredScore).coerceAtMost(1f)
            } else 0f

            val isUnlocked = progress >= 1f

            if (isUnlocked && !trophyStatus.isUnlocked) {
                showUnlockEffect(trophyStatus.trophy.name, trophyStatus.trophy.description, trophyStatus.trophy.emoji)
            }

            trophyStatus.copy(
                isUnlocked = isUnlocked || trophyStatus.isUnlocked,
                progress = progress,
                unlockedAt = if (isUnlocked && !trophyStatus.isUnlocked) System.currentTimeMillis() else trophyStatus.unlockedAt
            )
        }

        _uiState.update {
            it.copy(trophies = updatedTrophies)
        }
    }

    private fun calculateBadgeProgress(badge: Badge, totalLearned: Int, streakDays: Int, perfectScores: Int): Float {
        return when (badge.condition.type) {
            ConditionType.TOTAL_STARS -> (totalLearned.toFloat() / badge.condition.targetValue).coerceAtMost(1f)
            ConditionType.STREAK_DAYS -> (streakDays.toFloat() / badge.condition.targetValue).coerceAtMost(1f)
            ConditionType.PERFECT_SCORE -> (perfectScores.toFloat() / badge.condition.targetValue).coerceAtMost(1f)
            ConditionType.GAME_WIN_COUNT -> 0f  // 需要单独处理
            ConditionType.FIRST_SPEAK -> 0f  // 需要单独处理
            else -> 0f
        }
    }

    private fun calculateTrophyProgress(trophy: Trophy, totalLearned: Int, streakDays: Int, perfectScores: Int): Float {
        val currentValue = when (trophy.type) {
            TrophyType.LEARNING -> totalLearned
            TrophyType.GAME -> 0  // 需要单独处理
            TrophyType.SPECIAL -> when (trophy.id) {
                "trophy_streak_3" -> streakDays
                "trophy_streak_7" -> streakDays
                "trophy_dedicated" -> totalLearned
                else -> 0
            }
        }

        return if (trophy.requiredScore > 0) {
            (currentValue.toFloat() / trophy.requiredScore).coerceAtMost(1f)
        } else 0f
    }

    private fun showUnlockEffect(name: String, description: String, emoji: String) {
        val unlock = UnlockedBadge(name, description, emoji)

        _uiState.update {
            it.copy(
                showUnlockAnimation = true,
                currentUnlock = unlock,
                recentUnlocks = it.recentUnlocks + unlock
            )
        }
    }

    fun dismissUnlockEffect() {
        _uiState.update { it.copy(showUnlockAnimation = false, currentUnlock = null) }
    }

    fun getUnlockedBadgesCount(): Int = _uiState.value.badges.count { it.isUnlocked }

    fun getUnlockedTrophiesCount(): Int = _uiState.value.trophies.count { it.isUnlocked }

    fun getTotalProgress(): Float {
        val state = _uiState.value
        val totalItems = state.badges.size + state.trophies.size
        val unlockedItems = state.badges.count { it.isUnlocked } + state.trophies.count { it.isUnlocked }
        return if (totalItems > 0) unlockedItems.toFloat() / totalItems else 0f
    }
}
