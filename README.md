# Kerry's Chopper (Android, Kotlin + Jetpack Compose)

Endless arcade clicker about Kerry, a sarcastic lumberjack with a lifted white '78 Jeep, chopping through increasingly stubborn trees.

## Current Build Highlights

- **Intro splash video** plays full-screen in landscape before the game starts.
- **Sprite-driven gameplay** rendering (tree, stump, effects, and chopping animation with 145 frames).
- **Kerry + axe chopping action** uses `lumberjackswings` frame sequence (transparent backgrounds).
- **Endless progression** with waves and boss trees every 10 waves.
- **Seasonal modifiers** (spring / summer / autumn / winter with health/wood multipliers).
- **Upgrade shop** with 6 upgrades: axe speed, axe power, auto chop, lucky wood, fire axe, double chop.
  - Auto chop & fire axe now **show numeric damage markers** with gold outline
  - Passive upgrades **significantly buffed** for better feel (2.5x auto chop, 2.2x fire axe, 2x double chop frequency)
- **Combo system** (max 100, +5% damage per level, visual burst at combo 20+)
- **Stacked quote system** - Kerry's sarcastic comments appear at top of screen, stack vertically when multiple fire
- **No combo glow** behind Kerry (cleaner silhouette)
- **Daily challenge** (500 chops without miss) + 5 achievements
- **Parallel frame preloading** on app startup (no startup jank)
- **DataStore persistence** for all progression, upgrades, and stats

## Recent Improvements (Latest Session)

✅ **Upgrade Balance**: Passive upgrades (auto chop + fire axe) now deal meaningful damage
✅ **Damage Feedback**: Numeric hit markers with gold outline for passive upgrades
✅ **Quote Display**: Moved to top of screen, extended duration +30%, tighter stacking
✅ **Performance**: Frame loading fully parallellized (16x batch loading on IO thread)
✅ **Animation Assets**: Replaced with transparent-background variants for cleaner visuals

## Project Structure (important assets)

- `app/src/main/assets/sprites/`
  - `lumberjackswings/` (145 Kerry + axe animation frames, transparent backgrounds)
  - `tree_sheet2.png`
  - `tree_stump_sheet.png`
  - `effects_sheet.png`
  - `background_sheet.png`
- `app/src/main/res/raw/`
  - `kerrychopswoodintro.mp4`
  - `axe_swing.ogg`, `wood_crack.ogg`, `wood_collect.ogg`

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
