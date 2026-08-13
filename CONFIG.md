# Configuration

Four options, all server-side. Everything else about the mod — which effects exist, what they cost,
what augments do — is [datapack territory](DATAPACK.md), deliberately not config.

## Where the file lives

```
config/beaconpack-server.toml
```

That is the default for every world. To override it for one world only, copy the file to:

```
<world>/serverconfig/beaconpack-server.toml
```

On a client that is `saves/<world>/serverconfig/`, on a dedicated server `<level-name>/serverconfig/`.
A per-world copy wins over the global one.

Being a server config, it is **synced to clients on join**. Players connecting to your server get
your rules, not their local file, and the pack screen adapts to them — see `require_fuel` below.

## The options

All four live under a `[gameplay]` section.

### `require_fuel` — default `true`

Whether packs consume fuel at all.

Turn it off for a purely craft-gated mod: you earn the pack once, and it works forever. This is not
a cosmetic switch — with fuel off, the fuel slot, the gauge and every runtime figure disappear from
the screen entirely. A gauge that never moves and a slot that accepts nothing are worse than no
gauge and no slot.

Consider turning it off if your pack already gates progression hard elsewhere, or if your players
find resource upkeep tedious. Leave it on if you want carrying a pack to remain a running cost —
which is what makes the Efficiency and Capacity augments worth slotting.

### `aura_affects_non_team_players` — default `true`

Whether an effect set to the `allies` aura mode reaches players who are not on your scoreboard team.

**Servers with PvP usually want this off.** With it on, `allies` means "everyone nearby", so
standing near an enemy buffs them too. With it off, `allies` behaves like `team` unless a player is
actually on your team.

Note this does not affect the `team` mode, which always requires an actual scoreboard team
regardless of this setting.

### `free_while_near_beacon` — default `true`

Whether an effect a real beacon is already providing stops costing fuel.

This exists so a pack does not punish you for standing in your own base. With it on, the pack skips
those effects entirely rather than reapplying them, so it neither charges you nor fights the beacon.

Turn it off if you want the pack to be a strictly separate system with no interaction with placed
beacons.

### `require_beacon_to_configure` — default `false`

Whether changing a pack's effects requires standing within 16 blocks of a lit beacon.

Off by default, because the beacon block is already on the crafting path of every tier — you cannot
have a pack without having built a beacon first. Turn it on for a pack that wants stricter
progression, where reconfiguring is a trip home rather than something done in a cave.

What this gates is only *reconfiguration*: switching the pack or an individual effect **off** always
works, wherever you are. A player caught out in the field has to be able to stop the drain.
