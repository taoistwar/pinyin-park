package com.pinyinpark.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * 音频播放管理器
 * 支持两种模式：
 * 1. 本地音频资源播放 (MediaPlayer) —— res/raw 下存在对应音频时优先使用
 * 2. TTS 语音合成 —— 没有本地音频时用系统中文语音引擎朗读
 *
 * 注意：Android 11+ 需要在 AndroidManifest 中声明 TTS_SERVICE 的 <queries>，
 * 否则绑定不到系统语音引擎。
 */
class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var isReleased = false

    /** TTS 引擎初始化是异步的，初始化完成前的播放请求先缓存 */
    private var pendingSpeech: String? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // 音频资源映射表（拼音字符 -> res/raw 资源名）
    private val audioResourceMap = mapOf(
        // 声母
        "b" to "audio_b", "p" to "audio_p", "m" to "audio_m", "f" to "audio_f",
        "d" to "audio_d", "t" to "audio_t", "n" to "audio_n", "l" to "audio_l",
        "g" to "audio_g", "k" to "audio_k", "h" to "audio_h",
        "j" to "audio_j", "q" to "audio_q", "x" to "audio_x",
        "zh" to "audio_zh", "ch" to "audio_ch", "sh" to "audio_sh",
        "r" to "audio_r", "z" to "audio_z", "c" to "audio_c", "s" to "audio_s",
        // 韵母
        "a" to "audio_a", "o" to "audio_o", "e" to "audio_e",
        "i" to "audio_i", "u" to "audio_u", "ü" to "audio_v",
        "ai" to "audio_ai", "ei" to "audio_ei", "ui" to "audio_ui",
        "ao" to "audio_ao", "ou" to "audio_ou", "iu" to "audio_iu",
        "ie" to "audio_ie", "üe" to "audio_ve",
        "er" to "audio_er", "an" to "audio_an", "en" to "audio_en",
        "in" to "audio_in", "un" to "audio_un", "ün" to "audio_vn",
        "ang" to "audio_ang", "eng" to "audio_eng", "ing" to "audio_ing", "ong" to "audio_ong",
        // 四声
        "1" to "audio_1", "2" to "audio_2", "3" to "audio_3", "4" to "audio_4",
        // 整体认读
        "zhi" to "audio_zhi", "chi" to "audio_chi", "shi" to "audio_shi",
        "ri" to "audio_ri", "zi" to "audio_zi", "ci" to "audio_ci", "si" to "audio_si"
    )

    init {
        initTextToSpeech()
    }

    // ============================================================
    // TTS 初始化
    // ============================================================

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            val tts = textToSpeech
            if (status != TextToSpeech.SUCCESS || tts == null) {
                // 引擎初始化失败：清掉待播请求，避免按钮一直停在「播放中」
                pendingSpeech = null
                _isPlaying.value = false
                return@TextToSpeech
            }

            val languageResult = tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
            isTtsReady = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                languageResult != TextToSpeech.LANG_NOT_SUPPORTED

            if (!isTtsReady) {
                pendingSpeech = null
                _isPlaying.value = false
                return@TextToSpeech
            }

            tts.setSpeechRate(0.85f) // 儿童学习放慢一点
            tts.setPitch(1.05f)
            tts.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isPlaying.value = false
                }

                override fun onError(utteranceId: String?) {
                    _isPlaying.value = false
                }
            })

            // 补播初始化期间积压的请求
            pendingSpeech?.let { text ->
                pendingSpeech = null
                speakWithTts(text)
            }
        }
    }

    // ============================================================
    // 播放入口
    // ============================================================

    /**
     * 播放拼音发音
     *
     * @param pinyinChar 拼音字符（如 "b"、"ba"）
     * @param exampleWord 示例汉字（如 "爸"），TTS 朗读时优先使用，读出来更自然
     * @param speakText 指定朗读文本（优先级最高）
     * @return true 表示已触发播放；false 表示设备没有可用的中文语音引擎
     */
    fun playPinyin(
        pinyinChar: String,
        exampleWord: String? = null,
        speakText: String? = null
    ): Boolean {
        if (isReleased) return false
        stop()

        // 优先使用本地音频资源
        val resourceName = audioResourceMap[pinyinChar]
        if (resourceName != null && playLocalAudio(resourceName)) {
            return true
        }

        // 没有本地音频则由 TTS 朗读：示例汉字 > 指定文本 > 拼音本身
        val text = speakText?.takeIf { it.isNotBlank() }
            ?: exampleWord?.takeIf { it.isNotBlank() }
            ?: pinyinChar
        return speakWithTts(text)
    }

    /**
     * 播放示例词发音
     */
    fun playExampleWord(word: String): Boolean {
        if (isReleased) return false
        stop()
        return speakWithTts(word)
    }

    // ============================================================
    // 本地音频
    // ============================================================

    private fun playLocalAudio(resourceName: String): Boolean {
        return try {
            val resourceId = context.resources.getIdentifier(
                resourceName, "raw", context.packageName
            )
            if (resourceId == 0) return false

            val player = MediaPlayer.create(context, resourceId) ?: return false
            mediaPlayer = player
            player.setOnCompletionListener {
                _isPlaying.value = false
                releaseMediaPlayer()
            }
            player.setOnErrorListener { _, _, _ ->
                _isPlaying.value = false
                releaseMediaPlayer()
                true
            }
            player.start()
            _isPlaying.value = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            releaseMediaPlayer()
            false
        }
    }

    // ============================================================
    // TTS
    // ============================================================

    private fun speakWithTts(text: String): Boolean {
        val tts = textToSpeech ?: return false

        if (!isTtsReady) {
            // 引擎还没就绪（初始化回调未返回），先缓存，就绪后自动补播
            pendingSpeech = text
            // 尚未就绪时无法确认引擎可用，交由调用方决定是否提示
            return true
        }

        val result = tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "pinyin_${System.currentTimeMillis()}"
        )
        val started = result == TextToSpeech.SUCCESS
        _isPlaying.value = started
        return started
    }

    // ============================================================
    // 状态控制
    // ============================================================

    /**
     * 停止播放
     */
    fun stop() {
        pendingSpeech = null
        textToSpeech?.stop()

        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (e: IllegalStateException) {
                // 已释放或未就绪，忽略
            }
            it.release()
        }
        mediaPlayer = null
        _isPlaying.value = false
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    /**
     * 释放资源（页面销毁时调用）
     */
    fun release() {
        isReleased = true
        stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isTtsReady = false
    }

    /**
     * 检查 TTS 是否可用（引擎初始化完成且支持中文）
     */
    fun isTtsAvailable(): Boolean = isTtsReady

    /**
     * 获取当前播放状态
     */
    fun isCurrentlyPlaying(): Boolean = _isPlaying.value
}
