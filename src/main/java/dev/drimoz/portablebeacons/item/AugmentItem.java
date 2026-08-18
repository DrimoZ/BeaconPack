package dev.drimoz.portablebeacons.item;

import dev.drimoz.portablebeacons.core.AugmentDef;
import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.registry.BPComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * The single item behind every augment. Its identity, colour and stats all come from the
 * {@code portablebeacons:augment} component pointing into the datapack registry.
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
     * The same read, without building an {@link ItemStack} first.
     *
     * <p>Slots are read as resources now, and this runs once per augment slot per tick - turning
     * each one into a stack just to look at one component would allocate for nothing.
     */
    @Nullable
    public static AugmentInstance instanceOf(ItemResource resource) {
        return resource.get(BPComponents.AUGMENT.get());
    }

    /**
     * Names come from the registry key, so a datapack-added augment only needs a matching
     * translation key — no code, no model, no item registration.
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        AugmentInstance instance = instanceOf(stack);
        if (instance == null || context.registries() == null) {
            return;
        }
        if (!TooltipDetail.expanded()) {
            tooltip.accept(TooltipDetail.HINT);
            return;
        }
        context.registries().lookup(BPRegistryKeys.AUGMENT)
                .flatMap(lookup -> lookup.get(instance.type()))
                .ifPresent(holder -> {
                    for (AugmentDef.Operation op : holder.value().operations()) {
                        tooltip.accept(describe(op, instance.tier()));
                    }
                    tooltip.accept(Component.translatable("portablebeacons.tip.augment_rule")
                            .withStyle(ChatFormatting.DARK_GRAY));
                });
    }

    /** Reads the effect straight off the registry entry, so a datapack augment describes itself. */
    private static Component describe(AugmentDef.Operation op, int tier) {
        double value = op.valueFor(tier);
        String formatted = value == Math.rint(value)
                ? String.valueOf((int) value)
                : String.format(java.util.Locale.ROOT, "%.2f", value);
        return Component.translatable("portablebeacons.op." + op.type().getSerializedName(), formatted)
                .withStyle(ChatFormatting.GRAY);
    }

    @Override
    public Component getName(ItemStack stack) {
        AugmentInstance instance = instanceOf(stack);
        if (instance == null) {
            return super.getName(stack);
        }
        return Component.translatable(
                "augment." + instance.type().identifier().getNamespace()
                        + "." + instance.type().identifier().getPath(),
                Component.translatable("portablebeacons.tier." + instance.tier()));
    }
}
