# Radiance Patches

A Paddy's Dreamland™ mod for NeoForge 1.21.1.

A bundle of fixes for [Radiance](https://github.com/Minecraft-Radiance/Radiance), the Vulkan ray tracing renderer. It loads next to the official Radiance jar, so Radiance itself is never modified. Not affiliated with the Radiance team.

## What it fixes

| Fix | Switch in the config | What it does |
|---|---|---|
| Veil | `veil_fix` | No crash when Veil (Create Aeronautics, Levitite) draws vanilla chunk layers |
| Texture ids | `texture_id_fix` | No crash when mods create thousands of textures (Xaero's World Map and others). Radiance can't handle texture ids above 4096, so low ids are reserved and reused |
| Entity Model Features | `emf_fix` | EMF / Fresh Animations parts with their own texture (skeleton quivers, sheep wool, ...) no longer float at the camera |
| Level render models | `level_render_models_fix` | Models that mods draw during level render events (Create Overpressure bent tubes, CarryOn carried blocks and mobs) are ray traced instead of painted flat and unlit on top of the image |
| Block entity crash | `block_entity_crash_fix` | No ConcurrentModificationException crash when the list of always-visible block entities changes while Radiance reads it (for example tree rooms) |
| Modded villager layers | `modded_villager_layers_fix` | Profession and level layers of modded villagers no longer hide under the base skin |
| Extra PBR folders | `extra_pbr_folders_fix` | `_s`, `_n` and `_f` maps also work for textures in `textures/blockentity`, `block_entity`, `ctm`, `liquid` and `fluid` (TARDIS Refined consoles, Mekanism, Create and others) |
