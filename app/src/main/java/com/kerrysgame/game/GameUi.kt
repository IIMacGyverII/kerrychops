package com.kerrysgame.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlin.math.roundToInt

@Composable
fun KerryGameScreen(
    state: GameUiState,
    onChop: () -> Unit,
    onBuyUpgrade: (String) -> Unit,
    onStart: () -> Unit,
    onDismissSummary: () -> Unit,
    onRetire: () -> Unit,
    onRestart: () -> Unit
) {
    var shopOpen by remember { mutableStateOf(false) }
    var statsOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(state.bossDefeatPulse) {
        if (state.bossDefeatPulse > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    val shakeX = 0f
    val shakeY = 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0E2A1C), Color(0xFF204F33), Color(0xFF1C3B29))
                )
            )
            .offset { IntOffset(shakeX.roundToInt(), shakeY.roundToInt()) }
    ) {
        ForestBackdrop(state.wave, state.backgroundScroll)
        SeasonOverlay(state)

        if (state.missFlashMs > 0) {
            Box(modifier = Modifier.zIndex(30f)) {
                MissFlashOverlay(state.missFlashMs)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopHud(
                state = state,
                modifier = Modifier.zIndex(30f)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                ChoppableTree(
                    state = state,
                    onChop = onChop
                )
            }

            BottomControls(
                state = state,
                onUpgradeClick = { shopOpen = true }
            )
        }

        if (state.comboBurstMs > 0) {
            Box(modifier = Modifier.zIndex(30f)) {
                ComboBurstOverlay(state.comboBurstMs)
            }
        }

        // Render multiple quotes stacked vertically
        state.quotes.forEachIndexed { index, quote ->
            AnimatedVisibility(
                visible = true,
                modifier = Modifier
                    .zIndex(30f)
                    .align(Alignment.TopCenter)
                    .padding(top = (16 + index * 40).dp, start = 16.dp, end = 16.dp)
            ) {
                QuoteBubble(text = quote.text)
            }
        }

        if (shopOpen) {
            UpgradeShopSheet(
                state = state,
                onClose = { shopOpen = false },
                onBuyUpgrade = onBuyUpgrade,
                onRetire = onRetire,
                onShowStats = {
                    shopOpen = false
                    statsOpen = true
                }
            )
        }

        if (!state.started) {
            StartOverlay(onStart = onStart)
        }

        if (state.showSummary) {
            SummaryOverlay(state = state, onDismiss = onDismissSummary)
        }

        if (state.showGameOver) {
            GameOverOverlay(state = state, onRestart = onRestart)
        }

        if (statsOpen) {
            StatsOverlay(state = state, onClose = { statsOpen = false })
        }

        if (state.bossBannerMs > 0) {
            BossBanner()
        }
    }
}

@Composable
private fun BossBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color(0xCC3B120F)) {
            Text(
                text = "BOSS TREE INCOMING",
                color = Color(0xFFFFC7A6),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SeasonOverlay(state: GameUiState) {
    val tint = when (state.seasonEvent) {
        SeasonEvent.SPRING -> Color(0x00000000)
        SeasonEvent.SUMMER -> Color(0x22E2B85D)
        SeasonEvent.AUTUMN -> Color(0x22D47A3A)
        SeasonEvent.WINTER -> Color(0x33B7D9FF)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (tint.alpha > 0f) {
            drawRect(color = tint)
        }

        if (state.seasonEvent == SeasonEvent.WINTER) {
            val drift = (state.backgroundScroll % 200f)
            repeat(28) { i ->
                val x = (i * (size.width / 14f) + drift) % size.width
                val y = (i * 37f + drift * 1.3f) % size.height
                drawCircle(color = Color(0x88FFFFFF), radius = 3.5f, center = Offset(x, y))
            }
        }
    }
}

@Composable
private fun StartOverlay(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC0C1410)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("Kerrys Chopper", color = Color(0xFFFFD08A), style = MaterialTheme.typography.headlineLarge)
            Text("Endless chopping. Endless sarcasm.", color = Color(0xFFD6E6DB), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(14.dp))
            Text("Tap or hold to swing the axe.", color = Color(0xFFAFC4B8), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onStart) {
                Text("Start Chopping")
            }
        }
    }
}

