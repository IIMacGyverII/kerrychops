package com.kerrysgame.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kerrysgame.game.UpgradeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "kerry_game_prefs")

data class SavedGameData(
    val highScore: Long = 0,
    val totalWood: Long = 0,
    val woodBank: Int = 0,
    val bestCombo: Int = 0,
    val bestRunWood: Int = 0,
    val axeSpeed: Int = 0,
    val axePower: Int = 0,
    val autoChop: Int = 0,
    val luckyWood: Int = 0,
    val fireAxe: Int = 0,
    val doubleChop: Int = 0
) {
    /** Maps persisted upgrade levels to in-memory runtime state. */
    fun toUpgradeState(): UpgradeState = UpgradeState(
        axeSpeedLevel = axeSpeed,
        axePowerLevel = axePower,
        autoChopLevel = autoChop,
        luckyWoodLevel = luckyWood,
        fireAxeLevel = fireAxe,
        doubleChopLevel = doubleChop
    )
}

class GameDataStore(private val context: Context) {

    private object Keys {
        val HIGH_SCORE = longPreferencesKey("high_score")
        val TOTAL_WOOD = longPreferencesKey("total_wood")
        val WOOD_BANK = intPreferencesKey("wood_bank")
        val BEST_COMBO = intPreferencesKey("best_combo")
        val BEST_RUN_WOOD = intPreferencesKey("best_run_wood")
        val AXE_SPEED = intPreferencesKey("axe_speed")
        val AXE_POWER = intPreferencesKey("axe_power")
        val AUTO_CHOP = intPreferencesKey("auto_chop")
        val LUCKY_WOOD = intPreferencesKey("lucky_wood")
        val FIRE_AXE = intPreferencesKey("fire_axe")
        val DOUBLE_CHOP = intPreferencesKey("double_chop")
    }

    /** Continuous preference stream. ViewModel loads from this on app start. */
    val savedData: Flow<SavedGameData> = context.dataStore.data.map { prefs ->
        SavedGameData(
            highScore = prefs[Keys.HIGH_SCORE] ?: 0,
            totalWood = prefs[Keys.TOTAL_WOOD] ?: 0,
            woodBank = prefs[Keys.WOOD_BANK] ?: 0,
            bestCombo = prefs[Keys.BEST_COMBO] ?: 0,
            bestRunWood = prefs[Keys.BEST_RUN_WOOD] ?: 0,
            axeSpeed = prefs[Keys.AXE_SPEED] ?: 0,
            axePower = prefs[Keys.AXE_POWER] ?: 0,
            autoChop = prefs[Keys.AUTO_CHOP] ?: 0,
            luckyWood = prefs[Keys.LUCKY_WOOD] ?: 0,
            fireAxe = prefs[Keys.FIRE_AXE] ?: 0,
            doubleChop = prefs[Keys.DOUBLE_CHOP] ?: 0
        )
    }

    suspend fun saveProgress(
        highScore: Long,
        totalWood: Long,
        woodBank: Int,
        bestCombo: Int,
        bestRunWood: Int,
        upgrades: UpgradeState
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIGH_SCORE] = highScore
            prefs[Keys.TOTAL_WOOD] = totalWood
            prefs[Keys.WOOD_BANK] = woodBank
            prefs[Keys.BEST_COMBO] = bestCombo
            prefs[Keys.BEST_RUN_WOOD] = bestRunWood
            prefs[Keys.AXE_SPEED] = upgrades.axeSpeedLevel
            prefs[Keys.AXE_POWER] = upgrades.axePowerLevel
            prefs[Keys.AUTO_CHOP] = upgrades.autoChopLevel
            prefs[Keys.LUCKY_WOOD] = upgrades.luckyWoodLevel
            prefs[Keys.FIRE_AXE] = upgrades.fireAxeLevel
            prefs[Keys.DOUBLE_CHOP] = upgrades.doubleChopLevel
        }
    }
}