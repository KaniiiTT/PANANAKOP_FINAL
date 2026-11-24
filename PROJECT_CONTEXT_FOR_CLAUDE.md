# Pananakop — Claude Project Context

Use this file as context when asking Claude to modify the game. This repository is a Java 2D platformer with pixel-art assets, a Swing/AWT game loop, parallax backgrounds, enemies, traps, level files, and a small level editor.

## Project identity

- Root: D:\PANANAKOP_FINAL
- Language: Java
- Main entry point: src/main/MainClass.java
- Game loop and dimensions: src/main/Game.java
- Rendering: Java AWT/Swing, Graphics/Graphics2D, and BufferedImage
- World scale: 32-pixel default tiles, 26 tiles wide by 14 tiles high; runtime scale is calculated in main.Game
- Assets: res/ for original game assets and pananakop_modern_assets/ for newer tiles, backgrounds, traps, and environment assets

## Instructions for Claude

Before editing:

1. Inspect relevant existing classes and call sites.
2. Preserve package names and public APIs unless the task explicitly requires an API change.
3. Reuse existing constants, managers, loaders, collision helpers, and rendering conventions.
4. Keep pixel art crisp. Do not introduce smoothing or anti-aliasing unless requested.
5. Keep gameplay updates in update methods and drawing in draw/render methods.
6. Avoid allocating images, large collections, or new random generators every frame.
7. Do not silently replace existing assets or level files.
8. Explain every modified file, assumption, and validation result.
9. Inspect the project before choosing a build command; no obvious Maven/Gradle build file is present.

If a request is ambiguous, ask whether it affects only Stage 1, all levels, the editor, or the entire game.

## Runtime flow

MainClass starts Game on the Swing event thread. Game creates the menu, options, character select, and Playing state, then starts the update/render loop.

During gameplay:

1. Playing.update(...) updates the player, enemies, objects, effects, camera, and background.
2. Playing.draw(...) draws the background first, then level tiles, player, enemies, objects, UI, and overlays.
3. ParallaxBackground receives the camera offset and current level data.
4. Input comes from inputs.KeyboardInputs and inputs.MouseInputs.

Normal render order:

    parallax background
    level tiles and decorations
    player
    enemies
    objects and traps
    player UI / boss UI
    pause, game-over, or completion overlays

## Source tree

### src/main/

- MainClass.java: application entry point.
- Game.java: loop, state switching, scale, dimensions, and coordination.
- GamePanel.java: Swing panel that receives input and renders.
- GameWindow.java: JFrame/window setup and focus handling.

### src/background/

Background and ambient-scene systems.

- ParallaxBackground.java: assembles and draws background layers.
- BackgroundLayer.java: base drawable background abstraction.
- TiledBackgroundLayer.java: horizontally repeating image layer.
- ForestLayer.java: tiled forest layer with optional haze.
- SwayingForestLayer.java: forest layer whose upper strips move with wind.
- MountainLayer.java: mountain image layer with day/night tinting.
- SkyLayer.java, NightSkyLayer.java, CloudLayer.java: sky and cloud rendering.
- UndergroundLayer.java: soil/underground rendering.
- DayNightCycle.java and MoodTintLayer.java: time-of-day and global tint.
- BirdLayer, BatLayer, FireflyLayer, ForestLifeLayer, LightShaftLayer, ShootingStarLayer: ambient effects.
- TerrainAnalyzer.java, TerrainProfile.java, TileClassifier.java: terrain surface and background clipping.
- TropicalScenery.java: deterministic procedural tropical scenery generator.

### src/entities/

- Entity.java: common entity base.
- Player.java: movement, animation, attacks, health, collision, and rendering.
- Enemy.java: enemy base behavior.
- Crabby.java, Boar.java, Conquistador.java: regular enemies.
- Magellan.java: boss behavior.
- EnemyManager.java: enemy creation, update, draw, and hit handling.

### src/objects/

- GameObject.java: base object with position, hitbox, and rendering data.
- ObjectManager.java: object/trap creation, updating, drawing, and collision.
- Trap.java: common trap base and sprite-strip frame handling.
- Concrete traps include bamboo spikes, bounce pads, cannons, boulders, saws, flower traps, blowguns, pendulums, and volcanic fire jets.
- Projectile.java, Cannon.java, GameContainer.java, Potion.java, and Spike.java: interactive objects and projectiles.

### src/levels/

