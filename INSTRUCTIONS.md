# Kerry's Game - Instructions

## Core Gameplay

**Tap to chop trees.** Each tree takes multiple chops to fell. Progress through waves to encounter bigger, badder trees and bosses.

---

## Combos

- **Each chop adds to your combo** (max 100)
- **Fast chops (within 450ms) add +2** combo—tap repeatedly and quickly for bonus growth
- **Slower chops add +1** combo
- **Combo decays by 1 every 700ms** if you stop chopping—maintain rhythm to keep it alive
- **At combo 20+** you get a burst effect (visual feedback and bonus damage)
- **Each combo level gives +5% damage multiplier**—combo 40 = 2x damage, combo 100 = 6x damage!

**TL;DR:** Tap fast and consistently. Don't pause longer than 700ms or your combo resets.

---

## Waves & Progression

- **Waves increase** as you chop trees down (wave 1, 2, 3, etc.)
- **Every 10 waves** (10, 20, 30...) you face a **boss tree** (more health, more wood reward)
- **Boss defeat** triggers a wave summary showing wood gained and Kerry's sarcastic commentary
- **Progress is endless**—keep climbing the wave counter for bragging rights

---

## Wood Economy

- **Wood drops** from each successful tree chop (1-ish per chop base, affected by season)
- **Use wood** to buy upgrades from the shop
- **Retire your run** to bank all accumulated wood as permanent savings
- **Each upgrade** costs more as you level it up

---

## Upgrades (Shop)

All upgrades are cumulative—buy as many levels as you want. **Upgraded values now more impactful!**

| Upgrade | Effect | Per Level | Notes |
|---------|--------|-----------|-------|
| **Quicker Axe** | Reduces swing cooldown | -22ms per level | Faster tapping possible |
| **Heavier Head** | Increases damage per chop | +2.4 damage per chop | Direct damage boost |
| **Stronger Arms** | Auto-chop passive damage | +0.64 damage per 100ms | **Buffed** - 2.5x stronger, see red circles |
| **Wood Magnet** | Chance to spawn bonus wood | 6% per level chance of +8 wood | Triggers on tree death |
| **Fire Axe** | Burn damage ticking | +1.8 damage every 350ms | **Buffed** - 2.2x stronger, see red circles |
| **Double Chop** | Occasional double damage chop | Base 15% + 3% per level chance | **Buffed** - nearly 2x more frequent |

---

## Passive Upgrade Feedback

- **Red circles** appear on screen when Auto Chop or Fire Axe deal passive damage
- Helps visualize that passive upgrades are actually working
- Each circle represents invisible damage ticking away

---

## Seasons

Seasons cycle and modify tree health and wood rewards:

| Season | Tree Health | Wood Gain | Description |
|--------|-------------|----------|-------------|
| **Spring** | 1.0x | 1.0x | Baseline sap flow |
| **Summer Heat** | 0.9x | 1.15x | Dry logs crack faster |
| **Autumn** | 1.1x | 1.05x | Dense grain, harder chops |
| **Winter** | 1.25x | 1.2x | Frozen logs, frozen suffering |

---

## Misses & Penalty

- **Tap too soon** (within minimum gap set by axe cooldown) = **MISS**
- **On miss**: Combo resets to 0, daily challenge progress resets
- **Visual feedback**: Red flash on screen when you miss
- **Strategize**: Don't panic tap—let the cooldown end for guaranteed hits

---

## Daily Challenge

- **Objective**: Chop 500 logs without a single miss
- **Reward**: Bragging rights (sarcastic Kerry quote)
- **Resets**: Every time you miss; on run retirement
- **Progress bar**: Visible in bottom controls

---

## Kerry's Quotes

- **Appear at top of screen** during gameplay
- **Stay visible for 1.6 seconds** before fading
- **Stack vertically** when multiple quotes pop up simultaneously
- Random sarcastic commentary on your chopping prowess

---

## Achievements

Unlock these by playing:

| Achievement | Unlock Condition |
|-------------|------------------|
| **Axe Novice** | Land 100 chops |
| **Forest Tax Collector** | Reach wave 25 |
| **Timber Tycoon** | Earn 5000 wood total |
| **Bark Bouncer** | Defeat 5 boss trees |
| **Tap Goblin** | Hit 40+ combo |

---

## Stats & Persistence

- **High Score**: Highest total wood earned in a single run
- **Best Combo**: Max combo reached ever
- **Best Run Wood**: Most wood banked in one run before retiring
- **Total Wood**: Cumulative permanent wood (saved to device)
- **All upgrades**, **wood bank**, and **stats** persist across app restarts

---

## Retirement

- Tap **"Retire This Run"** in the shop to bank your current wood
- Confirms progress and resets the tree/wave counter for a fresh run
- You keep all purchased upgrades and bankable wood

---

## Dev Notes

**⚠️ IMPORTANT:** Update this file whenever game mechanics change, especially:

### Combo Mechanics
- Combo timing windows (currently 450ms for +2)
- Combo decay timing (currently 700ms)
- Damage multiplier formula (currently 1f + combo * 0.05f)
- Combo burst threshold (currently 20)
- Maximum combo value (currently 100)

### Upgrades & Passive Feedback
- Auto Chop level multiplier (currently 0.8f * (deltaMs / 100f))
- Fire Axe level multiplier (currently 2.0f per level, ticks every 350ms)
- Double Chop base chance (currently 0.15f = 15%)
- Damage number display duration (currently 1000ms fade window = 800ms lifeMs)

### Quote System
- Display duration (currently 1600ms)
- Vertical spacing between stacked quotes (currently 70.dp)
- Quote spawning conditions (every 10/25/50 chops or 8% random chance)

### Other
- Daily challenge target (currently 500 chops)
- Any achievement conditions changed
- Miss penalty updates
- Any new game mechanics
