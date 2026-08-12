# Roadmap to release

State at the time of writing: 46 commits on `feat/foundation`, nothing merged into `main`, nothing
pushed. `mod_version=0.1.0`. NeoForge 1.21.1 only.

Ordered by what blocks a release, not by effort.

---

## P0 — blocks publishing

These are cheap and every one of them is visible to a player or a reviewer.

| Item | Why |
|---|---|
| `issueTrackerURL` points at `github.com/theo/BeaconPack` | The repo is `DrimoZ/BeaconPack`. Every bug report link 404s. |
| No `logoFile` and no `logo.png` | The mod is a blank card in the in-game mod list, which is the first screen anyone judges it on. |
| No `displayURL` | No way from the mod list to the project page. |
| No `pack.mcmeta` in `src/main/resources` | Works today because NeoForge supplies a default; make the pack format explicit rather than inherited. |
| Version still `0.1.0` | Tag `1.0.0` at release; the version string is baked into the jar name and both platforms key updates off it. |
| Dedicated-server run never tested | The whole effect loop is server-side and the menu syncs across the wire. Single-player uses an integrated server, which hides a class of client/server-split bugs. Untested is the honest word here. |
| Multiplayer aura never tested | `AuraMode.ALLIES/TEAM/ALLIES_AND_PETS` has never had a second player in front of it. Team lookup and pet ownership are exactly where this breaks. |

## P1 — should ship in 1.0

**Curios slot type.** The integration works, but the mod declares no slot type, so out of the box
nobody can actually put a pack in a curio slot without a modpack author writing the config. Ship a
`charm` slot registration (still soft) or the feature reads as broken.

**Server-side action validation is untested.** `BeaconPackMenu#applyAction` does validate against
tier and stats — a modified client cannot trivially grant itself a tier IV aura. But it is the
largest untested surface in the mod and the one where a bug is an exploit on a public server.
Worth a test class that drives `applyAction` with hostile inputs: out-of-range slot indices,
effects the tier's pool rejects, amplifiers above the cap, aura modes above the tier.

**Game tests.** `neoforge.enabledGameTestNamespaces` is already set to the mod id and there is not a
single game test behind it. One test that places a player, gives them a pack, ticks, and asserts the
effect landed would cover the integration seam that unit tests structurally cannot reach.

**Data component migration.** `PackState`'s codec has no version field. The first time a released
pack's serialization changes, every pack in every existing world becomes an item with a component
that no longer parses. Decide now: either freeze the codec shape, or add a version int before
anyone has a world to lose.

**Datapack removal.** `sanitize` handles a pack whose effects exceed its stats. It does not obviously
handle a datapack that *removes* a tier or effect a saved pack still references. Worth a test.

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
