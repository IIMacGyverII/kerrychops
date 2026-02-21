# KerryChops Dev Notes

## Tree / Kerry Sprite Vertical Movement Bug

### Symptom
The tree and Kerry sprite drift up or down during gameplay — typically triggered by:
- Holding the screen for auto-chop
- Building a combo streak past ~10
- Waves advancing past 6, 9, or 12 (season changes)
- Upgrading axe speed / auto-chop (faster state updates)

### Root Cause (confirmed)
All vertical positions (tree, Kerry, health bar) are computed from `layoutHeight * multiplier` inside the Canvas draw block. Any change to `layoutHeight` shifts every sprite proportionally.

`layoutHeight` changes whenever the `Box(.weight(1f))` that wraps ChoppableTree resizes. That box resizes whenever its siblings in `Column(SpaceBetween)` — `TopHud` and `BottomControls` — change height. Those change height from:

1. **Streak badge appearing**: `if (combo >= 10)` conditionally adding a `Surface` row to TopHud right card.
2. **Season text wrapping**: `"Summer Heat: Dry logs crack faster in the heat."` is much longer than `"Spring: Regular sap flow, regular regret."` — wraps to 2 lines, growing TopHud.
3. **Daily challenge done text**: `if (dailyChallengeDone)` conditionally adding a Text row to BottomControls.
4. **SwingCache recompositions**: `swingCache` is a `mutableStateMapOf` that gets ~14+ updates as 145 frames load in batches at startup. Every update recomposes ChoppableTree, and during that recomposition `size.height` in the Canvas draw block can briefly report a stale/transitional value.

### Fixes Applied
- `TopHud` right card: Streak badge is always rendered but transparent/empty when `combo < 10` → card height is constant.
- `TopHud` left card: Season event text gets `maxLines = 1, overflow = Ellipsis` → never wraps.
- `BottomControls`: Daily challenge done text is always rendered (transparent when not done) → card height is constant.
- `ChoppableTree` Box: Uses `onSizeChanged` from the View measurement system (outside Compose recomposition) to lock `stableW/stableH` once on first measurement. These values are passed into the Canvas draw block.

### Current State (as of this note)
The `onSizeChanged` lock is in place and TopHud/BottomControls heights are now stable. Tree should no longer move after initial frame loading (~30 seconds at start). If movement resumes, look for any other conditional content in `TopHud` or `BottomControls` that can appear/disappear.

### Architecture Note
`ChoppableTree` sits inside:
```
Column(SpaceBetween, Modifier.fillMaxSize().padding(16.dp))
  TopHud          ← height must never change
  Box(weight(1f)) ← contains ChoppableTree
    ChoppableTree
      Box(height(420.dp), onSizeChanged)
        Canvas(fillMaxSize)
  BottomControls  ← height must never change
```
Any element that changes the intrinsic height of TopHud or BottomControls will shift the tree.

---

## Kerry Animation Frame Skipping

### Symptom
Kerry's chop animation has 145 frames but only ~12 are displayed per swing — it looks choppy/skippy.

### Root Cause
`swingPhase` decrements by `0.08f` per 16ms game tick, completing in ~12.5 ticks (200ms).
Frame selection: `((1f - swingPhase) * (frameCount - 1)).toInt()`
With 145 frames over 12.5 ticks = ~11.6 frames skipped per tick.

### Fix Applied
Added `swingFrameIndex: Int = 0` to `GameUiState`. Incremented by 1 every game tick in `onTick()` (always advancing, like a global frame counter). UI uses `swingFrameIndex % frameCount` as the display index when `swingPhase > 0`, cycling smoothly through all 145 frames at roughly one frame per 16ms (full 145-frame cycle ≈ 2.3 seconds).

---

## Key File Locations
- `app/src/main/java/com/kerrysgame/game/GameUi.kt` — all Composables, Canvas rendering
- `app/src/main/java/com/kerrysgame/game/KerryGameEngine.kt` — game logic, onTick, onChop
- `app/src/main/java/com/kerrysgame/game/GameModels.kt` — GameUiState, data classes
- `app/src/main/java/com/kerrysgame/game/GameViewModel.kt` — game loop (delay(16L) per tick)

## Build & Deploy
```powershell
$env:JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
$env:Path="$env:JAVA_HOME/bin;" + $env:Path
.\gradlew.bat :app:assembleDebug
adb -s 3B241FDJG001AM install -r app/build/outputs/apk/debug/app-debug.apk
```
