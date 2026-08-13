# Datapack guide

Almost everything in BeaconPack is a datapack entry: which effects a pack may project, what they
cost, what augments do, what each tier is worth, and what burns as fuel. The mod ships its own
content through exactly the mechanism described here — there is no private path.

Files go under your own namespace:

```
data/<your_namespace>/beaconpack/effect/<name>.json
data/<your_namespace>/beaconpack/augment/<name>.json
data/<your_namespace>/beaconpack/tier/<name>.json
data/<your_namespace>/beaconpack/fuel/<name>.json
```

The folder is `beaconpack/<registry>` because these are datapack registries, so the path is
`data/<namespace>/<registry namespace>/<registry path>/`. Since all four registries live in the
`beaconpack` namespace, that middle segment is always `beaconpack`.

> **A typo makes an entry vanish, it does not crash.** A file that fails to parse is dropped with a
> message in the log and nothing else — the symptom in game is an effect missing from the picker.
> If something you added is not showing up, read the log before re-reading the JSON.

---

## `effect` — what a pack may project

```json
// data/mypack/beaconpack/effect/fire_resistance.json
{
  "effect": "minecraft:fire_resistance",
  "cost": 2.0,
  "max_amplifier": 0,
  "min_tier": 2
}
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `effect` | mob effect id | **required** | The projected effect. Any registered effect works — vanilla, another mod's, or datapack-added. |
| `cost` | double | `1.0` | Fuel units per second at amplifier 0, self only. One unit ≈ one second of a basic effect. |
| `max_amplifier` | int 0–3 | `0` | Highest level this may be raised to. `0` means level I only. |
| `min_tier` | int 1–4 | `1` | Lowest pack tier allowed to select it. |
| `amplifier_cost_multiplier` | double | `2.0` | Cost factor per amplifier level above 0. |

Cost at runtime is `cost × amplifier_cost_multiplier^amplifier × aura multiplier`, where the aura
multipliers are `self` 1.0, `team` 1.7, `allies` 2.0, `allies_and_pets` 2.4.

No icon is needed: effect icons come from the vanilla effect atlas.

## `fuel` — what burns

```json
// data/mypack/beaconpack/fuel/copper_ingot.json
{ "item": "minecraft:copper_ingot", "units": 450 }
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `item` | item id | **required** | The consumed item. |
| `units` | int ≥ 1 | **required** | Fuel units it yields. |

For scale, the shipped values are iron 300, gold 900, emerald 1800, diamond 3600 and netherite
28800 — five minutes to eight hours of one basic effect. A tier IV pack's buffer holds 36000.

A registry rather than a tag, because a tag can say "this is fuel" but cannot carry a per-item
value, and hardcoding the values would undo the point of the rest being data-driven.

The pack refuses an item its buffer cannot hold whole, rather than burning most of a netherite
ingot for a small top-up — which is what gives the Capacity augment a purpose.

## `tier` — a pack item's base stats

```json
// data/mypack/beaconpack/tier/verdant.json
{
  "level": 3,
  "effect_slots": 2,
  "augment_slots": 2,
  "base_range": 12.0,
  "fuel_capacity": 18000,
  "max_amplifier": 0,
  "effect_pool": [
    "beaconpack:regeneration",
    "beaconpack:haste"
  ]
}
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `level` | int 1–4 | **required** | Used for ordering and for every `min_tier` check. |
| `effect_slots` | int 0–9 | **required** | How many effects may be configured. |
| `augment_slots` | int 0–3 | **required** | How many of the three drawn augment slots are unlocked. |
| `base_range` | double | **required** | Aura radius in blocks. Ignored by effects set to `self`. |
| `fuel_capacity` | int | **required** | Internal buffer, in fuel units. |
| `max_amplifier` | int 0–3 | `0` | Highest amplifier without an Amplification augment. |
| `effect_pool` | list of effect ids | `[]` | Which effects this tier accepts. |

**An empty `effect_pool` means "anything the effect registry allows".** That is what you get for
free, but every shipped tier declares one explicitly so a themed pack cannot quietly inherit the
standard list when someone adds a new effect.

Keep ranges modest. The vanilla beacon reaches 20–50 blocks; a beacon that follows you is worth far
more than a fixed one at equal reach, and the shipped tiers top out at 16 for that reason.

A tier entry alone does not create an item — the four numbered packs and the three themed ones are
registered in code, each pointing at a tier entry. A datapack can retune any of those seven, and can
add a tier for its own use, but adding an eighth *pack item* needs an addon mod.

## `augment` — what slots into a pack

```json
// data/mypack/beaconpack/augment/reach.json
{
  "max_tier": 3,
  "color": 5636095,
  "operations": [
    { "type": "add_range", "values": [4.0, 8.0, 12.0] }
  ]
}
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `max_tier` | int 1–3 | `3` | Highest tier this augment exists in. |
| `color` | int | `0xFFFFFF` | Tint applied to the augment texture. Alpha is forced opaque at render time. |
| `model_data` | int | `0` | Selects a model override so the augment gets its own glyph. `0` falls back to the generic gem. |
| `operations` | list | **required** | What it changes. |

Each operation carries **one value per augment tier**, so "Reach I/II/III" stays a single file.
Values shorter than `max_tier` clamp to the last entry.

| `type` | Effect |
|---|---|
| `add_range` | Adds blocks to the aura radius. |
| `add_effect_slot` | Adds configurable effect slots. |
| `add_amplifier` | Raises the reachable amplifier. |
| `mul_fuel` | Multiplies fuel cost — use values below 1.0 to make it cheaper. |
| `mul_capacity` | Multiplies the fuel buffer. |
| `unlock_aura` | Adds to the effective tier when checking aura modes, unlocking wider sharing. |
| `hide_effects` | `1` hides the particle swirl, `2` also hides the status icon. |

`hide_effects` is the one operation whose value names a behaviour instead of scaling one, so it is
read as a threshold and the highest value wins rather than summing.

Only one augment **of each type** may go in a pack at a time. That is enforced both by the slot and
by the resolver, so a stack built by command cannot stack two of the same.

### Getting your augment in game

Augments are all one item, `beaconpack:augment`, whose identity comes from a component:

```
/give @s beaconpack:augment[beaconpack:augment={type:"mypack:reach",tier:2}]
```

A recipe produces one the same way:

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "A": { "item": "minecraft:amethyst_shard" },
    "B": { "item": "minecraft:amethyst_block" }
  },
  "pattern": ["BAB", "A A", "BAB"],
  "result": {
    "id": "beaconpack:augment",
    "count": 1,
    "components": {
      "beaconpack:augment": { "type": "mypack:reach", "tier": 2 }
    }
  }
}
```

This is exactly why augments are one item with a component rather than one registered item each:
a datapack can introduce a brand new augment without any code, which registered items would make
impossible.

---

## Changing the shipped content

Same mechanism, same file paths, `beaconpack` as the namespace. To retune the standard Speed effect,
put your own file at `data/beaconpack/beaconpack/effect/speed.json` — later datapacks win, so yours
replaces the shipped one.

To *remove* something, override it with an entry no pack can select, for instance `"min_tier": 4` on
a tier-3-only pool. Packs already carrying an effect whose entry disappears drop it cleanly the next
time they tick; existing saves do not break.

## Checking your work

These are datapack **registries**, which are read when the world loads — reload the world (or
restart the server) after editing them, rather than relying on `/reload`. Recipes and tags you add
alongside them do follow `/reload` normally.

Watch the log as the world loads: that is where a rejected file reports itself, and it is the only
place it does. In game, an entry that loaded correctly shows up in the effect picker for any pack
whose tier allows it.
