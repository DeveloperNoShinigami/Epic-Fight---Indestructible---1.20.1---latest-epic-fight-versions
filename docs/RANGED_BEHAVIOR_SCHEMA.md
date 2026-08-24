# EFI ranged behavior schema for Epic Fight 20.14.17

This is the final verified 1.20.1 schema. It applies to generic mobs and
CustomNPCs through the same EFI provider system.

Advanced mob patches keep their behavior series under `combat_behavior`. Ranged behavior is expressed with explicit state transitions; the old generic `ranged`, `aim`, and `shoot` keys are not valid.

```json
{
  "conditions": [
    { "predicate": "within_distance", "min": 4.0, "max": 32.0 },
    { "predicate": "has_target" }
  ],
  "tacz_aim": {
    "hand": "main_hand",
    "inaction_time": 4,
    "stamina": 0.0,
    "command_list": []
  }
}
```

Use the following behavior forms:

- `reload`: integer tick duration. `hand`, `require_ammo`, `stamina`, and `command_list` are sibling fields on the same behavior object.
- `tacz_aim`: integer shorthand or object form with `hand`, `inaction_time`, `stamina`, and `command_list`.
- `tacz_shoot`: integer shorthand or object form with `fire_mode`, `stamina`, and `command_list`.
- `ammo_slots`: root-level ordered sources such as `customnpcs:projectile`, `inventory:0`, `customnpcs:drop:0`, or `mainhand`.

Native Epic Fight bow/crossbow motion is selected by the patch from item-use state. TACZ aim/reload/shoot behavior uses EFI's TACZ bridge and current gun cadence. Behavior order matters: conditions select a series, and the series should transition from aim to shoot to reload when ammunition is exhausted. `inaction_time` on `tacz_aim` is the minimum aim dwell in ticks; use at least 10 ticks for a visible aim phase and guard the first shot with `tacz_aiming`.

For sustained firing, keep the aim and shoot behaviors in a looping series.
The aim state is refreshed while the target remains valid, so the NPC keeps
looking at the target throughout the firing cycle. Put reload branches before
shoot branches when `ammo_not_full` or `ammo_is_empty` is true. Use separate
`has_target` and `no_target` reload branches when an NPC must refill after a
weapon swap or while idle.

The parser rejects obsolete generic ranged keys with a warning. This prevents a legacy datapack from loading successfully while doing nothing.

## CNPC NBT-selected patch

The CNPC addon supports selecting a normal or advanced patch using a user-owned
Forge persistent NBT marker. This is separate from the addon-only `efModel`
field and does not require `setEFModel`.

CNPC script:

```js
function init(e) {
    e.npc.setEFNbtMarker("efi_role", "ranged");
}
```

The helper writes this serialized entity data:

```snbt
ForgeData: { efi_role: "ranged" }
```

The matching datapack file uses the marker at its root:

```json
{
  "nbt_tag": "{ForgeData:{efi_role:\"ranged\"}}",
  "model": "epicfight:entity/biped",
  "armature": "epicfight:entity/biped",
  "renderer": "minecraft:zombie",
  "isHumanoid": true,
  "attributes": { "max_stun_shield": 0.0 },
  "stun_animations": {
    "short": "epicfight:biped/combat/hit_short",
    "long": "epicfight:biped/combat/hit_long",
    "knockdown": "epicfight:biped/combat/knockdown",
    "fall": "epicfight:biped/living/landing",
    "neutralize": "epicfight:biped/skill/guard_break1"
  }
}
```

For a raw script API equivalent:

```js
function init(e) {
    var npc = e.npc.getMCEntity();
    npc.getPersistentData().putString("efi_role", "ranged");
    npc.display.refreshEFPatch();
}
```

Verify the field with `/data get entity @e[type=customnpcs:customnpc,limit=1]
ForgeData`. The addon evaluates CNPC `nbt_tag` providers against serialized
CNPC NBT and refreshes the provider after `setEFNbtMarker`.

## Complete advanced EFI schema

The following fields are read by the current `AdvancedMobpatchReloader`.

### Provider fields

Common provider fields are `model`, `armature`, `renderer`, `faction`, `attributes`, and `default_livingmotions`. Optional client fields are `boss_bar`, `custom_name`, `custom_texture`, `swing_sound`, `hit_sound`, and `hit_particle`. Optional server fields are `ammo_slots`, `stun_animations`, `combat_behavior`, `humanoid_weapon_motions`, `custom_guard_motion`, and `stun_command_list`. `nbt_tag` can select an NBT-matched provider.

### Advanced attributes

Supported `attributes` keys are:

