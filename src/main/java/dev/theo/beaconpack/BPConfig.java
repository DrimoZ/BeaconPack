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

        builder.pop();
    }
}
