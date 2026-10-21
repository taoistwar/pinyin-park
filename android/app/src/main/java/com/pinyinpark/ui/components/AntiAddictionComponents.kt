package com.pinyinpark.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.pinyinpark.model.VerificationChallenge

// ============================================================
// 防沉迷验证弹窗组件
// ============================================================

/**
 * 验证码验证弹窗
 * 显示4位数字的大写汉字形式，要求用户点选正确答案
 *
 * @param challenge 验证码挑战
 * @param onVerified 验证成功回调
 * @param onFailed 验证失败回调
 * @param onTimeout 验证超时回调
 */
@Composable
fun VerificationDialog(
    challenge: VerificationChallenge,
    onVerified: () -> Unit,
    onFailed: (attempts: Int) -> Unit,
    onTimeout: () -> Unit
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableIntStateOf(0) }
    var showError by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(60) }

    // 倒计时逻辑
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown--
        }
        onTimeout()
    }

    AlertDialog(
        onDismissRequest = { /* 禁止关闭 */ },
        confirmButton = { },
        dismissButton = { },
        icon = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🛡️", fontSize = 48.sp)
                Text(
                    "👀 眼睛休息一下！",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        title = {
            Text(
                "请选择正确的汉字组合",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 说明文字
                Text(
                    "请在下方找出对应的汉字",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 验证码显示区域
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF8E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "对应的汉字是：",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            challenge.displayText,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 选项按钮
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        challenge.options.take(2).forEach { option ->
                            OptionButton(
                                text = option,
                                isSelected = selectedOption == option,
                                isCorrect = if (showError) option == challenge.correctAnswer else null,
                                isWrong = if (showError) selectedOption == option && option != challenge.correctAnswer else false,
                                onClick = { selectedOption = option },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        challenge.options.drop(2).forEach { option ->
                            OptionButton(
                                text = option,
                                isSelected = selectedOption == option,
                                isCorrect = if (showError) option == challenge.correctAnswer else null,
                                isWrong = if (showError) selectedOption == option && option != challenge.correctAnswer else false,
                                onClick = { selectedOption = option },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 错误提示
                AnimatedVisibility(
                    visible = showError,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Text(
                        "❌ 答错了，再试一次！",
                        color = Color(0xFFF44336),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 确认按钮
                Button(
                    onClick = {
                        selectedOption?.let { option ->
                            if (option == challenge.correctAnswer) {
                                onVerified()
                            } else {
                                showError = true
                                attempts++
                                if (attempts >= 3) {
                                    onFailed(attempts)
                                }
                            }
                        }
                    },
                    enabled = selectedOption != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("确认", fontSize = 18.sp)
                }
            }
        },
        containerColor = Color.White
    )
}

/**
 * 选项按钮
 */
@Composable
fun OptionButton(
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean?,
    isWrong: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isCorrect == true -> Color(0xFF4CAF50)
        isWrong -> Color(0xFFF44336)
        isSelected -> Color(0xFFFF9800)
        else -> Color(0xFFF5F5F5)
    }

    val borderColor = when {
        isCorrect == true -> Color(0xFF388E3C)
        isWrong -> Color(0xFFD32F2F)
        isSelected -> Color(0xFFE65100)
        else -> Color(0xFFE0E0E0)
    }

    Button(
        onClick = onClick,
        // 选项是 4 个大写汉字，高度用 minHeight 而不是固定高度，避免换行后被裁切
        modifier = modifier.heightIn(min = 60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor
        ),
        border = BorderStroke(3.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            color = if (isSelected || isCorrect == true || isWrong) Color.White else Color.Black
        )
    }
}

// ============================================================
// 游戏时间提醒组件
// ============================================================

/**
 * 游戏时间提醒弹窗
 * 提示用户休息一下
 */
@Composable
fun GameTimeReminderDialog(
    currentMinutes: Int,
    onContinue: () -> Unit,
    onTakeBreak: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("继续玩一会儿")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onTakeBreak) {
                Text("休息一下")
            }
        },
        icon = {
            Text("⏰", fontSize = 48.sp)
        },
        title = {
            Text(
                "已经玩了 $currentMinutes 分钟啦！",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("眼睛需要休息一下哦~", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("👀", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "可以去看看书、做运动，\n或者让眼睛休息一会儿",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

/**
 * 游戏时间耗尽弹窗
 */
@Composable
fun GameTimeLimitDialog(
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("好的，去休息")
            }
        },
        icon = {
            Text("🌙", fontSize = 48.sp)
        },
        title = {
            Text(
                "今日游戏时间已用完",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "今天玩得很棒！",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "明天再来继续学习拼音吧~\n记得早点休息哦！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("😴", fontSize = 48.sp)
            }
        }
    )
}

/**
 * 验证失败弹窗
 */
@Composable
fun VerificationFailedDialog(
    attempts: Int,
    onRetry: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) {
                Text("休息一下")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onRetry) {
                Text("再试一次")
            }
        },
        icon = {
            Text("🤔", fontSize = 48.sp)
        },
        title = {
            Text("验证未通过", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "答错了 $attempts 次",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "别着急，休息一下再继续！\n学习很重要，但身体更重要哦~",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

// ============================================================
// 防沉迷状态指示器
// ============================================================

/**
 * 游戏时间状态栏
 * 显示剩余时间
 */
@Composable
fun GameTimeIndicator(
    remainingMinutes: Int,
    modifier: Modifier = Modifier
) {
    val color = when {
        remainingMinutes <= 2 -> Color(0xFFF44336)  // 红色警告
        remainingMinutes <= 5 -> Color(0xFFFF9800)  // 橙色提醒
        else -> Color(0xFF4CAF50)                   // 绿色正常
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "${remainingMinutes}分钟",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
