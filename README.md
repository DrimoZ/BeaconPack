# BeaconPack

Beacon effects in an inventory item. Four tiers, augments you slot in, and a fuel cost — all
defined in datapacks rather than in code.

**NeoForge 1.21.1** · Java 21 · MIT

📖 **[The wiki](https://github.com/DrimoZ/BeaconPack/wiki) is the reference** — player guide,
config, datapack docs and FAQ. This README is the short version.

---

## What it does

A Beacon Pack sitting in your inventory projects beacon effects. Which effects, how far, how
strong, and what it costs are all configured through the pack's own screen.

| Tier | Effects | Amplitude | Aura range | Augment slots |
|---|---|---|---|---|
| I | 1 | I | self only | 0 |
| II | 1 | I | 8 blocks | 1 |
| III | 2 | I | 12 blocks | 2 |
| IV | 2 | one at II | 16 blocks | 3 |

Ranges are far below the vanilla beacon's 20–50 blocks on purpose: a beacon that follows you is
worth much more than a fixed one at equal reach.

### Augments

One augment of each type per pack, each with its own tiers. They are a single item whose identity
comes from a datapack registry entry, so a pack can add new ones without any code.

| Augment | Effect |
|---|---|
| Range | +4 / +8 / +12 blocks |
| Focus | +1 effect slot |
| Amplification | +1 effect level |
| Efficiency | −25 / −40 / −55 % fuel |
| Capacity | fuel buffer ×2 / ×3 / ×4 |
| Attunement | unlocks wider sharing modes |
| Discretion | hides effect particles, and the status icon at tier II |

### Themed packs

Cinder, Void and Tidal packs sit alongside tier III with narrower pools drawn from effects the
beacon never offered. Carrying one instead of a tier IV is a trade, not a downgrade.

| Pack | Pool | Edge |
|---|---|---|
| Cinder | Fire Resistance, Strength, Haste, Resistance | level II allowed |
| Void | Slow Falling, Speed, Jump Boost, Night Vision | longest reach, 14 blocks |
| Tidal | Water Breathing, Conduit Power, Dolphin's Grace, Night Vision | the only aquatic pool |

They needed no new mechanics: a tier entry declares which effects it accepts, so a themed pack is
a data file plus an item — and a datapack can add more the same way.

### Fuel

Each effect costs fuel per second, scaled by its level and by how widely it is shared. Iron,
gold, emerald, diamond and netherite are worth increasing amounts; the pack draws from its own
fuel slot. Sharing an effect with allies costs more than keeping it to yourself, which is the
main decision the mod asks you to make.

A master switch stops all consumption instantly, and each effect can be turned off individually
without losing its settings. An effect a real beacon is already providing is free, and the pack
refuses fuel its buffer cannot hold whole rather than burning most of a netherite ingot for
nothing — which is what gives Capacity and the higher tiers a purpose.

Turning `require_fuel` off removes fuel from the game rather than leaving it inert: no fuel slot,
no gauge, no runtime figures.

### The screen

Effects, their settings and the player's inventory are all the main panel carries. Stats, augments
and fuel live in side tabs, because they are configured once and then left alone. Effects are
picked from a searchable list filtered to what the pack accepts, with arrow-key navigation and a
four-segment meter comparing fuel costs.

---

## Configuration

Four server-side options: whether fuel exists, whether the aura reaches players off your team,
whether a real beacon makes an effect free, and whether reconfiguring needs a beacon nearby.

**[Config reference →](https://github.com/DrimoZ/BeaconPack/wiki/Configuration)**

---

## Keeping the vanilla beacon relevant

The obvious failure mode for a mod like this is making the beacon block pointless. Three
safeguards:

1. Every pack tier consumes a Beacon block in its recipe.
2. Aura ranges stay well below the block's, so a placed beacon is still better for a base.
3. `require_beacon_to_configure` (off by default) restricts changing effects to within 16 blocks
   of a lit beacon.

---

## Data-driven

Four datapack registries under `data/<namespace>/beaconpack/`:

| Registry | Controls |
|---|---|
| `effect` | which effects a pack may project, their cost, level cap and minimum tier |
| `augment` | augment types, their per-tier operations, colour and icon |
| `tier` | base stats of each pack tier |
| `fuel` | what an item is worth in fuel units |

```json
// data/mypack/beaconpack/effect/fire_resistance.json
{
  "effect": "minecraft:fire_resistance",
  "cost": 2.0,
  "max_amplifier": 0,
  "min_tier": 2
}
```

**[Full datapack guide →](https://github.com/DrimoZ/BeaconPack/wiki/Datapack-Guide)** — every field
of all four registries, with worked examples for adding an effect, an augment and a themed tier.

The screen adapts on its own: effects live in a scrollable, searchable picker rather than a
fixed grid, so declaring forty of them changes nothing about the layout. Effect icons come from
the vanilla effect atlas, so anything registered — vanilla, another mod's, or datapack-added —
displays correctly with no texture needed.

The default pool is deliberately limited to the vanilla beacon's effects. Widening it is a
datapack away, but it is not the default: an unrestricted pool turns a utility mod into a cheat
item.

---

## Compatibility

**Curios** is optional. With it installed, a pack worn in the `charm` slot works exactly like one
carried in the inventory — the binding ships with the mod, so nothing needs configuring. A pack in
the inventory still wins if you somehow carry two. Without Curios, none of that code is ever
touched.

---

## Building

Requires JDK 21.

```bash
./gradlew build
```

```bash
./gradlew runClient
```

Textures are generated rather than hand-drawn, so the GUI background stays in sync with the slot
coordinates in `BeaconPackMenu`:

```bash
java tools/GenerateTextures.java
```

### Layout

- `core/` — the data model and all of the arithmetic, with no dependency on data components,
  packets or rendering. Unit-testable without launching Minecraft, and the only layer a port to
  another Minecraft version leaves largely untouched.
- `registry/`, `item/`, `menu/`, `net/`, `client/` — the platform layer.

`PackState` and its codec are the single place serialization lives, which is what keeps a
backport to an older version down to rewriting one file.
