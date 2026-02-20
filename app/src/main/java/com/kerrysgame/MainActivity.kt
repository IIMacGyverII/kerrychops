package com.kerrysgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kerrysgame.audio.SoundEffectManager
import com.kerrysgame.data.GameDataStore
import com.kerrysgame.game.GameViewModel
import com.kerrysgame.game.KerryGameScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appContext = applicationContext
            val dataStore = remember { GameDataStore(appContext) }
            val soundManager = remember { SoundEffectManager(appContext) }

            DisposableEffect(Unit) {
                onDispose { soundManager.release() }
            }

            val viewModel: GameViewModel = viewModel(
                factory = GameViewModel.factory(
                    gameDataStore = dataStore,
                    soundEffectManager = soundManager
                )
            )
            val state by viewModel.uiState.collectAsState()

            KerryGameScreen(
                state = state,
                onChop = viewModel::onChop,
                onBuyUpgrade = viewModel::buyUpgrade,
                onStart = viewModel::startGame,
                onDismissSummary = viewModel::dismissSummary,
                onRetire = viewModel::retireRun,
                onRestart = viewModel::restartRun
            )
        }
    }
}