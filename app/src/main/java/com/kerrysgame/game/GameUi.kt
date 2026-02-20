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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
    val shakePx = with(LocalDensity.current) { state.shakeStrength.dp.toPx() }
    val shakeX = if (state.shakeStrength > 0f) ((Math.random() - 0.5f) * shakePx).toFloat() else 0f
    val shakeY = if (state.shakeStrength > 0f) ((Math.random() - 0.5f) * shakePx).toFloat() else 0f

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopHud(state)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
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
            ComboBurstOverlay(state.comboBurstMs)
        }

        if (state.missFlashMs > 0) {
            MissFlashOverlay(state.missFlashMs)
        }

        AnimatedVisibility(
            visible = state.quoteVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 102.dp, start = 16.dp, end = 16.dp)
        ) {
            QuoteBubble(text = state.displayedQuote)
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
private fun TopHud(state: GameUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xCC14261F)),
            modifier = Modifier.border(2.dp, Color(0xFF2A4A38), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("Wave ${state.wave}${if (state.isBossTree) " • BOSS" else ""}", color = Color.White, fontWeight = FontWeight.Bold)
                Text("${state.seasonEvent.label}: ${state.seasonEvent.description}", color = Color(0xFFB8DDC8), style = MaterialTheme.typography.bodySmall)
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
                if (state.combo >= 10) {
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF2F1F08)) {
                        Text(
                            text = "Streak ${state.combo}",
                            color = Color(0xFFFFE2A8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text("High ${state.highScore}", color = Color(0xFFFFE7BF), style = MaterialTheme.typography.bodySmall)
                Text("x${"%.2f".format(state.comboMultiplier)} combo", color = Color(0xFFFFE7BF), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ChoppableTree(state: GameUiState, onChop: () -> Unit) {
    val healthFraction = (state.treeHealth / state.treeMaxHealth).coerceIn(0f, 1f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onChop() },
                    onPress = {
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
            val centerX = size.width / 2
            val centerY = size.height / 2 + 20f
            val visualPulse = ((kotlin.math.sin(state.backgroundScroll * 0.035f) + 1f) * 0.5f).coerceIn(0f, 1f)

            // Ground layer - grass and dirt
            drawRect(
                color = Color(0xFF2E1F12),
                topLeft = Offset(0f, centerY + 110f),
                size = androidx.compose.ui.geometry.Size(size.width, size.height - (centerY + 110f))
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x663E2B18),
                        Color(0x00201008)
                    )
                ),
                topLeft = Offset(0f, centerY + 110f),
                size = androidx.compose.ui.geometry.Size(size.width, size.height - (centerY + 110f))
            )
            // Grass top layer
            drawRect(
                color = Color(0xFF3E7A37),
                topLeft = Offset(0f, centerY + 110f),
                size = androidx.compose.ui.geometry.Size(size.width, 12f)
            )
            // Grass tufts (pixel-art style)
            repeat(18) { i ->
                val gx = i * (size.width / 18f) + (i * 7f % 20f)
                drawRect(
                    color = Color(0xFF5A9E4A),
                    topLeft = Offset(gx, centerY + 106f + (i % 3) * 2f),
                    size = androidx.compose.ui.geometry.Size(8f, 8f)
                )
            }

            // Ground ambient light strip (gives scene depth)
            drawRect(
                color = Color(0x2259A74C).copy(alpha = 0.14f + visualPulse * 0.09f),
                topLeft = Offset(0f, centerY + 102f),
                size = androidx.compose.ui.geometry.Size(size.width, 20f)
            )
            
            // Environment props - rocks
            repeat(5) { i ->
                val rockX = centerX - 280f + (i * 140f)
                val rockY = centerY + 120f + (i % 2) * 10f
                // Rock base
                drawRect(
                    color = Color(0xFF5A5A5A),
                    topLeft = Offset(rockX, rockY),
                    size = androidx.compose.ui.geometry.Size(24f + (i % 2) * 8f, 16f + (i % 3) * 6f)
                )
                // Rock highlight
                drawRect(
                    color = Color(0xFF7A7A7A),
                    topLeft = Offset(rockX + 2f, rockY + 2f),
                    size = androidx.compose.ui.geometry.Size(8f, 6f)
                )
                // Rock shadow
                drawRect(
                    color = Color(0xFF3A3A3A),
                    topLeft = Offset(rockX + 12f, rockY + 8f),
                    size = androidx.compose.ui.geometry.Size(8f, 8f)
                )
            }
            
            // Tree stumps (chopped trees)
            repeat(3) { i ->
                val stumpX = centerX + 120f + (i * 60f)
                val stumpY = centerY + 115f
                // Stump
                drawRect(
                    color = Color(0xFF5A3220),
                    topLeft = Offset(stumpX, stumpY),
                    size = androidx.compose.ui.geometry.Size(28f, 28f)
                )
                // Tree rings
                drawCircle(
                    color = Color(0xFF4A2810),
                    radius = 8f,
                    center = Offset(stumpX + 14f, stumpY + 14f)
                )
                drawCircle(
                    color = Color(0xFF3A1808),
                    radius = 4f,
                    center = Offset(stumpX + 14f, stumpY + 14f)
                )
            }
            
            // Flowers and mushrooms
            repeat(8) { i ->
                val flowerX = centerX - 200f + (i * 60f)
                val flowerY = centerY + 118f + (i % 3) * 8f
                if (i % 3 == 0) {
                    // Mushroom
                    drawRect(
                        color = Color(0xFFEEDDCC),
                        topLeft = Offset(flowerX + 3f, flowerY + 4f),
                        size = androidx.compose.ui.geometry.Size(4f, 8f)
                    )
                    drawRect(
                        color = Color(0xFFCC4422),
                        topLeft = Offset(flowerX, flowerY),
                        size = androidx.compose.ui.geometry.Size(10f, 6f)
                    )
                    // White spots
                    drawRect(
                        color = Color(0xFFFFFFFF),
                        topLeft = Offset(flowerX + 2f, flowerY + 2f),
                        size = androidx.compose.ui.geometry.Size(2f, 2f)
                    )
                    drawRect(
                        color = Color(0xFFFFFFFF),
                        topLeft = Offset(flowerX + 6f, flowerY + 2f),
                        size = androidx.compose.ui.geometry.Size(2f, 2f)
                    )
                } else {
                    // Flower
                    drawRect(
                        color = Color(0xFF3A7A3A),
                        topLeft = Offset(flowerX + 3f, flowerY + 4f),
                        size = androidx.compose.ui.geometry.Size(2f, 10f)
                    )
                    val flowerColor = if (i % 2 == 0) Color(0xFFFFDD66) else Color(0xFFFF66AA)
                    repeat(4) { petal ->
                        val px = flowerX + 2f + (petal % 2) * 4f
                        val py = flowerY + (petal / 2) * 4f
                        drawRect(
                            color = flowerColor,
                            topLeft = Offset(px, py),
                            size = androidx.compose.ui.geometry.Size(4f, 4f)
                        )
                    }
                }
            }

            // Tree trunk - textured bark
            val trunkColor = if (state.isBossTree) Color(0xFF4A2C14) else Color(0xFF6A3D1F)
            val trunkDarkColor = if (state.isBossTree) Color(0xFF3A1F0E) else Color(0xFF5A2D12)
            val trunkLightColor = if (state.isBossTree) Color(0xFF5F3A1E) else Color(0xFF7F4D2C)

            // Tree contact shadow on ground
            drawRect(
                color = Color(0x66000000),
                topLeft = Offset(centerX - 116f, centerY + 108f),
                size = androidx.compose.ui.geometry.Size(232f, 18f)
            )
            
            // Main trunk
            drawRect(
                color = trunkColor,
                topLeft = Offset(centerX - 82f, centerY - 120f),
                size = androidx.compose.ui.geometry.Size(164f, 240f)
            )
            // Bark texture - vertical lines
            repeat(8) { i ->
                val barkX = centerX - 70f + (i * 20f)
                drawRect(
                    color = trunkDarkColor,
                    topLeft = Offset(barkX, centerY - 120f),
                    size = androidx.compose.ui.geometry.Size(3f, 240f)
                )
                // Lighter highlights
                drawRect(
                    color = trunkLightColor,
                    topLeft = Offset(barkX + 6f, centerY - 110f + (i % 3) * 20f),
                    size = androidx.compose.ui.geometry.Size(2f, 30f)
                )
            }
            // Bark dither texture bands
            repeat(10) { i ->
                drawRect(
                    color = Color(0x33210F06),
                    topLeft = Offset(centerX - 80f + (i % 2) * 8f, centerY - 114f + i * 24f),
                    size = androidx.compose.ui.geometry.Size(146f, 2f)
                )
            }
            // Trunk outline (pixel-art edge)
            drawRect(
                color = Color(0xFF2A1508),
                topLeft = Offset(centerX - 84f, centerY - 122f),
                size = androidx.compose.ui.geometry.Size(2f, 244f)
            )
            drawRect(
                color = Color(0xFF2A1508),
                topLeft = Offset(centerX + 82f, centerY - 122f),
                size = androidx.compose.ui.geometry.Size(2f, 244f)
            )

            // Wood knots and texture details on trunk
            repeat(4) { i ->
                val knotY = centerY - 100f + (i * 50f)
                val knotX = centerX - 40f + (i % 2) * 50f
                // Knot
                drawCircle(
                    color = Color(0xFF3A1D08),
                    radius = 8f,
                    center = Offset(knotX, knotY)
                )
                drawCircle(
                    color = Color(0xFF2A1008),
                    radius = 4f,
                    center = Offset(knotX, knotY)
                )
            }
            
            // Boss tree special features
            if (state.isBossTree) {
                // Glowing eyes on trunk
                val eyeGlow = Color(0xFFFF4422)
                // Left eye
                drawRect(
                    color = Color(0xFF000000),
                    topLeft = Offset(centerX - 35f, centerY - 50f),
                    size = androidx.compose.ui.geometry.Size(16f, 20f)
                )
                drawRect(
                    color = eyeGlow,
                    topLeft = Offset(centerX - 33f, centerY - 48f),
                    size = androidx.compose.ui.geometry.Size(12f, 16f)
                )
                drawRect(
                    color = Color(0xFFFFAA88),
                    topLeft = Offset(centerX - 30f, centerY - 45f),
                    size = androidx.compose.ui.geometry.Size(6f, 8f)
                )
                // Right eye
                drawRect(
                    color = Color(0xFF000000),
                    topLeft = Offset(centerX + 19f, centerY - 50f),
                    size = androidx.compose.ui.geometry.Size(16f, 20f)
                )
                drawRect(
                    color = eyeGlow,
                    topLeft = Offset(centerX + 21f, centerY - 48f),
                    size = androidx.compose.ui.geometry.Size(12f, 16f)
                )
                drawRect(
                    color = Color(0xFFFFAA88),
                    topLeft = Offset(centerX + 24f, centerY - 45f),
                    size = androidx.compose.ui.geometry.Size(6f, 8f)
                )
                
                // Thorns on trunk
                repeat(8) { i ->
                    val thornX = centerX - 84f + (i % 2) * 166f
                    val thornY = centerY - 100f + (i * 25f)
                    drawRect(
                        color = Color(0xFF2A1508),
                        topLeft = Offset(thornX, thornY),
                        size = androidx.compose.ui.geometry.Size(12f, 8f)
                    )
                    drawRect(
                        color = Color(0xFF3A2010),
                        topLeft = Offset(thornX + 2f, thornY + 2f),
                        size = androidx.compose.ui.geometry.Size(6f, 4f)
                    )
                }
            }
            
            // Tree foliage - layered circles for depth
            val foliageRadius = if (state.isBossTree) 120f else 98f
            val foliageColor = if (state.isBossTree) Color(0xFF2A602A) else Color(0xFF3A7A3A)
            val foliageDark = if (state.isBossTree) Color(0xFF1F4A1F) else Color(0xFF2A5A2A)
            val foliageLight = if (state.isBossTree) Color(0xFF3A7A3A) else Color(0xFF4A9A4A)
            val foliageBright = if (state.isBossTree) Color(0xFF4A9A4A) else Color(0xFF5ABA5A)
            
            // Shadow circles
            drawCircle(
                color = foliageDark,
                radius = foliageRadius * 0.7f,
                center = Offset(centerX - 35f, centerY - 125f)
            )
            drawCircle(
                color = foliageDark,
                radius = foliageRadius * 0.65f,
                center = Offset(centerX + 40f, centerY - 135f)
            )
            // Main foliage with more layers
            drawCircle(
                color = foliageDark,
                radius = foliageRadius * 1.05f,
                center = Offset(centerX - 8f, centerY - 138f)
            )
            drawCircle(
                color = foliageColor,
                radius = foliageRadius,
                center = Offset(centerX, centerY - 140f)
            )
            // Highlight circles (lighter) - more depth
            drawCircle(
                color = foliageLight,
                radius = foliageRadius * 0.55f,
                center = Offset(centerX + 20f, centerY - 155f)
            )
            drawCircle(
                color = foliageLight,
                radius = foliageRadius * 0.4f,
                center = Offset(centerX - 25f, centerY - 150f)
            )
            drawCircle(
                color = foliageBright,
                radius = foliageRadius * 0.3f,
                center = Offset(centerX + 15f, centerY - 165f)
            )
            drawCircle(
                color = foliageBright,
                radius = foliageRadius * 0.25f,
                center = Offset(centerX - 18f, centerY - 160f)
            )
            // Leaf sparkle clusters
            repeat(14) { i ->
                val lx = centerX - 76f + (i * 12f)
                val ly = centerY - 210f + (i % 4) * 18f
                drawRect(
                    color = Color(0x66CFF38A).copy(alpha = 0.2f + visualPulse * 0.2f),
                    topLeft = Offset(lx, ly),
                    size = androidx.compose.ui.geometry.Size(3f, 3f)
                )
            }

            // Health bar with pixel-art border
            val barW = 280f
            // Border
            drawRect(
                color = Color(0xFF000000),
                topLeft = Offset(centerX - barW / 2 - 2f, centerY + 148f),
                size = androidx.compose.ui.geometry.Size(barW + 4f, 22f)
            )
            // Background
            drawRect(
                color = Color(0xFF3A1010),
                topLeft = Offset(centerX - barW / 2, centerY + 150f),
                size = androidx.compose.ui.geometry.Size(barW, 18f)
            )
            // Health fill
            drawRect(
                color = Color(0xFFFF5D52),
                topLeft = Offset(centerX - barW / 2, centerY + 150f),
                size = androidx.compose.ui.geometry.Size(barW * healthFraction, 18f)
            )
            // Health highlight
            if (healthFraction > 0.1f) {
                drawRect(
                    color = Color(0x88FFAA99),
                    topLeft = Offset(centerX - barW / 2, centerY + 150f),
                    size = androidx.compose.ui.geometry.Size(barW * healthFraction, 6f)
                )
            }

            // Kerry character - more detailed pixel-art style
            val kerryBob = kotlin.math.sin(state.backgroundScroll * 0.1f) * 1.8f
            val kerryPivot = Offset(centerX - 160f, centerY + 100f + kerryBob)
            
            // Ground shadow under Kerry
            drawRect(
                color = Color(0x44000000),
                topLeft = Offset(kerryPivot.x - 28f, centerY + 116f),
                size = androidx.compose.ui.geometry.Size(56f, 8f)
            )
            
            // Combo glow aura around Kerry
            if (state.combo >= 20) {
                val glowPulse = ((state.combo % 10) / 10f)
                val glowRadius = 55f + (glowPulse * 10f)
                val glowAlpha = (0.3f + glowPulse * 0.2f).coerceIn(0f, 0.5f)
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = glowAlpha),
                    radius = glowRadius,
                    center = Offset(kerryPivot.x, kerryPivot.y - 20f)
                )
                drawCircle(
                    color = Color(0xFFFFE44D).copy(alpha = glowAlpha * 0.6f),
                    radius = glowRadius - 8f,
                    center = Offset(kerryPivot.x, kerryPivot.y - 20f)
                )
            }
            
            // Head outline
            drawRect(
                color = Color(0xFFD0A080),
                topLeft = Offset(kerryPivot.x - 19f, kerryPivot.y - 79f),
                size = androidx.compose.ui.geometry.Size(38f, 38f)
            )
            // Head
            drawRect(
                color = Color(0xFFE5C09C),
                topLeft = Offset(kerryPivot.x - 18f, kerryPivot.y - 78f),
                size = androidx.compose.ui.geometry.Size(36f, 36f)
            )
            // Hair with more detail
            drawRect(
                color = Color(0xFF4A3420),
                topLeft = Offset(kerryPivot.x - 18f, kerryPivot.y - 82f),
                size = androidx.compose.ui.geometry.Size(36f, 14f)
            )
            // Hair highlights
            drawRect(
                color = Color(0xFF5A4430),
                topLeft = Offset(kerryPivot.x - 14f, kerryPivot.y - 80f),
                size = androidx.compose.ui.geometry.Size(4f, 8f)
            )
            drawRect(
                color = Color(0xFF5A4430),
                topLeft = Offset(kerryPivot.x + 4f, kerryPivot.y - 80f),
                size = androidx.compose.ui.geometry.Size(4f, 8f)
            )
            // Eyebrows
            drawRect(
                color = Color(0xFF3A2A1A),
                topLeft = Offset(kerryPivot.x - 12f, kerryPivot.y - 70f),
                size = androidx.compose.ui.geometry.Size(6f, 2f)
            )
            drawRect(
                color = Color(0xFF3A2A1A),
                topLeft = Offset(kerryPivot.x + 6f, kerryPivot.y - 70f),
                size = androidx.compose.ui.geometry.Size(6f, 2f)
            )
            // Eyes
            drawRect(
                color = Color(0xFF2A2A2A),
                topLeft = Offset(kerryPivot.x - 10f, kerryPivot.y - 66f),
                size = androidx.compose.ui.geometry.Size(4f, 4f)
            )
            drawRect(
                color = Color(0xFF2A2A2A),
                topLeft = Offset(kerryPivot.x + 6f, kerryPivot.y - 66f),
                size = androidx.compose.ui.geometry.Size(4f, 4f)
            )
            // Eye shine
            drawRect(
                color = Color(0xFFFFFFFF),
                topLeft = Offset(kerryPivot.x - 9f, kerryPivot.y - 65f),
                size = androidx.compose.ui.geometry.Size(2f, 2f)
            )
            drawRect(
                color = Color(0xFFFFFFFF),
                topLeft = Offset(kerryPivot.x + 7f, kerryPivot.y - 65f),
                size = androidx.compose.ui.geometry.Size(2f, 2f)
            )
            // Nose
            drawRect(
                color = Color(0xFFD0A080),
                topLeft = Offset(kerryPivot.x - 2f, kerryPivot.y - 60f),
                size = androidx.compose.ui.geometry.Size(4f, 6f)
            )
            // Beard
            drawRect(
                color = Color(0xFF3A2816),
                topLeft = Offset(kerryPivot.x - 14f, kerryPivot.y - 54f),
                size = androidx.compose.ui.geometry.Size(28f, 14f)
            )
            // Beard detail (scruffy)
            repeat(6) { i ->
                drawRect(
                    color = Color(0xFF4A3420),
                    topLeft = Offset(kerryPivot.x - 12f + (i * 5f), kerryPivot.y - 40f),
                    size = androidx.compose.ui.geometry.Size(3f, (i % 2 + 1) * 3f)
                )
            }
            
            // Torso (flannel shirt with outline)
            drawRect(
                color = Color(0xFF7F2F2F),
                topLeft = Offset(kerryPivot.x - 21f, kerryPivot.y - 43f),
                size = androidx.compose.ui.geometry.Size(42f, 58f)
            )
            drawRect(
                color = Color(0xFF8F3F3F),
                topLeft = Offset(kerryPivot.x - 20f, kerryPivot.y - 42f),
                size = androidx.compose.ui.geometry.Size(40f, 56f)
            )
            // Flannel pattern (cross-hatch with more detail)
            repeat(4) { i ->
                drawRect(
                    color = Color(0xFF6F2A2A),
                    topLeft = Offset(kerryPivot.x - 16f + (i * 12f), kerryPivot.y - 38f),
                    size = androidx.compose.ui.geometry.Size(4f, 48f)
                )
            }
            repeat(6) { i ->
                drawRect(
                    color = Color(0xFF6F2A2A),
                    topLeft = Offset(kerryPivot.x - 18f, kerryPivot.y - 36f + (i * 9f)),
                    size = androidx.compose.ui.geometry.Size(36f, 3f)
                )
            }
            // Buttons
            repeat(3) { i ->
                drawRect(
                    color = Color(0xFF2A2A2A),
                    topLeft = Offset(kerryPivot.x - 2f, kerryPivot.y - 32f + (i * 14f)),
                    size = androidx.compose.ui.geometry.Size(4f, 4f)
                )
            }
            // Tool belt
            drawRect(
                color = Color(0xFF5A3820),
                topLeft = Offset(kerryPivot.x - 22f, kerryPivot.y + 8f),
                size = androidx.compose.ui.geometry.Size(44f, 8f)
            )
            drawRect(
                color = Color(0xFF4A2810),
                topLeft = Offset(kerryPivot.x - 22f, kerryPivot.y + 14f),
                size = androidx.compose.ui.geometry.Size(44f, 2f)
            )
            // Belt buckle
            drawRect(
                color = Color(0xFFD5DDE2),
                topLeft = Offset(kerryPivot.x - 4f, kerryPivot.y + 9f),
                size = androidx.compose.ui.geometry.Size(8f, 6f)
            )
            // Arms with outline
            drawRect(
                color = Color(0xFFD0A080),
                topLeft = Offset(kerryPivot.x + 19f, kerryPivot.y - 37f),
                size = androidx.compose.ui.geometry.Size(14f, 42f)
            )
            drawRect(
                color = Color(0xFFE5C09C),
                topLeft = Offset(kerryPivot.x + 20f, kerryPivot.y - 36f),
                size = androidx.compose.ui.geometry.Size(12f, 40f)
            )
            drawRect(
                color = Color(0xFFD0A080),
                topLeft = Offset(kerryPivot.x - 33f, kerryPivot.y - 37f),
                size = androidx.compose.ui.geometry.Size(14f, 30f)
            )
            drawRect(
                color = Color(0xFFE5C09C),
                topLeft = Offset(kerryPivot.x - 32f, kerryPivot.y - 36f),
                size = androidx.compose.ui.geometry.Size(12f, 28f)
            )
            
            // Legs (jeans)
            drawRect(
                color = Color(0xFF3A4A6A),
                topLeft = Offset(kerryPivot.x - 14f, kerryPivot.y + 14f),
                size = androidx.compose.ui.geometry.Size(12f, 46f)
            )
            drawRect(
                color = Color(0xFF3A4A6A),
                topLeft = Offset(kerryPivot.x + 2f, kerryPivot.y + 14f),
                size = androidx.compose.ui.geometry.Size(12f, 46f)
            )
            // Boots
            drawRect(
                color = Color(0xFF2A1810),
                topLeft = Offset(kerryPivot.x - 16f, kerryPivot.y + 56f),
                size = androidx.compose.ui.geometry.Size(14f, 10f)
            )
            drawRect(
                color = Color(0xFF2A1810),
                topLeft = Offset(kerryPivot.x + 2f, kerryPivot.y + 56f),
                size = androidx.compose.ui.geometry.Size(14f, 10f)
            )
            // Glove on right hand for readability near axe
            drawRect(
                color = Color(0xFF6B4A2A),
                topLeft = Offset(kerryPivot.x + 28f, kerryPivot.y - 6f),
                size = androidx.compose.ui.geometry.Size(8f, 8f)
            )

            // Axe with better pixel-art design
            val axeRotation = -35f + (state.swingPhase * 95f)
            val hasFireAxe = state.upgrades.fireAxeLevel > 0
            
            // Pivot at Kerry's hand position (where he grips the handle)
            rotate(axeRotation, pivot = Offset(kerryPivot.x + 28f, kerryPivot.y + 4f)) {
                // Handle (wooden stick from hand upward to axe head)
                drawRect(
                    color = Color(0xFF5A3520),
                    topLeft = Offset(kerryPivot.x + 24f, kerryPivot.y - 64f),
                    size = androidx.compose.ui.geometry.Size(8f, 68f)
                )
                // Handle highlight
                drawRect(
                    color = Color(0xFF7A5530),
                    topLeft = Offset(kerryPivot.x + 25f, kerryPivot.y - 62f),
                    size = androidx.compose.ui.geometry.Size(3f, 64f)
                )
                // Handle grip wrap (darker band near bottom where hand grips)
                drawRect(
                    color = Color(0xFF3A2010),
                    topLeft = Offset(kerryPivot.x + 24f, kerryPivot.y - 8f),
                    size = androidx.compose.ui.geometry.Size(8f, 12f)
                )
                
                // Fire axe glow effect (positioned at blade at top)
                if (hasFireAxe) {
                    val fireGlow = ((state.wave % 20) / 20f)
                    drawRect(
                        color = Color(0xFFFF6622).copy(alpha = 0.4f + fireGlow * 0.3f),
                        topLeft = Offset(kerryPivot.x + 6f, kerryPivot.y - 92f),
                        size = androidx.compose.ui.geometry.Size(40f, 30f)
                    )
                    // Fire particles above axe head
                    repeat(3) { i ->
                        drawRect(
                            color = Color(0xFFFF8844).copy(alpha = 0.6f),
                            topLeft = Offset(kerryPivot.x + 12f + (i * 8f), kerryPivot.y - 96f - (i * 3f)),
                            size = androidx.compose.ui.geometry.Size(4f, 4f)
                        )
                    }
                }
                
                // Axe head mounting (where blade connects to handle top)
                drawRect(
                    color = Color(0xFF4A3010),
                    topLeft = Offset(kerryPivot.x + 22f, kerryPivot.y - 72f),
                    size = androidx.compose.ui.geometry.Size(12f, 8f)
                )
                
                // Axe blade (positioned at TOP of handle)
                val bladeColor = if (hasFireAxe) Color(0xFFAA6A45) else Color(0xFF8A9AA5)
                // Main blade body
                drawRect(
                    color = bladeColor,
                    topLeft = Offset(kerryPivot.x + 8f, kerryPivot.y - 88f),
                    size = androidx.compose.ui.geometry.Size(36f, 24f)
                )
                // Blade edge (lighter metal - the cutting edge)
                val edgeColor = if (hasFireAxe) Color(0xFFFFAA77) else Color(0xFFD5DDE2)
                drawRect(
                    color = edgeColor,
                    topLeft = Offset(kerryPivot.x + 8f, kerryPivot.y - 86f),
                    size = androidx.compose.ui.geometry.Size(36f, 16f)
                )
                // Blade shine highlight
                drawRect(
                    color = Color(0xFFFFFFFF),
                    topLeft = Offset(kerryPivot.x + 10f, kerryPivot.y - 84f),
                    size = androidx.compose.ui.geometry.Size(4f, 8f)
                )
                // Blade dark edge/depth (bottom shadow)
                val depthColor = if (hasFireAxe) Color(0xFF6A3A25) else Color(0xFF4A5A65)
                drawRect(
                    color = depthColor,
                    topLeft = Offset(kerryPivot.x + 8f, kerryPivot.y - 70f),
                    size = androidx.compose.ui.geometry.Size(36f, 6f)
                )
                // Backside poll blade for full axe silhouette
                drawRect(
                    color = depthColor,
                    topLeft = Offset(kerryPivot.x + 0f, kerryPivot.y - 80f),
                    size = androidx.compose.ui.geometry.Size(8f, 10f)
                )
                // Blade point/tip (triangular cutting edge)
                drawRect(
                    color = edgeColor,
                    topLeft = Offset(kerryPivot.x + 40f, kerryPivot.y - 82f),
                    size = androidx.compose.ui.geometry.Size(4f, 12f)
                )
                // Handle cap
                drawRect(
                    color = Color(0xFF2B1307),
                    topLeft = Offset(kerryPivot.x + 23f, kerryPivot.y + 2f),
                    size = androidx.compose.ui.geometry.Size(10f, 4f)
                )

            }

            // Enhanced particle effects
            state.chips.forEach { chip ->
                val chipRotation = (chip.x + chip.y) * 3f
                
                // Wood chips - blocky particles with rotation effect
                rotate(chipRotation, pivot = Offset(centerX + chip.x, centerY + chip.y)) {
                    // Wood chip (rectangular)
                    drawRect(
                        color = Color(0xFFD8B077),
                        topLeft = Offset(centerX + chip.x - 5f, centerY + chip.y - 3f),
                        size = androidx.compose.ui.geometry.Size(10f, 6f)
                    )
                    // Chip highlight
                    drawRect(
                        color = Color(0xFFEED0A0),
                        topLeft = Offset(centerX + chip.x - 4f, centerY + chip.y - 2f),
                        size = androidx.compose.ui.geometry.Size(4f, 2f)
                    )
                    // Chip shadow
                    drawRect(
                        color = Color(0xFFB08855),
                        topLeft = Offset(centerX + chip.x, centerY + chip.y + 1f),
                        size = androidx.compose.ui.geometry.Size(5f, 2f)
                    )
                }
                
                // Impact sparks (on fresh chips near tree)
                if (chip.y < 20f && kotlin.math.abs(chip.vx) > 2f) {
                    repeat(2) { i ->
                        drawRect(
                            color = Color(0xFFFFDD88).copy(alpha = 0.8f),
                            topLeft = Offset(centerX + chip.x + (i * 3f), centerY + chip.y - (i * 2f)),
                            size = androidx.compose.ui.geometry.Size(3f, 3f)
                        )
                    }
                }
            }
            
            // Falling leaves from tree foliage
            if (state.swingPhase > 0.5f) {
                repeat(4) { i ->
                    val leafX = centerX + ((i - 2) * 30f) + (state.swingPhase * 15f)
                    val leafY = centerY - 140f + (state.swingPhase * 40f) + (i * 8f)
                    // Leaf
                    drawRect(
                        color = Color(0xFF4A8A4F),
                        topLeft = Offset(leafX, leafY),
                        size = androidx.compose.ui.geometry.Size(6f, 8f)
                    )
                    drawRect(
                        color = Color(0xFF5AAA5F),
                        topLeft = Offset(leafX + 1f, leafY + 1f),
                        size = androidx.compose.ui.geometry.Size(3f, 4f)
                    )
                }
            }
            
            // Dust cloud on impact
            if (state.swingPhase > 0.7f) {
                val dustAlpha = (1f - state.swingPhase) * 1.5f
                repeat(5) { i ->
                    drawCircle(
                        color = Color(0xFFCCBBAA).copy(alpha = dustAlpha.coerceIn(0f, 0.4f)),
                        radius = 6f + (i * 2f),
                        center = Offset(centerX + (i - 2) * 12f, centerY - 20f + (i * 4f))
                    )
                }
            }

            // Swing impact flash (short, pixel style)
            if (state.swingPhase > 0.78f) {
                val flashAlpha = ((state.swingPhase - 0.78f) / 0.22f).coerceIn(0f, 1f)
                drawRect(
                    color = Color(0xFFFFE9A8).copy(alpha = (1f - flashAlpha) * 0.7f),
                    topLeft = Offset(centerX + 58f, centerY - 84f),
                    size = androidx.compose.ui.geometry.Size(14f, 14f)
                )
            }

            // Subtle vignette for focus
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0x66101410)),
                    center = Offset(centerX, centerY - 20f),
                    radius = size.minDimension * 0.85f
                ),
                topLeft = Offset.Zero,
                size = size
            )
        }
    }
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
            .fillMaxWidth()
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
    val offsetSeed = (wave % 8) * 12
    val scrollOffset = backgroundScroll % 140f
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Sky gradient with atmospheric depth
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF334D40),
                    Color(0xFF203A2E),
                    Color(0xFF15281F)
                )
            ),
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.6f)
        )

        // Horizon glow layer
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x002FA671), Color(0x443EA469), Color(0x001A3A2A))
            ),
            topLeft = Offset(0f, size.height * 0.33f),
            size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.3f)
        )

        // Tiny sky speckles for atmosphere
        repeat(26) { i ->
            val sx = (i * (size.width / 24f) + (wave % 5) * 9f) % size.width
            val sy = size.height * 0.06f + (i % 6) * 16f
            drawRect(
                color = Color(0x33D8EFE0),
                topLeft = Offset(sx, sy),
                size = androidx.compose.ui.geometry.Size(2f, 2f)
            )
        }
        
        // Clouds (very slow parallax)
        val cloudScroll = scrollOffset * 0.1f
        repeat(5) { i ->
            val cx = ((i * (size.width / 4f)) + cloudScroll) % (size.width + 120f) - 60f
            val cy = size.height * 0.15f + (i % 3) * 35f
            // Cloud puffs
            drawCircle(
                color = Color(0x33D4E8DF),
                radius = 45f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0x33D4E8DF),
                radius = 38f,
                center = Offset(cx + 30f, cy + 8f)
            )
            drawCircle(
                color = Color(0x33D4E8DF),
                radius = 42f,
                center = Offset(cx + 55f, cy)
            )
        }
        
        // Distant fog/mist layer
        drawRect(
            color = Color(0x22A8C4B8),
            topLeft = Offset(0f, size.height * 0.42f),
            size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.12f)
        )
        drawRect(
            color = Color(0x1AA6CABB),
            topLeft = Offset(0f, size.height * 0.50f),
            size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.08f)
        )
        
        // Far background mountains/hills (darkest, slowest parallax)
        val mountainScroll = scrollOffset * 0.2f
        repeat(6) { i ->
            val mx = (i * (size.width / 5f)) + mountainScroll
            val mxWrapped = (mx % (size.width + 120f)) - 60f
            // Mountain with jagged top
            drawRect(
                color = Color(0xFF1A2C20),
                topLeft = Offset(mxWrapped - 35f, size.height * 0.48f - (i % 2) * 20f),
                size = androidx.compose.ui.geometry.Size(170f, size.height * 0.52f + (i % 2) * 20f)
            )
            // Jagged peaks
            repeat(8) { j ->
                drawRect(
                    color = Color(0xFF0E1A14),
                    topLeft = Offset(mxWrapped - 30f + (j * 22f), size.height * 0.48f - (i % 2) * 20f - (j % 3) * 12f),
                    size = androidx.compose.ui.geometry.Size(8f, (j % 3) * 12f + 8f)
                )
            }
        }
        
        // Mid background trees (darker, medium parallax)
        val midScroll = scrollOffset * 0.5f
        repeat(10) { i ->
            val baseX = (i * (size.width / 10f)) + offsetSeed * 0.6f + midScroll
            val x = (baseX % (size.width + 90f)) - 45f
            val treeHeight = size.height * 0.42f
            val yOffset = size.height * 0.58f + (i % 4) * 8f
            
            // Trunk
            drawRect(
                color = Color(0x88344D32),
                topLeft = Offset(x, yOffset),
                size = androidx.compose.ui.geometry.Size(18f, treeHeight)
            )
            // Foliage - layered for depth
            drawCircle(
                color = Color(0x88275A28),
                radius = 32f + (i % 3) * 6f,
                center = Offset(x + 9f, yOffset + 8f)
            )
            drawCircle(
                color = Color(0x88326E33),
                radius = 24f + (i % 2) * 4f,
                center = Offset(x + 9f, yOffset - 4f)
            )
        }
        
        // Foreground trees (lighter, faster parallax with sway)
        repeat(14) { i ->
            val baseX = (i * (size.width / 12f)) + offsetSeed + scrollOffset
            val x = (baseX % (size.width + 80f)) - 40f
            val treeHeight = size.height * 0.45f
            val yOffset = size.height * 0.55f + (i % 3) * 6f
            
            // Subtle sway animation
            val swayAmount = kotlin.math.sin((scrollOffset + (i * 30f)) * 0.05f) * 3f
            
            // Trunk with texture (with sway applied to top)
            val trunkW = 22f
            drawRect(
                color = Color(0xDD344D32),
                topLeft = Offset(x, yOffset),
                size = androidx.compose.ui.geometry.Size(trunkW, treeHeight)
            )
            // Bark lines
            drawRect(
                color = Color(0xDD2A3D28),
                topLeft = Offset(x + 4f, yOffset),
                size = androidx.compose.ui.geometry.Size(2f, treeHeight)
            )
            drawRect(
                color = Color(0xDD2A3D28),
                topLeft = Offset(x + 14f, yOffset),
                size = androidx.compose.ui.geometry.Size(2f, treeHeight)
            )
            
            // Foliage - multiple circles for bushiness (with sway)
            val foliageRadius = 28f + (i % 3) * 8f
            drawCircle(
                color = Color(0xDD2E5A2F),
                radius = foliageRadius * 0.8f,
                center = Offset(x + trunkW / 2 - 12f + swayAmount, yOffset + 5f)
            )
            drawCircle(
                color = Color(0xDD3E7144),
                radius = foliageRadius,
                center = Offset(x + trunkW / 2 + swayAmount, yOffset)
            )
            drawCircle(
                color = Color(0xDD4A8A4F),
                radius = foliageRadius * 0.65f,
                center = Offset(x + trunkW / 2 + 8f + swayAmount, yOffset - 8f)
            )
            
            // Bush at base
            drawCircle(
                color = Color(0xDD3A6A3E),
                radius = 14f + (i % 2) * 4f,
                center = Offset(x + trunkW / 2 + (swayAmount * 0.3f), yOffset + treeHeight - 8f)
            )
        }
        
        // Foreground grass/plants (with gentle sway)
        repeat(24) { i ->
            val gx = (i * (size.width / 24f) + scrollOffset * 1.3f) % size.width
            val gy = size.height * 0.85f + (i % 4) * 12f
            val grassSway = kotlin.math.sin((scrollOffset + (i * 20f)) * 0.07f) * 2f
            // Grass blades
            drawRect(
                color = Color(0xDD4A9A4F),
                topLeft = Offset(gx + grassSway, gy),
                size = androidx.compose.ui.geometry.Size(4f, 16f + (i % 3) * 6f)
            )
            drawRect(
                color = Color(0xDD5AAA5F),
                topLeft = Offset(gx + 1f + (grassSway * 0.7f), gy),
                size = androidx.compose.ui.geometry.Size(2f, 10f)
            )
        }
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
            center = Offset(size.width * 0.5f, size.height * 0.45f)
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