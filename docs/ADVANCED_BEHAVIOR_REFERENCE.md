# EFI Advanced Behavior Reference

This document describes the supported `combat_behavior` syntax in EFI-Unofficial, including behavior series fields, shared behavior fields, supported combat behaviors, supported predicates, and JSON syntax examples for each.

This reference is written from a datapack point of view.

Use it in this order:

1. Top-level behavior series structure
2. Shared behavior fields
3. Combat behavior types
4. Predicate types
5. Accepted slot and hand values

Where origin matters, entries are marked as either:

- `Original` for features already present in the original mod
- `EFI-Unofficial` for newer features added on top

---

## Table of Contents

1. [Top-Level Behavior Series Structure](#top-level-behavior-series-structure)
2. [Shared Behavior Fields](#shared-behavior-fields)
3. [Combat Behaviors](#combat-behaviors)
4. [Predicates](#predicates)
5. [Slot and Hand Values](#slot-and-hand-values)
6. [Notes and Gotchas](#notes-and-gotchas)

---

## Top-Level Behavior Series Structure

Each `weapon_categories` entry contains one or more `behavior_series` entries. A behavior series is a weighted sequence of behavior steps.

### Syntax

```json
{
  "weight": 1.0,
  "cooldown": 0,
  "canBeInterrupted": false,
  "looping": false,
  "behaviors": [
    {
      "conditions": [
        {
          "predicate": "within_distance",
          "min": 0.0,
          "max": 3.0
        }
      ],
      "animation": "epicfight:biped/combat/slash",
      "set_phase": 0
    }
  ]
}
```

### Supported Fields

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `weight` | number | yes | none | Relative selection weight when multiple series are valid |
| `cooldown` | integer | no | `0` | Ticks before this series can be selected again |
| `canBeInterrupted` | boolean | no | `false` | Whether the series can be interrupted once it starts |
| `looping` | boolean | no | `false` | Whether the series loops back to the first behavior after finishing |
| `behaviors` | list | yes | none | Ordered list of behavior steps |

---

## Shared Behavior Fields

Every behavior step supports these wrapper fields in addition to its own behavior-specific fields.

### Syntax

```json
{
  "conditions": [
    {
      "predicate": "has_target"
    }
  ],
  "animation": "epicfight:biped/combat/slash",
  "set_phase": 1,
  "end_by_hurt_level": 2
}
```

### Supported Fields

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `conditions` | list | no | empty list | All predicates in the list must pass for the behavior to run |
| `set_phase` | integer | no | `-1` | Sets the NPC's combat phase after the behavior executes |
| `end_by_hurt_level` | integer | no | `2` | Controls how easily incoming damage interrupts the behavior |

---

## Combat Behaviors

Each behavior object must contain one supported behavior key.

### `animation`

Primary Epic Fight animation behavior.

Origin: `Original`

#### Syntax

```json
{
  "conditions": [],
  "animation": "epicfight:biped/combat/slash",
  "play_speed": 1.0,
  "stamina": 0.0,
  "convert_time": 0.0,
  "damage_modifier": {
    "damage": 8.0
  },
  "command_list": [],
  "hit_command_list": [],
  "blocked_command_list": []
}
```

#### Supported Fields

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `animation` | string | yes | none | Animation registry key |
| `play_speed` | number | no | `1.0` | Playback speed multiplier |
| `stamina` | number | no | `0.0` | Stamina cost |
| `convert_time` | number | no | `0.0` | Transition time before fully committing to the animation |
| `damage_modifier` | object | no | `null` | Optional damage override block |
| `command_list` | list | no | `null` | Timed commands during the animation |
| `hit_command_list` | list | no | `null` | Commands fired when a hit lands |
| `blocked_command_list` | list | no | `null` | Commands fired when the attack is blocked |

---

### `guard`

Puts the NPC into a guard or parry state.

Origin: `Original`

#### Syntax

```json
{
  "conditions": [],
  "guard": 30,
  "counter": "indestructible:biped/counter_attack",
  "parry": true,
  "parry_times": 2,
  "stun_immunity_time": 10,
  "counter_cost": 3.0,
  "counter_chance": 0.3,
  "counter_speed": 1.0,
  "cancel_after_counter": true,
  "specific_guard_motion": {
    "guard": "epicfight:biped/guard"
  }
}
```

#### Supported Fields

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `guard` | integer | yes | none | Guard duration in ticks |
| `counter` | string | no | default guard counter animation | Counterattack animation |
| `parry` | boolean | no | `false` | Enables parry behavior |
| `parry_times` | integer | no | see below | Maximum number of parries |
| `parry_time` | integer | no | see below | Legacy alias used if `parry_times` is absent |
| `stun_immunity_time` | integer | no | `0` | Stun immunity time after successful guard/parry |
| `counter_cost` | number | no | `3.0` | Stamina cost of the counter |
| `counter_chance` | number | no | `0.3` | Chance of countering |
| `counter_speed` | number | no | `1.0` | Playback speed of counter animation |
| `cancel_after_counter` | boolean | no | `true` | Whether to stop the behavior series after the counter |
| `specific_guard_motion` | object | no | `null` | Optional guard motion override block |

If neither `parry_times` nor `parry_time` is provided, the parser uses an effectively unlimited value.

---

### `wander`

Moves the NPC without attacking, usually for strafing or repositioning.

Origin: `Original`

#### Syntax

```json
{
  "conditions": [],
  "wander": 20,
  "inaction_time": 20,
  "z_axis": 0.0,
  "x_axis": 1.0
}
```

#### Supported Fields

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `wander` | integer | yes | none | Movement duration in ticks |
| `inaction_time` | integer | no | same as `wander` | Recovery/pause time after movement |
| `z_axis` | number | no | `0.0` | Forward or backward movement bias |
| `x_axis` | number | no | `0.0` | Sideways or circular movement bias |

---

### `gear_swap` (single-object form)

Swaps one piece of gear during combat.

Origin: `EFI-Unofficial`

#### Syntax

```json
{
  "conditions": [],
  "gear_swap": {
    "item": "minecraft:iron_sword",
    "slot": "main_hand",
    "allow_equipped": true,
    "store_gear": true,
    "required_in_inv": true,
    "nbt": {
      "CustomModelData": 1
    },
    "stamina": 0.0,
    "inaction_time": 0,
    "sound": "minecraft:item.armor.equip_iron"
  }
}
```

#### TACZ gun syntax

```json
{
  "gear_swap": {
    "gun_id": "tacz:ak47",
    "slot": "main_hand",
    "required_in_inv": false,
    "stamina": 0.0,
    "inaction_time": 8
  }
}
```

#### Supported Fields

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `item` | string | conditionally | none | Item to equip; required unless `gun_id` is used |
| `gun_id` | string | no | `null` | TACZ `GunId`; if present and `item` is absent, item defaults to `tacz:modern_kinetic_gun` |
| `slot` | string | no | auto-detect | Target equipment slot |
| `allow_equipped` | boolean | no | `true` | Allows an already equipped matching item to satisfy the swap |
| `store_gear` | boolean | no | `true` | Whether the previous gear should be stored |
| `required_in_inv` | boolean | no | `true` | Requires the item to already exist in inventory; if `false`, EFI may generate it |
| `nbt` | object or string | no | `null` | Optional NBT to match or generate on the item |
| `stamina` | number | no | `0.0` | Stamina cost |
| `inaction_time` | integer | no | `0` | Pause after the swap |
| `sound` | string | no | `null` | Sound event to play on swap |

For single-object syntax, `stamina`, `inaction_time`, and `sound` live inside the `gear_swap` object.

---

### `gear_swap` (array form)

Atomically swaps multiple slots in one behavior step.

Origin: `EFI-Unofficial`

#### Syntax

```json
{
  "conditions": [],
  "gear_swap": [
    {
      "item": "minecraft:iron_sword",
      "slot": "main_hand",
      "store_gear": true,
      "required_in_inv": true,
      "nbt": {
        "CustomModelData": 1
      }
    },
    {
      "item": "minecraft:shield",
      "slot": "off_hand",
      "store_gear": true,
      "required_in_inv": true
    }
  ],
  "stamina": 0.0,
  "inaction_time": 8,
  "sound": "minecraft:item.armor.equip_iron"
}
```

#### TACZ gun + offhand example

```json
{
  "gear_swap": [
    {
      "gun_id": "tacz:ak47",
      "slot": "main_hand",
      "required_in_inv": false
    },
    {
      "item": "minecraft:iron_sword",
      "slot": "off_hand",
      "required_in_inv": false
    }
  ],
  "stamina": 0.0,
  "inaction_time": 8
}
```

#### Per-entry Supported Fields

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `item` | string | conditionally | none | Item to equip; required unless `gun_id` is used |
| `gun_id` | string | no | `null` | TACZ `GunId`; if present and `item` is absent, item defaults to `tacz:modern_kinetic_gun` |
| `slot` | string | no | auto-detect | Target equipment slot |
| `store_gear` | boolean | no | `true` | Whether the previous gear should be stored |
| `required_in_inv` | boolean | no | `true` | Requires the item to already exist in inventory |
| `nbt` | object or string | no | `null` | Optional NBT to match or generate on the item |

#### Outer Behavior Fields

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `stamina` | number | no | `0.0` | Total stamina cost for the full multi-swap |
| `inaction_time` | integer | no | `0` | Pause after the multi-swap |
| `sound` | string | no | `null` | Sound event to play after the multi-swap |

For array syntax, `stamina`, `inaction_time`, and `sound` belong on the outer behavior object, not inside each entry.

---

### `reload`

Triggers TACZ reload behavior.

Origin: `EFI-Unofficial`

#### Syntax

```json
{
  "conditions": [],
  "reload": 20,
  "hand": "main_hand",
  "require_ammo": true,
  "stamina": 0.0,
  "command_list": []
}
```

#### Supported Fields

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `reload` | integer | yes | none | Reload duration in ticks |
| `hand` | string | no | `null` | Which hand to reload |
| `require_ammo` | boolean | no | `true` | Whether reserve ammo is required |
| `stamina` | number | no | `0.0` | Stamina cost |
| `command_list` | list | no | `null` | Timed commands during reload |

---

### `tacz_aim`

Puts the NPC into TACZ aiming state.

Origin: `EFI-Unofficial`

#### Object-form syntax

```json
{
  "conditions": [],
  "tacz_aim": {
    "hand": "main_hand",
    "inaction_time": 8,
    "stamina": 0.0,
    "command_list": []
  }
}
```

#### Short-form syntax

```json
{
  "tacz_aim": 8,
  "hand": "main_hand",
  "stamina": 0.0,
  "command_list": []
}
```

#### Supported Fields

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `hand` | string | no | `null` | Which hand is aiming |
| `inaction_time` | integer | object form only | `4` | How long the aim behavior holds |
| `stamina` | number | no | `0.0` | Stamina cost |
| `command_list` | list | no | `null` | Timed commands during aim |

In short form, the integer value assigned to `tacz_aim` is used as `inaction_time`.

---

### `tacz_shoot`

Fires a TACZ weapon.

Origin: `EFI-Unofficial`

#### Object-form syntax

```json
{
  "conditions": [],
  "tacz_shoot": {
    "fire_mode": "semi",
    "stamina": 0.0,
    "command_list": []
  }
}
```

#### Short-form syntax

```json
{
  "tacz_shoot": 1,
  "fire_mode": "semi",
  "stamina": 0.0,
  "command_list": []
}
```

#### Supported Fields

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `fire_mode` | string | no | `null` | Fire mode override |
| `stamina` | number | no | `0.0` | Stamina cost |
| `command_list` | list | no | `null` | Timed commands during shooting |

In short form, the integer value on `tacz_shoot` is only used as the trigger form selector by the parser. The shot behavior itself is controlled by the surrounding fields.

---

## Predicates

Predicates are placed inside a behavior's `conditions` list.

General syntax:

```json
{
  "predicate": "within_distance",
  "min": 0.0,
  "max": 5.0
}
```

### `random_chance`

Origin: `Original`

#### Syntax

```json
{
  "predicate": "random_chance",
  "chance": 0.5
}
```

Fields: `chance`

---

### `within_eye_height`

Origin: `Original`

#### Syntax

```json
{
  "predicate": "within_eye_height"
}
```

Fields: none

---

### `within_distance`

Origin: `Original`

#### Syntax

```json
{
  "predicate": "within_distance",
  "min": 0.0,
  "max": 5.0
}
```

Fields: `min`, `max`

---

### `within_angle`

Origin: `Original`

#### Syntax

```json
{
  "predicate": "within_angle",
  "min": -45.0,
  "max": 45.0
}
```

Fields: `min`, `max`

---

### `within_angle_horizontal`

Origin: `Original`

#### Syntax

```json
{
  "predicate": "within_angle_horizontal",
  "min": -90.0,
  "max": 90.0
}
```

Fields: `min`, `max`

---

### `health`

Origin: `Original`

#### Syntax

```json
{
  "predicate": "health",
  "health": 10.0,
  "comparator": "LESS"
}
```

Fields: `health`, `comparator`

`comparator` is passed directly into the `HealthPoint.Comparator` enum using uppercase matching.

---

### `guard_break`

Origin: `Original`

#### Syntax

```json
{
  "predicate": "guard_break",
  "invert": false
}
```

Fields: `invert`

---

### `knock_down`

Origin: `Original`

#### Syntax

```json
{
  "predicate": "knock_down",
  "invert": false
}
```

Fields: `invert`

---

### `attack_level`

Origin: `Original`

#### Syntax

```json
{
  "predicate": "attack_level",
  "min": 0,
  "max": 2
}
```

Fields: `min`, `max`

---

### `stamina`

Origin: `Original`

#### Syntax

```json
{
  "predicate": "stamina",
  "stamina": 5.0,
  "comparator": "GREATER"
}
```

Fields: `stamina`, `comparator`

`comparator` is passed directly into the `HealthPoint.Comparator` enum using uppercase matching.

---

### `using_item`

Origin: `Original`

#### Syntax

```json
{
  "predicate": "using_item",
  "edible": false
}
```

Fields: `edible`

---

### `mainhand_weapon_category`

Origin: `EFI-Unofficial`

#### Syntax

```json
{
  "predicate": "mainhand_weapon_category",
  "category": "dagger"
}
```

Fields: `category`

---

### `offhand_weapon_category`

Origin: `EFI-Unofficial`

#### Syntax

```json
{
  "predicate": "offhand_weapon_category",
  "category": "shield"
}
```

Fields: `category`

---

### `has_gear_in_inventory`

Origin: `EFI-Unofficial`

#### Syntax

```json
{
  "predicate": "has_gear_in_inventory",
  "item": "minecraft:iron_sword",
  "slot": "main_hand",
  "include_equipped": true
}
```

Fields: `item`, `slot`, `include_equipped`

---

### `has_ammo`

Origin: `EFI-Unofficial`

#### Syntax

```json
{
  "predicate": "has_ammo",
  "hand": "main_hand",
  "invert": false
}
```

Fields: `hand`, `invert`

If `hand` is omitted, the parser defaults to `main_hand`.

---

### `ammo_is_empty`

Origin: `EFI-Unofficial`

#### Syntax

```json
{
  "predicate": "ammo_is_empty",
  "hand": "main_hand",
  "invert": false
}
```

Fields: `hand`, `invert`

---

### `ammo_not_full`

Origin: `EFI-Unofficial`

#### Syntax

```json
{
  "predicate": "ammo_not_full",
  "hand": "main_hand",
  "invert": false
}
```

Fields: `hand`, `invert`

---

### `ammo_has_reserve`

Origin: `EFI-Unofficial`

#### Syntax

```json
{
  "predicate": "ammo_has_reserve",
  "hand": "main_hand",
  "invert": false
}
```

Fields: `hand`, `invert`

---

### `target_blocking`

Origin: `EFI-Unofficial`

#### Syntax

```json
{
  "predicate": "target_blocking",
  "invert": false
}
```

Fields: `invert`

---

### `target_using_shield`

Origin: `EFI-Unofficial`

#### Syntax

```json
{
  "predicate": "target_using_shield",
  "invert": false
}
```

Fields: `invert`

---

### `no_target`

Origin: `EFI-Unofficial`

#### Syntax

```json
{
  "predicate": "no_target"
}
```

Fields: none

---

### `has_target`

Origin: `EFI-Unofficial`

#### Syntax

```json
{
  "predicate": "has_target"
}
```

Fields: none

---

### `phase`

Origin: `Original`

#### Syntax

```json
{
  "predicate": "phase",
  "min": 0,
  "max": 1
}
```

Fields: `min`, `max`

---

## Slot and Hand Values

### Equipment slot values

Supported values accepted by the parser:

- `mainhand`
- `main_hand`
- `offhand`
- `off_hand`
- `head`
- `helmet`
- `chest`
- `chestplate`
- `legs`
- `leggings`
- `feet`
- `boots`

### Hand values

Supported values accepted by the parser:

- `mainhand`
- `main_hand`
- `offhand`
- `off_hand`

---

## Notes and Gotchas

- `conditions` are ANDed together. Every predicate in the list must pass.
- Predicate names may optionally be namespaced. The parser strips the namespace and matches the final path segment.
- `gear_swap` supports both a single object and a list of objects. The list form is the EFI-Unofficial multi-slot extension.
- In multi-slot `gear_swap`, `allow_equipped` is not parsed per entry. Only `item`, `gun_id`, `slot`, `store_gear`, `required_in_inv`, and `nbt` are supported there.
- In single-slot `gear_swap`, stamina and timing fields live inside the `gear_swap` object. In multi-slot `gear_swap`, they live on the outer behavior object.
- `tacz_shoot` short form exists, but the integer value is not read as a duration. Use the object form when you want clearer authoring.
- `gun_id` writes TACZ `GunId` data onto generated gear when needed.
