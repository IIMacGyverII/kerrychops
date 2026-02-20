package com.kerrysgame

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kerrysgame.audio.SoundEffectManager
import com.kerrysgame.data.GameDataStore
import com.kerrysgame.game.GameViewModel
import com.kerrysgame.game.KerryGameScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var introCompleted by remember { mutableStateOf(false) }
            var showIntro by remember { mutableStateOf(true) }
            val introAlpha by animateFloatAsState(
                targetValue = if (introCompleted) 0f else 1f,
                animationSpec = tween(durationMillis = 300),
                finishedListener = { animatedValue ->
                    if (introCompleted && animatedValue == 0f) {
                        showIntro = false
                    }
                },
                label = "intro-crossfade"
            )

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

            Box(modifier = Modifier.fillMaxSize()) {
                KerryGameScreen(
                    state = state,
                    onChop = viewModel::onChop,
                    onBuyUpgrade = viewModel::buyUpgrade,
                    onStart = viewModel::startGame,
                    onDismissSummary = viewModel::dismissSummary,
                    onRetire = viewModel::retireRun,
                    onRestart = viewModel::restartRun
                )

                if (showIntro) {
                    IntroVideoSplash(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = introAlpha },
                        onFinished = { introCompleted = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroVideoSplash(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    DisposableEffect(activity) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                VideoView(viewContext).apply {
                    setVideoURI(
                        Uri.parse("android.resource://${viewContext.packageName}/${R.raw.kerrychopswoodintro}")
                    )
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = false
                        start()
                    }
                    setOnCompletionListener { onFinished() }
                    setOnErrorListener { _, _, _ ->
                        onFinished()
                        true
                    }
                    setOnTouchListener { _, _ -> true }
                }
            }
        )
    }
}