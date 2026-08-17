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

**Screenshots.** Still the single biggest thing standing between the page and a download. Three
carry it: the beacon screen with a drawer open, the effect picker mid-search, and the JEI fuel
category. A short GIF of a drawer opening would do more than any paragraph on the page.

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

## Next — the 26.x line

**1.21.1 → 26.1 → 26.2 → 26.3.** One hop at a time, a published file for each, rather than one jump
to whatever is newest.

This is a port, not a bump: ten primers separate 1.21.1 from 26.1, and the screen is rewritten
rather than migrated. The full analysis — what breaks per file, what the dependencies do, what the
toolchain needs — is in [PORTING.md](PORTING.md).

`1.21.1` stays maintained throughout. Every extra branch multiplies each bug report by the number of
branches, and reports have only just begun.

## Publishing mechanics

CurseForge only, and deliberately — one page to keep current beats two kept half-current, and the
audience this mod is aimed at is already there.

Manual uploads for now. Once CurseForge has accepted one by hand, `curseforge-gradle` can publish
from a local Gradle task — no CI needed, which is consistent with this project not having any.

Keep the changelog current as things land: both platforms render release notes straight from it.
