package com.pinyinpark.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pinyinpark.model.Badge
import com.pinyinpark.model.ConditionType
import com.pinyinpark.model.LessonChapter
import com.pinyinpark.model.PinyinData
import com.pinyinpark.model.UserProgress
import com.pinyinpark.viewmodel.Trophy
import com.pinyinpark.viewmodel.TrophyType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// ============================================================
// 用户进度仓库（DataStore 持久化）
// 记录星星、连续学习天数、已学知识点、徽章、宠物状态与游戏统计
// ============================================================

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

private object ProgressKeys {
    val TOTAL_STARS = intPreferencesKey("total_stars")
    val STREAK_DAYS = intPreferencesKey("streak_days")
    val LAST_LEARN_DATE = longPreferencesKey("last_learn_date")
    val COMPLETED_ITEMS = stringSetPreferencesKey("completed_items")
    val UNLOCKED_BADGES = stringSetPreferencesKey("unlocked_badges")
    val PET_LEVEL = intPreferencesKey("pet_level")
    val PET_HUNGER = intPreferencesKey("pet_hunger")
    val TOTAL_LEARNED = intPreferencesKey("total_learned")
    val COMPLETED_CHAPTERS = intPreferencesKey("completed_chapters")
    val GAMES_PLAYED = intPreferencesKey("games_played")
    val GAME_WINS = intPreferencesKey("game_wins")
    val PERFECT_GAMES = intPreferencesKey("perfect_games")
    val HAS_SPOKEN = booleanPreferencesKey("has_spoken")
}

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_progress"
)

class UserProgressRepository private constructor(private val context: Context) {

