package dev.theo.beaconpack.item;

import dev.theo.beaconpack.core.PackState;
import dev.theo.beaconpack.core.PackTierDef;
import dev.theo.beaconpack.registry.BPComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
    public boolean isBarVisible(ItemStack stack) {
        return stateOf(stack).fuel() > 0;
    }
}
