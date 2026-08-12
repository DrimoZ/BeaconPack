package dev.theo.beaconpack.item;

import dev.theo.beaconpack.core.PackState;
import dev.theo.beaconpack.core.PackTierDef;
import dev.theo.beaconpack.menu.PackMenuOpener;
import dev.theo.beaconpack.registry.BPComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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
    public boolean isBarVisible(ItemStack stack) {
        return stateOf(stack).fuel() > 0;
    }
}
