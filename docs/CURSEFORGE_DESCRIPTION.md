## EFI-Unofficial (Epic Fight Indestructible Unofficial)

EFI-Unofficial is a maintained continuation of Indestructible for Epic Fight `1.20.1`.

The original mod established the advanced mob patch system: behavior series, combat predicates, phase logic, guard logic, and datapack-driven combat behavior. EFI-Unofficial keeps that base and adds new routing, better gear control, and stronger support for Epic Fight, TACZ, and CustomNPCs workflows.

---

## What's Different from the Original

EFI-Unofficial does not replace the original advanced behavior system. It keeps the original features and adds new functionality on top.

EFI-Unofficial extends that base with:

- `nbt_tag` routing, allowing advanced patches to be selected by entity NBT instead of only fixed registration paths
- Extended `gear_swap` support, including richer item matching, TACZ `gun_id` support, and multi-slot array syntax
- Better support for modern Epic Fight, CustomNPCs, and TACZ-driven datapack workflows
- Universal runtime `nbt_tag` patch selection for generic mobs and CustomNPCs
- Equipment retention and native held-item living-motion synchronization
- Reliable NPC-to-NPC Epic Fight stun behavior and sustained TACZ aim/shoot/reload cycles
- Full documentation for the advanced behavior system and its syntax

---

## Compatibility

- Epic Fight `20.14.17`
- `CustomNPCs-1.20.1.20260711` (CurseForge file `8414335`)
- `tacz-1.20.1-1.1.8-hotfix`
- Forge `47.4.0` on Minecraft `1.20.1`

---

## Available Combat Features

### `nbt_tag` Mob Patching

EFI-Unofficial adds `nbt_tag`-based routing for advanced mob patches.
Instead of being limited to patch selection by entity registration alone, datapacks can now assign advanced combat behavior based on an entity's NBT data.

This makes it possible to give one specific mob variant a completely different Epic Fight behavior set without changing every entity of the same type.

### TACZ Combat Support

EFI-Unofficial supports TACZ ranged combat directly in datapacks.
Humanoid mobs can aim, shoot, and reload through datapack-defined behavior logic, making ranged combat patterns possible without relying on command chains.

### Gear Swapping

The `gear_swap` behavior allows NPCs to switch weapons or equipment mid-combat when behavior conditions are met.
EFI-Unofficial expands this further with richer TACZ handling and multi-slot swap support, enabling melee-to-ranged transitions, shield swaps, dual-wield setups, and coordinated loadout changes directly inside the combat behavior system.

### Ammo Routing

The `ammo_slots` field gives datapack authors control over where TACZ NPCs source reserve ammo.
This makes custom loadouts more reliable and allows tighter control over reload behavior and ammunition management.

---

## Supported Predicates

These predicates are available in EFI-Unofficial's advanced patch system.

Some of these were already part of the original mod, while others were added by EFI-Unofficial.

- Original predicates include `phase`, `stamina`, `guard_break`, `knock_down`, `attack_level`, and `using_item`
- EFI-Unofficial additions include gear, ammo, weapon-category, and target-state predicates used for newer combat logic

### Combat & State

`phase` · `stamina` · `guard_break` · `knock_down` · `attack_level`

### Target & Defense

`has_target` · `no_target` · `target_blocking` · `target_using_shield` · `using_item`

### Weapon, Gear & Ammo

`mainhand_weapon_category` · `offhand_weapon_category` · `has_gear_in_inventory` · `has_ammo` · `ammo_is_empty` · `ammo_not_full` · `ammo_has_reserve`

---

## Advanced Behavior Breakdown

At the datapack level, combat logic is built from behavior series, individual behaviors, and predicate-based conditions.

### Behavior Series Fields

Every entry in a `weapon_categories` block is made of one or more behavior series.

- `weight`
  Relative selection weight when multiple series are valid
- `cooldown`
  How many ticks must pass before this same series can be selected again
- `canBeInterrupted`
  Whether the series can be interrupted once it starts
- `looping`
  Whether the series repeats from the beginning after the last step
- `behaviors`
  The ordered list of actions that make up the series

#### Syntax

```json
{
  "weight": 1.0,
  "cooldown": 0,
  "canBeInterrupted": false,
  "looping": false,
  "behaviors": [
    {
      "animation": "epicfight:biped/combat/slash"
    }
  ]
}
```

