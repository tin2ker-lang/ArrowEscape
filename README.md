# Arrow Escape

A tap-to-escape arrow puzzle for Android (Kotlin + Jetpack Compose).

## How to play
- Each line is an arrow. Tap it and it slides out head-first along its path.
- If something is in the way, it bumps, turns red and you lose a drop (3 per level).
- Clear the board to win. 💡 highlights a free arrow (3 hints per level).
- Pinch to zoom and drag to pan on big boards. ⟳ restarts the level.

## Run it
1. Open this folder in Android Studio (Ladybug or newer).
2. Let Gradle sync. It downloads Gradle 8.10.2, AGP 8.7.3 and Kotlin 2.0.21.
3. Press Run with a phone or emulator connected.

## Code map
- `game/Model.kt`: grid, snakes and the "is the path clear?" check
- `game/LevelGenerator.kt`: endless seeded levels that are always solvable
- `ui/GameScreen.kt`: drawing, animations, taps, lives, hints and dialogs
