package org.xmsleep.app.ui.breathing

/**
 * 呼吸法数据模型
 */
data class BreathingMethod(
    val id: String,
    val name: String,
    val subtitle: String,
    val inhale: Int,
    val hold: Int,
    val exhale: Int,
    val holdAfter: Int = 0,
    val description: String,
    val steps: List<String>,
    val tags: List<String>,
    val defaultDurationMinutes: Int = 5,
    val isPrimary: Boolean = false
)