### Shared Behavior Fields

Every behavior step can also use these wrapper fields:

- `conditions`
  A list of predicates that must all pass before the behavior runs
- `set_phase`
  Sets the NPC's internal combat phase when the behavior executes
- `end_by_hurt_level`
  Controls how easily incoming damage interrupts the behavior

#### Syntax

```json
{
  "animation": "epicfight:biped/combat/slash",
  "set_phase": 1,
  "end_by_hurt_level": 2,
  "conditions": [
    {
      "predicate": "has_target"
    }
  ]
}
```

---

## Supported Combat Behaviors

### `animation`

Plays an Epic Fight attack or action animation.

Supported fields:

- `animation`
- `play_speed`
- `stamina`
- `convert_time`
- `damage_modifier`
- `command_list`
- `hit_command_list`
- `blocked_command_list`

#### Syntax

```json
{
  "animation": "epicfight:biped/combat/slash",
  "play_speed": 1.0,
  "stamina": 0.0,
  "convert_time": 0.0,
  "command_list": [],
  "hit_command_list": [],
  "blocked_command_list": []
}
```

### `guard`

Places the NPC into a guard or parry state.

Supported fields:

- `guard`
- `counter`
- `parry`
- `parry_times` or `parry_time`
- `stun_immunity_time`
- `counter_cost`
- `counter_chance`
- `counter_speed`
- `cancel_after_counter`
- `specific_guard_motion`

#### Syntax

```json
{
  "guard": 30,
  "counter": "indestructible:biped/counter_attack",
  "parry": true,
  "parry_times": 2,
  "stun_immunity_time": 10,
  "counter_cost": 3.0,
  "counter_chance": 0.3,
  "counter_speed": 1.0,
  "cancel_after_counter": true
}
```

### `wander`

Moves the NPC without attacking, usually for repositioning or strafing.

Supported fields:

- `wander`
- `inaction_time`
- `z_axis`
- `x_axis`

#### Syntax

```json
{
  "wander": 20,
  "inaction_time": 20,
  "z_axis": 0.0,
  "x_axis": 1.0
}
```

### `gear_swap`

Swaps gear during combat.

EFI supports both a single-slot form and a multi-slot array form.

Supported single-swap fields:

- `item`
- `gun_id`
- `slot`
- `allow_equipped`
- `store_gear`
- `required_in_inv`
- `nbt`
- `stamina`
- `inaction_time`
- `sound`

#### Single-swap syntax

```json
{
  "gear_swap": {
    "item": "minecraft:iron_sword",
    "slot": "main_hand",
    "allow_equipped": true,
    "store_gear": true,
    "required_in_inv": true,
    "stamina": 0.0,
    "inaction_time": 8,
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
    "inaction_time": 8
  }
}
```

#### Multi-swap syntax

