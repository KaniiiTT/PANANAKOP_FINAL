# PANANAKOP - Audio Pack Guide
123 original, royalty-free audio files, all synthesized for this project (no samples from other sources).
Formats: music = OGG (use this for seamless loops) + MP3; SFX = WAV (44.1 kHz mono 16-bit) + OGG; ambience = OGG.
`audio_manifest.json` lists every file with category, loop flag, duration, loop points and a suggested volume (dB).

## Music map
| Where | Track | Loop | Notes |
|---|---|---|---|
| Title / menu | `bgm_main_menu` | yes | choir pad, bamboo flute, gong, soft kudyapi-style plucks |
| Story cutscenes | `bgm_cutscene_story` | yes | slow, sparse flute + strings |
| Stage 1 Barangay Mactan | `bgm_stage1_barangay` | yes | kulintang pattern, dabakan drums, light and welcoming (tutorial) |
| Stage 2 Forest | `bgm_stage2_forest` | yes | dark phrygian mood, sparse drums, drones |
| Stage 2 mini boss (bear) | `bgm_miniboss_bear` | yes | gallop bass, toms, low horn |
| Stage 3 Island | `bgm_stage3_island` | yes | marching snare + brass for the Spanish, kulintang counter-rhythm |
| Stage 4 Punta Engano | `bgm_stage4_punta_engano` | yes | heroic sunset, flute + strings + taiko |
| Stage 4 mini boss (captain) | `bgm_miniboss_captain` | yes | military snare, brass fanfare |
| Stage 5 Liberty Shrine (approach) | `bgm_stage5_liberty_shrine` | yes | choir, gongs, deep drums, solemn and epic |
| Stage 5 final boss (Magellan) | `bgm_boss_magellan_final` | yes | fastest, densest track; choir, brass, kulintang ostinato |
| Ending / credits | `bgm_ending_credits` | yes | triumphant major pentatonic |
Jingles (play once, duck/pause the music): `jingle_level_start`, `jingle_checkpoint`, `jingle_skill_acquired`, `jingle_stage_clear`, `jingle_boss_defeated`, `jingle_game_over`.

## Ambience (loop under the music at about -18 dB)
`amb_stage1_village`, `amb_stage2_forest`, `amb_stage3_beach`, `amb_stage4_cliff`, `amb_stage5_shrine_night`.

## SFX groups (file name prefixes)
- `player_*` jump, double jump, land soft/hard, dash, wall slide (loop) / wall jump, attack 1-3, slash hit, block, parry, hurt, death, heal, effort grunts, special charge / release.
- `step_<surface>_1..3` grass, dirt, sand, stone, wood, water. Pick a random variant, pitch +/-5%, play at about -14 dB.
- Pickups: `coin`, `coin_big`, `heart_pickup`, `spirit_orb`, `skill_pickup`, `key_pickup`, `dash_crystal_collect/respawn`, `checkpoint_activate/rest`.
- World and hazards: spikes, saw (loop + hit), fire warning / jet (+ loop), arrow trap click / shot / wall hit, pendulum swish (loop), rock fall warning / land, stalactite break, crumble shake / fall, cannon fire / explosion, crate break, breakable wall, jar break, gate open / close, lever, pressure plate, spring + mushroom bounce, boss barrier open / close, water / poison splash.
- Enemies and bosses: `enemy_hit/death`, `animal_snarl`, `bear_roar`, `bear_stomp`, `bat_flap`, `snake_hiss`, `spanish_sword_clash`, `crossbow_shot`, `musket_shot`, `boss_hit`, `boss_stagger`, `boss_roar_intro`, `boss_death_explosion`.
- UI: move, select, back, pause, unpause, error, coin tick, stage start, achievement.

## Implementation tips
1. **Looping:** use the OGG music files and loop the whole file (loop_start 0, loop_end = duration). MP3 adds encoder padding, so use it only for non-looping playback.
2. **Layer mix (suggested):** music -8 dB, ambience -18 dB, SFX -6 dB, footsteps -14 dB. Duck music by 6 dB for 1.5 s on `skill_pickup` / `checkpoint_*`.
3. **Transitions:** crossfade stage music over 1 s. Entering a boss arena: fade the stage music out (0.5 s), play `boss_roar_intro`, then start the boss track. On victory: stop the boss music, play `jingle_boss_defeated`, then `jingle_stage_clear`.
4. **Telegraphs:** play `fire_warning_spark`, `arrow_trap_click`, `rock_fall_warning`, `crumble_shake` 0.4-0.6 s before the hazard fires so the player can react.
5. **Loop SFX** (`*_loop`): start when the object is on screen, scale volume by distance to the player, stop when it leaves.
6. **Variation:** attacks 1-3 chain with combo; randomise pitch for coins, hits and steps.

## Honest notes
- Everything is synthesized by code (Filipino-inspired instruments: kulintang chimes, agung-style gongs, bamboo flute, kudyapi-style pluck, dabakan/taiko drums). It is a complete, usable first pass, but I could not listen to it, so treat it as a strong draft and tweak by ear.
- Player grunts / hurt / death voice effects are synthetic placeholders. Replace them with recorded voice acting for a professional result.
- You can swap in a composer's music later by keeping the same file names.
