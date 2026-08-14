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

## Next session, in this order

1. The second client run below — it closes a 1.0 criterion without waiting for anyone.
2. Whatever else the release needs: screenshots, Modrinth, cutting 0.9.1.
3. The renaming question, only after those.

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

## Deferred — a second client for multiplayer testing

A second `runs` entry with its own game directory and username launches a second client against the
same dev server, which is what the open 1.0 criterion "the aura confirmed by someone who is not the
author" actually needs. It is the one thing that would close that item without waiting for a
player.

The reference that prompted this is [ForgeGradle](https://github.com/Create-Nuclear-Team/CreateNuclearForge/blob/V2/build.gradle#L113-L124),
so it cannot be pasted as-is: `workingDirectory`, `parent runs.client` and the mixin SRG properties
have no ModDevGradle equivalent. In MDG the shape is a plain second run —

```gradle
client2 {
    client()
    gameDirectory = project.file('run-client2')
    programArguments.addAll '--username', 'Dev2'
}
```

— which is the same pattern `gameTestServer` already uses here. Untested; verify when picking this
up. Its own directory matters for the reason the game test run needed one: runs that share `run/`
share `run/mods` and each other's world.

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
