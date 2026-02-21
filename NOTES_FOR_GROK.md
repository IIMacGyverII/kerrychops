# Notes for Grok — Kerry Animation Problem

## What We're Trying to Do

Kerry (the lumberjack character) has a chop swing animation with **145 individual frame images** stored in `app/src/main/assets/sprites/lumberjackswings/`. Each frame is a separate PNG file loaded into a `swingCache: Map<Int, ImageBitmap>`.

The goal is to play through **all 145 frames** for each chop swing so the animation looks smooth and complete — not choppy or truncated.

---

## How the Game Loop Works

- **Game loop**: `delay(16L)` per tick in `GameViewModel.startGameLoop()` — approximately 60 ticks/second
- **`onTick()`** in `KerryGameEngine.kt` handles all state updates including `swingPhase`
- **`onChop()`** sets `swingPhase = 1.0f` on each tap/hold
- **`swingPhase`** decays from 1.0 → 0.0 each tick. Current rate: `0.08f` per tick = **~12.5 ticks** (200ms) per swing
- The UI in `GameUi.kt` reads `swingPhase > 0` to know Kerry is mid-swing, and picks which frame to display

---

## The Core Problem

**At 0.08/tick decay, a swing lasts ~12.5 ticks. But there are 145 frames. You can only display ~12–13 distinct frames per swing.**

Three approaches have been tried, all failing for different reasons:

### Attempt 1 — Drive frame index from swingPhase interpolation (original)
```kotlin
val swingIndex = ((1f - state.swingPhase) * (frameCount - 1)).toInt()
```
**Problem**: This maps 12.5 tick steps across 145 frames → jumps ~11 frames per tick. Produces choppy animation that skips most frames.

### Attempt 2 — Separate swingFrameIndex counter advancing every tick
Added `swingFrameIndex: Int = 0` to `GameUiState`, incremented every tick unconditionally.  
**Problem**: Counter advances during idle time between taps, so each swing starts at a random frame mid-cycle. First swing happened to look smooth (counter was near 0), subsequent ones didn't.

### Attempt 3 — Advance swingFrameIndex only when swingPhase > 0
Only advance `swingFrameIndex` while actively swinging.  
**Problem**: Still only 12–13 ticks per swing → still only shows the first 12 frames before swingPhase hits 0.

### Attempt 4 — Advance by 12 frames per tick
`swingFrameIndex += 12` per tick to cover all 145 frames in 12 ticks.  
**Problem**: Effectively the same as attempt 1 — still skips most frames, just in a different way.

### Attempt 5 — Slow swingPhase decay to 1/145 per tick
`swingPhase -= 0.0069f` so the swing lasts 145 ticks (~2.3 seconds), one frame per tick.  
**Problem**: The animation plays beautifully BUT the swing now lasts 2.3 seconds, which feels very wrong for holds/auto-chop. Each hold-chop fires every ~130ms (8 ticks) but swingPhase stays at 1.0 the whole time, so the animation runs continuously which is actually OK visually — but on single taps, Kerry takes 2.3 seconds to finish the animation while gameplay has already moved on.

### Attempt 6 — Even distribution sampling (current)
Reset `swingFrameIndex = 0` on each tap. Advance by 1 per tick. In the UI:
```kotlin
val swingIndex = ((state.swingFrameIndex * frameCount) / 13).coerceIn(0, frameCount - 1)
```
This evenly samples: tick 0 → frame 0, tick 6 → frame ~67, tick 12 → frame 133.  
**Result**: Covers the start, middle, and end of the animation with even spacing — but still only 13 frames total visible. User reports it still looks like skipping.

---

## What the User Wants

The user wants **all 145 frames to visibly play through** on each swing. They understand they can't have 145 frames at 16ms each within a 200ms window — that would require 2.3 seconds per swing. Their specific ask:

> "if you only have 1 second and can only play 12 total frames then don't play frame 1 then 145. Take 145 and divide by the 12 frames you can play and equally jump through the frames with an even amount of frames skipped throughout"

So the even-distribution approach (Attempt 6) IS what they asked for. The remaining problem is it still LOOKS skippy because only 12–13 frames are shown regardless.

---

## Possible Real Solutions

1. **Increase swing duration to match frame count** — slow `swingPhase` decay so swing lasts ~145 ticks, but handle hold-chop differently so it doesn't feel delayed. E.g. on hold-chop, don't reset swingPhase but DO keep advancing swingFrameIndex — so the animation runs continuously during holds.

2. **Drive animation independently from a wall-clock timer** — decouple frame playback entirely from `swingPhase`. Use `System.currentTimeMillis()` in the UI to compute which frame to show based on elapsed time since last chop, at a target FPS of e.g. 30fps → 145 frames / 30fps = 4.8 seconds per full cycle, or 60fps → 2.4 seconds. `swingPhase` only controls HUD/effects visibility, not frame selection.

3. **Reduce frame count** — if 145 is more than needed, sub-sample to 24 or 30 key frames during loading and play those. Would require changes to `SpriteSheetSupport.loadFrame`.

4. **Use a real animation timer in onTick** — track `swingElapsedMs` in `GameUiState`, advance it by `deltaMs` each tick while swinging, use `(swingElapsedMs / totalAnimDurationMs) * frameCount` as frame index. Keep `swingPhase` fast for gameplay but `swingElapsedMs` can run independently longer if desired.

---

## Key Files

| File | Role |
|------|------|
| `app/src/main/java/com/kerrysgame/game/GameUi.kt` | Canvas rendering, frame selection logic (look for `swingIndex`) |
| `app/src/main/java/com/kerrysgame/game/KerryGameEngine.kt` | `onTick()` swingPhase decay and `swingFrameIndex` advance; `onChop()` resets |
| `app/src/main/java/com/kerrysgame/game/GameModels.kt` | `GameUiState` — contains `swingPhase: Float` and `swingFrameIndex: Int` |
| `app/src/main/java/com/kerrysgame/game/GameViewModel.kt` | `startGameLoop()` — `delay(16L)` tick rate |
| `app/src/main/assets/sprites/lumberjackswings/` | 145 PNG frames, loaded on startup into `swingCache` |

---

## Current Code State

**`KerryGameEngine.kt` — onTick swing section:**
```kotlin
if (updated.swingPhase > 0f) {
    updated = updated.copy(
        swingPhase = (updated.swingPhase - 0.08f).coerceAtLeast(0f),
        swingFrameIndex = updated.swingFrameIndex + 1
    )
} else if (updated.swingFrameIndex != 0) {
    updated = updated.copy(swingFrameIndex = 0)
}
```

**`KerryGameEngine.kt` — onChop:**
```kotlin
swingPhase = 1f,
swingFrameIndex = 0,
```

**`GameUi.kt` — frame selection:**
```kotlin
val swingIndex = if (swingFrameCount > 0 && state.swingPhase > 0f) {
    ((state.swingFrameIndex * swingFrameCount) / 13).coerceIn(0, swingFrameCount - 1)
} else if (swingFrameCount > 0) {
    0
} else {
    -1
}
```
