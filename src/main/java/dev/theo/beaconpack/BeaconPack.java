package dev.theo.beaconpack;

import dev.theo.beaconpack.core.BPRegistryKeys;
import dev.theo.beaconpack.registry.BPComponents;
import dev.theo.beaconpack.registry.BPCreativeTabs;
import dev.theo.beaconpack.registry.BPItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(BeaconPack.MOD_ID)
public class BeaconPack {

    public static final String MOD_ID = BPRegistryKeys.MOD_ID;

    public BeaconPack(IEventBus modEventBus, ModContainer modContainer) {
        BPComponents.REGISTRAR.register(modEventBus);
        BPItems.ITEMS.register(modEventBus);
        BPCreativeTabs.TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, BPConfig.SPEC);
    }
}
