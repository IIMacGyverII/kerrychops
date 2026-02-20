# Sprite Resources for Kerry's Chopper

## Free Sprite Sheet Resources

### Recommended Sites
- **Kenney.nl** - https://kenney.nl/assets
  - High-quality free game assets
  - Search for: "pixel art", "platformer", "character"
  
- **OpenGameArt.org** - https://opengameart.org
  - Community-contributed sprites
  - Filter by: "Pixel Art" tag
  - Check licenses before use (most are CC-BY or CC0)
  
- **itch.io Assets** - https://itch.io/game-assets/free
  - Many free pixel art packs
  - Search: "pixel art sprites", "character sprites"
  
- **Craftpix.net** - https://craftpix.net/freebies/
  - Free tier with various game assets
  - Look for platformer/character packs

## Tools to Create Your Own

### Free Tools
- **Piskel** - https://www.piskelapp.com
  - Browser-based pixel art editor
  - Easy to learn
  
- **LibreSprite** - https://libresprite.github.io
  - Free, open-source
  - Fork of Aseprite
  
### Paid Tools
- **Aseprite** - https://www.aseprite.org ($20)
  - Industry standard for pixel art
  - Animation support built-in

## What to Look For
- **Character sprite**: Kerry (lumberjack/chopper character)
- **Tree sprites**: Various stages of chopped trees
- **Effects**: Wood chips, sparkles, combo effects
- **Background**: Forest/woodland parallax layers
- **UI elements**: Buttons, panels, badges

## File Format Tips
- PNG with transparency
- Power-of-2 dimensions work best (16x16, 32x32, 64x64)
- Organize as sprite sheets (multiple frames in one image)
- Keep consistent pixel density across all assets

## Project Import Paths (Prepared)
- Drop sheets into: `app/src/main/assets/sprites/`
- Current scaffold file: `app/src/main/java/com/kerrysgame/game/SpriteSheetSupport.kt`
- Placeholder/readme: `app/src/main/assets/sprites/README.txt`

## Expected Starter Sheets
- `sprites/kerry_sheet.png` (32x32 frames)
- `sprites/tree_sheet.png` (64x64 frames)
- `sprites/effects_sheet.png` (16x16 frames)
- `sprites/background_sheet.png` (16x16 tiles)

## OpenGameArt Checklist
- Confirm license allows game distribution (prefer CC0 or CC-BY)
- Keep the attribution text for your final credits screen/readme
- Ensure each sheet has consistent frame size per row
- If needed, convert and trim sheets in LibreSprite/Piskel before import