| Key | Purpose |
|---|---|
| `weight`, `impact`, `armor_negation` | Epic Fight combat attributes. |
| `max_stamina`, `stamina_regan_multiply` | Stamina capacity and regeneration. |
| `max_strikes`, `attack_damage` | Strike count and optional vanilla attack damage. |
| `scale`, `max_stun_shield` | Entity scale and stun-shield capacity. |
| `chasing_speed`, `attack_radius`, `guard_radius` | Advanced movement/combat ranges. |
| `stamina_regan_delay`, `stamina_lose_multiply` | Stamina timing and loss. |
| `has_stun_reduction`, `stun_shield_regan_delay` | Stun and shield recovery controls. |
| `stun_shield_regan_multiply` | Stun-shield regeneration multiplier. |
| `stun_shield_multiply` | Legacy alias for the shield regeneration multiplier. |

### Living motions, guards, and stun

`default_livingmotions` maps motion names such as `idle`, `walk`, `chase`, `fall`, and `death` to animation resource locations. `humanoid_weapon_motions` maps current Epic Fight weapon categories and styles to motion animations. For EF 20.14.17, ranged categories are `BOW` and `CROSSBOW`; `WeaponCategories.RANGED` no longer exists.

`custom_guard_motion` supports `weapon_categories` (string or list), `style`, `guard`, `stamina_cost_multiply`, `can_block_projectile`, `parry_cost_multiply`, and `parry_animation`. `stun_animations` maps EFI stun types including short stun, long stun, knockdown, fall, and neutralize. `stun_command_list` runs events when a stun transition occurs.

### Gear swap

`gear_swap` accepts one object or a list. Entries support `item`, TACZ `gun_id`, `slot`, `allow_equipped`, `store_gear`, `required_in_inv`, `nbt`, `stamina`, `inaction_time`, and `sound`. A TACZ-only entry defaults the item to `tacz:modern_kinetic_gun` and writes the `GunId` tag when needed.

### Behavior series

Each `combat_behavior` entry uses `weapon_categories`, `style`, and `behavior_series`. A series supports `weight`, `cooldown`, `canBeInterrupted`, `looping`, and `behaviors`. Each behavior can also contain `conditions`, `set_phase`, and `end_by_hurt_level`.

Behavior types are:

- `animation`: `play_speed`, `stamina`, `convert_time`, `command_list`, `hit_command_list`, `blocked_command_list`, and `damage_modifier`.
- `guard`: `counter`, `parry`, `parry_times`, `stun_immunity_time`, `counter_cost`, `counter_chance`, `counter_speed`, `cancel_after_counter`, and `specific_guard_motion`.
- `wander`: `inaction_time`, `z_axis`, and `x_axis`.
- `gear_swap`: the single or list form described above.
- `reload`: integer duration plus `hand`, `require_ammo`, `stamina`, and `command_list`.
- `tacz_aim`: integer shorthand or object with `hand`, `inaction_time`, `stamina`, and `command_list`.
- `tacz_shoot`: integer-compatible form or object with `fire_mode`, `stamina`, and `command_list`.

### Conditions and predicates

Supported predicate names are `random_chance`, `within_eye_height`, `within_distance`, `within_angle`, `within_angle_horizontal`, `health`, `guard_break`, `knock_down`, `attack_level`, `stamina`, `using_item`, `mainhand_weapon_category`, `offhand_weapon_category`, `has_gear_in_inventory`, `has_ammo`, `ammo_is_empty`, `ammo_not_full`, `ammo_has_reserve`, `target_blocking`, `target_using_shield`, `no_target`, `has_target`, `tacz_aiming`, and `phase`.

`tacz_aiming` is a zero-argument state predicate. It is true only when the held TACZ gun reports its synchronized aiming state. Use it on the first `tacz_shoot` behavior so firing cannot bypass aiming:

```json
{
  "tacz_aim": { "inaction_time": 10 }
},
{
  "conditions": [
    { "predicate": "has_target" },
    { "predicate": "within_distance", "min": 6.0, "max": 18.0 },
    { "predicate": "tacz_aiming" }
  ],
  "tacz_shoot": {}
}
```

`within_distance`, `within_angle`, and `within_angle_horizontal` use `min` and `max`. `health` and `stamina` use a numeric value plus `comparator`. `attack_level` and `phase` use `min` and `max`. Hand-based ammo predicates use `hand`; inventory predicates use the configured item/slot fields. Most target-state predicates accept `invert`.

### Command and damage events

`command_list` is a timestamped event list. `hit_command_list` and `blocked_command_list` are combat-result event lists. `stun_command_list` is a stun-result event list. `damage_modifier` is valid on `animation` behaviors and uses EFI's damage-source modifier object. These lists must match the event shape expected by EFI; malformed entries are not silently treated as valid transitions.

### Compatibility rules

The parser rejects obsolete behavior keys `ranged`, `ranged_attack`, `ranged_behavior`, `aim`, and `shoot` with a warning. Migrate them explicitly to the forms above. Unknown predicates remain errors so datapack mistakes are visible during reload.

Do not put the same `nbt_tag` marker on both a normal and an advanced CNPC
file. Use one marker for the intended provider. In the bundled example,
`efi_role=ranged` selects the advanced `solider.json` provider, whose
`attack_radius` is `6.0` so the NPC remains at ranged distance.
