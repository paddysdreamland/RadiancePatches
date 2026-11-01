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
| Modded Villager Layers | `modded_villager_layers_fix` | Profession and level layers of modded villagers no longer hide under the base skin. |
| Extra PBR Folders | `extra_pbr_folders_fix` | `_s`, `_n` and `_f` maps also work for textures in `textures/blockentity`, `block_entity`, `ctm`, `liquid` and `fluid` (TARDIS Refined consoles, Mekanism, Create and others). |

Not affiliated with the Radiance team.