@Composable
private fun GameOverOverlay(state: GameUiState, onRestart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD0A1110)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("Run Retired", color = Color(0xFFFFD08A), style = MaterialTheme.typography.headlineSmall)
            Text("Waves cleared: ${state.lastRunWave}", color = Color(0xFFD6E6DB), style = MaterialTheme.typography.bodyLarge)
            Text("Chops thrown: ${state.lastRunChops}", color = Color(0xFFAFC4B8), style = MaterialTheme.typography.bodyMedium)
            Text("Wood banked: ${state.lastRunWood}", color = Color(0xFFAFC4B8), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Text("Total wood: ${state.totalWoodChopped}", color = Color(0xFFD6E6DB), style = MaterialTheme.typography.bodyMedium)
            Text("High score: ${state.highScore}", color = Color(0xFFD6E6DB), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onRestart) {
                Text("Start New Run")
            }
        }
    }
}

@Composable
private fun StatsOverlay(state: GameUiState, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD30A1110)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("Kerry's Stats", color = Color(0xFFFFD08A), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))
            Text("Total wood: ${state.totalWoodChopped}", color = Color(0xFFD6E6DB), style = MaterialTheme.typography.bodyLarge)
            Text("High score: ${state.highScore}", color = Color(0xFFD6E6DB), style = MaterialTheme.typography.bodyLarge)
            Text("Best combo: ${state.bestCombo}", color = Color(0xFFAFC4B8), style = MaterialTheme.typography.bodyMedium)
            Text("Best run wood: ${state.bestRunWood}", color = Color(0xFFAFC4B8), style = MaterialTheme.typography.bodyMedium)
            Text("Current wave: ${state.wave}", color = Color(0xFFAFC4B8), style = MaterialTheme.typography.bodyMedium)
            Text("Total chops: ${state.chopCount}", color = Color(0xFFAFC4B8), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onClose) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun TopHud(state: GameUiState, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xCC14261F)),
            modifier = Modifier.border(2.dp, Color(0xFF2A4A38), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("Wave ${state.wave}${if (state.isBossTree) " • BOSS" else ""}", color = Color.White, fontWeight = FontWeight.Bold)
                Text("${state.seasonEvent.label}: ${state.seasonEvent.description}", color = Color(0xFFB8DDC8), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("Lifted white '78 Jeep • blue side letters", color = Color(0xFF9CC3FF), style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xCC3D2D10)),
            modifier = Modifier.border(2.dp, Color(0xFF6A4A28), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text("Wood ${state.wood}", color = Color(0xFFFFD08A), fontWeight = FontWeight.Bold)
                // Always reserve streak height — transparent when combo < 10 so card never resizes
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (state.combo >= 10) Color(0xFF2F1F08) else Color.Transparent
                ) {
                    Text(
                        text = if (state.combo >= 10) "Streak ${state.combo}" else "Streak 0",
                        color = if (state.combo >= 10) Color(0xFFFFE2A8) else Color.Transparent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text("High ${state.highScore}", color = Color(0xFFFFE7BF), style = MaterialTheme.typography.bodySmall)
                Text("x${"%.2f".format(state.comboMultiplier)} combo", color = Color(0xFFFFE7BF), style = MaterialTheme.typography.bodySmall)

                // Active upgrades — only show owned ones
                val u = state.upgrades
                data class UpgradeLine(val label: String)
                val activeUpgrades = buildList {
                    if (u.axeSpeedLevel > 0) add(UpgradeLine("Quicker Axe Lv${u.axeSpeedLevel} -${u.axeSpeedLevel * 22}ms cd"))
                    if (u.axePowerLevel > 0) add(UpgradeLine("Heavier Head Lv${u.axePowerLevel} +${"%.1f".format(u.axePowerLevel * 2.4f)}dmg"))
                    if (u.autoChopLevel > 0) add(UpgradeLine("Stronger Arms Lv${u.autoChopLevel} +${"%.1f".format(u.autoChopLevel * 0.8f)}dps for 3s"))
                    if (u.luckyWoodLevel > 0) add(UpgradeLine("Wood Magnet Lv${u.luckyWoodLevel} ${u.luckyWoodLevel * 10}% bonus"))
                    if (u.fireAxeLevel > 0) add(UpgradeLine("Fire Axe Lv${u.fireAxeLevel} ${u.fireAxeLevel * 2}burn/0.35s for 3s"))
                    if (u.doubleChopLevel > 0) add(UpgradeLine("Double Chop Lv${u.doubleChopLevel} ${((0.15f + u.doubleChopLevel * 0.03f) * 100).toInt()}% chance"))
                }
                if (activeUpgrades.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    activeUpgrades.forEach { up ->
                        Text(up.label, color = Color(0xFFB8DDC8), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoppableTree(state: GameUiState, onChop: () -> Unit) {
    val healthFraction = (state.treeHealth / state.treeMaxHealth).coerceIn(0f, 1f)
    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()
    val treeSpec = SpriteSheetCatalog.tree
    val stumpSpec = SpriteSheetCatalog.treeStump
    val effectsSpec = SpriteSheetCatalog.effects
    val treeBitmap = remember(context) { SpriteSheetSupport.loadImageBitmap(context, treeSpec) }
    val stumpBitmap = remember(context) { SpriteSheetSupport.loadImageBitmap(context, stumpSpec) }
    val effectsBitmap = remember(context) { SpriteSheetSupport.loadImageBitmap(context, effectsSpec) }
    val swingFolder = "sprites/lumberjackswings"
    val swingFrameNames = remember(context) { 
        SpriteSheetSupport.listAssets(context, swingFolder)
    }
    val swingCache = remember { mutableStateMapOf<Int, ImageBitmap>() }
    var showWarmupOverlay by remember { mutableStateOf(swingFrameNames.isNotEmpty()) }
    val firstSwingFrame = remember(context, swingFrameNames) {
        swingFrameNames.firstOrNull()?.let { firstFrameName ->
            SpriteSheetSupport.loadFrame(
                context = context,
                assetPath = "$swingFolder/$firstFrameName",
                targetHeightPx = 360,
                removeWhite = true
            )
        }
    }

    LaunchedEffect(swingFrameNames) {
        if (swingFrameNames.isEmpty()) return@LaunchedEffect
        if (swingCache.size > 5) return@LaunchedEffect

        // Load frames in parallel batches of 10
        val batchSize = 10
        for (batchStart in swingFrameNames.indices step batchSize) {
            val batchEnd = minOf(batchStart + batchSize, swingFrameNames.size)
            val jobs = (batchStart until batchEnd).map { index ->
                async(Dispatchers.IO) {
                    if (swingCache.containsKey(index)) {
                        return@async index
                    }
                    val frameName = swingFrameNames[index]
                    val frame = SpriteSheetSupport.loadFrame(
                        context = context,
                        assetPath = "$swingFolder/$frameName",
                        targetHeightPx = 360,
                        removeWhite = true
                    )
                    if (frame != null) {
                        swingCache[index] = frame
                    }
                    index
                }
            }
            jobs.awaitAll()
        }
    }

    LaunchedEffect(swingFrameNames, swingCache.size) {
        if (!showWarmupOverlay) return@LaunchedEffect
        if (swingFrameNames.isEmpty()) {
            showWarmupOverlay = false
            return@LaunchedEffect
        }

        val threshold = minOf(18, swingFrameNames.size)
        if (swingCache.size >= threshold) {
            delay(450)
            showWarmupOverlay = false
        }
    }

    // Fallback: timeout after 3 seconds to ensure overlay dismisses
    LaunchedEffect(Unit) {
        delay(3000)
        if (showWarmupOverlay) {
            showWarmupOverlay = false
        }
    }

    // Capture canvas size exactly once from the View measurement system.
    // This is immune to Compose recomposition races (swingCache loads, combo changes, etc.)
    val stableW = remember { mutableStateOf(0f) }
    val stableH = remember { mutableStateOf(0f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .onSizeChanged { newSize ->
                if (stableW.value == 0f && newSize.width > 0) stableW.value = newSize.width.toFloat()
                if (stableH.value == 0f && newSize.height > 0) stableH.value = newSize.height.toFloat()
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!showWarmupOverlay) {
                            onChop()
                        }
                    },
                    onPress = {
                        if (showWarmupOverlay) {
                            tryAwaitRelease()
                            return@detectTapGestures
                        }
                        var keepHolding = true
                        val holdJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            while (keepHolding) {
                                onChop()
                                kotlinx.coroutines.delay(130L)
                            }
                        }
                        tryAwaitRelease()
                        keepHolding = false
                        holdJob.cancel()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Use the View-measured locked dimensions; fallback to draw-scope size only on first frame
            val layoutWidth = if (stableW.value > 0f) stableW.value else size.width
            val layoutHeight = if (stableH.value > 0f) stableH.value else size.height
            val centerX = layoutWidth / 2
            val centerY = layoutHeight / 2 + 20f
            val sceneOffsetX = layoutWidth * 0.15f
            val sceneOffsetY = layoutHeight * 0.5f
            val spriteScaleMultiplier = 1.5f
            val kerryExtraOffsetX = layoutWidth * 0.02f
            val kerryExtraOffsetY = -layoutHeight * 0.15f
            val treeExtraOffsetX = layoutWidth * 0.05f
            val treeExtraOffsetY = layoutHeight * 0.2f
            val treeSizeBoost = 1.3f
            val timerGaugeOffsetX = layoutWidth * 0.22f
            val timerGaugeOffsetY = -layoutHeight * 0.05f
            val effectsOffsetX = layoutWidth * 0.22f
            val effectsOffsetY = layoutHeight * 0.42f

            repeat(3) { i ->
                val stumpX = centerX + 120f + (i * 60f)
                val stumpY = centerY + 115f
                if (stumpBitmap != null) {
                    val stumpAspect = stumpSpec.frameWidth.toFloat() / stumpSpec.frameHeight.toFloat()
                    val stumpHeight = 34f
                    val stumpWidth = stumpHeight * stumpAspect
                    drawSpriteFrame(
                        bitmap = stumpBitmap,
                        frameWidth = stumpSpec.frameWidth,
                        frameHeight = stumpSpec.frameHeight,
                        frameIndex = 0,
                        dstTopLeft = Offset(stumpX, stumpY),
                        dstSize = androidx.compose.ui.geometry.Size(stumpWidth, stumpHeight)
                    )
                }
            }

            if (treeBitmap != null) {
                val treeAspect = treeSpec.frameWidth.toFloat() / treeSpec.frameHeight.toFloat()
                val scale = 2.16f * spriteScaleMultiplier * treeSizeBoost
                val treeHeight = treeSpec.frameHeight * scale
                val treeWidth = treeHeight * treeAspect
                val treeTop = (centerY + 230f) - treeHeight + sceneOffsetY + treeExtraOffsetY
                val treeLeft = centerX - treeWidth / 2f + 60f + sceneOffsetX + treeExtraOffsetX
                drawSpriteFrame(
                    bitmap = treeBitmap,
                    frameWidth = treeSpec.frameWidth,
                    frameHeight = treeSpec.frameHeight,
                    frameIndex = 0,
                    dstTopLeft = Offset(treeLeft, treeTop),
                    dstSize = androidx.compose.ui.geometry.Size(treeWidth, treeHeight)
                )
            }

            // Health bar with pixel-art border
            val barW = 280f
            // Border
            drawRect(
                color = Color(0xFF000000),
                topLeft = Offset(centerX - barW / 2 - 2f + timerGaugeOffsetX, centerY + 148f + timerGaugeOffsetY),
                size = androidx.compose.ui.geometry.Size(barW + 4f, 22f)
            )
            // Background
            drawRect(
                color = Color(0xFF3A1010),
                topLeft = Offset(centerX - barW / 2 + timerGaugeOffsetX, centerY + 150f + timerGaugeOffsetY),
                size = androidx.compose.ui.geometry.Size(barW, 18f)
            )
            // Health fill
            drawRect(
                color = Color(0xFFFF5D52),
                topLeft = Offset(centerX - barW / 2 + timerGaugeOffsetX, centerY + 150f + timerGaugeOffsetY),
                size = androidx.compose.ui.geometry.Size(barW * healthFraction, 18f)
            )
            // Health highlight
            if (healthFraction > 0.1f) {
                drawRect(
                    color = Color(0x88FFAA99),
                    topLeft = Offset(centerX - barW / 2 + timerGaugeOffsetX, centerY + 150f + timerGaugeOffsetY),
                    size = androidx.compose.ui.geometry.Size(barW * healthFraction, 6f)
                )
            }

            // Kerry character - more detailed pixel-art style
            val kerryBob = kotlin.math.sin(state.backgroundScroll * 0.1f) * 1.8f
            val kerryPivot = Offset(
                centerX - 160f + sceneOffsetX + kerryExtraOffsetX,
                centerY + 100f + kerryBob + sceneOffsetY + kerryExtraOffsetY
            )
            
            // Ground shadow under Kerry
            drawRect(
                color = Color(0x44000000),
                topLeft = Offset(kerryPivot.x - 28f, kerryPivot.y + 16f),
                size = androidx.compose.ui.geometry.Size(56f, 8f)
            )
            
            val swingFrameCount = swingFrameNames.size
            // Decoupled time-based frame selection: animElapsedMs drives which frame to show.
            // animElapsedMs runs from 0..2400 ms independently of swingPhase, so all 145 frames
            // are sampled at ~60 fps during each 2.4 s animation cycle.
            val ANIM_TOTAL_MS = 480L
            val isAnimating = state.swingPhase > 0f || state.animElapsedMs > 0L
            val swingIndex = if (swingFrameCount > 0 && isAnimating) {
                ((state.animElapsedMs * swingFrameCount) / ANIM_TOTAL_MS).toInt()
                    .coerceIn(0, swingFrameCount - 1)
            } else if (swingFrameCount > 0) {
                0
            } else {
                -1
            }
            val swingBitmap = if (swingIndex >= 0) {
                // Only use cached frames, no on-demand loading
                swingCache[swingIndex]
                    ?: swingCache[0]
                    ?: firstSwingFrame
            } else {
                null
            }

            if (swingBitmap != null) {
                val swingHeight = 360f * spriteScaleMultiplier
                val swingWidth = swingHeight * (swingBitmap.width.toFloat() / swingBitmap.height.toFloat())
                val swingTop = (kerryPivot.y + 100f) - swingHeight
                val swingLeft = kerryPivot.x - swingWidth * 0.5f + 6f
                drawImage(
                    image = swingBitmap,
                    dstOffset = IntOffset(swingLeft.roundToInt(), swingTop.roundToInt()),
                    dstSize = IntSize(swingWidth.roundToInt(), swingHeight.roundToInt())
                )
            }

            // Enhanced particle effects
            val effectsFramesPerRow = effectsBitmap?.let { (it.width / effectsSpec.frameWidth).coerceAtLeast(1) } ?: 1

            // Falling leaves from tree foliage
            if (state.swingPhase > 0.5f) {
                repeat(4) { i ->
                    val leafX = centerX + effectsOffsetX + ((i - 2) * 30f) + (state.swingPhase * 15f)
                    val leafY = centerY - 140f + effectsOffsetY + (state.swingPhase * 40f) + (i * 8f)
                    if (effectsBitmap != null && effectsFramesPerRow > 0) {
                        val leafFrame = ((state.swingPhase * 10f).toInt() + i) % effectsFramesPerRow
                        val leafIndex = (effectsFramesPerRow * 3) + leafFrame
                        val leafSize = 26f
                        drawRect(
                            color = Color(0xFFB8E89C).copy(alpha = 0.35f),
                            topLeft = Offset(leafX - 4f, leafY - 4f),
                            size = androidx.compose.ui.geometry.Size(leafSize + 8f, leafSize + 8f)
                        )
                        drawSpriteFrame(
                            bitmap = effectsBitmap,
                            frameWidth = effectsSpec.frameWidth,
                            frameHeight = effectsSpec.frameHeight,
                            frameIndex = leafIndex,
                            dstTopLeft = Offset(leafX, leafY),
                            dstSize = androidx.compose.ui.geometry.Size(leafSize, leafSize)
                        )
                    }
                }
            }

            // Dust cloud on impact
            if (state.swingPhase > 0.7f) {
                if (effectsBitmap != null && effectsFramesPerRow > 0) {
                    val dustFrame = ((state.swingPhase * 12f).toInt() % effectsFramesPerRow)
                    val dustIndex = (effectsFramesPerRow * 2) + dustFrame
                    val dustSize = 64f
                    drawRect(
                        color = Color(0xFFFFE8C4).copy(alpha = 0.35f),
                        topLeft = Offset(centerX + effectsOffsetX - dustSize * 0.55f, centerY - 34f + effectsOffsetY),
                        size = androidx.compose.ui.geometry.Size(dustSize * 1.1f, dustSize * 0.9f)
                    )
                    drawSpriteFrame(
                        bitmap = effectsBitmap,
                        frameWidth = effectsSpec.frameWidth,
                        frameHeight = effectsSpec.frameHeight,
                        frameIndex = dustIndex,
                        dstTopLeft = Offset(centerX + effectsOffsetX - dustSize * 0.5f, centerY - 30f + effectsOffsetY),
                        dstSize = androidx.compose.ui.geometry.Size(dustSize, dustSize)
                    )
                }
            }

            // Damage numbers floating up from center
            state.damageNumbers.forEach { dmg ->
                val alpha = (dmg.lifeMs / 1000f).coerceIn(0f, 1f)
                val displayX = centerX + effectsOffsetX + dmg.x + dmg.jitterX
                val displayY = centerY + effectsOffsetY + dmg.y + dmg.jitterY
                val text = dmg.damage.toString()

                val outlineStyle = TextStyle(
                    color = Color(0xFFFFD700).copy(alpha = alpha),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    drawStyle = Stroke(width = 2f)
                )
                val fillStyle = TextStyle(
                    color = Color(0xFFFF6B6B).copy(alpha = alpha),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = text,
                    topLeft = Offset(displayX - 12f, displayY - 14f),
                    style = outlineStyle
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = text,
                    topLeft = Offset(displayX - 12f, displayY - 14f),
                    style = fillStyle
                )
            }
        }

        if (showWarmupOverlay) {
            val loaded = swingCache.size
            val total = swingFrameNames.size.coerceAtLeast(1)
            val pct = ((loaded.toFloat() / total.toFloat()) * 100f).toInt().coerceIn(0, 100)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC08110E))
                    .zIndex(40f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text("Loading Kerry...", color = Color(0xFFFFD08A), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Preparing chop animation: $pct%", color = Color(0xFFD6E6DB), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Tip: Tap or hold to swing when ready.", color = Color(0xFFAFC4B8), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun DrawScope.drawSpriteFrame(
    bitmap: ImageBitmap,
    frameWidth: Int,
    frameHeight: Int,
    frameIndex: Int,
    dstTopLeft: Offset,
    dstSize: androidx.compose.ui.geometry.Size
) {
    val safeFrameWidth = if (bitmap.width % frameWidth == 0) frameWidth else bitmap.width
    val safeFrameHeight = if (bitmap.height % frameHeight == 0) frameHeight else bitmap.height
    val framesPerRow = (bitmap.width / safeFrameWidth).coerceAtLeast(1)
    val framesPerColumn = (bitmap.height / safeFrameHeight).coerceAtLeast(1)
    val framesTotal = (framesPerRow * framesPerColumn).coerceAtLeast(1)
    val safeIndex = frameIndex.coerceIn(0, framesTotal - 1)
    val srcX = (safeIndex % framesPerRow) * safeFrameWidth
    val srcY = (safeIndex / framesPerRow) * safeFrameHeight
    drawImage(
        image = bitmap,
        srcOffset = IntOffset(srcX, srcY),
        srcSize = IntSize(safeFrameWidth, safeFrameHeight),
        dstOffset = IntOffset(dstTopLeft.x.roundToInt(), dstTopLeft.y.roundToInt()),
        dstSize = IntSize(dstSize.width.roundToInt(), dstSize.height.roundToInt())
    )
}

@Composable
private fun BottomControls(state: GameUiState, onUpgradeClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xCC111A16)),
            modifier = Modifier.border(2.dp, Color(0xFF2A3A32), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Daily Challenge", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    "Chop 500 logs without missing: ${state.dailyChallengeProgress}/${state.dailyChallengeTarget}",
                    color = Color(0xFFBFDACB),
                    style = MaterialTheme.typography.bodySmall
                )
                if (state.dailyChallengeDone) {
                    Text("Done. You can now brag to exactly nobody.", color = Color(0xFFFFD08A), style = MaterialTheme.typography.bodySmall)
                } else {
                    // Reserve same height so layout doesn't shift when done text appears
                    Text(" ", color = Color.Transparent, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Button(
            onClick = onUpgradeClick,
            modifier = Modifier.border(2.dp, Color(0xFF4A6A58), RoundedCornerShape(4.dp))
        ) {
            Text("Upgrades")
        }
    }
}

@Composable
private fun QuoteBubble(text: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xEE101010),
        tonalElevation = 2.dp,
        modifier = Modifier
            .border(1.dp, Color(0xFF6E6E6E), RoundedCornerShape(14.dp))
    ) {
        Text(
            text = "Kerry: \"$text\"",
            color = Color(0xFFF4F4F4),
            modifier = Modifier.padding(12.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ForestBackdrop(wave: Int, backgroundScroll: Float) {
    val context = LocalContext.current
    val backgroundSpec = SpriteSheetCatalog.background
    val backgroundBitmap = remember(context) { SpriteSheetSupport.loadImageBitmap(context, backgroundSpec) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (backgroundBitmap == null) {
            drawRect(color = Color.Black, size = size)
            return@Canvas
        }

        val srcWidth = backgroundBitmap.width
        val srcHeight = backgroundBitmap.height
        val sourceAspect = srcWidth.toFloat() / srcHeight.toFloat()
        val targetAspect = size.width / size.height

        val baseSrcWidth: Int
        val baseSrcHeight: Int
        if (sourceAspect > targetAspect) {
            baseSrcHeight = srcHeight
            baseSrcWidth = (srcHeight * targetAspect).toInt().coerceAtMost(srcWidth)
        } else {
            baseSrcWidth = srcWidth
            baseSrcHeight = (srcWidth / targetAspect).toInt().coerceAtMost(srcHeight)
        }

        val maxPan = (srcWidth - baseSrcWidth).coerceAtLeast(0)
        val pan = if (maxPan > 0) {
            ((backgroundScroll * 0.35f).toInt() % (maxPan + 1)).coerceIn(0, maxPan)
        } else {
            0
        }
        val srcX = ((srcWidth - baseSrcWidth) / 2 + pan).coerceIn(0, srcWidth - baseSrcWidth)
        val srcY = ((srcHeight - baseSrcHeight) / 2).coerceAtLeast(0)

        drawImage(
            image = backgroundBitmap,
            srcOffset = IntOffset(srcX, srcY),
            srcSize = IntSize(baseSrcWidth, baseSrcHeight),
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize(size.width.toInt(), size.height.toInt())
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpgradeShopSheet(
    state: GameUiState,
    onClose: () -> Unit,
    onBuyUpgrade: (String) -> Unit,
    onRetire: () -> Unit,
    onShowStats: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(Unit) { }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF192A22)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(430.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Kerry's Upgrade Shop", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text("Wood: ${state.wood}", color = Color(0xFFFFD08A))
            Spacer(Modifier.height(12.dp))

            shopUpgrades.forEach { upgrade ->
                val level = when (upgrade.key) {
                    "axe_speed" -> state.upgrades.axeSpeedLevel
                    "axe_power" -> state.upgrades.axePowerLevel
                    "auto_chop" -> state.upgrades.autoChopLevel
                    "lucky_wood" -> state.upgrades.luckyWoodLevel
                    "fire_axe" -> state.upgrades.fireAxeLevel
                    "double_chop" -> state.upgrades.doubleChopLevel
                    else -> 0
                }
                val cost = (upgrade.baseCost * Math.pow(upgrade.costScale.toDouble(), level.toDouble())).toInt()

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC243A31)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${upgrade.title} (Lv $level)", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(upgrade.description, color = Color(0xFFCBE1D5), style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { onBuyUpgrade(upgrade.key) }, enabled = state.wood >= cost) {
                            Text("Buy $cost")
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0x664F705F), modifier = Modifier.padding(vertical = 8.dp))
            Text("Achievements", color = Color.White, fontWeight = FontWeight.SemiBold)
            state.achievements.forEach { achievement ->
                Text(
                    text = if (achievement.unlocked) "✔ ${achievement.title}: ${achievement.sarcasticDescription}" else "⬜ ${achievement.title}: ???",
                    color = if (achievement.unlocked) Color(0xFFFFD08A) else Color(0xFFAFC4B8),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(onClick = onShowStats) {
                Text("View Stats")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetire, enabled = state.started) {
                Text("Retire This Run")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SummaryOverlay(state: GameUiState, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC0C1410)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("Wave ${state.summaryWave} cleared", color = Color(0xFFFFD08A), style = MaterialTheme.typography.headlineSmall)
            Text("Wood gained: ${state.summaryWoodGained}", color = Color(0xFFD6E6DB), style = MaterialTheme.typography.bodyLarge)
            Text("Total wood: ${state.totalWoodChopped}", color = Color(0xFFAFC4B8), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Text("Kerry: \"${state.summaryMessage}\"", color = Color(0xFFF4F4F4), textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onDismiss) {
                Text("Keep Chopping")
            }
        }
    }
}

@Composable
private fun ComboBurstOverlay(comboBurstMs: Int) {
    val alpha = (comboBurstMs / 420f).coerceIn(0f, 1f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color(0x55FFF2B0).copy(alpha = alpha),
            radius = size.minDimension * (0.12f + (1f - alpha) * 0.2f),
            center = Offset(size.width * 1.0f, size.height * 0.62f)
        )
    }
}

@Composable
private fun MissFlashOverlay(missFlashMs: Int) {
    val alpha = (missFlashMs / 650f).coerceIn(0f, 1f)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xCC2A0B0B).copy(alpha = alpha)) {
            Text(
                text = "Miss! Combo reset",
                color = Color(0xFFFFB6A8),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}