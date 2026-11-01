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
| Extra Map Folders | `extra_map_roots` | `_s` and `_n` maps also work for textures that mods keep outside the `textures` folder (Mekanism's `render` folder). Comma-separated for more folders. |
| Armor Glint | `armor_glint_fix` | Enchanted armor gets Radiance's moving glint instead of a solid, unmoving purple layer. |
| Overlay Layers | `overlay_layers_fix` | Extra layers on top of a mob's skin (glowing eyes, emissive patterns, clothes) always stay on top instead of randomly hiding under it and glowing through. |
| Glow Layers | `glow_layers_fix`, `glow_layers_strength` | Layers that are drawn to glow glow by themselves, no emission maps needed: mob eyes, the Warden's spots, TARDIS Refined's shell lights, and mod render types with "glowing" or "emissive" in their name (Create's Wand of Symmetry). Transparency sets the glow strength. |
| Emissive Faces | `emissive_faces_fix`, `emissive_faces_strength` | Block and item faces that models mark as glowing (`"block_light"` in the model JSON, used by Undergarden's shimmerweed and others) really glow, no emission maps needed. |
| Modded Fluids | `modded_fluids_fix` | Radiance draws every fluid except lava with water's textures and colour. Modded fluids get their own back, so their resource-pack maps work, and they stop counting as water. |
| Dimension Profiles | `dimension_profiles_fix`, `dimension_profiles` | Tells the shader pack which dimension you're in, so it can give dimensions their own sky. Default: TARDIS Refined interiors. |
| Lava Marker | (always on) | Marks lava so shader packs can recognise it. Other shader packs ignore it. |
| Lava Shore Edges | `lava_shore_edges` | Lava remembers which blocks next to it are walls, so the shader pack can darken lava along the shore. |
| Glow Finder | `glow_finder` (off by default) | A tool that lists every texture mods draw at full brightness on purpose in `logs/radiance_patches_glow_finder.txt`, and whether it still needs an emission map. Use it somewhere enclosed with no sky above. |

Dimension Profiles, Lava Marker and Lava Shore Edges only do something with a shader pack that uses them, such as [Advanced Patches](link).

Not affiliated with the Radiance team.
