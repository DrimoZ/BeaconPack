package dev.drimoz.beaconpack.registry;

import dev.drimoz.beaconpack.BeaconPack;
import dev.drimoz.beaconpack.core.AugmentDef;
import dev.drimoz.beaconpack.core.BPRegistryKeys;
import dev.drimoz.beaconpack.core.BeaconEffectDef;
import dev.drimoz.beaconpack.core.FuelDef;
import dev.drimoz.beaconpack.core.PackTierDef;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Declares the three datapack registries. Every one of them passes a network codec so the client
 * gets the definitions too — the GUI has to render costs, caps and tier requirements without a
 * round trip.
 */
@EventBusSubscriber(modid = BeaconPack.MOD_ID)
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
