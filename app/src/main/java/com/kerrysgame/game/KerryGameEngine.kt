package com.kerrysgame.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Pure gameplay engine for Kerry's endless chopper loop.
 *
 * Responsibilities:
 * - Chop/tick simulation
 * - Wave + boss progression
 * - Seasonal modifiers
 * - Upgrade effects
 * - Quotes, combos, particles, achievements
 */
class KerryGameEngine {

    private var nextParticleId = 0L
    private var lastChopTs = 0L
    private var comboDecayAccumulator = 0L
    private var fireDamageAccumulator = 0L

    fun applySavedData(
        savedWood: Int,
        savedHighScore: Long,
        savedTotalWood: Long,
        savedBestCombo: Int,
        savedBestRunWood: Int,
        upgrades: UpgradeState
    ): GameUiState {
        return GameUiState(
            wood = savedWood,
            highScore = savedHighScore,
            totalWoodChopped = savedTotalWood,
            bestCombo = savedBestCombo,
            bestRunWood = savedBestRunWood,
            upgrades = upgrades,
            achievements = baseAchievements()
        )
    }

    fun initialState(): GameUiState = GameUiState(achievements = baseAchievements())

    /**
     * Advances non-input simulation such as auto-chop, burn damage, animation decay and particles.
     */
    fun onTick(state: GameUiState, deltaMs: Long): GameUiState {
        var updated = state

        if (!state.started) {
            return state.copy(backgroundScroll = (state.backgroundScroll + (deltaMs * 0.03f)) % 10000f)
        }

        comboDecayAccumulator += deltaMs
        fireDamageAccumulator += deltaMs

        if (comboDecayAccumulator >= 700) {
            comboDecayAccumulator = 0
            if (updated.combo > 0) {
                updated = updated.copy(
                    combo = max(0, updated.combo - 1),
                    comboMultiplier = 1f + (max(0, updated.combo - 1) * 0.05f)
                )
            }
        }

        val autoDamage = updated.upgrades.autoChopLevel * 0.32f * (deltaMs / 100f)
        if (autoDamage > 0f) {
            updated = applyDamage(updated, autoDamage, isManual = false)
        }

        if (updated.upgrades.fireAxeLevel > 0 && fireDamageAccumulator >= 350) {
            fireDamageAccumulator = 0
            val burn = updated.upgrades.fireAxeLevel * 0.9f
            updated = applyDamage(updated, burn, isManual = false)
        }

        if (updated.shakeStrength > 0f) {
            updated = updated.copy(shakeStrength = (updated.shakeStrength - 0.8f).coerceAtLeast(0f))
        }

        if (updated.swingPhase > 0f) {
            updated = updated.copy(swingPhase = (updated.swingPhase - 0.08f).coerceAtLeast(0f))
        }

        val chipped = updated.chips.mapNotNull { chip ->
            val remaining = chip.lifeMs - deltaMs
            if (remaining <= 0L) {
                null
            } else {
                chip.copy(
                    x = chip.x + chip.vx,
                    y = chip.y + chip.vy,
                    vy = chip.vy + 0.6f,
                    lifeMs = remaining
                )
            }
        }

        updated = updated.copy(
            chips = chipped,
            backgroundScroll = (updated.backgroundScroll + (deltaMs * 0.03f)) % 10000f,
            bossBannerMs = (updated.bossBannerMs - deltaMs.toInt()).coerceAtLeast(0),
            comboBurstMs = (updated.comboBurstMs - deltaMs.toInt()).coerceAtLeast(0),
            missFlashMs = (updated.missFlashMs - deltaMs.toInt()).coerceAtLeast(0)
        )

        if (updated.quoteVisible && Random.nextFloat() < 0.0111f) {
            updated = updated.copy(quoteVisible = false)
        }

        return updated
    }

