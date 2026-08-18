package dev.drimoz.portablebeacons.gametest;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.PackTicker;
import dev.drimoz.portablebeacons.core.AuraMode;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.core.BeaconEffectDef;
import dev.drimoz.portablebeacons.core.EffectSlotConfig;
import dev.drimoz.portablebeacons.core.PackState;
import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import dev.drimoz.portablebeacons.menu.PortableBeaconMenu;
import dev.drimoz.portablebeacons.registry.BPItems;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-game tests for the seam the unit tests cannot reach: real players, in a real level, with the
 * aura actually looking around for them.
 *
 * <p>The aura is the one part of the mod whose behaviour depends on other entities existing, so it
 * could never be checked in single player - there, the carrier is the only player and every mode
 * looks identical.
 *
 * <p>These drive {@link PackTicker#tickPlayer} directly rather than waiting for the 40-tick
 * interval to come round, which also keeps them independent of server timing they do not control.
 *
 * <p><b>What this does not cover:</b> the {@code PlayerTickEvent.Post} subscription that calls
 * {@code tickPlayer} in the real game. The players here are synthetic - they are in the level and
 * tick as entities, but their connection never reaches the state where the server drives a player
 * tick, so the event never fires for them. A test that waited for it failed for that reason alone
 * and was removed rather than left red or weakened into passing. That one annotation is covered by
 * the mod being played.
 *
 * <p>Run headlessly with {@code ./gradlew runGameTestServer}, or {@code /test run portablebeacons} in a
 * client.
 */
@GameTestHolder(PortableBeacons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BPGameTests {

    private static final String PLATFORM = "platform";
    private static final int TIMEOUT = 200;

    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);

    private static final ResourceKey<BeaconEffectDef> SPEED =
            ResourceKey.create(BPRegistryKeys.EFFECT, BPRegistryKeys.id("speed"));

    /** Distinct names per player - see {@link #spawnPlayer(GameTestHelper, Cleanup, String)}. */
    private static final AtomicInteger NEXT_PLAYER = new AtomicInteger();

    @GameTest(template = PLATFORM, timeoutTicks = TIMEOUT)
    public static void packAppliesItsEffectToTheCarrier(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            givePack(carrier, AuraMode.SELF);

            PackTicker.tickPlayer(carrier);

            helper.assertTrue(carrier.getEffect(MobEffects.MOVEMENT_SPEED) != null,
                    "the carrier did not receive the effect their own pack projects");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = TIMEOUT)
    public static void anInactivePackAppliesNothing(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            ItemStack pack = givePack(carrier, AuraMode.SELF);
            PortableBeaconItem.setState(pack, PortableBeaconItem.stateOf(pack).withActive(false));

            PackTicker.tickPlayer(carrier);

            helper.assertTrue(carrier.getEffect(MobEffects.MOVEMENT_SPEED) == null,
                    "a pack switched off still applied its effect");
        });
    }

    /** The half of the aura that would otherwise need a second client to test by hand. */
    @GameTest(template = PLATFORM, timeoutTicks = TIMEOUT)
    public static void auraReachesASecondPlayer(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            ServerPlayer bystander = spawnPlayer(helper, cleanup);
            givePack(carrier, AuraMode.ALLIES);

            PackTicker.tickPlayer(carrier);

            helper.assertTrue(bystander.getEffect(MobEffects.MOVEMENT_SPEED) != null,
                    "a player standing next to the carrier was not reached by the aura");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = TIMEOUT)
    public static void selfModeReachesNobodyElse(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            ServerPlayer bystander = spawnPlayer(helper, cleanup);
            givePack(carrier, AuraMode.SELF);

            PackTicker.tickPlayer(carrier);

            helper.assertTrue(carrier.getEffect(MobEffects.MOVEMENT_SPEED) != null,
                    "the carrier lost their own effect");
            helper.assertTrue(bystander.getEffect(MobEffects.MOVEMENT_SPEED) == null,
                    "a pack set to self only leaked its effect to a bystander");
        });
    }

    /**
     * Team mode is the one that excludes, and the exclusion is what cannot be seen in single
     * player: there, the carrier is trivially allied with everyone who exists.
     */
    @GameTest(template = PLATFORM, timeoutTicks = TIMEOUT)
    public static void teamModeExcludesPlayersOffTheTeam(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            ServerPlayer outsider = spawnPlayer(helper, cleanup);

            Scoreboard scoreboard = helper.getLevel().getScoreboard();
            PlayerTeam team = scoreboard.addPlayerTeam("portablebeacons_test_team");
            cleanup.add(() -> scoreboard.removePlayerTeam(team));
            scoreboard.addPlayerToTeam(carrier.getScoreboardName(), team);

            givePack(carrier, AuraMode.TEAM);
            PackTicker.tickPlayer(carrier);

            helper.assertTrue(carrier.getEffect(MobEffects.MOVEMENT_SPEED) != null,
                    "the carrier lost their own effect in team mode");
            helper.assertTrue(outsider.getEffect(MobEffects.MOVEMENT_SPEED) == null,
                    "team mode reached a player who is not on the team");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = TIMEOUT)
    public static void auraReachesATamedPetButNotAStrayOne(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            givePack(carrier, AuraMode.ALLIES_AND_PETS);

            Wolf pet = helper.spawnWithNoFreeWill(EntityType.WOLF, CENTRE);
            pet.tame(carrier);
            Wolf stray = helper.spawnWithNoFreeWill(EntityType.WOLF, CENTRE);

            PackTicker.tickPlayer(carrier);

            helper.assertTrue(pet.getEffect(MobEffects.MOVEMENT_SPEED) != null,
                    "a tamed pet was not reached by allies_and_pets");
            helper.assertTrue(stray.getEffect(MobEffects.MOVEMENT_SPEED) == null,
                    "an untamed animal was reached by allies_and_pets");
        });
    }

    /**
     * Regression test for negative indices.
     *
     * <p>slot and value are var-ints on the wire, so a modified client can send -1. Every guard in
     * applyAction was an upper bound, so a negative index used to reach List.set / List.remove and
     * throw on the server thread.
     */
    @GameTest(template = PLATFORM, timeoutTicks = TIMEOUT)
    public static void hostileActionIndicesAreRejected(GameTestHelper helper) {
        run(helper, cleanup -> {
            ServerPlayer carrier = spawnPlayer(helper, cleanup);
            ItemStack pack = givePack(carrier, AuraMode.SELF);

            // Built directly rather than through PackMenuOpener: openMenu writes a screen payload,
            // and a mock player's connection refuses one outright ("Payload
            // neoforge:advanced_open_screen may not be sent to the client"). Nothing in the action
            // path needs a real connection - broadcastChanges is a no-op without a synchronizer.
            PortableBeaconMenu menu = new PortableBeaconMenu(1, carrier.getInventory(), 0);
            PackState before = PortableBeaconItem.stateOf(pack);

            int[] actions = {
                    PortableBeaconMenu.ACTION_SET_EFFECT,
                    PortableBeaconMenu.ACTION_CLEAR_EFFECT,
                    PortableBeaconMenu.ACTION_CYCLE_AMPLIFIER,
                    PortableBeaconMenu.ACTION_TOGGLE_EFFECT,
                    PortableBeaconMenu.ACTION_CYCLE_AURA,
            };
            for (int action : actions) {
                // Each of these threw before the guard was added. The real assertion is that
                // nothing escapes, but the return value is checked too, so a silent no-op that
                // reported success would still fail.
                helper.assertFalse(menu.applyAction(action, -1, 0),
                        "a negative slot index was accepted by action " + action);
                helper.assertFalse(menu.applyAction(action, 0, -1),
                        "a negative value was accepted by action " + action);
                helper.assertFalse(menu.applyAction(action, Integer.MIN_VALUE, Integer.MIN_VALUE),
                        "Integer.MIN_VALUE was accepted by action " + action);
            }

            helper.assertTrue(PortableBeaconItem.stateOf(pack).equals(before),
                    "a rejected action still changed the pack");
        });
    }

    /**
     * Places a real {@link ServerPlayer} in the level - not a mock object: the aura queries the
     * level for nearby players, so a player that is not in it would pass every test vacuously.
     */
    private static ServerPlayer spawnPlayer(GameTestHelper helper, Cleanup cleanup) {
        return spawnPlayer(helper, cleanup, "bp_test_" + NEXT_PLAYER.getAndIncrement());
    }

    /**
     * Built here rather than through {@code GameTestHelper#makeMockServerPlayerInLevel} for one
     * reason: that helper names every player it makes {@code test-mock-player}, and a scoreboard
     * team is keyed by name. Two players sharing a name are the same team member, which made the
     * team test assert something that could never be false.
     */
    private static ServerPlayer spawnPlayer(GameTestHelper helper, Cleanup cleanup, String name) {
        ServerLevel level = helper.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), name), false);
        ServerPlayer player = new ServerPlayer(
                level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);

        BlockPos where = helper.absolutePos(CENTRE);
        player.teleportTo(where.getX() + 0.5, where.getY(), where.getZ() + 0.5);
        cleanup.add(() -> level.getServer().getPlayerList().remove(player));
        return player;
    }

    /** A tier IV pack, switched on, projecting Speed, with more fuel than a test can burn. */
    private static ItemStack givePack(ServerPlayer player, AuraMode aura) {
        ItemStack pack = new ItemStack(BPItems.PACK_IV.get());
        PortableBeaconItem.setState(pack, new PackState(
                List.of(new EffectSlotConfig(SPEED, 0, true, aura)),
                Integer.MAX_VALUE / 2, true, 0));
        player.getInventory().setItem(0, pack);
        return pack;
    }

    private static void run(GameTestHelper helper, java.util.function.Consumer<Cleanup> body) {
        Cleanup cleanup = new Cleanup();
        cleanup.around(() -> {
            body.accept(cleanup);
            helper.succeed();
        });
    }

    /**
     * Undoes what a test added to shared server state.
     *
     * <p>Mock players join the real player list and a scoreboard team is global, so a test that
     * failed halfway would otherwise poison every test after it - which surfaces as a cascade of
     * unrelated failures. Hence running the body inside a finally.
     */
    private static final class Cleanup {
        private final List<Runnable> actions = new ArrayList<>();

        void add(Runnable action) {
            actions.add(action);
        }

        void around(Runnable body) {
            try {
                body.run();
            } finally {
                for (int i = actions.size() - 1; i >= 0; i--) {
                    actions.get(i).run();
                }
            }
        }
    }

    private BPGameTests() {}
}
