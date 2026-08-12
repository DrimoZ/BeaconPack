package dev.theo.beaconpack;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/** Server-side knobs. Kept deliberately short: content belongs in datapacks, not in a config file. */
public final class BPConfig {

    public static final ModConfigSpec SPEC;
    public static final BPConfig INSTANCE;

    static {
        Pair<BPConfig, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(BPConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    public final ModConfigSpec.BooleanValue requireFuel;
    public final ModConfigSpec.BooleanValue auraAffectsNonTeamPlayers;
    public final ModConfigSpec.BooleanValue freeWhileNearBeacon;
    public final ModConfigSpec.BooleanValue requireBeaconToConfigure;

    private BPConfig(ModConfigSpec.Builder builder) {
        builder.push("gameplay");

        requireFuel = builder
                .comment("Whether packs consume fuel. Disable for a purely craft-gated mod.")
                .define("require_fuel", true);

        auraAffectsNonTeamPlayers = builder
                .comment("Allow aura effects to reach players who are not on your scoreboard team.",
                        "Servers with PvP usually want this off.")
                .define("aura_affects_non_team_players", true);

        freeWhileNearBeacon = builder
                .comment("Stop charging for an effect a real beacon is already providing.")
                .define("free_while_near_beacon", true);

        requireBeaconToConfigure = builder
                .comment("Require a lit beacon within 16 blocks to change a pack's effects.",
                        "Off by default: the beacon block is already on the crafting path of every",
                        "tier. Turn it on for a pack that wants the stricter progression.")
                .define("require_beacon_to_configure", false);

        builder.pop();
    }
}