- Level.java: level data, entity/object spawning, player spawn, and level-specific data.
- LevelManager.java: loads and draws levels.
- LevelDecoration.java: decoration metadata and rendering information.

### src/gamestates/

- Gamestate.java: current global state.
- State.java: shared state helpers.
- Playing.java: active gameplay state.
- Menu.java, CharacterSelect.java, GameOptions.java: menu/setup states.
- Statemethods.java: common state interface.

### src/ui/

Menu buttons, pause/game-over/completion overlays, character selection UI, audio controls, volume controls, and boss UI.

### src/inputs/

Keyboard and mouse listeners. Input should generally update state flags or call existing game/state methods rather than directly changing rendering code.

### src/effects/

- HitEffects.java: hit effects and screen shake.

### src/utilz/

- Constants.java: tile, entity, object, projectile, UI, direction, animation, and gameplay constants.
- HelpMethods.java: collision, level-data, spawn, and geometry helpers.
- LoadSave.java: asset loading, project-root discovery, level-file loading, modern tile loading, trap loading, and Magellan animation loading.
- SoundManager.java: sound loading and playback.

### src/GameEditor/

- GameEditor.java: Swing editor for level tiles, backgrounds, and decorations. Keep it compatible with res/lvls/ formats.

## Assets and level layout

### res/

Original sprite atlases, UI images, level images, level metadata, and the Magellan boss animation package.

Relevant examples:

