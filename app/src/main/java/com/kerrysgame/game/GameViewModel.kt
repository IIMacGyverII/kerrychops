package com.kerrysgame.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kerrysgame.audio.SoundEffectManager
import com.kerrysgame.data.GameDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(
    private val gameDataStore: GameDataStore,
    private val soundEffectManager: SoundEffectManager
) : ViewModel() {

    private val engine = KerryGameEngine()
    private val _uiState = MutableStateFlow(engine.initialState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null

    init {
        loadSavedDataOnce()
        startGameLoop()
    }

    fun onChop() {
        val previous = _uiState.value
        val (newState, accepted) = engine.onChop(previous, System.currentTimeMillis())
        if (!accepted) {
            if (previous.started && !previous.showSummary && !previous.showGameOver) {
                _uiState.value = engine.onMiss(previous)
                persist()
            }
            return
        }

        _uiState.value = newState
        if (newState.wave > previous.wave) {
            soundEffectManager.playCrack()
            soundEffectManager.playCollect()
            if (previous.isBossTree) {
                _uiState.value = _uiState.value.copy(bossDefeatPulse = _uiState.value.bossDefeatPulse + 1)
            }
        }
        persist()
    }

    fun buyUpgrade(key: String) {
        val updated = engine.buyUpgrade(_uiState.value, key)
        if (updated != _uiState.value) {
            _uiState.value = updated
            soundEffectManager.playCollect()
            persist()
        }
    }

    fun startGame() {
        if (!_uiState.value.started) {
            _uiState.value = _uiState.value.copy(started = true)
        }
    }

    fun dismissSummary() {
        if (_uiState.value.showSummary) {
            _uiState.value = _uiState.value.copy(showSummary = false)
        }
    }

    fun retireRun() {
        val state = _uiState.value
        if (state.showGameOver) return
        _uiState.value = state.copy(
            showGameOver = true,
            started = false,
            lastRunWave = state.wave,
            lastRunChops = state.chopCount,
            lastRunWood = state.wood
        )
        persist()
    }

    fun restartRun() {
        _uiState.value = engine.resetRun(_uiState.value)
    }

    private fun loadSavedDataOnce() {
        viewModelScope.launch {
            val saved = gameDataStore.savedData.first()
            _uiState.value = engine.applySavedData(
                savedWood = saved.woodBank,
                savedHighScore = saved.highScore,
                savedTotalWood = saved.totalWood,
                savedBestCombo = saved.bestCombo,
                savedBestRunWood = saved.bestRunWood,
                upgrades = saved.toUpgradeState()
            )
        }
    }

    private fun startGameLoop() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(16L)
                val previous = _uiState.value
                val newState = engine.onTick(previous, 16L)
                _uiState.value = newState
                // Trigger swing sound when animElapsedMs crosses 240ms (halfway through 480ms animation)
                if (previous.animElapsedMs in 1L until 120L && newState.animElapsedMs >= 120L) {
                    soundEffectManager.playSwing()
                }
            }
        }
    }

    private fun persist() {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            gameDataStore.saveProgress(
                highScore = state.highScore,
                totalWood = state.totalWoodChopped,
                woodBank = state.wood,
                bestCombo = state.bestCombo,
                bestRunWood = state.bestRunWood,
                upgrades = state.upgrades
            )
        }
    }

    override fun onCleared() {
        tickJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun factory(gameDataStore: GameDataStore, soundEffectManager: SoundEffectManager): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return GameViewModel(gameDataStore, soundEffectManager) as T
                }
            }
        }
    }
}