package dev.theo.beaconpack.menu;

import dev.theo.beaconpack.core.AuraMode;
import dev.theo.beaconpack.core.BeaconEffectDef;
import dev.theo.beaconpack.core.EffectSlotConfig;
import dev.theo.beaconpack.core.PackResolver;
import dev.theo.beaconpack.core.PackState;
import dev.theo.beaconpack.core.PackStats;
import dev.theo.beaconpack.core.PackTierDef;
import dev.theo.beaconpack.item.AugmentItem;
import dev.theo.beaconpack.item.BeaconPackItem;
import dev.theo.beaconpack.registry.BPLookups;
import dev.theo.beaconpack.registry.BPMenus;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The pack's container menu.
 *
 * <p>Effect slots are deliberately not {@link Slot}s: they hold data, not stacks, so they are drawn
 * by the screen and mutated through {@link #clickMenuButton}. Only augments and fuel are real slots.
 */
public class BeaconPackMenu extends AbstractContainerMenu {

    public static final int ACTION_TOGGLE_ACTIVE = 0;
    public static final int ACTION_SET_EFFECT = 1;
    public static final int ACTION_CLEAR_EFFECT = 2;
    public static final int ACTION_CYCLE_AMPLIFIER = 3;
    public static final int ACTION_TOGGLE_EFFECT = 4;
    public static final int ACTION_CYCLE_AURA = 5;

    private static final int FIRST_AUGMENT_SLOT_X = 35;
    private static final int SLOT_Y = 112;
    private static final int FUEL_SLOT_X = 140;
    private static final int INVENTORY_X = 34;
    private static final int INVENTORY_Y = 150;
    private static final int HOTBAR_Y = 208;

    private final Player player;
    private final int packSlotIndex;
    private final ItemStack pack;

    public BeaconPackMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readVarInt());
    }

    public BeaconPackMenu(int containerId, Inventory playerInventory, int packSlotIndex) {
        super(BPMenus.BEACON_PACK.get(), containerId);
        this.player = playerInventory.player;
        this.packSlotIndex = packSlotIndex;
        this.pack = playerInventory.getItem(packSlotIndex);

        IItemHandler handler = pack.getCapability(Capabilities.ItemHandler.ITEM);
        if (handler != null) {
            for (int i = 0; i < BeaconPackItem.AUGMENT_SLOTS; i++) {
                addSlot(new AugmentSlot(handler, i, FIRST_AUGMENT_SLOT_X + i * 18, SLOT_Y));
            }
            addSlot(new FuelSlot(handler, BeaconPackItem.FUEL_SLOT, FUEL_SLOT_X, SLOT_Y));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                addSlot(playerSlot(playerInventory, index,
                        INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(playerSlot(playerInventory, col, INVENTORY_X + col * 18, HOTBAR_Y));
        }
    }

    /**
     * The pack's own slot is present but frozen. Without this the player could move or drop the
     * stack the open menu is writing into, which duplicates its contents.
     */
    private Slot playerSlot(Inventory inventory, int index, int x, int y) {
        if (index == packSlotIndex) {
            return new Slot(inventory, index, x, y) {
                @Override
                public boolean mayPickup(Player who) {
                    return false;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            };
        }
        return new Slot(inventory, index, x, y);
    }

    public ItemStack pack() {
        return pack;
    }

    public PackState state() {
        return BeaconPackItem.stateOf(pack);
    }

    /**
     * Resolved stats. Both sides compute this from the same synced registries and the same synced
     * stack, so the figure the GUI prints is the figure the ticker charges.
     */
    public PackStats stats() {
        RegistryAccess access = player.level().registryAccess();
        PackTierDef tier = tierDef();
        if (tier == null) {
            return new PackStats(0, 0, 0.0, 0, 0, 1.0, java.util.EnumSet.of(AuraMode.SELF));
        }
        return PackResolver.resolve(
                tier, BPLookups.installedAugments(pack), BPLookups.augments(access));
    }

    public PackTierDef tierDef() {
        if (!(pack.getItem() instanceof BeaconPackItem item)) {
            return null;
        }
        return BPLookups.tier(player.level().registryAccess(), item);
    }

    @Override
    public boolean stillValid(Player who) {
        // Identity, not equality: a same-looking pack moved into the slot is not this pack.
        return who == player
                && packSlotIndex < who.getInventory().getContainerSize()
                && who.getInventory().getItem(packSlotIndex) == pack;
    }

    @Override
    public boolean clickMenuButton(Player who, int id) {
        int action = id >> 20 & 0xF;
        int slotIndex = id >> 16 & 0xF;
        int value = id & 0xFFFF;

        PackStats stats = stats();
        PackTierDef tier = tierDef();
        if (tier == null) {
            return false;
        }
        PackResolver.Lookup<BeaconEffectDef> lookup =
                BPLookups.effects(who.level().registryAccess());

        List<EffectSlotConfig> effects = new ArrayList<>(state().effects());
        PackState updated = switch (action) {
            case ACTION_TOGGLE_ACTIVE -> state().withActive(!state().active());
            case ACTION_SET_EFFECT -> state().withEffects(
                    setEffect(effects, slotIndex, value, stats, tier, lookup, who.level()
                            .registryAccess()));
            case ACTION_CLEAR_EFFECT -> {
                if (slotIndex < effects.size()) {
                    effects.remove(slotIndex);
                }
                yield state().withEffects(effects);
            }
            case ACTION_CYCLE_AMPLIFIER -> state().withEffects(
                    mutate(effects, slotIndex, slot -> cycleAmplifier(slot, stats, lookup)));
            case ACTION_TOGGLE_EFFECT -> state().withEffects(
                    mutate(effects, slotIndex, slot -> slot.withEnabled(!slot.enabled())));
            case ACTION_CYCLE_AURA -> state().withEffects(
                    mutate(effects, slotIndex, slot -> slot.withAura(nextAura(slot.aura(), stats))));
            default -> null;
        };

        if (updated == null) {
            return false;
        }
        BeaconPackItem.setState(pack, PackResolver.sanitize(updated, stats, lookup, tier.level()));
        broadcastChanges();
        return true;
    }

    private List<EffectSlotConfig> setEffect(List<EffectSlotConfig> effects, int slotIndex,
                                             int effectIndex, PackStats stats, PackTierDef tier,
                                             PackResolver.Lookup<BeaconEffectDef> lookup,
                                             RegistryAccess access) {
        if (slotIndex >= stats.effectSlots()) {
            return effects;
        }
        List<ResourceKey<BeaconEffectDef>> keys = BPLookups.sortedEffectKeys(access);
        if (effectIndex >= keys.size()) {
            return effects;
        }
        ResourceKey<BeaconEffectDef> key = keys.get(effectIndex);

        Optional<BeaconEffectDef> def = lookup.get(key);
        // Every one of these is re-checked here even though the GUI greys them out: the button id
        // arrives from the client and cannot be trusted on its own.
        if (def.isEmpty() || def.get().minTier() > tier.level()) {
            return effects;
        }
        if (effects.stream().anyMatch(slot -> slot.effect().equals(key))) {
            return effects;
        }

        EffectSlotConfig created = new EffectSlotConfig(key, 0, true, AuraMode.SELF);
        if (slotIndex < effects.size()) {
            effects.set(slotIndex, created);
        } else {
            effects.add(created);
        }
        return effects;
    }

    private static List<EffectSlotConfig> mutate(List<EffectSlotConfig> effects, int slotIndex,
                                                 java.util.function.UnaryOperator<EffectSlotConfig> op) {
        if (slotIndex < effects.size()) {
            effects.set(slotIndex, op.apply(effects.get(slotIndex)));
        }
        return effects;
    }

    private static EffectSlotConfig cycleAmplifier(EffectSlotConfig slot, PackStats stats,
                                                   PackResolver.Lookup<BeaconEffectDef> lookup) {
        int cap = lookup.get(slot.effect())
                .map(def -> Math.min(def.maxAmplifier(), stats.maxAmplifier()))
                .orElse(0);
        return slot.withAmplifier(cap <= 0 ? 0 : (slot.amplifier() + 1) % (cap + 1));
    }

    private static AuraMode nextAura(AuraMode current, PackStats stats) {
        AuraMode[] modes = AuraMode.values();
        for (int step = 1; step <= modes.length; step++) {
            AuraMode candidate = modes[(current.ordinal() + step) % modes.length];
            if (stats.allows(candidate)) {
                return candidate;
            }
        }
        return AuraMode.SELF;
    }

    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int packSlots = BeaconPackItem.CONTAINER_SIZE;

        if (index < packSlots) {
            if (!moveItemStackTo(stack, packSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, packSlots, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** Rejects a second augment of a type already installed, so the rule is visible, not hidden. */
    private class AugmentSlot extends SlotItemHandler {
        AugmentSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            var instance = AugmentItem.instanceOf(stack);
            if (instance == null) {
                return false;
            }
            if (getSlotIndex() >= stats().augmentSlots()) {
                return false;
            }
            return BPLookups.installedAugments(pack).stream()
                    .noneMatch(other -> other.type().equals(instance.type()));
        }
    }

    private class FuelSlot extends SlotItemHandler {
        FuelSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return BPLookups.fuelValue(player.level().registryAccess(), stack.getItem()) > 0;
        }
    }
}
