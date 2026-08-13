# Roadmap to release

Merged into `main` and pushed. `mod_version=0.1.0`. NeoForge 1.21.1 only.

Ordered by what blocks a release, not by effort.

---

## P0 — blocks publishing

| Item | State |
|---|---|
| `issueTrackerURL` pointed at a repo that does not exist | Done — every bug report link 404d. |
| No `logoFile` / `logo.png` / `displayURL` | Done — generated from the same script as the item icons. |
| No `pack.mcmeta` | Done. |
| Package and author said `theo` | Done — `dev.drimoz`, author `DrimoZ`. |
| Licence was MIT by default, undecided | Done — MIT for code, assets reserved with redistribution granted, permissions block in the README. |
| Dedicated-server run never tested | Done — boots clean, config loads server-side, 1312 recipes, no tag or registry errors. |
| Multiplayer aura never tested | Done, and not by hand — the game tests place real players in a level and check who is reached. See P1. |
| **Version still `0.1.0`** | Open. Bump to `1.0.0` at publish time; both platforms key updates off it. |
| **No screenshots or GIF** | Open, and the last real blocker. A store page without them does not convert, and the GUI is the differentiator. Needs a person at a client. |

## P1 — should ship in 1.0

**Curios slot type.** Done. Packs bind to the `charm` slot through a shipped tag; both files are
additive so a datapack can still move them.

**Server-side action validation.** Done. Auditing it found a real hole: `slot` and `value` are
var-ints, so a modified client could send `-1`, and every existing check was an upper bound — the
negative index reached `List.set` / `List.remove` and threw on the server thread. Fixed at the entry
point, and now covered by a game test that fires hostile indices at every action.

**Game tests.** Done — seven of them, run headlessly with `./gradlew runGameTestServer`. They cover
what single player structurally cannot: the aura reaching a second player, `self` not leaking to a
bystander, `team` excluding someone off the team, and pets being reached while a stray wolf is not.

Not covered, deliberately: the `PlayerTickEvent` subscription itself. Synthetic players tick as
entities but their connection never reaches the state where the server drives a player tick, so a
test of it failed for that reason alone and was removed rather than weakened into passing.

**Documentation.** Done, and the [wiki](https://github.com/DrimoZ/BeaconPack/wiki) is the single
source of truth — fourteen pages covering playing, running and extending the mod. The repo's own
copies were deleted rather than left to drift.

**Data component migration.** Decided: no version field. Every field on `PackState` is
optional-with-default, so additions and removals already migrate themselves and the absence of a
field is the version. The rule is now documented on the record: never change what a field means,
add a new one.

**Datapack removal.** Done — `sanitize` already dropped an effect whose entry has gone; there is
now a test proving it.

## P2 — after 1.0, driven by feedback

- **JEI plugin, shipped.** JEI is dev-only today. A real plugin can show the augment tiers as
  variants and make the component-carrying recipes browsable. **EMI** matters at least as much now —
  arguably more on 1.21.
- **Convention tags for fuel.** Fuel is per-item today. Accepting `c:ingots/*` style tags would let
  the mod pick up modded metals with no data file.
- **Player-facing progression.** Advancements for crafting the first pack and each augment line.
- **Sounds.** There is one deactivate sound. Slotting an augment and switching aura mode are both
  silent.

## Ports

1.21.1 first is right — it is still where the modpack mass sits. The order the code is already
shaped for: **1.21.1 → 1.21.x current → 1.20.1 backport**, and `core/` plus `PackState`'s codec are
the only layers that should need thought on each hop.

Do not port before 1.0 has been in players' hands for a few weeks. Porting multiplies every bug
report by the number of branches, and the bug reports have not started yet.

## Docs

The wiki covers playing, running and extending the mod, and the README points at it rather than
restating it. What is left:

- **Screenshots and a GIF.** Non-negotiable for a store page, and the GUI is the differentiator —
  the searchable picker and the side drawers are what makes this not just another beacon item.
  Nobody but you can take these.
- **CHANGELOG**: keep it current as things land, since both platforms render release notes from it.

## Publishing

Manual for the first release on both platforms — automating a pipeline before it has run once by
hand is how you debug a pipeline instead of a mod.

- **Modrinth**: fast, near-instant listing. Needs description, licence, categories
  (`utility`, `equipment`), gallery, and a source link. The licence is a custom dual one, so pick
  the custom option and point it at `LICENSE` — and paste the README's permissions block into the
  description, since that is what pack authors and moderators actually read.
- **CurseForge**: manual approval, typically a day or three. Read their file rules before the first
  upload; rejections are slow to round-trip.
- Verify the jar first: JEI and Curios are `compileOnly` / `additionalRuntimeClasspath`, so neither
  should be inside it. Confirm, don't assume.
- Once both have accepted a manual upload, `minotaur` (Modrinth) and `curseforge-gradle` publish
  from a local Gradle task — no CI needed, which is consistent with dropping the Actions workflow.

## Promotion

The GUI is the hook. A ten-second GIF of opening a pack, searching the effect picker and sliding a
drawer out will carry further than any paragraph.

- r/feedthebeast and r/MinecraftMods, on a weekend.
- The NeoForged Discord's showcase channel.
- Modrinth's own discovery does real work if the page has a gallery and a clear one-liner.
- Modpack authors are the actual growth channel. A small utility mod with a clean GUI and datapack
  hooks is easy to include; reach out to a few kitchen-sink pack authors directly once 1.0 is up.
