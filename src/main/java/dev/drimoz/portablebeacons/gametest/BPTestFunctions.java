package dev.drimoz.portablebeacons.gametest;

import dev.drimoz.portablebeacons.PortableBeacons;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunctionLoader;
import net.minecraft.resources.ResourceKey;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Puts the seven test bodies into the {@code test_function} registry.
 *
 * <p>26.1 dropped the {@code @GameTest} annotation: a test is now a registered function plus a
 * {@code test_instance} data file naming it, its structure and its timeout. The bodies did not
 * change - only how the game finds them. The pairing is the cost: a function with no data file
 * never runs, and a data file naming a function that does not exist fails to load.
 */
public final class BPTestFunctions extends TestFunctionLoader {

    public static void register() {
        TestFunctionLoader.registerLoader(new BPTestFunctions());
    }

    @Override
    public void load(BiConsumer<ResourceKey<Consumer<GameTestHelper>>, Consumer<GameTestHelper>> register) {
        register.accept(key("pack_applies_its_effect_to_the_carrier"),
                BPGameTests::packAppliesItsEffectToTheCarrier);
        register.accept(key("an_inactive_pack_applies_nothing"),
                BPGameTests::anInactivePackAppliesNothing);
        register.accept(key("aura_reaches_a_second_player"),
                BPGameTests::auraReachesASecondPlayer);
        register.accept(key("self_mode_reaches_nobody_else"),
                BPGameTests::selfModeReachesNobodyElse);
        register.accept(key("team_mode_excludes_players_off_the_team"),
                BPGameTests::teamModeExcludesPlayersOffTheTeam);
        register.accept(key("aura_reaches_a_tamed_pet_but_not_a_stray_one"),
                BPGameTests::auraReachesATamedPetButNotAStrayOne);
        register.accept(key("hostile_action_indices_are_rejected"),
                BPGameTests::hostileActionIndicesAreRejected);
    }

    private static ResourceKey<Consumer<GameTestHelper>> key(String name) {
        return ResourceKey.create(Registries.TEST_FUNCTION,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(
                        PortableBeacons.MOD_ID, name));
    }

    private BPTestFunctions() {}
}
