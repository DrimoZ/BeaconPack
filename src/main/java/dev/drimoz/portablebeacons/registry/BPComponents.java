package dev.drimoz.portablebeacons.registry;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.core.BeaconState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The two data components the mod owns. Slot contents are not among them: augments and fuel live in
 * vanilla's {@code minecraft:container}, which lets NeoForge's item-backed handler write straight
 * back into the stack instead of us shuttling a copied inventory around.
 */
public final class BPComponents {

    public static final DeferredRegister.DataComponents REGISTRAR =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, PortableBeacons.MOD_ID);

    /** Everything a beacon remembers: effect slots, fuel buffer, master switch. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BeaconState>> PACK =
            REGISTRAR.registerComponentType("beacon", builder -> builder
                    .persistent(BeaconState.CODEC)
                    .networkSynchronized(BeaconState.STREAM_CODEC));

    /** What turns the single generic augment item into a specific augment. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AugmentInstance>>
            AUGMENT = REGISTRAR.registerComponentType("augment", builder -> builder
                    .persistent(AugmentInstance.CODEC)
                    .networkSynchronized(AugmentInstance.STREAM_CODEC));

    private BPComponents() {}
}
