PANANAKOP - BOSS: FERDINAND MAGELLAN (sprite pack)
===================================================
Design from your reference painting: black flat beret, full black beard + mustache, ochre-gold belted doublet, black sleeves slashed with white,
white ruffled cuffs, gold chain + medallion, black hose and shoes, rapier on a long scabbard. Same modern pixel style as the Lapu-Lapu hero.

FRAME: 160x128 px (wider than tall for the rapier lunge). Feet rest on y=122 in every frame; soft ground shadow included.
FACING: spritesheets_left/ (default - faces the hero) and spritesheets_right/. Pivot x = 95 (left) / 64 (right).
        Boss is ~1.6x the hero's size (hero 64px).

ANIMATIONS (frames / fps)                                                   notes
  idle 8 / 8 loop              walk 8 / 12 loop              run 6 / 14 loop (dash)        jump 5 / 10 (crouch, rise, apex, fall, land)
  block_parry 5 / 12           attack_1_slash 8 / 14         attack_2_lunge 8 / 14         attack_3_spin_cut 10 / 14
  skill_pistol_shot 8 / 12     skill_cannon_command 10 / 10  skill_summon_crew 10 / 10     skill_rage 8 / 12 (phase 2)
  hurt 4 / 12                  die 12 / 9  (beret flies off, rapier dropped, falls - hold last frame)
  Hit/trigger frames per skill are listed in data/atlas.json.

SKILLS
  Slash          diagonal rapier cut with sweep trail baked in          (+ fx/slash_wave ranged crescent)
  Lunge          fencing thrust, longest reach                         (+ fx/thrust_streak)
  Spin Cut       aerial spinning cut                                   
  Pistol Shot    flintlock                                             (+ fx/bullet, muzzle_flash, smoke_puff)
  Cannon Command "Fuego!" - sword raised, ships bombard the arena      (+ fx/target_marker -> cannon_shell -> cannon_explosion + shockwave)
  Summon Crew    plants the sword, sigil glows, soldiers spawn         (+ fx/summon_sigil, spawn_flash)
  Rage           phase-2 transition with flame aura                    (+ fx/rage_aura overlay)
  Parry          draws the rapier, spark on impact                     (+ fx/parry_spark)

FOLDERS  spritesheets_*/ one strip per animation | frames_*/ every frame | gifs/ previews | fx/ effect sprites | portrait/ 64px + 256px
         magellan_ALL_left.png / _right.png = every animation, one per row | previews/ arena + overview | data/atlas.json