    fun onChop(state: GameUiState, nowMs: Long): Pair<GameUiState, Boolean> {
        if (!state.started || state.showSummary || state.showGameOver) {
            return state to false
        }
        val elapsed = nowMs - lastChopTs
        val minGap = max(80L, 360L - (state.upgrades.axeSpeedLevel * 22L))
        if (elapsed in 1 until minGap) {
            return state to false
        }
        lastChopTs = nowMs

        var damage = 10f + state.upgrades.axePowerLevel * 2.4f
        if (state.upgrades.doubleChopLevel > 0 && Random.nextFloat() < (0.08f + state.upgrades.doubleChopLevel * 0.03f)) {
            damage *= 2f
        }

        val comboInc = if (elapsed <= 450L) 2 else 1
        val chipWood = max(1, (1f * state.seasonEvent.woodMultiplier).toInt())
        var updated = state.copy(
            wood = state.wood + chipWood,
            totalWoodChopped = state.totalWoodChopped + chipWood,
            highScore = max(state.highScore, state.totalWoodChopped + chipWood),
            chopCount = state.chopCount + 1,
            combo = min(100, state.combo + comboInc),
            swingPhase = 1f,
            shakeStrength = (state.shakeStrength + 4.5f).coerceAtMost(16f)
        )
        val reachedBurst = updated.combo >= 20 && state.combo < 20
        updated = updated.copy(
            comboMultiplier = 1f + (updated.combo * 0.05f),
            comboBurstMs = if (reachedBurst) 420 else updated.comboBurstMs
        )

        updated = applyDamage(updated, damage, isManual = true)
        updated = updated.copy(
            bestCombo = max(updated.bestCombo, updated.combo),
            bestRunWood = max(updated.bestRunWood, updated.wood)
        )

        val shouldTalk =
            updated.chopCount % 10L == 0L ||
                updated.chopCount % 25L == 0L ||
                updated.chopCount % 50L == 0L ||
                Random.nextFloat() < 0.08f
        if (shouldTalk) {
            updated = updated.copy(
                displayedQuote = kerryQuotes.random(),
                quoteVisible = true
            )
        }

        updated = updated.copy(chips = updated.chips + spawnChips(updated.isBossTree))
        updated = updateAchievements(updated)
        return updated to true
    }

    fun onMiss(state: GameUiState): GameUiState {
        return state.copy(
            combo = 0,
            comboMultiplier = 1f,
            dailyChallengeProgress = 0,
            dailyChallengeDone = false,
            missFlashMs = 650
        )
    }

    fun buyUpgrade(state: GameUiState, key: String): GameUiState {
        val level = currentLevel(state.upgrades, key)
        val definition = shopUpgrades.firstOrNull { it.key == key } ?: return state
        val cost = upgradeCost(definition, level)

        if (state.wood < cost) return state

        val newUpgrades = when (key) {
            "axe_speed" -> state.upgrades.copy(axeSpeedLevel = state.upgrades.axeSpeedLevel + 1)
            "axe_power" -> state.upgrades.copy(axePowerLevel = state.upgrades.axePowerLevel + 1)
            "auto_chop" -> state.upgrades.copy(autoChopLevel = state.upgrades.autoChopLevel + 1)
            "lucky_wood" -> state.upgrades.copy(luckyWoodLevel = state.upgrades.luckyWoodLevel + 1)
            "fire_axe" -> state.upgrades.copy(fireAxeLevel = state.upgrades.fireAxeLevel + 1)
            "double_chop" -> state.upgrades.copy(doubleChopLevel = state.upgrades.doubleChopLevel + 1)
            else -> state.upgrades
        }

        return state.copy(wood = state.wood - cost, upgrades = newUpgrades)
    }

    fun upgradeCost(definition: UpgradeDefinition, level: Int): Int {
        return (definition.baseCost * Math.pow(definition.costScale.toDouble(), level.toDouble())).toInt()
    }

    private fun currentLevel(upgrades: UpgradeState, key: String): Int = when (key) {
        "axe_speed" -> upgrades.axeSpeedLevel
        "axe_power" -> upgrades.axePowerLevel
        "auto_chop" -> upgrades.autoChopLevel
        "lucky_wood" -> upgrades.luckyWoodLevel
        "fire_axe" -> upgrades.fireAxeLevel
        "double_chop" -> upgrades.doubleChopLevel
        else -> 0
    }

