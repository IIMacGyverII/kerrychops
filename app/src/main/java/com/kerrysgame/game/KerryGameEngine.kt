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
    private var nextDamageNumberId = 0L
    private var nextQuoteId = 0L
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

        // Stronger Arms: continuous damage only while autoChopActiveMs > 0 (triggered by a hit)
        if (updated.autoChopActiveMs > 0) {
            updated = updated.copy(autoChopActiveMs = (updated.autoChopActiveMs - deltaMs).coerceAtLeast(0L))
            val autoDamage = updated.upgrades.autoChopLevel * 0.8f * (deltaMs / 100f)
            if (autoDamage > 0f) {
                updated = applyDamage(updated, autoDamage, isManual = false, showDamageNumber = true)
            }
        }

        // Fire axe: burn ticks every 350ms while fireAxeActiveMs > 0 (triggered by a hit)
        if (updated.fireAxeActiveMs > 0) {
            updated = updated.copy(fireAxeActiveMs = (updated.fireAxeActiveMs - deltaMs).coerceAtLeast(0L))
            if (updated.upgrades.fireAxeLevel > 0 && fireDamageAccumulator >= 350) {
                fireDamageAccumulator = 0
                val burn = updated.upgrades.fireAxeLevel * 2.0f
                updated = applyDamage(updated, burn, isManual = false, showDamageNumber = true)
            }
        } else {
            fireDamageAccumulator = 0
        }

        if (updated.shakeStrength > 0f) {
            updated = updated.copy(shakeStrength = (updated.shakeStrength - 0.8f).coerceAtLeast(0f))
        }

        if (updated.swingPhase > 0f) {
            updated = updated.copy(
                swingPhase = (updated.swingPhase - 0.08f).coerceAtLeast(0f)
            )
        }

        // Decoupled animation timer — advances independently from swingPhase so all 145 frames play.
        // During a swing: swingPhase keeps it alive. After swingPhase hits 0, follow-through continues
        // until the full animation cycle (ANIM_TOTAL_MS) completes, then resets to idle (0).
        // During hold-chop: swingPhase is refreshed every ~130 ms so the timer loops seamlessly.
        val ANIM_TOTAL_MS = 480L  // ~30 frames visible per swing
        val animRunning = updated.swingPhase > 0f || updated.animElapsedMs in 1 until ANIM_TOTAL_MS
        if (animRunning) {
            val newMs = updated.animElapsedMs + deltaMs
            updated = updated.copy(
                animElapsedMs = when {
                    newMs >= ANIM_TOTAL_MS && updated.swingPhase > 0f -> newMs % ANIM_TOTAL_MS  // looping hold
                    newMs >= ANIM_TOTAL_MS -> 0L                                                 // follow-through done → idle
                    else -> newMs
                }
            )
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

        val decayedDamageNumbers = updated.damageNumbers.mapNotNull { dmg ->
            val remaining = dmg.lifeMs - deltaMs
            if (remaining <= 0L) {
                null
            } else {
                dmg.copy(y = dmg.y - 1f, lifeMs = remaining)
            }
        }

        val decayedQuotes = updated.quotes.mapNotNull { quote ->
            val remaining = quote.lifeMs - deltaMs
            if (remaining <= 0L) {
                null
            } else {
                quote.copy(lifeMs = remaining)
            }
        }

        updated = updated.copy(
            chips = chipped,
            damageNumbers = decayedDamageNumbers,
            quotes = decayedQuotes,
            backgroundScroll = (updated.backgroundScroll + (deltaMs * 0.03f)) % 10000f,
            bossBannerMs = (updated.bossBannerMs - deltaMs.toInt()).coerceAtLeast(0),
            comboBurstMs = (updated.comboBurstMs - deltaMs.toInt()).coerceAtLeast(0),
            missFlashMs = (updated.missFlashMs - deltaMs.toInt()).coerceAtLeast(0)
        )

        return updated
    }

    fun onChop(state: GameUiState, nowMs: Long): Pair<GameUiState, Boolean> {
        if (!state.started || state.showSummary || state.showGameOver) {
            return state to false
        }
        val elapsed = nowMs - lastChopTs
        val minGap = max(80L, 844L - (state.upgrades.axeSpeedLevel * 22L))
        if (elapsed in 1 until minGap) {
            return state to false
        }
        lastChopTs = nowMs

        var damage = 10f + state.upgrades.axePowerLevel * 2.4f
        if (state.upgrades.doubleChopLevel > 0 && Random.nextFloat() < (0.15f + state.upgrades.doubleChopLevel * 0.03f)) {
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
            fireAxeActiveMs = if (state.upgrades.fireAxeLevel > 0) 3000L else state.fireAxeActiveMs,
            autoChopActiveMs = if (state.upgrades.autoChopLevel > 0) 3000L else state.autoChopActiveMs,
            shakeStrength = (state.shakeStrength + 4.5f).coerceAtMost(16f)
        )
        val reachedBurst = updated.combo >= 20 && state.combo < 20
        updated = updated.copy(
            comboMultiplier = 1f + (updated.combo * 0.05f),
            comboBurstMs = if (reachedBurst) 420 else updated.comboBurstMs
        )

        updated = applyDamage(updated, damage, isManual = true, showDamageNumber = true)
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
            val newQuote = Quote(
                id = nextQuoteId++,
                text = kerryQuotes.random(),
                lifeMs = 3900L
            )
            updated = updated.copy(quotes = updated.quotes + newQuote)
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

    private fun applyDamage(state: GameUiState, rawDamage: Float, isManual: Boolean, showDamageNumber: Boolean = false): GameUiState {
        val comboDamage = rawDamage * state.comboMultiplier
        val newHealth = state.treeHealth - comboDamage
        
        var damageNumbers = state.damageNumbers
        if (showDamageNumber && comboDamage > 0.1f) {
            val baseX = 0f
            val baseY = if (isManual) -120f else -60f
            val jitterRangeX = 20f
            val jitterRangeY = 20f
            val dmgNum = DamageNumber(
                id = nextDamageNumberId++,
                damage = comboDamage.toInt(),
                x = baseX,
                y = baseY,
                jitterX = Random.nextFloat() * 2f * jitterRangeX - jitterRangeX,
                jitterY = Random.nextFloat() * 2f * jitterRangeY - jitterRangeY,
                lifeMs = 1000L
            )
            val capped = (damageNumbers + dmgNum).takeLast(8)
            damageNumbers = capped
        }
        
        if (newHealth > 0f) {
            return state.copy(treeHealth = newHealth, damageNumbers = damageNumbers)
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
            dailyChallengeDone = challengeDone,
            damageNumbers = damageNumbers
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
            quotes = emptyList(),
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