```json
{
  "gear_swap": [
    {
      "item": "minecraft:iron_sword",
      "slot": "main_hand",
      "store_gear": true,
      "required_in_inv": true
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

In the multi-swap form, each entry supports:

- `item`
- `gun_id`
- `slot`
- `store_gear`
- `required_in_inv`
- `nbt`

And the outer behavior supports:

- `stamina`
- `inaction_time`
- `sound`

### `reload`

Triggers TACZ reload behavior.

Supported fields:

- `reload`
- `hand`
- `require_ammo`
- `stamina`
- `command_list`

#### Syntax

```json
{
  "reload": 20,
  "hand": "main_hand",
  "require_ammo": true,
  "stamina": 0.0,
  "command_list": []
}
```

### `tacz_aim`

Puts the NPC into TACZ aiming state.

Supported fields:

- `hand`
- `inaction_time`
- `stamina`
- `command_list`

#### Object-form syntax

```json
{
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

### `tacz_shoot`

Fires a TACZ weapon.

Supported fields:

- `fire_mode`
- `stamina`
- `command_list`

#### Object-form syntax

```json
{
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

---

## Supported Predicates

Predicates go inside a behavior's `conditions` list.

General syntax:

```json
{
  "predicate": "within_distance",
  "min": 0.0,
  "max": 5.0
}
```

### Original Indestructible predicates

#### `random_chance`

Passes based on a random probability.

```json
{
  "predicate": "random_chance",
  "chance": 0.5
}
```

#### `within_eye_height`

Checks whether the target is within vertical eye-height range.

```json
{
  "predicate": "within_eye_height"
}
```

#### `within_distance`

Checks target distance range.

```json
{
  "predicate": "within_distance",
  "min": 0.0,
  "max": 5.0
}
```

#### `within_angle`

Checks full angle-to-target range.

```json
{
  "predicate": "within_angle",
  "min": -45.0,
  "max": 45.0
}
```

#### `within_angle_horizontal`

Checks horizontal angle only.

```json
{
  "predicate": "within_angle_horizontal",
  "min": -90.0,
  "max": 90.0
}
```

#### `health`

Checks the NPC's current health.

```json
{
  "predicate": "health",
  "health": 10.0,
  "comparator": "LESS"
}
```

#### `stamina`

Checks the NPC's current stamina.

```json
{
  "predicate": "stamina",
  "stamina": 5.0,
  "comparator": "GREATER"
}
```

#### `guard_break`

Checks whether the target is guard-broken.

```json
{
  "predicate": "guard_break",
  "invert": false
}
```

#### `knock_down`

Checks whether the target is knocked down.

```json
{
  "predicate": "knock_down",
  "invert": false
}
```

#### `attack_level`

Checks the target's current attack state range.

```json
{
  "predicate": "attack_level",
  "min": 0,
  "max": 2
}
```

#### `using_item`

Checks whether the target is actively using an item.

```json
{
  "predicate": "using_item",
  "edible": false
}
```

#### `phase`

Checks the NPC's current combat phase.

```json
{
  "predicate": "phase",
  "min": 0,
  "max": 1
}
```

### EFI-added predicates

#### `mainhand_weapon_category`

Checks the NPC's main-hand Epic Fight weapon category.

```json
{
  "predicate": "mainhand_weapon_category",
  "category": "dagger"
}
```

#### `offhand_weapon_category`

Checks the NPC's off-hand Epic Fight weapon category.

```json
{
  "predicate": "offhand_weapon_category",
  "category": "shield"
}
```

#### `has_gear_in_inventory`

Checks whether the NPC has a specific item available.

```json
{
  "predicate": "has_gear_in_inventory",
  "item": "minecraft:iron_sword",
  "slot": "main_hand",
  "include_equipped": true
}
```

#### `has_ammo`

Checks whether a weapon has loaded ammo.

```json
{
  "predicate": "has_ammo",
  "hand": "main_hand",
  "invert": false
}
```

#### `ammo_is_empty`

Checks whether the current magazine is empty.

```json
{
  "predicate": "ammo_is_empty",
  "hand": "main_hand",
  "invert": false
}
```

#### `ammo_not_full`

Checks whether the current magazine is not full.

```json
{
  "predicate": "ammo_not_full",
  "hand": "main_hand",
  "invert": false
}
```

#### `ammo_has_reserve`

Checks whether reserve ammo exists for reload.

```json
{
  "predicate": "ammo_has_reserve",
  "hand": "main_hand",
  "invert": false
}
```

#### `target_blocking`

Checks whether the target is blocking.

```json
{
  "predicate": "target_blocking",
  "invert": false
}
```

#### `target_using_shield`

Checks whether the target is using a shield specifically.

```json
{
  "predicate": "target_using_shield",
  "invert": false
}
```

#### `no_target`

Checks whether the NPC currently has no combat target.

```json
{
  "predicate": "no_target"
}
```

#### `has_target`

Checks whether the NPC currently has a combat target.

```json
{
  "predicate": "has_target"
}
```

---

## Accepted Slot and Hand Values

### Equipment slots

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

### Hands

- `mainhand`
- `main_hand`
- `offhand`
- `off_hand`

---

## Notes

- All predicates inside `conditions` are ANDed together.
- Predicate names may be written with or without a namespace. EFI strips the namespace and matches the final path segment.
- `gear_swap` supports both a single object and a list of objects.
- Multi-slot `gear_swap` is an EFI-Unofficial extension that allows coordinated multi-slot swaps in one behavior step.
- In multi-slot `gear_swap`, `allow_equipped` is not parsed per entry.
- In single-slot `gear_swap`, `stamina`, `inaction_time`, and `sound` live inside the `gear_swap` object.
- In multi-slot `gear_swap`, `stamina`, `inaction_time`, and `sound` live on the outer behavior object.
- If `gun_id` is present and `item` is omitted, EFI defaults to `tacz:modern_kinetic_gun`.
