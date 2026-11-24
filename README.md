# Radiance Patches

A bundle of fixes for [Radiance](https://github.com/Minecraft-Radiance/Radiance), the Vulkan ray-tracing renderer. It's an add-on for Radiance itself and does not replace it.

> I love ray-tracing and have been playing with Radiance for a while now until I started discovering issue after issue and crash after crash with other mods. So I started vibe-coding these patches that fix the problems listed below. I originally intended this to be private, but surely someone else might be experiencing the same issues, so I published it here.

## List of Patches

| Patch | Config Variable | Result |
|---|---|---|
| Veil | `veil_fix` | No crash when Veil (Create Aeronautics, Levitite) draws vanilla chunk layers. |
| Texture IDs | `texture_id_fix` | No crash when mods create thousands of textures (Xaero's World Map and others). Radiance can't handle texture IDs above 4096, so low IDs are reserved and reused. |
| Entity Model Features | `emf_fix` | EMF / Fresh Animations parts with their own texture (skeleton quivers, sheep wool, ...) no longer float at the camera. |
| Level Render Models | `level_render_models_fix` | Models that mods draw during level render events (Create Overpressure bent tubes, CarryOn carried blocks and mobs) are ray-traced instead of painted flat and unlit on top of the final render. |
| Block Entity Crash | `block_entity_crash_fix` | No `ConcurrentModificationException` crash when the list of always-visible block entities changes while Radiance reads it (for example the tree room from TARDIS Refined). |
| Unknown Transparency | `unknown_transparency_fix` | No `Invalid render layer` crash when mods draw see-through layers Radiance doesn't recognise, such as GeckoLib glowmasks (Companions mobs and effects, and other GeckoLib mods). |
| Slabbed + Copycats | `slabbed_copycats_crash_fix` | No `Tesselating block model` crash (`Index -2 out of bounds for length 9`) when Slabbed lowers Copycats blocks next to a chunk edge. Slabbed already handles these reads, but didn't recognise Copycats' wrapper around the chunk region. |
| Knight Lib Textures | `knightlib_textures_fix` | Knight Lib's animated textures (Companions' Great Chalice and others) show their real texture instead of random coloured squares. Knight Lib uploads animation frames from its own sprite sheet, which Radiance never copied into its textures. |
| Chunk Buffer Leak | `chunk_buffer_leak_fix` | RAM no longer climbs by a few hundred MB on every dimension change, render distance or video settings change, F3+A or resource reload. Radiance replaced its chunk-building threads without freeing the old threads' native vertex buffers. |
| Modded Villager Layers | `modded_villager_layers_fix` | Profession and level layers of modded villagers no longer hide under the base skin. |
| Extra PBR Folders | `extra_pbr_folders_fix` | `_s`, `_n` and `_f` maps also work for textures in `textures/blockentity`, `block_entity`, `ctm`, `liquid` and `fluid` (TARDIS Refined consoles, Mekanism, Create and others). |
| Particle Culling | `particle_culling_fix` | Less lag when lots of blocks break at once (Vein Miner, tree felling). Radiance rebuilt every particle every frame, including the ones behind you; particles outside the view are now skipped like in NeoForge. |
| Particle Glow | `particle_glow_fix` | Particles that light themselves up (flames, lava sparks, end rods, soul fire, glow squid sparkles, sculk souls, explosions) are ray-traced and really glow, instead of being painted flat on top of the final render. Brightness is set with `particle_glow_strength`. |
| Particle Maps | `particle_maps_fix` | `_s`, `_n` and `_f` maps also work for particles in `textures/particle` and `textures/particles`. Particles with maps are ray-traced so the whole map applies, not just its glow. |

Not affiliated with the Radiance team.