package dev.drimoz.beaconpack.registry;

import dev.drimoz.beaconpack.core.AugmentDef;
import dev.drimoz.beaconpack.core.AugmentInstance;
import dev.drimoz.beaconpack.core.BPRegistryKeys;
import dev.drimoz.beaconpack.core.BeaconEffectDef;
import dev.drimoz.beaconpack.core.FuelDef;
import dev.drimoz.beaconpack.core.PackResolver;
import dev.drimoz.beaconpack.core.PackTierDef;
import dev.drimoz.beaconpack.item.AugmentItem;
import dev.drimoz.beaconpack.item.BeaconPackItem;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bridge between the pure {@code core} layer and the live registries.
 *
 * <p>Both sides may call this: the four registries are network-synced, so the client resolves the
 * same numbers the server charges instead of asking for them.
 */
public final class BPLookups {

    public static PackResolver.Lookup<AugmentDef> augments(RegistryAccess access) {
        Registry<AugmentDef> registry = access.registryOrThrow(BPRegistryKeys.AUGMENT);
        return registry::getOptional;
    }

    public static PackResolver.Lookup<BeaconEffectDef> effects(RegistryAccess access) {
        Registry<BeaconEffectDef> registry = access.registryOrThrow(BPRegistryKeys.EFFECT);
        return registry::getOptional;
    }

    @Nullable
    public static PackTierDef tier(RegistryAccess access, BeaconPackItem item) {
        return access.registryOrThrow(BPRegistryKeys.TIER).get(item.tier());
    }

    /**
     * Effect keys in a stable order.
     *
     * <p>The GUI addresses effects by index in this list, because {@code clickMenuButton} can only
     * carry an int. Sorting by id is what makes client and server agree on that index without
     * inventing a second packet.
     */
    public static List<ResourceKey<BeaconEffectDef>> sortedEffectKeys(RegistryAccess access) {
        return access.registryOrThrow(BPRegistryKeys.EFFECT).registryKeySet().stream()
                .sorted(Comparator.comparing(key -> key.location().toString()))
                .toList();
    }

    /** Fuel units the given item is worth, or 0 if it is not fuel. */
    public static int fuelValue(RegistryAccess access, Item item) {
        for (FuelDef def : access.registryOrThrow(BPRegistryKeys.FUEL)) {
            if (def.item().value() == item) {
                return def.units();
            }
        }
        return 0;
    }

    /** The augments currently installed in a pack, read straight from its container component. */
    public static List<AugmentInstance> installedAugments(ItemStack packStack) {
        IItemHandler handler = packStack.getCapability(Capabilities.ItemHandler.ITEM);
        if (handler == null) {
            return List.of();
        }
        List<AugmentInstance> found = new ArrayList<>(BeaconPackItem.AUGMENT_SLOTS);
        for (int slot = 0; slot < Math.min(BeaconPackItem.AUGMENT_SLOTS, handler.getSlots()); slot++) {
            AugmentInstance instance = AugmentItem.instanceOf(handler.getStackInSlot(slot));
            if (instance != null) {
                found.add(instance);
            }
        }
        return found;
    }

    private BPLookups() {}
}
