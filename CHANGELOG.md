# Changelog

All notable changes to this project are documented here, newest first.
Versions follow `{mod version}+{minecraft version}`.

## Unreleased

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
