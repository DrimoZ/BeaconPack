# Roadmap

**1.0.0.** 0.9.0 shipped as a beta on CurseForge; the aura has since been verified on a dedicated
server with two players, which was the last criterion. NeoForge 1.21.1. Everything below is what comes
after that, ordered by what actually moves the mod forward now — which is no longer "what is
missing" but "what do players hit first".

The pre-release checklist that used to live here is done; its history is in the git log and the
[changelog](CHANGELOG.md).

---

## Now

**Upload 1.0.0** to CurseForge, release type Release, and to Modrinth. Changelog and listing copy
are ready in [CHANGELOG.md](CHANGELOG.md) and [STORE.md](STORE.md).

## Then — the first week

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

## What 1.0 rested on

1.0 was never a feature list, it was **a version someone had actually played**. Two of the three
are in:

- The aura confirmed on a dedicated server with two players - *Allies* reaches the second player,
  *Team* excludes one off the team. Done, with the second client run this repo now carries.
- No open report of lost pack contents, duplicated items or a crashed server.
- **Still open:** one translation reviewed by a native speaker. Fifteen locales shipped and all
  fifteen are mine. The first correction from a real player outranks anything I wrote.

## Open question — rename to "Portable Beacon"?

Clearer than "BeaconPack", which reads as a modpack. Worth doing, but the cost splits sharply in
two and only one half is cheap.

**The display name is free.** `mod_name`, the store pages, the wiki, the README, the creative tab
and the item names are all just text. "Portable Beacon" could ship in the next release with no
consequence beyond a changelog line.

**The mod id is not.** `beaconpack` is baked into every datapack registry path, the item and
advancement ids, the `#beaconpack:packs` tag, the data components stored on every pack in every
existing world, and the CurseForge project. Changing it after publishing means every pack in every
existing save becomes an unknown item. Addons and datapacks written against it break too.

So: rename the display name if it reads better, keep the id. Mods do this routinely — the id is an
internal name, not a brand. If the id truly must change, it belongs to a 2.0 with a migration, not
to a patch.

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
