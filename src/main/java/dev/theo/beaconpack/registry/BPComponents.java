package dev.theo.beaconpack.registry;

import dev.theo.beaconpack.BeaconPack;
import dev.theo.beaconpack.core.AugmentInstance;
import dev.theo.beaconpack.core.PackState;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The two data components the mod owns. Slot contents are not among them: augments and fuel live in
 * vanilla's {@code minecraft:container}, which lets NeoForge's item-backed handler write straight
 * back into the stack instead of us shuttling a copied inventory around.
 */
public final class BPComponents {

    public static final DeferredRegister.DataComponents REGISTRAR =
            DeferredRegister.createDataComponents(BeaconPack.MOD_ID);

    /** Everything a pack remembers: effect slots, fuel buffer, master switch. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PackState>> PACK =
            REGISTRAR.registerComponentType("pack", builder -> builder
                    .persistent(PackState.CODEC)
                    .networkSynchronized(PackState.STREAM_CODEC));

    /** What turns the single generic augment item into a specific augment. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AugmentInstance>>
            AUGMENT = REGISTRAR.registerComponentType("augment", builder -> builder
                    .persistent(AugmentInstance.CODEC)
                    .networkSynchronized(AugmentInstance.STREAM_CODEC));

    private BPComponents() {}
}