- res/lvls/1.png, 2.png, etc.: level images.
- res/lvls/*.tiles: modern tile sidecar data where present.
- res/lvls/1.background: background metadata.
- res/lvls/1.decorations: decoration metadata.
- res/outside_sprites.png, res/trap_atlas.png, res/objects_sprites.png: sprite atlases.
- res/pananakop_boss_magellan/: Magellan animations and effects.

### pananakop_modern_assets/

Modern replacement assets loaded through LoadSave.GetProjectRoot(). Top-level areas include Background/, Building/, Environment/, Platformer/, TILED_files/, and traps/.

ParallaxBackground first checks for a newer set under pananakop_modern_assets/backgrounds/day/. If all required modern files exist, it uses that layered set. Otherwise it uses the generated legacy-compatible scene based on Background/x32/ assets and TropicalScenery. Changes to TropicalScenery affect the fallback/generated path, not necessarily the modern-background path.

## TropicalScenery.java

File: src/background/TropicalScenery.java

This is a stateless utility that creates 960x544 ARGB images once. It does not animate scenery and should not become a per-frame renderer.

### Public API

- farMountains(): pale distant blue mountain range.
- nearMountains(): darker nearer mountain range.
- hills(): rolling green hills with a lit rim and distant tree clumps.
- withPalms(BufferedImage far): adds coconut palms behind the existing far forest.
- midForest(): creates middle forest with tree clusters, banana groves, broad leaves, palms, and open gaps.
- raised(BufferedImage art, int rows): moves artwork upward and mirrors bottom rows.
- withFerns(BufferedImage near): adds ferns, grass tufts, and low shrubs to the near forest.

### Integration

In the generated/fallback branch of ParallaxBackground:

    farMountains -> MountainLayer
    nearMountains -> MountainLayer
    hills -> ForestLayer
    withPalms(far forest image) -> ForestLayer
    midForest -> ForestLayer
    withFerns(near forest image), then raised -> SwayingForestLayer

Layer parallax factors, movement, haze, sway, and draw order belong to ParallaxBackground and the layer classes. For scrolling, haze, sway, vertical positioning, or day/night tint, inspect those classes first.

### Rendering invariants

Preserve these unless a redesign is requested:

- Source image size is exactly 960x544.
- All horizontal positions wrap at 960 pixels.
- Artwork tiles horizontally without a visible seam.
- Fixed seeds keep scenery identical between starts.
- Drawing is pixel-based with BufferedImage.TYPE_INT_ARGB.
- Reuse subdued palette families for pixel-art depth.
- Generate scenery once, not during update/draw.
- Transparent pixels remain transparent when composing existing forest images.
- Keep upper-left lighting.
- Preserve depth order: mountains behind hills, hills behind far forest, middle forest behind near forest.

### Existing procedural helpers

- Canvas: wrapping pixel buffer with set, get, line, over, and toImage.
- Wobble: periodic sine-wave variation with whole-number frequencies.
- Palette: light/body/dark/deep/trunk colors.
- dither(...): ordered Bayer dithering.
- blob(...): irregular leaf clump.
- blade(...): curved pointed leaf blade.
- frond(...): palm frond with leaflets.
- palm(...): curved trunk, crown, fronds, and coconuts.

When adding a plant or scenery type, prefer a helper built from these primitives. Use a new fixed seed for independent variation and avoid changing existing seeds unless reproducibility changes are intentional.

## Safe extension patterns

### Add a background element

1. Decide whether it belongs in the modern asset path, generated legacy path, or both.
2. If procedural and static, add a factory or private drawing helper to TropicalScenery.
3. Add the image to ParallaxBackground in correct back-to-front order.
4. Give it a parallax and vertical factor consistent with its depth.
5. If it should sway, use SwayingForestLayer or another existing layer.
6. Keep generation outside update() and draw().

### Add a gameplay entity

1. Inspect Entity, Enemy, Player, and EnemyManager.
2. Reuse Constants and HelpMethods for hitboxes, directions, collision, and animation.
3. Add creation/update/draw handling through the appropriate manager.
4. Load sprites through LoadSave.
5. Check spawning in Level.java and interactions in Playing.java.

### Add a trap or object

1. Inspect GameObject, Trap, ObjectManager, and nearby concrete traps.
2. Add constants in Constants.ObjectConstants only when needed.
3. Reuse trap atlas/frame slicing conventions.
4. Add spawning and collision through existing manager methods.

### Add a level or decoration

Inspect Level, LevelManager, LevelDecoration, LoadSave, and GameEditor before editing level files. Preserve existing formats and coordinate conventions. Do not assume every level has the same optional sidecar files.

## Common pitfalls

- Editing TropicalScenery will not change the modern background branch when required modern background files are present.
- A non-periodic procedural image shows a seam when TiledBackgroundLayer wraps it.
- Wrong draw order can cover the player, terrain, or nearer vegetation.
- Changing near-forest source alignment can affect ground placement; inspect raised(...) and NEAR_FOREST_LIFT.
- Adding animation to TropicalScenery duplicates background-layer responsibilities.
- A non-fixed Random seed makes visual debugging difficult.
- Hard-coded absolute asset paths break classpath/project-root fallback loading.
- Changing Game.GAME_WIDTH, GAME_HEIGHT, or scale assumptions affects collision and UI coordinates.
- Runtime code should not depend on editor-only classes.

## Recommended Claude response format

For implementation requests, ask Claude to provide:

1. A short interpretation of the feature.
2. Files it plans to inspect or change.
3. The implementation, preserving existing architecture.
4. Validation performed and its result.
5. Visual/gameplay assumptions or remaining manual checks.

## Copy-paste prompt template

    You are modifying the Pananakop Java 2D platformer.

    Read PROJECT_CONTEXT_FOR_CLAUDE.md first, then inspect the relevant source files before editing.

    Task:
    [Describe the feature or bug precisely. Include which level, state, entity, object, or background layer it affects.]

    Scope:
    - Affect only: [Stage 1 / all levels / editor / gameplay / background / UI]
    - Keep existing behavior for: [list anything that must not change]
    - Assets allowed: [existing assets only / may add assets / procedural pixel art]

    Requirements:
    - Preserve existing package structure and conventions.
    - Reuse existing managers, constants, loaders, collision helpers, and rendering layers.
    - Do not allocate per-frame resources unless necessary.
    - If modifying TropicalScenery, preserve 960x544 size, horizontal tiling, deterministic seeds, ARGB output, palette style, and one-time generation.

    Before coding, briefly identify the relevant classes and integration point. Then implement the change, validate it as far as this repository allows, and summarize changed files, validation, and any manual visual checks needed.

## TropicalScenery prompt template

    I want to add [new tropical scenery feature] to the generated Stage 1 background.

    Use PROJECT_CONTEXT_FOR_CLAUDE.md and inspect src/background/TropicalScenery.java, src/background/ParallaxBackground.java, and the relevant layer class.

    The feature should appear in [far mountains / hills / far forest / middle forest / near forest] and should be drawn [behind/in front of] [existing element].

    Preserve:
    - 960x544 source images
    - seamless horizontal wrapping
    - fixed deterministic random seeds
    - one-time image generation
    - upper-left pixel-art lighting
    - existing subdued palette and depth order
    - no new per-frame rendering system

    Implement the smallest compatible change. Tell me which helper/factory and integration point changed, and how I can verify the result visually.

