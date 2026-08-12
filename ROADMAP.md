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
| Version still `0.1.0` | Bump to `1.0.0` at publish time; both platforms key updates off it. |
| **Dedicated-server run never tested** | Open. Needs `run/eula.txt`. The whole effect loop is server-side; single-player runs an integrated server, which hides a class of client/server-split bugs. |
| **Multiplayer aura never tested** | Open. `AuraMode.ALLIES/TEAM/ALLIES_AND_PETS` has never had a second player in front of it, and team lookup and pet ownership are exactly where this breaks. Needs two clients. |

## P1 — should ship in 1.0

**Curios slot type.** Done. Packs bind to the `charm` slot through a shipped tag; both files are
additive so a datapack can still move them.

**Server-side action validation.** Partly done. Auditing it found a real hole: `slot` and `value`
are var-ints, so a modified client could send `-1`, and every existing check was an upper bound —
the negative index reached `List.set` / `List.remove` and threw on the server thread. Fixed at the
entry point. The rest of the validation (tier, pool, amplifier cap, aura mode) was already correct.
It is still not *covered*, which is what the game tests below are for.

**Game tests.** `neoforge.enabledGameTestNamespaces` is set to the mod id with nothing behind it.
The natural first two: give a player a pack, tick, assert the effect landed; and drive
`applyAction` with hostile inputs. This is the integration seam unit tests structurally cannot
reach.

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

The README is in good shape. What is missing is for the two audiences that are not you:

- **A datapack guide.** All four registries with their full field lists and a worked example of
  adding an effect, an augment and a themed tier. This is the mod's main selling point and it is
  currently only inferable from the shipped JSON.
- **A config reference.** Four options, each with what it changes and why you would touch it.
- **Screenshots and a GIF.** Non-negotiable for a store page, and the GUI is the differentiator —
  the searchable picker and the side drawers are what makes this not just another beacon item.
- **CHANGELOG** in Keep a Changelog form, since both platforms render release notes from it.

## Publishing

Manual for the first release on both platforms — automating a pipeline before it has run once by
hand is how you debug a pipeline instead of a mod.

- **Modrinth**: fast, near-instant listing. Needs description, MIT licence, categories
  (`utility`, `equipment`), gallery, and a source link.
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
