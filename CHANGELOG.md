# Changelog

All notable changes to this project are documented here, newest first.
Versions follow `{mod version}+{minecraft version}`.

## 1.0.1

Finishes the rename. 1.0.0 changed the mod id and left the item ids alone, so the beacons were still
`portablebeacons:beacon_pack_i` and the themed ones were still named after dimensions rather than
after themselves.

| was | is |
|---|---|
| `beacon_pack_i` … `beacon_pack_iv` | `beacon_i` … `beacon_iv` |
| `nether_pack` | `cinder_beacon` |
| `end_pack` | `void_beacon` |
| `tidal_pack` | `tidal_beacon` |

The themed three were the worst of it: their ids said Nether, End and Tidal while the game showed
Cinder Beacon, Void Beacon and Tidal Beacon. Anyone reading a crash report, writing a datapack or
typing `/give` met a name the mod never uses out loud.

**This breaks beacons saved with 1.0.0 or the 0.9.0 beta** — they become unknown items, and a
datapack written against the old ids needs updating. Same reasoning as the rename itself: the ids
are wrong now and will only get more expensive to fix, so it happens at 1.0.1 or never.

The data component and the item tag moved too — `portablebeacons:pack` is now
`portablebeacons:beacon`, and the tag `packs` is now `beacons`. A datapack naming either needs
updating.

### Fixed
- **Attunement did nothing, on any beacon.** It raised the beacon's effective tier for the sharing
  check, and every tier already cleared the threshold it raised — so there was never a mode left for
  it to unlock. Sharing is now gated by an `aura_rank` on each tier, which Attunement adds to.

  | Beacon | Shares unaided | + Attunement I | + Attunement II |
  |---|---|---|---|
  | I, II | self only | Team | Allies |
  | III, IV, themed | Team | Allies | Allies and Pets |

  This is a real balance change: sharing with every nearby player now costs an augment slot on every
  beacon. It also makes Team meaningful, since it now sits *below* Allies on the ladder — narrower
  reach, lower cost, the cheaper first step out of keeping everything to yourself.

- **Amplification did nothing on the beacons that could hold it.** It lifts the beacon's level
  ceiling, but each effect carries a second ceiling and the lower one wins — and every effect stopped
  at level II while Beacon IV and Cinder already granted level II unaided. Speed, Haste, Jump Boost,
  Resistance, Strength and Regeneration now allow level III, so the augment has somewhere to go. The
  utility effects stay at level I, where a second level means nothing in vanilla.

### Changed
- The GUI texture and the item textures are named after their items again.
- Datagen runs in its own game folder, so a mod left in `run/mods` for a different Minecraft version
  can no longer stop the generator before it writes anything.

### Notes
- `aura_rank` is a new tier field, `0`–`3`, defaulting to `0`. Sharing was the one mechanic in the
  mod that a datapack could not touch; it now is one. See the
  [wiki](https://github.com/DrimoZ/PortableBeacons/wiki/Augments#attunement).

## 1.0.0

Renamed from **BeaconPack** to **Portable Beacons**, and renamed all the way down: the mod id, the
item, recipe and advancement ids, the datapack paths, the item tag and the Java package are all
`portablebeacons` now. The old name read as a modpack, which is the one thing this is not.

**This breaks worlds saved with the 0.9.0 beta.** A beacon stored under the old id becomes an
unknown item, and any datapack written against `beaconpack:` needs its ids updated. The beta was up
for a day, so renaming now costs a handful of saves instead of every save the mod will ever touch —
which is exactly why it had to happen before 1.0 and cannot happen after.

The aura is now verified on a dedicated server with two players: an effect set to *Allies* reaches
the second player, and *Team* excludes one who is not on the team. That exclusion cannot be observed
in single player — there the carrier is trivially allied with everyone who exists — and it was the
last thing standing between the beta and a release.

### Added
- EMI plugin, matching the JEI one: the fifteen augments are listed separately instead of
  collapsing into a single entry, and a fuel category shows what each item is worth as runtime.
- Fuel entries can be priced by an item **tag** as well as by a single item, so one line covers
  every modded metal that follows the convention. Copper is now fuel, at 150 units.
- A chime when an augment is fitted or pulled. Every button in the screen made a sound; the slot
  that changes the most did not.
- Curios, JEI and EMI are declared as optional dependencies, so the loader and the store pages say
  so rather than leaving players to guess.

### Fixed
- The rename left every id in the Java sources pointing at the old namespace, so 44 translation
  keys and the GUI background resolved to nothing: the screen, every tooltip, the creative tab, the
  keybind category and the out-of-fuel message all rendered as raw ids over a missing texture.
  Caught before the 1.0.0 file went up, by a new test that checks every key named in the code is one
  the language files actually define — the existing test only compared the locales to each other,
  and all fifteen were consistently wrong together.
- EMI showed an on-screen error for every fuel row: rows built from a registry rather than from a
  JSON recipe need EMI's synthetic-id form.

## 0.9.0 — first public build

Released as a **beta**: everything worked and was tested, but nobody outside the author had played
it yet. Report anything odd on the [issue tracker](https://github.com/DrimoZ/PortableBeacons/issues).

### Added
- Four beacon tiers projecting beacon effects from the inventory, configured through their own
  screen.
- Seven augments: Range, Focus, Amplification, Efficiency, Capacity, Attunement and Discretion,
  one of each type per beacon.
- Three themed beacons — Cinder, Void and Tidal — with effect pools drawn from effects the vanilla
  beacon never offered.
- Fuel: items are consumed per second of projection, priced by a datapack registry and reported as
  remaining runtime rather than as points.
- Four datapack registries (`effect`, `augment`, `tier`, `fuel`), so effects, augments, tiers and
  fuel values can all be retuned or extended without code.
- Server config for fuel, aura reach, free coverage near a real beacon, and an optional requirement
  to stand near a lit beacon to reconfigure.
- Optional Curios support: one worn in the `charm` slot works like one carried in the inventory.
- Advancements: get one, fit an augment, then specialise or finish the ladder.
- Fifteen languages: English, French, Spanish, German, Portuguese (BR), Russian, Simplified and
  Traditional Chinese, Japanese, Korean, Polish, Italian, Dutch, Turkish, Ukrainian.
- JEI plugin, and a key bind (**B** by default) that opens the one you are carrying or wearing.

### Changed
- The screen's side tabs are split across both edges — power and stats on the left, augments and
  fuel on the right — and open in place with a short animation instead of appearing as detached
  panels. One drawer per side can be open at a time.
- One worn in a Curios slot takes priority over one loose in the inventory.
- Built against NeoForge 21.1.248.

### Fixed
- One worn as a Curio could not be opened at all: the key bind's request was discarded before it
  reached the code that opens the menu.
- A modified client could send a negative slot or value in a configuration action. Every existing
  check was an upper bound, so it reached `List.set` / `List.remove` and threw on the server thread.
- Two labels in the effect panel could run past the frame, and one was truncated to nonsense.

### Notes
- Documentation lives in the [wiki](https://github.com/DrimoZ/PortableBeacons/wiki).
- Code is MIT; the artwork is reserved, with redistribution as part of the mod — modpacks included —
  granted explicitly. See the README's Permissions section.
