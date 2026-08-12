package dev.theo.beaconpack.item;

import dev.theo.beaconpack.core.AugmentDef;
import dev.theo.beaconpack.core.AugmentInstance;
import dev.theo.beaconpack.core.BPRegistryKeys;
import dev.theo.beaconpack.registry.BPComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

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
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        AugmentInstance instance = instanceOf(stack);
        if (instance == null || context.registries() == null) {
            return;
        }
        if (!TooltipDetail.expanded()) {
            tooltip.add(TooltipDetail.HINT);
            return;
        }
        context.registries().lookup(BPRegistryKeys.AUGMENT)
                .flatMap(lookup -> lookup.get(instance.type()))
                .ifPresent(holder -> {
                    for (AugmentDef.Operation op : holder.value().operations()) {
                        tooltip.add(describe(op, instance.tier()));
                    }
                    tooltip.add(Component.translatable("beaconpack.tip.augment_rule")
                            .withStyle(ChatFormatting.DARK_GRAY));
                });
    }

    /** Reads the effect straight off the registry entry, so a datapack augment describes itself. */
    private static Component describe(AugmentDef.Operation op, int tier) {
        double value = op.valueFor(tier);
        String formatted = value == Math.rint(value)
                ? String.valueOf((int) value)
                : String.format(java.util.Locale.ROOT, "%.2f", value);
        return Component.translatable("beaconpack.op." + op.type().getSerializedName(), formatted)
                .withStyle(ChatFormatting.GRAY);
    }

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
