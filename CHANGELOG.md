# Changelog

All notable changes to this project are documented here, newest first.
Versions follow `{mod version}+{minecraft version}`.

## 0.9.0 — first public build

Released as a **beta**: everything here works and is tested, but nobody outside the author has
played it yet. Report anything odd on the [issue tracker](https://github.com/DrimoZ/BeaconPack/issues).

### Added
- Advancements: get a pack, fit an augment, then specialise or finish the ladder.
- Fifteen languages: English, French, Spanish, German, Portuguese (BR), Russian, Simplified and
  Traditional Chinese, Japanese, Korean, Polish, Italian, Dutch, Turkish, Ukrainian.
- JEI plugin: augments are shown as the fifteen distinct items they are, and a fuel category lists
  what each burnable item is worth as runtime.
- A key bind (**B** by default) that opens the pack you are carrying or wearing, without holding it.

### Changed
- The pack screen's side tabs are split across both edges — power and stats on the left, augments
  and fuel on the right — and open in place with a short animation instead of appearing as detached
  panels. One drawer per side can be open at a time.
- A pack worn in a Curios slot now takes priority over one loose in the inventory.
- Built against NeoForge 21.1.248.

### Fixed
- A pack worn as a Curio could not be opened at all: the key bind's request was discarded before it
  reached the code that opens the menu.
- Two labels in the effect panel could run past the frame, and one was truncated to nonsense.

## 1.0.0

The aura has now been verified on a dedicated server with two players: an effect set to *Allies*
reaches a second player, and *Team* excludes one who is not on the team. That exclusion cannot be
observed in single player — there the carrier is trivially allied with everyone who exists — and it
was the last thing standing between the beta and a release.

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
- EMI showed an on-screen error for every fuel row: rows built from a registry rather than from a
  JSON recipe need EMI's synthetic-id form.

### Added
- Four Beacon Pack tiers projecting beacon effects from the inventory, configured through their
  own screen.
- Seven augments: Range, Focus, Amplification, Efficiency, Capacity, Attunement and Discretion,
  one of each type per pack.
- Three themed packs — Cinder, Void and Tidal — with effect pools drawn from effects the beacon
  never offered.
- Fuel: items are consumed per second of projection, priced by a datapack registry and reported
  as remaining runtime rather than as points.
- Four datapack registries (`effect`, `augment`, `tier`, `fuel`), so effects, augments, tiers and
  fuel values can all be retuned or extended without code.
- Server config for fuel, aura reach, free coverage near a real beacon, and an optional
  requirement to stand near a lit beacon to reconfigure.
- Optional Curios support: a pack worn in the `charm` slot works like one carried in the inventory.
  A pack in the inventory still wins if you carry both.

### Fixed
- A modified client could send a negative slot or value in a configuration action. Every existing
  check was an upper bound, so it reached `List.set` / `List.remove` and threw on the server thread.

### Notes
- Documentation lives in the [wiki](https://github.com/DrimoZ/BeaconPack/wiki).
- Code is MIT; the artwork is reserved, with redistribution as part of the mod — modpacks
  included — granted explicitly. See the README's Permissions section.
