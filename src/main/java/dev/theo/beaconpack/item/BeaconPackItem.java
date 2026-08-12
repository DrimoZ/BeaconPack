package dev.theo.beaconpack.item;

import dev.theo.beaconpack.core.BPRegistryKeys;
import dev.theo.beaconpack.core.EffectSlotConfig;
import dev.theo.beaconpack.core.PackState;
import dev.theo.beaconpack.core.PackTierDef;
import dev.theo.beaconpack.menu.PackMenuOpener;
import dev.theo.beaconpack.registry.BPComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** The portable beacon itself. One class, four registered instances, one per tier. */
public class BeaconPackItem extends Item {

    /** Slots 0..2 hold augments, slot 3 holds fuel. Slots above the tier's count stay locked. */
    public static final int AUGMENT_SLOTS = 3;
    public static final int FUEL_SLOT = AUGMENT_SLOTS;
    public static final int CONTAINER_SIZE = AUGMENT_SLOTS + 1;

    private final ResourceKey<PackTierDef> tier;

    public BeaconPackItem(Properties properties, ResourceKey<PackTierDef> tier) {
        super(properties);
        this.tier = tier;
    }

    public ResourceKey<PackTierDef> tier() {
        return tier;
    }

    public static PackState stateOf(ItemStack stack) {
        return stack.getOrDefault(BPComponents.PACK.get(), PackState.EMPTY);
    }

    public static void setState(ItemStack stack, PackState state) {
        stack.set(BPComponents.PACK.get(), state);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer && hand == InteractionHand.MAIN_HAND) {
            PackMenuOpener.open(serverPlayer, player.getInventory().selected);
        }
        // The offhand has no drawn slot to freeze, so it opens nothing rather than opening a menu
        // whose source stack the player could still move. It keeps working while carried.
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        PackState state = stateOf(stack);
        tooltip.add(Component.translatable(state.active()
                        ? "beaconpack.gui.active" : "beaconpack.gui.inactive")
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
                .ifPresent(holder -> tooltip.add(Component.translatable("beaconpack.tip.pack_tier",
                                holder.value().level(),
                                holder.value().effectSlots(),
                                holder.value().augmentSlots())
                        .withStyle(ChatFormatting.DARK_GRAY)));

        // The configured effects, so a pack in a chest can be identified without opening it.
        context.registries().lookup(BPRegistryKeys.EFFECT).ifPresent(lookup -> {
            for (EffectSlotConfig slot : state.effects()) {
                lookup.get(slot.effect()).ifPresent(holder -> tooltip.add(Component.empty()
                        .append(holder.value().effect().value().getDisplayName())
                        .append(" ")
                        .append(String.valueOf(slot.amplifier() + 1))
                        .append(" - ")
                        .append(Component.translatable(
                                "beaconpack.aura." + slot.aura().getSerializedName()))
                        .withStyle(slot.enabled() ? ChatFormatting.GRAY
                                : ChatFormatting.DARK_GRAY)));
            }
        });
        tooltip.add(Component.translatable("beaconpack.tip.fuel_short", state.fuel())
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** A running pack glints. Cheapest possible "this is on" signal, visible from the hotbar. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return stateOf(stack).active();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stateOf(stack).fuel() > 0;
    }
}
