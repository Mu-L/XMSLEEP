package org.xmsleep.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.audio.BilibiliApi
import org.xmsleep.app.audio.RadioPlayer
import org.xmsleep.app.audio.model.StationType
import org.xmsleep.app.audio.model.RadioStation
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.timer.CountdownTimer
import org.xmsleep.app.utils.Logger

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val _radioPlayer = RadioPlayer()
    val radioPlayer: RadioPlayer get() = _radioPlayer
    
    private val _countdownTimer = CountdownTimer(viewModelScope) { _radioPlayer.pause() }
    val countdownTimer: CountdownTimer get() = _countdownTimer

    private val _currentStation = MutableStateFlow(RadioStation.defaultStation)
    val currentStation: StateFlow<RadioStation> = _currentStation.asStateFlow()

    private val _volume = MutableStateFlow(0.5f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    val isPlaying: StateFlow<Boolean> = _radioPlayer.isPlaying
    val isBuffering: StateFlow<Boolean> = _radioPlayer.isBuffering

    private val _bilibiliRooms = MutableStateFlow<List<BilibiliApi.LiveRoom>>(emptyList())
    val bilibiliRooms: StateFlow<List<BilibiliApi.LiveRoom>> = _bilibiliRooms.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    private val _lottieAnimationFile = MutableStateFlow("dq.lottie")
    val lottieAnimationFile: StateFlow<String> = _lottieAnimationFile.asStateFlow()

    private val _playingRoomId = MutableStateFlow<String?>(null)
    val playingRoomId: StateFlow<String?> = _playingRoomId.asStateFlow()

    private val _playingRoomInfo = MutableStateFlow<BilibiliApi.LiveRoom?>(null)
    val playingRoomInfo: StateFlow<BilibiliApi.LiveRoom?> = _playingRoomInfo.asStateFlow()

    private val _pinnedRoomIds = MutableStateFlow<Set<String>>(emptySet())
    val pinnedRoomIds: StateFlow<Set<String>> = _pinnedRoomIds.asStateFlow()

    private val _pinnedRoomInfos = MutableStateFlow<List<BilibiliApi.LiveRoom>>(emptyList())
    val pinnedRoomInfos: StateFlow<List<BilibiliApi.LiveRoom>> = _pinnedRoomInfos.asStateFlow()

    private var hasAutoPlayed = false
    private var bilibiliPlayJob: Job? = null
    private var bilibiliWatchdog: Job? = null
    private var searchJob: Job? = null

    init {
        loadSavedStation()
        val context = getApplication<Application>()
        _pinnedRoomIds.value = PreferencesManager.getBilibiliPinnedRooms(context)
        _pinnedRoomInfos.value = PreferencesManager.getBilibiliPinnedRoomsInfo(context)
        viewModelScope.launch {
            BilibiliApi.prewarm()
        }
        val audioManager = AudioManager.getInstance()
        viewModelScope.launch {
            _radioPlayer.isPlaying.collect { playing ->
                audioManager.setRadioPlaying(playing)
            }
        }
        audioManager.setOnStopRadioRequested {
            _radioPlayer.pause()
        }
        audioManager.setOnRadioResumeRequested {
            val station = _currentStation.value
            if (station.url.isNotEmpty()) {
                _radioPlayer.play(getApplication(), station)
                _radioPlayer.setVolume(_volume.value)
            } else if (station.roomId != null) {
                playBilibiliRoom(station.roomId)
            }
        }
    }

    private fun loadSavedStation() {
        val context = getApplication<Application>()
        val savedId = PreferencesManager.getRadioStationId(context)
        val station = savedId?.let { RadioStation.getById(it) } ?: RadioStation.defaultStation
        _currentStation.value = station
        _volume.value = PreferencesManager.getRadioVolume(context)
        _lottieAnimationFile.value = PreferencesManager.getLottieAnimation(context)
    }

    fun play(station: RadioStation = _currentStation.value) {
        val context = getApplication<Application>()
        _currentStation.value = station

        when (station.type) {
            StationType.Stream -> {
                bilibiliWatchdog?.cancel()
                _radioPlayer.play(context, station)
                _radioPlayer.setVolume(_volume.value)
                PreferencesManager.saveRadioStationId(context, station.id)
            }
            StationType.BilibiliLive -> {
                if (station.roomId != null) playBilibiliRoom(station.roomId)
            }
            StationType.BilibiliCategory -> {
                val keyword = station.searchKeyword ?: return
                searchBilibiliRooms(keyword)
            }
        }
    }

    fun playBilibiliRoom(roomId: String, room: BilibiliApi.LiveRoom? = null) {
        val context = getApplication<Application>()
        bilibiliWatchdog?.cancel()
        bilibiliPlayJob?.cancel()
        _radioPlayer.stop()
        _radioPlayer.setVolume(_volume.value)
        _playingRoomId.value = roomId
        if (room != null) {
            _playingRoomInfo.value = room
        }
        bilibiliPlayJob = viewModelScope.launch {
            val roomInfo = BilibiliApi.getRoomInfo(roomId)
            if (roomInfo == null || !roomInfo.isLive) {
                Logger.w("RadioViewModel", "Bilibili room $roomId 未开播")
                return@launch
            }
            // use the long room ID (realRoomId) for stream URL request
            val playRoomId = roomInfo.realRoomId
            val liveUrl = BilibiliApi.getLiveUrl(playRoomId)
            if (liveUrl == null) {
                Logger.w("RadioViewModel", "获取 Bilibili 直播地址失败: $playRoomId")
                return@launch
            }
            _playingRoomId.value = roomId
            val playable = _currentStation.value.copy(
                url = liveUrl, isHls = liveUrl.contains(".m3u8") || liveUrl.contains(".m3u"),
                roomId = roomId, type = StationType.BilibiliLive
            )
            _currentStation.value = playable
            _radioPlayer.play(context, playable)
            _radioPlayer.setVolume(_volume.value)
            PreferencesManager.saveRadioStationId(context, _currentStation.value.id)
            startBilibiliWatchdog(roomId)
        }
    }

    // 排除的游戏/娱乐分区 (old_area_id): 1=网游 2=手游 3=单机 4=娱乐 7=赛事 13=赛事
    private val excludeAreas = setOf(1, 2, 3, 4, 7, 13)

    private val excludeTitleKeywords = listOf(
        "聊天", "唱歌", "陪打", "声控", "连麦", "喊麦", "脱口秀",
        "恋爱", "交友", "颜值", "舞蹈", "游戏", "直播打", "pk",
        "王者", "原神", "吃鸡", "英雄联盟", "LOL", "永劫", "CF",
        "第五人格", "和平精英", "金铲铲", "蛋仔", "无畏契约",
        "点歌", "互动", "陪玩", "陪看", "接单", "上分", "代练",
        "娱乐", "搞笑", "整蛊", "挑战", "抽奖", "红包", "唱歌",
        "御姐", "萝莉", "大叔", "姐姐", "妹妹", "男友", "女友",
        "哄睡", "耳语", "舔耳", "磕CP", "心动", "大冒险"
    )

    private fun searchBilibiliRooms(keyword: String) {
        searchJob?.cancel()
        _isSearching.value = true
        _searchKeyword.value = keyword
        searchJob = viewModelScope.launch {
            val results = BilibiliApi.searchRooms(keyword)
                .filter { room ->
                    room.area !in excludeAreas &&
                    excludeTitleKeywords.none { room.title.contains(it) } &&
                    (room.title.contains(keyword))
                }
                .sortedByDescending { it.online }
            if (_searchKeyword.value == keyword) {
                _bilibiliRooms.value = results
                _isSearching.value = false
            }
        }
    }

    fun searchBilibili(keyword: String) {
        searchBilibiliRooms(keyword)
    }

    fun refreshBilibiliSearch() {
        val kw = _searchKeyword.value
        if (kw.isNotEmpty()) searchBilibiliRooms(kw)
    }

    private fun startBilibiliWatchdog(roomId: String) {
        bilibiliWatchdog?.cancel()
        bilibiliWatchdog = viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                val info = BilibiliApi.getRoomInfo(roomId)
                if (info == null || !info.isLive) {
                    Logger.d("RadioViewModel", "Bilibili 直播已结束, 自动停止: $roomId")
                    _radioPlayer.pause()
                    break
                }
            }
        }
    }

    fun playIfNeeded() {
        if (!hasAutoPlayed) {
            hasAutoPlayed = true
            viewModelScope.launch {
                play()
            }
        } else if (!_radioPlayer.isPlaying.value) {
            searchBilibiliRooms("白噪音")
        }
    }

    fun togglePlayPause() {
        val context = getApplication<Application>()
        if (_radioPlayer.isPlaying.value) {
            _radioPlayer.pause()
        } else {
            val station = _currentStation.value
            if (station.url.isNotEmpty()) {
                _radioPlayer.play(context, station)
                _radioPlayer.setVolume(_volume.value)
            } else if (station.type == StationType.BilibiliCategory) {
                autoSearchAndPlay(station.searchKeyword ?: "白噪音")
            }
        }
    }

    private fun autoSearchAndPlay(keyword: String) {
        searchJob?.cancel()
        _isSearching.value = true
        searchJob = viewModelScope.launch {
            val results = BilibiliApi.searchRooms(keyword)
                .filter { room ->
                    room.area !in excludeAreas &&
                    excludeTitleKeywords.none { room.title.contains(it) } &&
                    (room.title.contains(keyword))
                }
                .sortedByDescending { it.online }
            _bilibiliRooms.value = results
            _isSearching.value = false
            if (results.isNotEmpty()) {
                playBilibiliRoom(results.first().roomId, results.first())
            }
        }
    }

    fun pause() {
        _radioPlayer.pause()
    }

    fun setVolume(v: Float) {
        _volume.value = v
        _radioPlayer.setVolume(v)
        PreferencesManager.saveRadioVolume(getApplication(), v)
    }

    fun setLottieAnimationFile(fileName: String) {
        _lottieAnimationFile.value = fileName
        PreferencesManager.saveLottieAnimation(getApplication(), fileName)
    }

    override fun onCleared() {
        searchJob?.cancel()
        bilibiliPlayJob?.cancel()
        bilibiliWatchdog?.cancel()
        _radioPlayer.release()
        _countdownTimer.release()
        val audioManager = AudioManager.getInstance()
        audioManager.setOnStopRadioRequested(null)
        audioManager.setOnRadioResumeRequested(null)
    }

    fun setPlayingRoomInfo(room: BilibiliApi.LiveRoom) {
        _playingRoomInfo.value = room
        _playingRoomId.value = room.roomId
    }

    fun clearPlayingRoomInfo() {
        _playingRoomInfo.value = null
        _playingRoomId.value = null
    }

    fun togglePinRoom(roomId: String, room: BilibiliApi.LiveRoom? = null) {
        val current = _pinnedRoomIds.value.toMutableSet()
        val currentInfos = _pinnedRoomInfos.value.toMutableList()
        if (current.contains(roomId)) {
            current.remove(roomId)
            currentInfos.removeAll { it.roomId == roomId }
        } else {
            current.add(roomId)
            currentInfos.removeAll { it.roomId == roomId }
            if (room != null) {
                currentInfos.add(room)
            }
        }
        _pinnedRoomIds.value = current
        _pinnedRoomInfos.value = currentInfos.toList()
        val context = getApplication<Application>()
        PreferencesManager.saveBilibiliPinnedRooms(context, current)
        PreferencesManager.saveBilibiliPinnedRoomsInfo(context, currentInfos.toList())
    }

    fun stopBilibiliRoom() {
        bilibiliWatchdog?.cancel()
        bilibiliPlayJob?.cancel()
        _radioPlayer.stop()
        _playingRoomId.value = null
        _playingRoomInfo.value = null
        if (_currentStation.value.type == StationType.BilibiliLive) {
            _currentStation.value = RadioStation.defaultStation
            PreferencesManager.saveRadioStationId(getApplication(), RadioStation.defaultStation.id)
        }
    }

    fun isPinned(roomId: String): Boolean = _pinnedRoomIds.value.contains(roomId)

    fun syncPinnedRoomInfos(rooms: List<BilibiliApi.LiveRoom>) {
        val existingIds = _pinnedRoomInfos.value.map { it.roomId }.toSet()
        val newInfos = rooms.filter { it.roomId in _pinnedRoomIds.value && it.roomId !in existingIds }
        if (newInfos.isNotEmpty()) {
            val combined = _pinnedRoomInfos.value.toMutableList()
            combined.addAll(newInfos)
            _pinnedRoomInfos.value = combined
            PreferencesManager.saveBilibiliPinnedRoomsInfo(getApplication(), _pinnedRoomInfos.value)
        }
    }

    companion object {
        fun factory(application: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RadioViewModel(application) as T
            }
        }
    }
}
