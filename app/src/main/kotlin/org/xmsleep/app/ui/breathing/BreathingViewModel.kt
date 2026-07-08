package org.xmsleep.app.ui.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 呼吸练习 ViewModel - 管理呼吸状态和计时器
 */
class BreathingViewModel : ViewModel() {

    // 默认呼吸方法（用于初始化，后续会被真实方法替换）
    private val defaultMethod = BreathingMethod(
        id = "sleep_478",
        name = "4-7-8",
        subtitle = "",
        inhale = 4,
        hold = 7,
        exhale = 8,
        holdAfter = 0,
        defaultDurationMinutes = 10,
        isPrimary = true,
        description = "",
        steps = emptyList(),
        tags = emptyList()
    )

    private val _currentMethod = MutableStateFlow(defaultMethod)
    val currentMethod = _currentMethod.asStateFlow()

    private val _durationMinutes = MutableStateFlow(defaultMethod.defaultDurationMinutes)
    val durationMinutes = _durationMinutes.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive = _isActive.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds = _remainingSeconds.asStateFlow()

    private var timerJob: Job? = null

    /**
     * 选择呼吸法
     */
    fun selectMethod(method: BreathingMethod) {
        _currentMethod.value = method
        _durationMinutes.value = method.defaultDurationMinutes
    }

    /**
     * 设置时长（分钟）
     */
    fun setDuration(minutes: Int) {
        _durationMinutes.value = minutes
    }

    /**
     * 开始呼吸练习
     */
    fun startBreathing() {
        if (_isActive.value) return

        _isActive.value = true
        _remainingSeconds.value = _durationMinutes.value * 60

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000L)
                _remainingSeconds.value -= 1
            }
            // 计时结束自动停止
            stopBreathing()
        }
    }

    /**
     * 停止呼吸练习
     */
    fun stopBreathing() {
        timerJob?.cancel()
        timerJob = null
        _isActive.value = false
        _remainingSeconds.value = 0
    }

    override fun onCleared() {
        stopBreathing()
        super.onCleared()
    }
}
