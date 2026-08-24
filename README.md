# EFI-Unofficial — Epic Fight Indestructible (1.20.1)

EFI-Unofficial is a maintained continuation of Indestructible for Epic Fight `1.20.1`.

The original mod established the advanced mob patch system — behavior series, combat predicates, phase logic, guard logic, and datapack-driven combat behavior. EFI-Unofficial keeps that entire base intact and adds new routing, extended gear control, and improved support for Epic Fight, TACZ, and CustomNPCs workflows.

---

## What EFI-Unofficial Adds

| Feature | Status |
|---|---|
| `nbt_tag` routing — assign advanced patches by entity NBT | EFI-Unofficial |
| Extended `gear_swap` — TACZ `gun_id`, generated gear, multi-slot array syntax | EFI-Unofficial |
| Improved TACZ + CustomNPCs combat workflow support | EFI-Unofficial |
| Runtime patch refresh for all mobs and CustomNPCs | EFI-Unofficial |
| Native held-item living motions after equipment changes | EFI-Unofficial |
| Epic Fight stun, guard, knockdown, and neutralize support | EFI-Unofficial |
| Full documentation for the advanced behavior system | EFI-Unofficial |

Everything else — behavior series, predicates, animations, guard logic, phase logic — is original.

---

## Compatibility

| Mod | Version |
|---|---|
| Minecraft | `1.20.1` |
| Forge | `47.4.0` |
| Epic Fight | `20.14.17` | 
| CustomNPCs | `1.20.1.20260711` (Optional compatability) |
| TACZ | `1.20.1-1.1.8-hotfix` | (optional, required for Tijon's Epic Arsenal)
| Epic Arsenal | `1.0.1` (optional, required for Tacz)
| Packet Fixer | `2.0.0` (optional incase you get any packet errors) |

---

## Datapack Usage

Drop your advanced mob patch files under:

```
data/<namespace>/epicfight_mobpatch/
```

Each entity entry can use `nbt_tag` to route different patch configs by NBT value.

For full behavior and predicate syntax, use the Advanced Reference:
- [Advanced Behavior Reference](docs/ADVANCED_BEHAVIOR_REFERENCE.md)
- [Ranged Behavior Schema](docs/RANGED_BEHAVIOR_SCHEMA.md)

`nbt_tag` is universal: it can select a patch for any living mob, including
CustomNPCs. A matching NBT patch takes priority over the entity's normal
registry-path patch and is checked again at runtime, so multiple entities can
share one NBT-selected JSON file.

For TACZ combat, use the explicit sequence `tacz_aim` → `tacz_shoot` →
`reload`. Keep `looping: true` on the firing series when sustained aim and
fire behavior is required. Omit `humanoid_weapon_motions` when native Epic
Fight held-item living motions should be used.

---

## Documentation

- [Advanced Behavior Reference](docs/ADVANCED_BEHAVIOR_REFERENCE.md) — full syntax guide for behaviors, predicates, and gear_swap
- [Wiki](docs/WIKI.md) — general usage and setup
- [CurseForge Description](docs/CURSEFORGE_DESCRIPTION.md) — full feature breakdown
- [Changelog](changelog.txt) — version history

---

## Building

```bash
./gradlew clean reobfJar
```

Output: `build/libs/`

---

## License

See [LICENSE](LICENSE) and [CREDITS.txt](CREDITS.txt).