    /** 响应式进度流，界面直接 collect 即可 */
    val progress: Flow<UserProgress> = context.progressDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs.toUserProgress() }

    suspend fun currentProgress(): UserProgress = progress.first()

    /**
     * 完成一个章节：加星星、记录已学知识点、更新连续学习天数
     */
    suspend fun completeChapter(chapter: LessonChapter, starsEarned: Int) {
        update { current ->
            val now = System.currentTimeMillis()
            val today = now / DAY_MILLIS
            val lastDay = if (current.lastLearnDate > 0L) current.lastLearnDate / DAY_MILLIS else -1L

            val newStreak = when {
                lastDay == today -> maxOf(current.streakDays, 1)
                lastDay == today - 1 -> current.streakDays + 1
                else -> 1
            }

            val learnedIds = chapter.pinyinItems.map { it.id }.toSet()

            current.copy(
                totalStars = current.totalStars + starsEarned,
                streakDays = newStreak,
                lastLearnDate = now,
                completedItems = current.completedItems + learnedIds,
                totalLearned = current.totalLearned + learnedIds.size,
                completedChapters = current.completedChapters + 1
            )
        }
    }

    /**
     * 记录一局游戏结果
     */
    suspend fun recordGame(starsEarned: Int, isPerfect: Boolean) {
        update { current ->
            current.copy(
                totalStars = current.totalStars + starsEarned,
                gamesPlayed = current.gamesPlayed + 1,
                gameWins = current.gameWins + if (starsEarned > 0) 1 else 0,
                perfectGames = current.perfectGames + if (isPerfect) 1 else 0
            )
        }
    }

    /** 完成过一次语音跟读 */
    suspend fun markSpoken() {
        update { current -> current.copy(hasSpoken = true) }
    }

    /** 喂食宠物 */
    suspend fun feedPet() {
        update { current -> current.copy(petHunger = minOf(100, current.petHunger + 20)) }
    }

    // ============================================================
    // 内部实现
    // ============================================================

    private suspend fun update(transform: (UserProgress) -> UserProgress) {
        val updated = transform(currentProgress())
        val newBadges = BadgeRules.evaluate(updated)
        write(updated.copy(unlockedBadges = updated.unlockedBadges + newBadges))
    }

    private suspend fun write(data: UserProgress) {
        context.progressDataStore.edit { prefs ->
            prefs[ProgressKeys.TOTAL_STARS] = data.totalStars
            prefs[ProgressKeys.STREAK_DAYS] = data.streakDays
            prefs[ProgressKeys.LAST_LEARN_DATE] = data.lastLearnDate
            prefs[ProgressKeys.COMPLETED_ITEMS] = data.completedItems
            prefs[ProgressKeys.UNLOCKED_BADGES] = data.unlockedBadges
            prefs[ProgressKeys.PET_LEVEL] = data.petLevel
            prefs[ProgressKeys.PET_HUNGER] = data.petHunger
            prefs[ProgressKeys.TOTAL_LEARNED] = data.totalLearned
            prefs[ProgressKeys.COMPLETED_CHAPTERS] = data.completedChapters
            prefs[ProgressKeys.GAMES_PLAYED] = data.gamesPlayed
            prefs[ProgressKeys.GAME_WINS] = data.gameWins
            prefs[ProgressKeys.PERFECT_GAMES] = data.perfectGames
            prefs[ProgressKeys.HAS_SPOKEN] = data.hasSpoken
        }
    }

    companion object {
        @Volatile
        private var instance: UserProgressRepository? = null

        fun get(context: Context): UserProgressRepository {
            return instance ?: synchronized(this) {
                instance ?: UserProgressRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}

// ============================================================
// 徽章判定规则（界面进度条与自动解锁共用）
// ============================================================

object BadgeRules {

    fun currentValue(progress: UserProgress, type: ConditionType): Int = when (type) {
        ConditionType.STREAK_DAYS -> progress.streakDays
        ConditionType.TOTAL_STARS -> progress.totalStars
        ConditionType.COMPLETE_CHAPTER -> progress.completedChapters
        ConditionType.GAME_WIN_COUNT -> progress.gameWins
        ConditionType.PERFECT_SCORE -> progress.perfectGames
        ConditionType.FIRST_SPEAK -> if (progress.hasSpoken) 1 else 0
    }

    fun progressOf(badge: Badge, progress: UserProgress): Float {
        val target = badge.condition.targetValue
        if (target <= 0) return 1f
        return (currentValue(progress, badge.condition.type).toFloat() / target).coerceIn(0f, 1f)
    }

    fun evaluate(progress: UserProgress): Set<String> {
        return PinyinData.allBadges
            .filter { progressOf(it, progress) >= 1f }
            .map { it.id }
            .toSet()
    }

    /** 奖杯当前进度值 */
    fun trophyValue(progress: UserProgress, trophy: Trophy): Int = when (trophy.type) {
        TrophyType.LEARNING -> progress.totalLearned
        TrophyType.GAME ->
            if (trophy.id.contains("perfect")) progress.perfectGames else progress.gameWins
        TrophyType.SPECIAL ->
            if (trophy.id.contains("streak")) progress.streakDays else progress.totalLearned
    }

    fun trophyProgressOf(progress: UserProgress, trophy: Trophy): Float {
        if (trophy.requiredScore <= 0) return 1f
        return (trophyValue(progress, trophy).toFloat() / trophy.requiredScore).coerceIn(0f, 1f)
    }
}

// ============================================================
// Compose 便捷获取
// ============================================================

@Composable
fun rememberUserProgressRepository(): UserProgressRepository {
    val context = LocalContext.current
    return remember(context) { UserProgressRepository.get(context) }
}

private fun Preferences.toUserProgress(): UserProgress = UserProgress(
    totalStars = this[ProgressKeys.TOTAL_STARS] ?: 0,
    streakDays = this[ProgressKeys.STREAK_DAYS] ?: 0,
    lastLearnDate = this[ProgressKeys.LAST_LEARN_DATE] ?: 0L,
    completedItems = this[ProgressKeys.COMPLETED_ITEMS] ?: emptySet(),
    unlockedBadges = this[ProgressKeys.UNLOCKED_BADGES] ?: emptySet(),
    petLevel = this[ProgressKeys.PET_LEVEL] ?: 1,
    petHunger = this[ProgressKeys.PET_HUNGER] ?: 100,
    totalLearned = this[ProgressKeys.TOTAL_LEARNED] ?: 0,
    completedChapters = this[ProgressKeys.COMPLETED_CHAPTERS] ?: 0,
    gamesPlayed = this[ProgressKeys.GAMES_PLAYED] ?: 0,
    gameWins = this[ProgressKeys.GAME_WINS] ?: 0,
    perfectGames = this[ProgressKeys.PERFECT_GAMES] ?: 0,
    hasSpoken = this[ProgressKeys.HAS_SPOKEN] ?: false
)
