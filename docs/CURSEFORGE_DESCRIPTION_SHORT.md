## EFI-Unofficial (Epic Fight Indestructible Unofficial)

EFI-Unofficial is a maintained continuation of Indestructible for Epic Fight `1.20.1`, built to make advanced datapack-driven combat easier to use with Epic Fight, TACZ, and CustomNPCs.

---

## What EFI-Unofficial Adds

- `nbt_tag` routing for assigning advanced patches by entity NBT
- Extended `gear_swap` support with TACZ `gun_id`, generated gear support, and multi-slot array swaps
- Better support for Epic Fight, TACZ, and CustomNPCs combat workflows
- Updated documentation for the full advanced behavior system

---

## Included Combat Features

- Weighted behavior series with cooldowns, interruption control, and phase logic
- Combat predicates for range, angle, stamina, phases, target state, gear, and ammo
- Built-in Epic Fight attack, guard, and movement behaviors
- TACZ ranged combat with `tacz_aim`, `tacz_shoot`, `reload`, and `ammo_slots`
- Dynamic gear switching with extended `gear_swap` support

EFI-Unofficial keeps the original advanced behavior system and adds new routing and gear-control features on top of it.

---

## Compatibility

- Epic Fight `20.14.11+`
- `CustomNPCs-1.20.1-GBPort-Unofficial-1.20.1.20260227`
- `tacz-1.20.1-1.1.7-hotfix2`

---

## Datapack Highlights

- Behavior series use `weight`, `cooldown`, `canBeInterrupted`, and `looping`
- Shared behavior fields include `conditions`, `set_phase`, and `end_by_hurt_level`
- Supported combat behaviors include `animation`, `guard`, `wander`, `gear_swap`, `reload`, `tacz_aim`, and `tacz_shoot`
- Supported predicates include `within_distance`, `within_angle`, `health`, `stamina`, `phase`, `has_target`, `target_blocking`, `has_ammo`, and more

For the full syntax reference, see [docs/ADVANCED_BEHAVIOR_REFERENCE.md](/c:/Antigravity_EpicFightCNPCADDON_Agent/Epic-Fight---Indestructible---1.20.1---latest-epic-fight-versions/docs/ADVANCED_BEHAVIOR_REFERENCE.md).