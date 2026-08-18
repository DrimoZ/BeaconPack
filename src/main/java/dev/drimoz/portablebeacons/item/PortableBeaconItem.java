package dev.drimoz.portablebeacons.item;

import dev.drimoz.portablebeacons.BPConfig;
import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.core.BeaconResolver;
import dev.drimoz.portablebeacons.core.BeaconStats;
import dev.drimoz.portablebeacons.core.EffectSlotConfig;
import dev.drimoz.portablebeacons.core.Durations;
import dev.drimoz.portablebeacons.core.BeaconState;
import dev.drimoz.portablebeacons.core.BeaconTierDef;
import dev.drimoz.portablebeacons.menu.BeaconMenuOpener;
import dev.drimoz.portablebeacons.registry.BPComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The portable beacon itself. One class, four registered instances, one per tier. */
public class PortableBeaconItem extends Item {

    /** Slots 0..2 hold augments, slot 3 holds fuel. Slots above the tier's count stay locked. */
    public static final int AUGMENT_SLOTS = 3;
    public static final int FUEL_SLOT = AUGMENT_SLOTS;
    public static final int CONTAINER_SIZE = AUGMENT_SLOTS + 1;

    private final ResourceKey<BeaconTierDef> tier;

    public PortableBeaconItem(Properties properties, ResourceKey<BeaconTierDef> tier) {
        super(properties);
        this.tier = tier;
    }

    public ResourceKey<BeaconTierDef> tier() {
        return tier;
    }

    public static BeaconState stateOf(ItemStack stack) {
        return stack.getOrDefault(BPComponents.BEACON.get(), BeaconState.EMPTY);
    }

    public static void setState(ItemStack stack, BeaconState state) {
        stack.set(BPComponents.BEACON.get(), state);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer && hand == InteractionHand.MAIN_HAND) {
            BeaconMenuOpener.open(serverPlayer, player.getInventory().selected);
        }
        // The offhand has no drawn slot to freeze, so it opens nothing rather than opening a menu
        // whose source stack the player could still move. It keeps working while carried.
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        BeaconState state = stateOf(stack);
        tooltip.add(Component.translatable(state.active()
                        ? "portablebeacons.gui.active" : "portablebeacons.gui.inactive")
                .withStyle(state.active() ? ChatFormatting.GREEN : ChatFormatting.GRAY));

        if (!TooltipDetail.expanded()) {
            tooltip.add(TooltipDetail.HINT);
            return;
        }
        if (context.registries() == null) {
            return;
        }
        context.registries().lookup(BPRegistryKeys.TIER)
                .flatMap(lookup -> lookup.get(tier))
                .ifPresent(holder -> tooltip.add(Component.translatable("portablebeacons.tip.beacon_tier",
                                holder.value().level(),
                                holder.value().effectSlots(),
                                holder.value().augmentSlots())
                        .withStyle(ChatFormatting.DARK_GRAY)));

        // The configured effects, so a beacon in a chest can be identified without opening it.
        context.registries().lookup(BPRegistryKeys.EFFECT).ifPresent(lookup -> {
            for (EffectSlotConfig slot : state.effects()) {
                lookup.get(slot.effect()).ifPresent(holder -> tooltip.add(Component.empty()
                        .append(holder.value().effect().value().getDisplayName())
                        .append(" ")
                        .append(String.valueOf(slot.amplifier() + 1))
                        .append(" - ")
                        .append(Component.translatable(
                                "portablebeacons.aura." + slot.aura().getSerializedName()))
                        .withStyle(slot.enabled() ? ChatFormatting.GRAY
                                : ChatFormatting.DARK_GRAY)));
            }
        });
        // Installed augments, which were missing entirely: two beacons of the same tier can behave
        // completely differently and looked identical in a chest.
        List<AugmentInstance> augments = augmentsOf(stack);
        for (AugmentInstance augment : augments) {
            tooltip.add(Component.translatable("portablebeacons.tip.augment_line",
                            Component.translatable("augment." + augment.type().location().getNamespace()
                                    + "." + augment.type().location().getPath(),
                                    Component.translatable("portablebeacons.tier." + augment.tier())))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        appendRuntime(stack, context, state, tooltip);
    }

    /**
     * Remaining runtime rather than a fuel count: the stored number is an implementation detail of
     * the datapack format, and only the time it buys is actionable.
     */
    private void appendRuntime(ItemStack stack, TooltipContext context, BeaconState state,
                               List<Component> tooltip) {
        HolderLookup.Provider registries = context.registries();
        if (registries == null || state.fuel() <= 0 || !BPConfig.fuelEnabled()) {
            return;
        }
        BeaconTierDef tierDef = lookup(registries, BPRegistryKeys.TIER, tier);
        if (tierDef == null) {
            return;
        }
        BeaconStats stats = BeaconResolver.resolve(tierDef, augmentsOf(stack),
                key -> Optional.ofNullable(lookup(registries, BPRegistryKeys.AUGMENT, key)));
        double perSecond = BeaconResolver.fuelPerSecond(state, stats,
                key -> Optional.ofNullable(lookup(registries, BPRegistryKeys.EFFECT, key)));
        if (perSecond <= 0.0) {
            return;
        }
        tooltip.add(Component.translatable("portablebeacons.gui.runtime",
                        Durations.format((int) (state.fuel() / perSecond)))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static List<AugmentInstance> augmentsOf(ItemStack stack) {
        IItemHandler handler = stack.getCapability(Capabilities.ItemHandler.ITEM);
        if (handler == null) {
            return List.of();
        }
        List<AugmentInstance> found = new ArrayList<>(AUGMENT_SLOTS);
        for (int slot = 0; slot < Math.min(AUGMENT_SLOTS, handler.getSlots()); slot++) {
            AugmentInstance instance = AugmentItem.instanceOf(handler.getStackInSlot(slot));
            if (instance != null) {
                found.add(instance);
            }
        }
        return found;
    }

    @Nullable
    private static <T> T lookup(HolderLookup.Provider registries,
                                ResourceKey<Registry<T>> registry, ResourceKey<T> key) {
        return registries.lookup(registry)
                .flatMap(lookup -> lookup.get(key))
                .map(Holder::value)
                .orElse(null);
    }


    /** A running beacon glints. Cheapest possible "this is on" signal, visible from the hotbar. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return stateOf(stack).active();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stateOf(stack).capacity() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(stateOf(stack).fillRatio() * 13.0);
    }

    /** Green to red as it empties, so a nearly dry beacon is obvious without reading the tooltip. */
    @Override
    public int getBarColor(ItemStack stack) {
        float ratio = (float) stateOf(stack).fillRatio();
        return Mth.hsvToRgb(ratio / 3.0F, 1.0F, 1.0F);
    }
}
