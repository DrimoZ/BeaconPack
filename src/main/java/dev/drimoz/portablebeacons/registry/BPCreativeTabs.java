package dev.drimoz.portablebeacons.registry;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.core.AugmentDef;
import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BPCreativeTabs {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PortableBeacons.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
            "main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.portablebeacons"))
                    .icon(() -> new ItemStack(BPItems.BEACON_IV.get()))
                    .displayItems((parameters, output) -> {
                        BPItems.beacons().forEach(beacon -> output.accept(beacon.get()));

                        // Augments are enumerated from the registry, so datapack-added ones show up
                        // here on their own - the whole point of them not being separate items.
                        parameters.holders().lookup(BPRegistryKeys.AUGMENT).ifPresentOrElse(
                                lookup -> lookup.listElements()
                                        .forEach(holder -> addAugmentTiers(output, holder)),
                                // Only reachable if the tab is built before the datapack registry
                                // is available, which is silent otherwise: the tab simply comes out
                                // short and nothing is logged.
                                () -> LOGGER.warn("Augment registry unavailable while building the "
                                        + "creative tab; no augments were listed."));
                    })
                    .build());

    private static void addAugmentTiers(CreativeModeTab.Output output,
                                        Holder.Reference<AugmentDef> holder) {
        for (int tier = 1; tier <= holder.value().maxTier(); tier++) {
            ItemStack stack = new ItemStack(BPItems.AUGMENT.get());
            stack.set(BPComponents.AUGMENT.get(), new AugmentInstance(holder.key(), tier));
            output.accept(stack);
        }
    }

    private BPCreativeTabs() {}
}
