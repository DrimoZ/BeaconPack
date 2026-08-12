package dev.theo.beaconpack.item;

import dev.theo.beaconpack.core.AugmentInstance;
import dev.theo.beaconpack.registry.BPComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * The single item behind every augment. Its identity, colour and stats all come from the
 * {@code beaconpack:augment} component pointing into the datapack registry.
 */
public class AugmentItem extends Item {

    public AugmentItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static AugmentInstance instanceOf(ItemStack stack) {
        return stack.get(BPComponents.AUGMENT.get());
    }

    /**
     * Names come from the registry key, so a datapack-added augment only needs a matching
     * translation key — no code, no model, no item registration.
     */
    @Override
    public Component getName(ItemStack stack) {
        AugmentInstance instance = instanceOf(stack);
        if (instance == null) {
            return super.getName(stack);
        }
        return Component.translatable(
                "augment." + instance.type().location().getNamespace()
                        + "." + instance.type().location().getPath(),
                Component.translatable("beaconpack.tier." + instance.tier()));
    }
}
