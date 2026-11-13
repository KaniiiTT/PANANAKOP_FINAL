PANANAKOP - MODERN 2D ASSET KIT (32x32 tiles, matches the 64x64 Lapu-Lapu hero)
================================================================================
Modern pixel-art style: 3D bevel lighting from the top-left, ambient occlusion, hue-shifted shadows/highlights, selective
coloured outlines, glow + additive light overlays. NEAREST filtering only.

BLOCKS  blocks/<material>/  23 materials, each a full 47-tile blob autotile set (8x6 sheet of 32x32) x3 variants (v0,v1,v2)
  dirt, grass*, stone, sand, sandstone, clay, mud, jungle_grass*, coral_limestone, volcanic_ash, obsidian, magma_rock*, wood_planks,
  bamboo_block, nipa_thatch, stone_brick, crystal_block*, ore_copper*, ore_iron*, ore_gold*, ore_jade*, ore_sapphire*, ore_ruby*
  * = ANIMATED: blocks/<material>/anim/<material>_blob47_f<k>.png (grass 8 frames swaying blades+flowers, magma 6 glowing cracks,
    crystal 4 pulse, ores 4 sparkle) - same layout as the static sheet, just swap the texture at ~10 fps.
  tiles/ = every v0 tile as NNN.png (NNN = blob mask).   tiled/*.tsx = Tiled tilesets with ready blob47 wangsets.
  autotile/ = mask table + reference implementation (bits N=1 NE=2 E=4 SE=8 S=16 SW=32 W=64 NW=128).
WALLS   walls/  darker background-wall sets (dirt, stone, sandstone, mud, coral, wood, bamboo, brick, clay, thatch)
LIQUIDS liquids/  water, lava, toxic swamp: 8-frame surface + 8-frame deep (caustics), waterfall (8f) + splash (6f)
PLATFORMS platforms/  bamboo L/M/R, rope bridge   (crumbling platform is in traps/)
TRAPS   traps/<name>/ frames + strip + GIF, damaging frames listed in atlas.json 'note':
  bamboo_spike_trap (up/down/left/right), saw_blade + saw_rail, kampilan_pendulum, volcanic_fire_jet, blowgun_dart_trap + poison_dart,
  spanish_cannon_trap + cannonball + cannon_explosion, falling_boulder, crumbling_platform, bamboo_bounce_pad, rolling_boulder,
  carnivorous_flower
ENVIRONMENT  environment/animated/  swaying palms x3, banana, bamboo, grass x3, flowers x3, vines, bush breeze, parol lantern swing,
  torch / campfire / brazier flames, checkpoint banner, coin, chest open, big crystal glow
  environment/particles/  additive glows (warm/cool/lava/green), fireflies, falling leaves x3, rain drop + splash, embers, bubbles,
  dust puff, smoke, sparkle glint, god-rays overlay (640x360, 4 frames)
  environment/static/  hut, signboard, crate, barrel, cannons, bushes, rocks, coral, shells, starfish, clouds, mist band ...
BACKGROUNDS backgrounds/{day,sunset,night}/  640x360, horizontally tileable layers: 1_sky, 3_mid_jungle, 4_near_jungle, 5_front_mist
  + animated 2_far_sea (6 frames, ships bob) and for night 1b_stars (4-frame twinkle). Parallax speeds in atlas.json.
PREVIEWS previews/  demo_scene_animated.gif (everything running together), traps_showcase_animated.gif, background GIFs, blocks overview

LIGHT OVERLAYS: draw glow_* sprites with ADDITIVE blending on top of torches/lava/crystals for the modern bloom look.
