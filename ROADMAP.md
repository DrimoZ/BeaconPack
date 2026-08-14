# Roadmap

**1.0.0, renamed to Portable Beacons.** 0.9.0 shipped as a beta under the old name; since then the
aura has been verified on a dedicated server with two players, and the mod, its id, its repository
and its CurseForge page have all been renamed. NeoForge 1.21.1.

What follows is ordered by what actually moves the mod forward now — which is no longer "what is
missing" but "what do players hit first". The pre-release checklist that used to live here is done;
its history is in the git log and the [changelog](CHANGELOG.md).

---

## Now

**Upload 1.0.0** to CurseForge as release type *Release*. The project is live at
<https://www.curseforge.com/minecraft/mc-mods/portables-beacons> and already renamed; the changelog
section and the listing copy are ready in [CHANGELOG.md](CHANGELOG.md) and [STORE.md](STORE.md).

**Say the rename out loud on the page.** Anyone who played the 0.9.0 beta loses their saved beacons
- the ids moved. That is worth one line at the top of the description, not a footnote.

## Then — the first week

**Watch the issue tracker.** This is the whole point of shipping. Until reports come in, every
priority below is a guess, and a guess should lose to a real report every time.

**Modrinth.** Not created yet. One upload, lists near-instantly, reaches a different crowd from
CurseForge. Copy from [STORE.md](STORE.md); pick the custom licence option and point it at
`LICENSE`.

**Screenshots.** Still the single biggest thing standing between the page and a download, whichever
platform. Three carry it: the beacon screen with a drawer open, the effect picker mid-search, and the
JEI fuel category. A short GIF of a drawer opening would do more than any paragraph on the page.

**Tell people it exists.** r/feedthebeast and r/MinecraftMods on a weekend, the NeoForged Discord's
showcase channel. Modpack authors are the real growth channel — a small utility mod with a clean
GUI and datapack hooks is easy to include, and the permissions block already says yes for them.

## What 1.0 rested on

1.0 was never a feature list, it was **a version someone had actually played**. Two of the three
are in:

- The aura confirmed on a dedicated server with two players - *Allies* reaches the second player,
  *Team* excludes one off the team. Done, with the second client run this repo now carries.
- No open report of lost beacon contents, duplicated items or a crashed server.
- **Still open:** one translation reviewed by a native speaker. Fifteen locales shipped and all
  fifteen are mine. The first correction from a real player outranks anything I wrote.

## Settled — the rename

Done, and done fully: display name, mod id, Java package, every item and datapack id, the repository
and the CurseForge page. "BeaconPack" read as a modpack, which is the one thing this is not.

I argued for changing only the display name and keeping the id, because an id is baked into the data
component on every saved beacon and into any datapack written against it. That reasoning is right in
general and was wrong here: the beta had been public for a day, so the choice was a handful of saves
now against every save the mod would ever touch. Before 1.0 or never.

If the id ever has to move again, it belongs to a 2.0 with a migration, not to a patch.

## Soon — worth doing regardless

Nothing left here. EMI, fuel tags, the deprecations and the augment chime all shipped in 1.0.0.
What comes next should come from a report, not from a guess.

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
