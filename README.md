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

## Config

After the first launch: `config/radiance_patches.properties` in the Minecraft folder. Every fix is on by default and has a comment explaining it. Set one to `false` to turn it off, then restart the game.

## Requirements

- Minecraft 1.21.1, NeoForge
- Radiance 0.1.6 (the untouched official jar is fine, so is one patched by the old patch script)
- Client only. On a server it does nothing.

## Replaces

This mod replaces these older single-fix mods. Don't run them together with it:

- Radiance Veil Fix (`radiance-veil-fix-...jar`)
- Radiance Texture ID Fix (`radiance-texid-fix-...jar`)
- Radiance EMF Fix (`radiance-emf-fix-...jar`)
- Radiance Stage Model Fix (`radiance-stage-fix-...jar`)
- Radiance Fixes, this mod's earlier name (`radiance_fixes-...jar`)
- The `radiance-fix` patch script (the block entity crash, modded villager and extra PBR folder fixes are in here now)

The **Copy mod into modpack** task moves the old jars out of the mods folder for you, into a folder called `old-radiance-fix-jars` next to it.

## Building it

Java 21 and VS Code are already set up from Mob Scent Candles, so nothing new to install.

1. Double-click **`RadiancePatches.code-workspace`** in this folder. If VS Code asks whether you trust the folder, click yes.
2. Wait for the bottom-right status to finish importing. The first time takes a few minutes.
3. Open **Terminal > Run Task...** and pick a task:

| Task | What it does |
|---|---|
| **Copy mod into modpack** | Builds the mod, puts the jar into your modpack's mods folder (replacing the old version) and moves the old single-fix jars out. Shortcut: **Ctrl+Shift+B** |
| **Build mod** | Only builds the jar, into `build/libs` |
| **Clean build files** | Deletes build output. Try this if something gets stuck |

Building needs a Radiance jar and an Entity Texture Features jar to build against. The build finds them by name in the mods folder set in `gradle.properties` (any file starting with `Radiance-` or `entity_texture_features-`, ending in `.jar`), so renaming or updating them is fine. They are only used while building and never end up inside the finished jar.

## Project map

For when you want to change something with Claude:

| File | What's in it |
|---|---|
| `gradle.properties` | Settings: modpack path, version number |
| `src/main/templates/META-INF/neoforge.mods.toml` | Mod name, description and dependencies |
| `src/main/resources/radiance_patches.mixins.json` | The list of all mixins |
| `src/main/java/.../RadiancePatchesConfig.java` | The config file and the list of switches |
| `src/main/java/.../RadiancePatchesMixinPlugin.java` | Which mixins belong to which switch |
| `src/main/java/.../mixin/veil/` | Veil fix |
| `src/main/java/.../mixin/texid/` and `texid/TexIdPool.java` | Texture id fix |
| `src/main/java/.../mixin/emf/` and `emf/CurrentBuffers.java` | Entity Model Features fix |
| `src/main/java/.../mixin/radiance/EntityProxyStageMixin.java` and `stage/StageModelCapture.java` | Level render models fix |
| `src/main/java/.../mixin/radiance/EntityProxyPatchMixin.java` | Block entity crash and modded villager layers fixes |
| `src/main/java/.../mixin/radiance/AuxiliaryTexturesMixin.java` | Extra PBR folders fix |

## License

MIT. See `LICENSE`.