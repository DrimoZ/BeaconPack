package dev.drimoz.portablebeacons.registry;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.core.AugmentDef;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.core.BeaconEffectDef;
import dev.drimoz.portablebeacons.core.FuelDef;
import dev.drimoz.portablebeacons.core.PackTierDef;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Declares the three datapack registries. Every one of them passes a network codec so the client
 * gets the definitions too — the GUI has to render costs, caps and tier requirements without a
 * round trip.
 */
@EventBusSubscriber(modid = PortableBeacons.MOD_ID)
public final class BPDatapackRegistries {

    @SubscribeEvent
    public static void register(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(BPRegistryKeys.EFFECT, BeaconEffectDef.CODEC, BeaconEffectDef.CODEC);
        event.dataPackRegistry(BPRegistryKeys.AUGMENT, AugmentDef.CODEC, AugmentDef.CODEC);
        event.dataPackRegistry(BPRegistryKeys.TIER, PackTierDef.CODEC, PackTierDef.CODEC);
        event.dataPackRegistry(BPRegistryKeys.FUEL, FuelDef.CODEC, FuelDef.CODEC);
    }

    private BPDatapackRegistries() {}
}
