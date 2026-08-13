# Roadmap

**0.9.0 is published on CurseForge as a beta.** NeoForge 1.21.1. Everything below is what comes
after that, ordered by what actually moves the mod forward now — which is no longer "what is
missing" but "what do players hit first".

The pre-release checklist that used to live here is done; its history is in the git log and the
[changelog](CHANGELOG.md).

---

## Now — the first week

**Watch the issue tracker.** This is the whole point of shipping a beta. Until reports come in,
every priority below is a guess, and a guess should lose to a real report every time.

**Modrinth.** Publish the same jar there. It costs one upload, lists near-instantly, and reaches a
different crowd from CurseForge. Copy from [STORE.md](STORE.md); pick the custom licence option and
point it at `LICENSE`.

**Screenshots.** Still the single biggest thing standing between the page and a download, whichever
platform. Three carry it: the pack screen with a drawer open, the effect picker mid-search, and the
JEI fuel category. A short GIF of a drawer opening would do more than any paragraph on the page.

**Tell people it exists.** r/feedthebeast and r/MinecraftMods on a weekend, the NeoForged Discord's
showcase channel. Modpack authors are the real growth channel — a small utility mod with a clean
GUI and datapack hooks is easy to include, and the permissions block already says yes for them.

## Next — what 1.0 needs

1.0 is not a feature list, it is **a version other people have played**. Concretely:

- No open report of anything losing a pack's contents, duplicating items, or crashing a server.
- The aura confirmed working by someone who is not the author, on a real multiplayer server. The
  game tests cover it, but a test and a server are not the same evidence.
- One translation reviewed by a native speaker of that language. Fifteen locales shipped, and all
  fifteen are mine; the first correction from a real player should outrank anything I wrote.

Then bump to 1.0.0 and change the release type. Nothing else is required.

## Soon — worth doing regardless

**EMI plugin.** JEI's is shipped; EMI matters at least as much on 1.21 and its absence means
augments collapse into one entry there, exactly the defect the JEI plugin exists to fix. Same
subtype idea, different API.

**Convention tags for fuel.** Fuel is per-item today. Accepting `c:ingots/*` style tags would pick
up modded metals with no data file per mod — the sort of thing modpack authors notice.

**`EventBusSubscriber.Bus` is deprecated for removal** on 21.1.248 — ten warnings at compile. Clear
it before any port, not during one.

**Sounds for slotting an augment.** Every button clicks; the slots are silent.

## Later — ports

The order the code is shaped for: **1.21.1 → current 1.21.x → 1.20.1 backport**. `core/` and
`PackState`'s codec are the only layers that should need thought on each hop.

Do not start before 1.0 has been in players' hands for a few weeks. Every extra branch multiplies
each bug report by the number of branches, and reports have only just begun.

## Publishing mechanics

Manual uploads for now. Once both platforms have accepted one by hand, `minotaur` (Modrinth) and
`curseforge-gradle` can publish from a local Gradle task — no CI needed, which is consistent with
this project not having any.

Keep the changelog current as things land: both platforms render release notes straight from it.
