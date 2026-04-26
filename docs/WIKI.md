# EFI-Unofficial — Developer Wiki

**Version:** 20.14.16-unofficial  
**Platform:** Minecraft 1.20.1 · Forge 47.4.0  
**Dependencies:** EpicFight · TACZ (optional) · CustomNPCs (optional)

---

## Table of Contents

1. [Overview](#overview)
2. [Datapack Registration](#datapack-registration)
3. [Root JSON Fields](#root-json-fields)
4. [Attributes Reference](#attributes-reference)
5. [Ammo Slots](#ammo-slots)
6. [Combat Behaviors](#combat-behaviors)
7. [Behavior Types](#behavior-types)
8. [Predicates (Conditions)](#predicates-conditions)
9. [Guard Motions](#guard-motions)
10. [Events & Commands](#events--commands)
11. [TACZ Integration](#tacz-integration)
12. [Idle Reload](#idle-reload)
13. [Feature Status](#feature-status)
14. [Known Gotchas](#known-gotchas)

---

## Overview

EFI-Unofficial (Epic Fight Indestructible – Unofficial) extends the upstream
EFI mod with additional combat AI features, TACZ gun integration, CustomNPCs
support, and a richer datapack condition/predicate system.

The mod reads JSON datapacks from `data/<namespace>/advanced_mobpatch/` and
applies an `AdvancedCustomHumanoidMobPatch` to any registered EntityType,
layering combat behaviors on top of standard EpicFight AI.

This means you can keep base EpicFight behavior and add your own TACZ-style
combat flow (aim, shoot, reload, weapon swap, phase logic) through datapacks.

---

## Datapack Registration

Place a JSON file at:

```
data/<your_namespace>/advanced_mobpatch/<entity_registry_path>.json
```

Example for `customnpcs:customnpc`:
```
data/mypack/advanced_mobpatch/customnpcs/customnpc.json
```

The entity registry key must exist in the Forge entity registry. If the entity
is not found, a warning is logged and the file is skipped.

---

## Root JSON Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `model` | string (ResourceLocation) | ✅ | Path to the EpicFight JSON model |
| `armature` | string (ResourceLocation) | ✅ | Path to the EpicFight armature |
| `renderer` | string (ResourceLocation) | ✅ | Renderer key (or use `preset`) |
| `preset` | string | — | Renderer preset name (overrides `renderer`) |
| `faction` | string | ✅ | EpicFight faction (e.g. `"undead"`, `"monster"`) |
| `attributes` | object | ✅ | Stat block — see [Attributes](#attributes-reference) |
| `default_livingmotions` | object | — | Override default living motion animations |
| `stun_animations` | object | — | Override animations for each stun type |
| `combat_behavior` | list | — | Combat behavior series list |
| `humanoid_weapon_motions` | list | — | Per-weapon-category motion overrides |
| `custom_guard_motion` | list | — | Per-weapon-category guard animations |
| `ammo_slots` | list | — | Ammo source slots for TACZ guns |
| `boss_bar` | boolean | — | Show a boss health bar |
| `custom_name` | string (translatable key) | — | Boss bar name (requires `boss_bar: true`) |
| `custom_texture` | string (ResourceLocation) | — | Boss bar texture (requires `boss_bar: true`) |
| `swing_sound` | string (ResourceLocation) | — | Custom swing sound |
| `hit_sound` | string (ResourceLocation) | — | Custom hit sound |
| `hit_particle` | string (ResourceLocation) | — | Custom hit particle |
| `stun_command_list` | list | — | Commands triggered on each stun type |

---

## Attributes Reference

All attributes live inside the top-level `"attributes": {}` object.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `weight` | double | `40` | EpicFight weight attribute |
| `impact` | double | `0.5` | Impact (stun power) on hit |
| `armor_negation` | double | `0.0` | % armor ignored on hit |
| `max_stamina` | double | `15.0` | Maximum stamina pool |
| `stamina_regan_multiply` | double | `1.0` | Stamina regen rate multiplier |
| `max_strikes` | int | `1` | Max simultaneous hit targets |
| `attack_damage` | double | — | Override base attack damage |
| `max_stun_shield` | double | `0` | Stun shield HP (0 = disabled) |
| `scale` | double | `1.0` | Entity render scale |
| `chasing_speed` | double | `0` | Speed boost while chasing |
| `attack_radius` | double | `1.5` | Melee attack trigger radius |
| `guard_radius` | double | `3.0` | Guard/block engagement radius |
| `stamina_regan_delay` | int | `30` | Ticks to wait before regen starts |
| `stamina_lose_multiply` | double | `0.0` | Stamina drain multiplier on block |
| `has_stun_reduction` | boolean | `true` | Whether stun reduction is active |
| `stun_shield_regan_delay` | int | `30` | Ticks before stun shield regens |
| `stun_shield_regan_multiply` | double | `1.0` | Stun shield regen rate multiplier |

---

## Ammo Slots

Defines where the entity draws ammo from when using a TACZ gun.

`ammo_slots` works for both regular mobs and CustomNPCs. CustomNPCs are still
entity types in the same patch pipeline, so they follow the same field and
behavior rules.

Omit entirely if using `require_ammo: false` (infinite ammo mode).

```json
"ammo_slots": [
  { "slot": "customnpcs:projectile", "rule": "supply_or_reload_from" },
  { "slot": "inventory:0",           "rule": "supply_or_reload_from" },
  { "slot": "customnpcs:drop:0",     "rule": "supply_or_reload_from" },
  { "slot": "mainhand",              "rule": "supply_or_reload_from" }
]
```

### Slot Selectors

| Value | Meaning |
|-------|---------|
| `customnpcs:projectile` | CustomNPCs projectile slot |
| `customnpcs:drop:<N>` | CustomNPCs drop tab slot index N |
| `inventory:<N>` | Entity SimpleContainer inventory slot N |
| `mainhand` / `offhand` | Equipment slots |
| `head` / `chest` / `legs` / `feet` | Armor equipment slots |

### Rules

| Value | Meaning |
|-------|---------|
| `supply_or_reload_from` | Pull ammo from this slot for reloading |

---

## Combat Behaviors

`combat_behavior` is a list of **behavior entries**, each targeting specific
weapon categories and styles.

```json
"combat_behavior": [
  {
    "weapon_categories": ["sword"],
    "style": "one_hand",
    "behavior_series": [ ... ]
  }
]
```

### Behavior Series

Each entry in `behavior_series` defines a weighted, optionally-looping
sequence of actions:

```json
{
  "weight": 1.0,
  "cooldown": 0,
  "canBeInterrupted": false,
  "looping": false,
  "behaviors": [ ... ]
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `weight` | double | required | Selection weight vs other series |
| `cooldown` | int | `0` | Ticks before this series can be selected again |
| `canBeInterrupted` | boolean | `false` | If true, a stun during the series resets it |
| `looping` | boolean | `false` | If true, the series loops until interrupted |

---

## Behavior Types

Each object in `behaviors` must contain exactly one behavior key, plus an
optional `conditions` list and optional shared fields.

### Shared Fields (all behavior types)

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `conditions` | list | — | Predicates that must all pass to run this behavior |
| `set_phase` | int | `-1` | Change the NPC's phase to this value after the behavior |
| `end_by_hurt_level` | int | `2` | Hurt level at which this behavior is interrupted |

---

### `animation` — Play an attack animation

```json
{
  "animation": "epicfight:biped/attack/basic_attack",
  "play_speed": 1.0,
  "stamina": 0.0,
  "convert_time": 0.0,
  "damage_modifier": { ... },
  "command_list": [ ... ],
  "hit_command_list": [ ... ],
  "blocked_command_list": [ ... ]
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `animation` | string (ResourceLocation) | required | Animation to play |
| `play_speed` | double | `1.0` | Playback speed multiplier |
| `stamina` | double | `0.0` | Stamina cost to execute |
| `convert_time` | double | `0.0` | Time to wait before "committing" to the animation |
| `damage_modifier` | object | — | Override damage values for this animation |
| `command_list` | list | — | Time-stamped commands during the animation |
| `hit_command_list` | list | — | Commands triggered on hit |
| `blocked_command_list` | list | — | Commands triggered when blocked |

---

### `guard` — Block/parry stance

```json
{
  "guard": 40,
  "parry": false,
  "parry_times": 3,
  "stun_immunity_time": 0,
  "counter": "indestructible:biped/counter_attack",
  "counter_cost": 3.0,
  "counter_chance": 0.3,
  "counter_speed": 1.0,
  "cancel_after_counter": true,
  "specific_guard_motion": { ... }
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `guard` | int | required | Duration in ticks to hold guard |
| `parry` | boolean | `false` | Enable parry window |
| `parry_times` | int | `MAX_INT` | Maximum number of parries allowed |
| `stun_immunity_time` | int | `0` | Ticks of stun immunity after parry |
| `counter` | string (ResourceLocation) | default | Counter-attack animation |
| `counter_cost` | double | `3.0` | Stamina cost for counter |
| `counter_chance` | double | `0.3` | Probability (0–1) of triggering counter |
| `counter_speed` | double | `1.0` | Counter animation speed |
| `cancel_after_counter` | boolean | `true` | End the behavior series after counter |

---

### `wander` — Strafe / reposition

```json
{
  "wander": 20,
  "inaction_time": 20,
  "z_axis": 0.0,
  "x_axis": 0.0
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `wander` | int | required | Strafing duration in ticks |
| `inaction_time` | int | same as `wander` | Inaction ticks (blocks next behavior) |
| `z_axis` | double | `0.0` | Forward/backward strafe factor |
| `x_axis` | double | `0.0` | Left/right strafe factor |

---

### `reload` — Reload a held weapon

```json
{
  "reload": 40,
  "hand": "main_hand",
  "require_ammo": false,
  "stamina": 0.0,
  "command_list": [ ... ]
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `reload` | int | required | Inaction time while reloading |
| `hand` | string | `null` (auto) | `"main_hand"` or `"off_hand"` |
| `require_ammo` | boolean | `true` | `false` = infinite ammo, direct-fill |
| `stamina` | double | `0.0` | Stamina cost |
| `command_list` | list | — | Timed commands during reload |

> **`require_ammo: false`**: Bypasses all ammo item checks and directly fills
> the gun magazine to max capacity via TACZ's internal `setCurrentAmmoCount`.
> The NPC does not consume any items. Ideal for scripted enemies that should
> never run out of bullets.

---

### `tacz_aim` — Raise gun and aim at target

Long form (compound value):
```json
{
  "tacz_aim": { "hand": "main_hand", "inaction_time": 4, "stamina": 0.0, "command_list": [] }
}
```

Short form (integer = inaction_time):
```json
{
  "tacz_aim": 4,
  "hand": "main_hand",
  "stamina": 0.0
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `hand` | string | auto | Which hand holds the gun |
| `inaction_time` | int | `4` | Ticks of aim-settling before shoot |
| `stamina` | double | `0.0` | Stamina cost |

---

### `tacz_shoot` — Fire the held gun

Long form (compound value):
```json
{
  "tacz_shoot": { "fire_mode": "AUTO", "stamina": 0.0, "command_list": [] }
}
```

Short form (boolean `true` = use gun's default fire mode):
```json
{
  "tacz_shoot": true,
  "fire_mode": "AUTO",
  "stamina": 0.0
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `fire_mode` | string | `null` (gun default) | `"AUTO"`, `"SEMI"`, `"BURST"` |
| `stamina` | double | `0.0` | Stamina cost per shot |
| `command_list` | list | — | Timed commands triggered on fire |

> **Fire mode values** correspond to TACZ's `FireMode` enum names. If the
> requested mode is not available on the gun, the current mode is kept.

> **Fire rate**: After a successful shot, `inactionTime` is set to
> `getShootIntervalTicks()` — derived directly from TACZ's `GunData`
> shoot interval in milliseconds (÷ 50 ms/tick, floored). The shooting
> animation plays before this override, ensuring the gun's RPM governs pace
> rather than the animation duration.

---

## Predicates (Conditions)

Each `conditions` entry has the form:

```json
{ "predicate": "<type>", ... }
```

Namespace prefix (`epicfight:`, `indestructible:`) is stripped — only the
local name is matched.

### Upstream EpicFight Predicates

| Predicate | Arguments | Description |
|-----------|-----------|-------------|
| `random_chance` | `chance: double` | Fires with probability `chance` (0–1) |
| `within_eye_height` | — | Target is at eye height |
| `within_distance` | `min: double, max: double` | Target distance range |
| `within_angle` | `min: double, max: double` | Target angle range (degrees) |
| `within_angle_horizontal` | `min: double, max: double` | Horizontal angle range |
| `health` | `health: double, comparator: string` | Self health check (`LESS_ABSOLUTE`, `GREATER_ABSOLUTE`, `LESS_RATIO`, `GREATER_RATIO`) |

### EFI-Unofficial Predicates

| Predicate | Arguments | Description |
|-----------|-----------|-------------|
| `guard_break` | `invert: boolean` | Target is in a guard-break state |
| `knock_down` | `invert: boolean` | Target is knocked down |
| `attack_level` | `min: int, max: int` | Target hurt level is within range |
| `stamina` | `stamina: double, comparator: string` | Self stamina check (same comparators as `health`) |
| `using_item` | `edible: boolean` | Target is using an item (`edible: true` = food/potions only) |
| `mainhand_weapon_category` | `category: string` | Self main-hand weapon category matches |
| `offhand_weapon_category` | `category: string` | Self off-hand weapon category matches |
| `has_gear_in_inventory` | `item: string, slot?: string, include_equipped?: boolean` | Checks inventory (and optionally equipped slots) for an item |
| `has_ammo` | `hand?: string, invert?: boolean` | Gun in hand has ammo loaded |
| `ammo_is_empty` | `hand?: string, invert?: boolean` | Gun magazine is at 0 |
| `ammo_not_full` | `hand?: string, invert?: boolean` | Gun magazine is below max |
| `ammo_has_reserve` | `hand?: string, invert?: boolean` | Reserve ammo exists in `ammo_slots` |
| `target_blocking` | `invert?: boolean` | Target is actively blocking |
| `target_using_shield` | `invert?: boolean` | Target is holding a shield |
| `no_target` | — | NPC has no current attack target |
| `has_target` | — | NPC has a current attack target |
| `phase` | `min: int, max: int` | NPC's current phase is within range |

> **Note:** `no_target` and `has_target` only fire via behavior predicates
> when the combat goal is active (i.e., a target was recently valid). For
> truly target-free reload logic, use the automatic [Idle Reload](#idle-reload)
> instead.

---

## Guard Motions

Override the guard/block animation per weapon category and style:

```json
"custom_guard_motion": [
  {
    "weapon_categories": ["sword", "longsword"],
    "style": "one_hand",
    "guard": "indestructible:biped/guard/guard_sword",
    "can_block_projectile": false,
    "stamina_cost_multiply": 1.0,
    "parry_cost_multiply": 0.5,
    "parry_animation": [
      "indestructible:biped/parry/parry_sword"
    ]
  }
]
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `weapon_categories` | string or list | required | Weapon category/categories |
| `style` | string | required | Weapon style |
| `guard` | string (ResourceLocation) | default longsword guard | Guard animation |
| `can_block_projectile` | boolean | `false` | Whether this guard blocks arrows/bullets |
| `stamina_cost_multiply` | double | `1.0` | Stamina drain multiplier per blocked hit |
| `parry_cost_multiply` | double | `0.5` | Stamina drain for a parry |
| `parry_animation` | list of strings | — | Parry animation(s), chosen randomly |

---

## Events & Commands

### `command_list` (timed, during animation)

```json
"command_list": [
  { "time": 0.3, "command": "/say Hello" }
]
```

`time` is a normalized value from `0.0` to `1.0` representing the progress
through the animation when the command fires.

### `hit_command_list`

Fires when an attack animation successfully hits an entity.

### `blocked_command_list`

Fires when an attack animation hit is blocked by the target.

### `stun_command_list` (root level)

```json
"stun_command_list": [
  { "stun_type": "SHORT", "command": "/say I was stunned" }
]
```

Fires at the root mob level on each stun event.

---

## TACZ Integration

The entire TACZ API is accessed through reflection so the mod does not require
TACZ as a compile-time dependency. All TACZ features degrade gracefully to
no-ops if TACZ is absent.

### Current Datapack Setup (TACZ-style NPCs)

For TACZ-style combat entities, the recommended behavior flow is:

1. `tacz_aim`
2. `tacz_shoot`
3. `reload` (or `require_ammo: false` for infinite-ammo NPCs)
4. Optional `gear_swap` / phase transition / movement behavior

This setup works for both standard mobs and CustomNPCs.

Use `ammo_slots` when you want reserve-ammo behavior from inventory/equipment,
or set `require_ammo: false` when you want scripted enemies to always reload
without consuming items.

### How Shooting Works (per tick)

1. **Combat goal fires** `tryTaczShoot()` when the behavior predicate passes.
2. `TaczCompat.tryShoot()` calls `IGunOperator.shoot(pitchSupplier, yawSupplier)` via reflection.
3. TACZ's internal rate limiter handles actual bullet emission.
4. On success, `inactionTime = getShootIntervalTicks()` is set directly (not via `Math.max`) so the gun's own RPM governs how fast the NPC fires.
5. If the magazine is empty on a failed shot, an immediate reload is triggered.

### `getShootIntervalTicks()`

Reads `GunData.getShootInterval(entity, fireMode, stack)` (returns ms), divides
by 50 ms/tick using integer floor division:

```
ticks = max(1, (int)(intervalMs / 50.0))
```

This matches player fire rate exactly. (A `Math.round` was the prior bug — for
a 75 ms interval, round gives 2 ticks instead of the correct 1 tick.)

### Reload Modes

| Mode | `require_ammo` | Behavior |
|------|---------------|----------|
| Standard | `true` (default) | Pulls ammo from `ammo_slots`, triggers TACZ reload animation |
| Infinite | `false` | Directly calls `IGun.setCurrentAmmoCount(stack, maxAmmo)` — no item consumed |

### Fire Mode Selection

If `fire_mode` is specified in `tacz_shoot`, the system enumerates the gun's
supported modes and switches to the requested one before firing. If the mode
is unavailable on that gun, the current mode is unchanged.

---

## Idle Reload

When the NPC has no active target (or target is dead), the server tick
automatically reloads the gun every 40 ticks (~2 seconds) if the magazine is
not full. This ensures the NPC enters the next fight with a full magazine.

- Condition: `target == null || !target.isAlive()`
- Condition: not already reloading
- Condition: `isAmmoNotFull(null)` returns true
- Uses `require_ammo=false` → direct-fill, no item consumed

This logic runs unconditionally inside `serverTick` and does not require any
datapack entry.

---

## Feature Status

### ✅ Confirmed Working

| Feature | Notes |
|---------|-------|
| TACZ gun detection | Via reflection, degrades to no-op without TACZ |
| TACZ shooting | `tryShoot` with pitch/yaw suppliers |
| Fire mode selection (`AUTO`, `SEMI`, `BURST`) | Enumerates supported modes |
| Infinite ammo reload (`require_ammo: false`) | Direct `setCurrentAmmoCount` fill |
| Ammo predicates (`ammo_is_empty`, `ammo_not_full`, `ammo_has_reserve`) | All functional |
| CustomNPCs slot access (`customnpcs:projectile`, `customnpcs:drop:<N>`) | Via reflected `ItemStackWrapper` constructor |
| Idle reload (serverTick auto-fill) | 40-tick interval, target-free only |
| Stun shield system | `max_stun_shield` > 0 enables |
| Stamina system | Regen, drain, predicates all functional |
| Guard / parry / counter system | Full guard behavior pipeline |
| Boss bar | `boss_bar: true` with optional custom texture |
| Gear swap behavior | `gear_swap` with slot targeting |
| Phase system | `set_phase`, `phase` predicate |
| TACZ aim behavior | `tacz_aim` — sets IGunOperator aim state |
| All behavior predicates | 20 predicate types functional |
| Wander / strafe behavior | `wander` with axis control |
| Timed command events | `command_list`, `hit_command_list`, `blocked_command_list` |
| Stun command events | `stun_command_list` |
| Custom animations | `animation` behavior with full modifier support |

### ⚠️ Untested / Experimental

| Feature | Risk |
|---------|------|
| Crossbow / ProjectileWeaponItem reload via `reload` behavior | Vanilla path — should work but not verified |
| `ammo_has_reserve` with `customnpcs:drop` slots | CustomNPCs API edge cases possible |
| `has_gear_in_inventory` with `SimpleContainer` mobs | Only works if entity implements `InventoryCarrier` |
| Multiple concurrent TACZ guns (dual-wield) | Hand resolution logic uses `findGunHand`; only one gun fires at a time |
| `stun_command_list` format | Parsing confirmed; command execution not stress-tested |
| Non-humanoid entity types | `AdvancedCustomHumanoidMobPatch` expects humanoid armature |

---

## Known Gotchas

### Datapack path must match entity id

The file path must match the entity registry key exactly.
Example: `customnpcs:customnpc` ->
`data/<namespace>/advanced_mobpatch/customnpcs/customnpc.json`

### `ammo_slots` order is priority order

Slots are checked top to bottom. Put your preferred source first.
Example: keep `customnpcs:projectile` before `inventory:<N>` if projectile ammo
should be consumed first.

### `require_ammo: false` is infinite-ammo mode

When `require_ammo` is `false`, reload fills the gun directly and does not
consume reserve ammo items.

### Use `has_target` / `no_target` intentionally

Target-based predicates are best used for combat-state branching. For simple
out-of-combat refill behavior, rely on idle reload.

### Works for mobs and CustomNPCs

Do not split configs by "mob vs CNPC" logic. Use the same behavior model and
slot system unless you need CustomNPC-specific slot selectors.
