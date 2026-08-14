# Store listing copy

Paste-ready text for the CurseForge and Modrinth project pages. Not documentation — the
[wiki](https://github.com/DrimoZ/BeaconPack/wiki) is that.

---

## Short description (one line, both platforms)

> Beacon effects in an inventory item. Four tiers, augments you slot in, and a fuel cost — all
> defined in datapacks.

## Categories

- CurseForge: *Adventure and RPG*, *Equipment*, *Utility & QoL*
- Modrinth: *Utility*, *Equipment*

## Licence field

Custom. Point it at `LICENSE` in the repository: MIT for the code, all rights reserved for the
artwork with redistribution granted. Paste the Permissions block below into the description too —
both platforms read the page, not the repo.

---

## Description

### BeaconPack

A Beacon Pack in your inventory projects beacon effects on you, and — from tier II up — on the
people around you. Which effects, how far, how strong, and what it costs are all configured in the
pack's own screen.

It is deliberately **not** a beacon replacement. Every pack tier consumes a Beacon block in its
recipe, the aura tops out at 16 blocks against the block's 20–50, and an effect your own beacon
already provides costs the pack nothing. The block stays the better choice for a base; the pack is
what you take with you.

### Four tiers, and three that specialise

| Pack | Effects | Aura | Augments |
|---|---|---|---|
| Beacon Pack I | 1 | self only | — |
| Beacon Pack II | 1 | 8 blocks | 1 |
| Beacon Pack III | 2 | 12 blocks | 2 |
| Beacon Pack IV | 2, one at level II | 16 blocks | 3 |

**Cinder**, **Void** and **Tidal** packs branch off tier II with narrow pools drawn from effects the
vanilla beacon never offered — Fire Resistance, Slow Falling, Water Breathing, Conduit Power,
Dolphin's Grace. Carrying one instead of a tier IV is a trade, not a downgrade.

### Seven augments

Range, Focus, Amplification, Efficiency, Capacity, Attunement and Discretion — one of each type per
pack, most with three tiers. More reach, more effects at once, cheaper fuel, a bigger buffer, wider
sharing, or no particles at all.

### Fuel you can read

Copper through netherite, priced by a datapack — per item, or by a convention tag, so a modded
metal is accepted without a file of its own. The screen shows **remaining runtime**, not
a unit count: "4 h" answers the question you actually have. Each effect chooses its own audience —
just you, your team, everyone nearby, or your pets — and sharing costs more. That is the main
decision the mod asks of you.

Fuel can be switched off entirely in the config, and when it is, every trace of it disappears from
the screen rather than sitting there inert.

### Data-driven, properly

Four datapack registries: which effects a pack may project and what they cost, what augments do,
what each tier is worth, and what burns as fuel. The mod's own content ships through exactly that
mechanism — there is no private path. Adding another mod's effect, or a whole themed pack, is a JSON
file. See the [datapack guide](https://github.com/DrimoZ/BeaconPack/wiki/Datapack-Guide).

### Compatibility

- **Curios** (optional): wear a pack in the charm slot. Ships configured; nothing to set up.
- **JEI and EMI** (optional): all fifteen augments listed separately instead of collapsing into one,
  plus a fuel category showing what each item is worth as runtime.
- Server-side config, synced to clients. Required on both sides.

---

## Permissions

**Modpacks: yes.** No permission needed, no message required, public or private, monetised or not,
on any platform or launcher. If you are reading this to find out whether you may include
BeaconPack, the answer is yes and you can stop reading.

**Credit** is appreciated and never required.

**Forks and addons: yes**, under the MIT terms. Please do not publish a fork under the name
*BeaconPack* — the name is not covered by the licence, and two mods sharing one name only confuses
players trying to work out which one broke their world.

**Assets** — icons, GUI artwork, logo — are the one exception: redistribute them with the mod
freely, but do not lift them into another project.

---

## Upload checklist

- [ ] Create the project on the platform first; the jar cannot be uploaded to nothing.
- [ ] Release type: **Beta**. It is tested but unplayed by anyone else.
- [ ] Game version 1.21.1, loader NeoForge.
- [ ] Mark Curios, JEI and EMI as **optional** dependencies, not required.
- [ ] Changelog: paste the newest section from `CHANGELOG.md`.
- [ ] Gallery: screenshots are still missing, and this is the one thing that decides whether anyone
      clicks. At minimum: the pack screen with a drawer open, the effect picker, and the JEI fuel
      category, and the EMI or JEI fuel category.
- [ ] Link the wiki and the issue tracker.
