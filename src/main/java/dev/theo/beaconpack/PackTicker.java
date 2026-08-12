package dev.theo.beaconpack;

import dev.theo.beaconpack.core.AuraMode;
import dev.theo.beaconpack.core.BeaconEffectDef;
import dev.theo.beaconpack.core.EffectSlotConfig;
import dev.theo.beaconpack.core.PackResolver;
import dev.theo.beaconpack.core.PackState;
import dev.theo.beaconpack.core.PackStats;
import dev.theo.beaconpack.core.PackTierDef;
import dev.theo.beaconpack.item.BeaconPackItem;
import dev.theo.beaconpack.registry.BPLookups;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Applies pack effects and charges for them.
 *
 * <p>Runs every {@link #INTERVAL} ticks rather than every tick: a beacon that follows the player
 * does not need 20 Hz precision, and the aura scan is an AABB query per projecting effect.
 */
@EventBusSubscriber(modid = BeaconPack.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class PackTicker {

    private static final int INTERVAL = 40;
    private static final double SECONDS_PER_INTERVAL = INTERVAL / 20.0;

    /** Comfortably longer than the interval, so effects never flicker between two ticks. */
    private static final int EFFECT_DURATION = 220;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % INTERVAL != 0) {
            return;
        }

        ItemStack pack = findActivePack(player);
        if (pack.isEmpty()) {
            return;
        }
        tickPack(player, pack);
    }

    /**
     * Only the first active pack does anything. Letting several stack would make the tier ladder
     * pointless — four tier-I packs would beat one tier-IV.
     */
    private static ItemStack findActivePack(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof BeaconPackItem && BeaconPackItem.stateOf(stack).active()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void tickPack(Player player, ItemStack pack) {
        RegistryAccess access = player.level().registryAccess();
        BeaconPackItem item = (BeaconPackItem) pack.getItem();
        PackTierDef tier = BPLookups.tier(access, item);
        if (tier == null) {
            return;
        }

        PackResolver.Lookup<BeaconEffectDef> effectLookup = BPLookups.effects(access);
        PackStats stats = PackResolver.resolve(
                tier, BPLookups.installedAugments(pack), BPLookups.augments(access));

        PackState state = PackResolver.sanitize(
                BeaconPackItem.stateOf(pack), stats, effectLookup, tier.level());

        List<EffectSlotConfig> toApply = new ArrayList<>(state.effects().size());
        double owed = 0.0;
        for (EffectSlotConfig slot : state.effects()) {
            if (!slot.enabled()) {
                continue;
            }
            if (isCoveredByRealBeacon(player, slot, effectLookup)) {
                // Skipped entirely, not just made free. Re-applying over the beacon's instance
                // would replace an ambient effect with a non-ambient one, the coverage check would
                // fail on the next tick, and the pack would start charging again - flipping between
                // free and paid every two seconds.
                continue;
            }
            owed += PackResolver.fuelPerSecond(slot, stats, effectLookup) * stats.fuelMultiplier();
            toApply.add(slot);
        }

        int cost = (int) Math.ceil(owed * SECONDS_PER_INTERVAL);
        if (BPConfig.INSTANCE.requireFuel.get() && cost > 0) {
            state = refuel(pack, state, stats, cost, player.level().registryAccess());
            if (state.fuel() < cost) {
                runDry(player, pack, state);
                return;
            }
            state = state.withFuel(state.fuel() - cost);
        }

        BeaconPackItem.setState(pack, state);
        for (EffectSlotConfig slot : toApply) {
            apply(player, slot, stats, effectLookup);
        }
    }

    /**
     * Switches the pack off and says so.
     *
     * <p>Effects stopping with no explanation reads as a bug. This fires once by construction: an
     * inactive pack is not ticked again until the player turns it back on.
     */
    private static void runDry(Player player, ItemStack pack, PackState state) {
        BeaconPackItem.setState(pack, state.withActive(false));
        player.displayClientMessage(
                Component.translatable("beaconpack.msg.out_of_fuel").withStyle(ChatFormatting.RED),
                true);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 1.0F);
    }

    /**
     * Tops the buffer up from the fuel slot, one item at a time, only while it is short. Burning a
     * netherite ingot to cover a 3-unit shortfall would be an unpleasant surprise, so a single item
     * is consumed per tick at most.
     */
    private static PackState refuel(ItemStack pack, PackState state, PackStats stats,
                                    int cost, RegistryAccess access) {
        if (state.fuel() >= cost) {
            return state;
        }
        IItemHandler handler = pack.getCapability(Capabilities.ItemHandler.ITEM);
        if (handler == null || handler.getSlots() <= BeaconPackItem.FUEL_SLOT) {
            return state;
        }
        ItemStack fuel = handler.getStackInSlot(BeaconPackItem.FUEL_SLOT);
        if (fuel.isEmpty()) {
            return state;
        }
        int units = BPLookups.fuelValue(access, fuel.getItem());
        if (units <= 0) {
            return state;
        }
        handler.extractItem(BeaconPackItem.FUEL_SLOT, 1, false);
        return state.withFuel(Math.min(stats.fuelCapacity(), state.fuel() + units));
    }

    /**
     * A real beacon already covering this effect makes the pack free for it.
     *
     * <p>Detection leans on the fact that beacons apply <em>ambient</em> instances while the pack
     * deliberately does not — see {@link #apply}.
     */
    private static boolean isCoveredByRealBeacon(Player player, EffectSlotConfig slot,
                                                 PackResolver.Lookup<BeaconEffectDef> lookup) {
        if (!BPConfig.INSTANCE.freeWhileNearBeacon.get()) {
            return false;
        }
        Optional<BeaconEffectDef> def = lookup.get(slot.effect());
        if (def.isEmpty()) {
            return false;
        }
        MobEffectInstance existing = player.getEffect(def.get().effect());
        return existing != null && existing.isAmbient() && existing.getAmplifier() >= slot.amplifier();
    }

    private static void apply(Player carrier, EffectSlotConfig slot, PackStats stats,
                              PackResolver.Lookup<BeaconEffectDef> lookup) {
        Optional<BeaconEffectDef> maybeDef = lookup.get(slot.effect());
        if (maybeDef.isEmpty()) {
            return;
        }
        BeaconEffectDef def = maybeDef.get();

        for (LivingEntity target : targets(carrier, slot.aura(), stats.range())) {
            // Not ambient on purpose: that flag is what lets isCoveredByRealBeacon tell a genuine
            // beacon apart from our own effect a tick later.
            target.addEffect(new MobEffectInstance(
                    def.effect(), EFFECT_DURATION, slot.amplifier(), false, true, true));
        }
    }

    private static List<LivingEntity> targets(Player carrier, AuraMode mode, double range) {
        List<LivingEntity> found = new ArrayList<>();
        found.add(carrier);
        if (!mode.isAura() || range <= 0.0) {
            return found;
        }

        AABB box = carrier.getBoundingBox().inflate(range);
        for (Player other : carrier.level().getEntitiesOfClass(Player.class, box)) {
            if (other != carrier && reaches(carrier, other, mode)) {
                found.add(other);
            }
        }
        if (mode == AuraMode.ALLIES_AND_PETS) {
            for (TamableAnimal pet : carrier.level().getEntitiesOfClass(TamableAnimal.class, box)) {
                if (pet.isTame() && pet.getOwner() == carrier) {
                    found.add(pet);
                }
            }
        }
        return found;
    }

    private static boolean reaches(Player carrier, Player other, AuraMode mode) {
        if (mode == AuraMode.TEAM) {
            return other.isAlliedTo(carrier);
        }
        return BPConfig.INSTANCE.auraAffectsNonTeamPlayers.get() || other.isAlliedTo(carrier);
    }

    private PackTicker() {}
}
