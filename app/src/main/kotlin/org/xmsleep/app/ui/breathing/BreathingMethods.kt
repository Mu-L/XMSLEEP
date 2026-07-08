package org.xmsleep.app.ui.breathing

import android.content.Context
import org.xmsleep.app.R

/**
 * 呼吸法定义 - 使用字符串资源支持多语言
 */
object BreathingMethods {
    
    fun getMethods(context: Context): List<BreathingMethod> {
        return listOf(
            // 4-7-8 助眠呼吸法
            BreathingMethod(
                id = "sleep_478",
                name = context.getString(R.string.breathing_method_478_name),
                subtitle = context.getString(R.string.breathing_method_478_subtitle),
                inhale = 4,
                hold = 7,
                exhale = 8,
                holdAfter = 0,
                defaultDurationMinutes = 10,
                isPrimary = true,
                description = context.getString(R.string.breathing_method_478_desc),
                steps = listOf(
                    context.getString(R.string.breathing_method_478_step1),
                    context.getString(R.string.breathing_method_478_step2),
                    context.getString(R.string.breathing_method_478_step3),
                    context.getString(R.string.breathing_method_478_step4),
                    context.getString(R.string.breathing_method_478_step5)
                ),
                tags = listOf(
                    context.getString(R.string.breathing_method_478_tag1),
                    context.getString(R.string.breathing_method_478_tag2),
                    context.getString(R.string.breathing_method_478_tag3)
                )
            ),
            // 箱式呼吸
            BreathingMethod(
                id = "box_4444",
                name = context.getString(R.string.breathing_method_box_name),
                subtitle = context.getString(R.string.breathing_method_box_subtitle),
                inhale = 4,
                hold = 4,
                exhale = 4,
                holdAfter = 4,
                defaultDurationMinutes = 5,
                description = context.getString(R.string.breathing_method_box_desc),
                steps = listOf(
                    context.getString(R.string.breathing_method_box_step1),
                    context.getString(R.string.breathing_method_box_step2),
                    context.getString(R.string.breathing_method_box_step3),
                    context.getString(R.string.breathing_method_box_step4),
                    context.getString(R.string.breathing_method_box_step5)
                ),
                tags = listOf(
                    context.getString(R.string.breathing_method_box_tag1),
                    context.getString(R.string.breathing_method_box_tag2),
                    context.getString(R.string.breathing_method_box_tag3)
                )
            ),
            // 基础腹式呼吸
            BreathingMethod(
                id = "belly_46",
                name = context.getString(R.string.breathing_method_belly_name),
                subtitle = context.getString(R.string.breathing_method_belly_subtitle),
                inhale = 4,
                hold = 0,
                exhale = 6,
                holdAfter = 0,
                defaultDurationMinutes = 5,
                description = context.getString(R.string.breathing_method_belly_desc),
                steps = listOf(
                    context.getString(R.string.breathing_method_belly_step1),
                    context.getString(R.string.breathing_method_belly_step2),
                    context.getString(R.string.breathing_method_belly_step3),
                    context.getString(R.string.breathing_method_belly_step4)
                ),
                tags = listOf(
                    context.getString(R.string.breathing_method_belly_tag1),
                    context.getString(R.string.breathing_method_belly_tag2),
                    context.getString(R.string.breathing_method_belly_tag3)
                )
            ),
            // 4-2-6 快速减压呼吸
            BreathingMethod(
                id = "stress_426",
                name = context.getString(R.string.breathing_method_426_name),
                subtitle = context.getString(R.string.breathing_method_426_subtitle),
                inhale = 4,
                hold = 2,
                exhale = 6,
                holdAfter = 0,
                defaultDurationMinutes = 5,
                description = context.getString(R.string.breathing_method_426_desc),
                steps = listOf(
                    context.getString(R.string.breathing_method_426_step1),
                    context.getString(R.string.breathing_method_426_step2),
                    context.getString(R.string.breathing_method_426_step3)
                ),
                tags = listOf(
                    context.getString(R.string.breathing_method_426_tag1),
                    context.getString(R.string.breathing_method_426_tag2),
                    context.getString(R.string.breathing_method_426_tag3)
                )
            )
        )
    }
}
