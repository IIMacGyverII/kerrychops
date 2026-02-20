package com.kerrysgame.game

enum class SeasonEvent(
    val label: String,
    val treeHealthMultiplier: Float,
    val woodMultiplier: Float,
    val description: String
) {
    SPRING("Spring", 1.0f, 1.0f, "Regular sap flow, regular regret."),
    SUMMER("Summer Heat", 0.9f, 1.15f, "Dry logs crack faster in the heat."),
    AUTUMN("Autumn", 1.1f, 1.05f, "Dense grain, cozy suffering."),
    WINTER("Frozen Logs", 1.25f, 1.2f, "Frozen logs resist axes and hope.")
}

data class UpgradeDefinition(
    val key: String,
    val title: String,
    val description: String,
    val baseCost: Int,
    val costScale: Float = 1.35f
)

data class UpgradeState(
    val axeSpeedLevel: Int = 0,
    val axePowerLevel: Int = 0,
    val autoChopLevel: Int = 0,
    val luckyWoodLevel: Int = 0,
    val fireAxeLevel: Int = 0,
    val doubleChopLevel: Int = 0
)

data class ParticleChip(
    val id: Long,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var lifeMs: Long
)

data class Achievement(
    val id: String,
    val title: String,
    val sarcasticDescription: String,
    val unlocked: Boolean
)

data class GameUiState(
    val wood: Int = 0,
    val totalWoodChopped: Long = 0,
    val highScore: Long = 0,
    val bestCombo: Int = 0,
    val bestRunWood: Int = 0,
    val treeHealth: Float = 100f,
    val treeMaxHealth: Float = 100f,
    val wave: Int = 1,
    val chopCount: Long = 0,
    val combo: Int = 0,
    val comboMultiplier: Float = 1f,
    val displayedQuote: String = "",
    val quoteVisible: Boolean = false,
    val started: Boolean = false,
    val showSummary: Boolean = false,
    val summaryWave: Int = 0,
    val summaryWoodGained: Int = 0,
    val summaryMessage: String = "",
    val showGameOver: Boolean = false,
    val lastRunWave: Int = 0,
    val lastRunChops: Long = 0,
    val lastRunWood: Int = 0,
    val bossBannerMs: Int = 0,
    val bossDefeatPulse: Int = 0,
    val comboBurstMs: Int = 0,
    val missFlashMs: Int = 0,
    val seasonEvent: SeasonEvent = SeasonEvent.SPRING,
    val isBossTree: Boolean = false,
    val upgrades: UpgradeState = UpgradeState(),
    val dailyChallengeProgress: Int = 0,
    val dailyChallengeTarget: Int = 500,
    val dailyChallengeDone: Boolean = false,
    val achievements: List<Achievement> = emptyList(),
    val shakeStrength: Float = 0f,
    val swingPhase: Float = 0f,
    val backgroundScroll: Float = 0f,
    val chips: List<ParticleChip> = emptyList()
)

val shopUpgrades = listOf(
    UpgradeDefinition(
        key = "axe_speed",
        title = "Quicker Axe",
        description = "Swing cooldown drops, your shoulders complain louder.",
        baseCost = 30
    ),
    UpgradeDefinition(
        key = "axe_power",
        title = "Heavier Head",
        description = "Each chop hits harder because subtlety is dead.",
        baseCost = 40
    ),
    UpgradeDefinition(
        key = "auto_chop",
        title = "Stronger Arms",
        description = "Auto-chop bonus. Kerry can suffer in passive mode.",
        baseCost = 60
    ),
    UpgradeDefinition(
        key = "lucky_wood",
        title = "Wood Magnet",
        description = "Chance to spawn bonus wood. Economics, but dumb.",
        baseCost = 75
    ),
    UpgradeDefinition(
        key = "fire_axe",
        title = "Fire Axe",
        description = "Burn damage over time. OSHA unfriendly.",
        baseCost = 90
    ),
    UpgradeDefinition(
        key = "double_chop",
        title = "Double Chop",
        description = "Occasionally lands two chops in one swing.",
        baseCost = 120
    )
)