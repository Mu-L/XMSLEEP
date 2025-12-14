# 最近播放功能修复说明

## 问题描述
最近播放功能应该只播放用户**当前正在播放的音频**，而不是累积所有播放过的音频。用户主动关闭的音频不应该出现在最近播放中。

## 用户期望
- 用户播放了音频A、B、C
- 用户主动停止了音频A
- 此时只有B、C在播放
- 最近播放应该只恢复B、C，而不包括A

## 修复方案

### 核心策略：在状态变化时保存当前播放列表

1. **单个音频暂停/停止时保存**
   - `AudioManager.pauseSound()` - 单个本地声音暂停后保存
   - `AudioManager.pauseRemoteSound()` - 单个远程声音暂停后保存
   - `LocalAudioPlayer.stopAudio()` - 单个本地音频文件停止后保存
   - 保存的是**剩余正在播放的音频**，不包括刚停止的

2. **所有音频暂停时保存**
   - `AudioManager.pauseAllSounds()` - 暂停所有音频时保存
   - 保存当前正在播放的所有音频

3. **所有音频停止时不保存**
   - `AudioManager.stopAllSounds()` - 用户主动停止所有音频时不保存
   - 避免清空最近播放记录

4. **应用进入后台时保存**
   - `MainActivity.onStop()` - 应用进入后台时保存
   - 保存当前正在播放的所有音频

5. **空列表保护**
   - 如果当前没有正在播放的音频，不会覆盖之前的记录
   - 添加详细日志输出，方便调试

## 修改的文件

1. `app/src/main/kotlin/org/xmsleep/app/audio/AudioManager.kt`
   - 在 `pauseSound()` 方法中添加 `saveRecentPlayingSounds()` 调用
   - 在 `pauseRemoteSound()` 方法中添加 `saveRecentPlayingSounds()` 调用
   - 在 `stopAllSounds()` 方法中移除 `saveRecentPlayingSounds()` 调用
   - 在 `saveRecentPlayingSounds()` 方法中添加空列表保护和日志

2. `app/src/main/kotlin/org/xmsleep/app/audio/LocalAudioPlayer.kt`
   - 在 `stopAudio()` 方法中添加 `saveRecentPlayingSounds()` 调用

## 测试场景

### 场景1：部分停止
1. 播放音频A、B、C
2. 停止音频A
3. 退出应用
4. 重新打开应用
5. 点击"最近播放"
6. **预期**：只播放B、C

### 场景2：全部停止
1. 播放音频A、B、C
2. 停止所有音频
3. 退出应用
4. 重新打开应用
5. **预期**：仍然显示"最近播放"弹窗（保留上次的记录）
6. 点击"最近播放"
7. **预期**：播放A、B、C（上次停止前的状态）

### 场景3：正常退出
1. 播放音频A、B、C
2. 直接退出应用（不停止音频）
3. 重新打开应用
4. 点击"最近播放"
5. **预期**：播放A、B、C

## 预期行为
- 最近播放记录的是用户**当前正在播放的音频列表**
- 用户主动停止某个音频后，该音频会从最近播放列表中移除
- 用户停止所有音频后，不会清空最近播放记录（保留上次的状态）
- 重新打开应用时，点击"最近播放"会播放上次正在播放的音频