    private fun applyDamage(state: GameUiState, rawDamage: Float, isManual: Boolean): GameUiState {
        val comboDamage = rawDamage * state.comboMultiplier
        val newHealth = state.treeHealth - comboDamage
        if (newHealth > 0f) {
            return state.copy(treeHealth = newHealth)
        }

        val woodGainBase = if (state.isBossTree) 35 else 12
        val bonusFromCombo = (state.combo * 0.25f).toInt()
        val luckyBonus = if (Random.nextFloat() < state.upgrades.luckyWoodLevel * 0.06f) 8 else 0
        val eventAdjusted = ((woodGainBase + bonusFromCombo + luckyBonus) * state.seasonEvent.woodMultiplier).toInt()
        val woodGain = max(1, eventAdjusted)

        val newWave = state.wave + 1
        val boss = newWave % 10 == 0
        val bannerMs = if (boss) 1800 else 0
        val nextSeason = when {
            newWave % 12 == 0 -> SeasonEvent.WINTER
            newWave % 9 == 0 -> SeasonEvent.SUMMER
            newWave % 6 == 0 -> SeasonEvent.AUTUMN
            else -> SeasonEvent.SPRING
        }
        val baseHealth = 100f + (newWave * 14f)
        val bossBonus = if (boss) 220f else 0f
        val nextTreeHealth = (baseHealth + bossBonus) * nextSeason.treeHealthMultiplier

        val newTotalWood = state.totalWoodChopped + woodGain
        val challengeProgress = if (isManual) min(state.dailyChallengeTarget, state.dailyChallengeProgress + 1) else state.dailyChallengeProgress
        val challengeDone = challengeProgress >= state.dailyChallengeTarget

        val showSummary = newWave % 10 == 0

        return state.copy(
            wood = state.wood + woodGain,
            totalWoodChopped = newTotalWood,
            highScore = max(state.highScore, newTotalWood),
            treeHealth = nextTreeHealth,
            treeMaxHealth = nextTreeHealth,
            wave = newWave,
            bossBannerMs = bannerMs,
            showSummary = showSummary,
            summaryWave = if (showSummary) newWave else state.summaryWave,
            summaryWoodGained = if (showSummary) woodGain else state.summaryWoodGained,
            summaryMessage = if (showSummary) kerrySummaryQuotes.random() else state.summaryMessage,
            seasonEvent = nextSeason,
            isBossTree = boss,
            shakeStrength = (state.shakeStrength + if (boss) 10f else 6f).coerceAtMost(20f),
            dailyChallengeProgress = challengeProgress,
            dailyChallengeDone = challengeDone
        )
    }

    private fun spawnChips(isBoss: Boolean): List<ParticleChip> {
        val count = if (isBoss) 12 else 7
        return List(count) {
            ParticleChip(
                id = nextParticleId++,
                x = 0f,
                y = 0f,
                vx = Random.nextFloat() * 16f - 8f,
                vy = Random.nextFloat() * -10f,
                lifeMs = Random.nextLong(280, 520)
            )
        }
    }

    private fun baseAchievements() = listOf(
        Achievement("chops_100", "Splinter Apprentice", "Chop 100 times. Congrats, you're now legally stubborn.", false),
        Achievement("wave_25", "Forest Tax Collector", "Reach wave 25 and bill the trees emotionally.", false),
        Achievement("wood_5k", "Timber Tycoon", "Earn 5000 wood. Late-stage capitalism, but rustic.", false),
        Achievement("boss_5", "Bark Bouncer", "Take down 5 boss trees with zero sympathy.", false),
        Achievement("combo_40", "Tap Goblin", "Hit a 40 combo before your thumbs file complaints.", false)
    )

    private fun updateAchievements(state: GameUiState): GameUiState {
        val bossKills = state.wave / 10
        val updated = state.achievements.map { achievement ->
            val unlock = when (achievement.id) {
                "chops_100" -> state.chopCount >= 100
                "wave_25" -> state.wave >= 25
                "wood_5k" -> state.totalWoodChopped >= 5000
                "boss_5" -> bossKills >= 5
                "combo_40" -> state.combo >= 40
                else -> false
            }
            if (unlock) achievement.copy(unlocked = true) else achievement
        }
        return state.copy(achievements = updated)
    }

    fun resetRun(state: GameUiState): GameUiState {
        return state.copy(
            treeHealth = 100f,
            treeMaxHealth = 100f,
            wave = 1,
            chopCount = 0,
            combo = 0,
            comboMultiplier = 1f,
            displayedQuote = "",
            quoteVisible = false,
            showSummary = false,
            summaryWave = 0,
            summaryWoodGained = 0,
            summaryMessage = "",
            showGameOver = false,
            bossBannerMs = 0,
            bossDefeatPulse = 0,
            comboBurstMs = 0,
            missFlashMs = 0,
            seasonEvent = SeasonEvent.SPRING,
            isBossTree = false,
            shakeStrength = 0f,
            swingPhase = 0f,
            chips = emptyList(),
            started = true
        )
    }
}