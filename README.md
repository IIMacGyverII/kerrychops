# Kerry Chops (Android, Kotlin + Jetpack Compose)

Endless arcade clicker about Kerry, a sarcastic lumberjack with a lifted white '78 Jeep, chopping through increasingly stubborn trees.

## Current Build Highlights

- **Intro splash video** plays full-screen in landscape before the game starts.
- **Sprite-driven gameplay** rendering (tree, stump, effects, and chopping animation with 145 frames).
- **Kerry + axe chopping action** uses `lumberjackswings` frame sequence (transparent backgrounds).
- **Endless progression** with waves and boss trees every 10 waves.
- **Seasonal modifiers** (spring / summer / autumn / winter with health/wood multipliers).
- **Upgrade shop** with 6 upgrades: axe speed, axe power, auto chop, lucky wood, fire axe, double chop.
- **Combo system** (max 100, +5% damage per level, visual burst at combo 20+)
- **Stacked quote system** — Kerry's sarcastic comments appear at top of screen, stack when multiple fire
- **Daily challenge** (500 chops without miss) + 5 achievements
- **Parallel frame preloading** on app startup (no startup jank)
- **DataStore persistence** for all progression, upgrades, and stats

## Recent Improvements (Latest Session)

✅ **App rename**: App is now called **Kerry Chops** (was "Kerrys Chopper").  
✅ **Custom icon**: Adaptive launcher icon — axe silhouette (brown handle, silver blade with gleam) on a deep forest green background.  
✅ **Animation**: Decoupled 145-frame animation timer from `swingPhase`. `animElapsedMs` runs independently so the full animation cycles smoothly over 480ms regardless of chop speed upgrades.  
✅ **Sound sync**: Swing sound fires when `animElapsedMs` crosses 60ms (1/8 into the 480ms animation). Leading 412ms of silence trimmed from `axe_swing.ogg` via ffmpeg.  
✅ **Chop speed**: Base chop cooldown set to 844ms (~1.2 chops/sec). Each Quicker Axe level reduces by 22ms, floor at 80ms.  
✅ **Fire Axe**: Now hit-triggered DoT — each chop refreshes a 3-second burn window, ticking every 350ms. Stops if no new hit lands.  
✅ **Stronger Arms**: Same hit-triggered pattern — active for 3 seconds after a chop, passive DPS stops when timer expires.  
✅ **Upgrade HUD**: Top-right score card now lists owned upgrades with level and live effect values.

## Upgrade Details

| Upgrade | Effect per level |
|---|---|
| Quicker Axe | -22ms cooldown per level |
| Heavier Head | +2.4 damage per level |
| Stronger Arms | +0.8 DPS for 3s after each hit |
| Wood Magnet | +10% bonus wood chance per level |
| Fire Axe | +2 burn every 350ms for 3s after each hit |
| Double Chop | +3% double-hit chance per level (base 15%) |

## Project Structure (important assets)

- `app/src/main/assets/sprites/`
  - `lumberjackswings/` (145 Kerry + axe animation frames, transparent backgrounds)
  - `tree_sheet2.png`
  - `tree_stump_sheet.png`
  - `effects_sheet.png`
  - `background_sheet.png`
- `app/src/main/res/raw/`
  - `kerrychopswoodintro.mp4`
  - `axe_swing.ogg` (leading silence trimmed), `wood_crack.ogg`, `wood_collect.ogg`

## Key Constants (easy to tune)

| Constant | File | Value | Purpose |
|---|---|---|---|
| `ANIM_TOTAL_MS` | `KerryGameEngine.kt` + `GameUi.kt` | `480L` | Full animation cycle duration |
| Sound trigger | `GameViewModel.kt` | `60L` | ms into animation when swing sound fires (1/8 of cycle) |
| Base cooldown | `KerryGameEngine.kt` | `844L` | Base ms between chops |
| Burn/auto duration | `KerryGameEngine.kt` | `3000L` | ms fire axe / stronger arms stay active after hit |

## Build & Run

1. Open the folder in Android Studio.
2. Let Gradle sync.
3. Run on emulator/device (API 26+).

Or from terminal:

```bash
./gradlew :app:installDebug
```

Then launch:

```bash
adb shell am start -n com.kerrysgame/.MainActivity
```

## Notes

- The game is tuned for landscape orientation.
- If chopping feels janky on first launch, ensure sprite frames in `lumberjackswings` exist and are accessible in assets.
