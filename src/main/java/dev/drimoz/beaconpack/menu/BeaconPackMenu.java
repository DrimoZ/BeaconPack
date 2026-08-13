package dev.drimoz.beaconpack.menu;

import dev.drimoz.beaconpack.BPConfig;
import dev.drimoz.beaconpack.BeaconProximity;
import dev.drimoz.beaconpack.compat.CuriosCompat;
import dev.drimoz.beaconpack.core.AuraMode;
import dev.drimoz.beaconpack.core.BeaconEffectDef;
import dev.drimoz.beaconpack.core.EffectSlotConfig;
import dev.drimoz.beaconpack.core.PackResolver;
import dev.drimoz.beaconpack.core.PackState;
import dev.drimoz.beaconpack.core.PackStats;
import dev.drimoz.beaconpack.core.PackTierDef;
import dev.drimoz.beaconpack.item.AugmentItem;
import dev.drimoz.beaconpack.item.BeaconPackItem;
import dev.drimoz.beaconpack.registry.BPLookups;
import dev.drimoz.beaconpack.registry.BPMenus;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The pack's container menu.
 *
 * <p>Effect slots are deliberately not {@link Slot}s: they hold data, not stacks, so they are drawn
 * by the screen and mutated through {@link #applyAction}. Only augments and fuel are real slots.
 */
public class BeaconPackMenu extends AbstractContainerMenu {

    public static final int ACTION_TOGGLE_ACTIVE = 0;
    public static final int ACTION_SET_EFFECT = 1;
    public static final int ACTION_CLEAR_EFFECT = 2;
    public static final int ACTION_CYCLE_AMPLIFIER = 3;
    public static final int ACTION_TOGGLE_EFFECT = 4;
    public static final int ACTION_CYCLE_AURA = 5;

    /**
     * Augment and fuel slots live in side drawers, outside the main frame.
     *
     * <p>Their positions are fixed rather than moved when a drawer opens: {@code Slot.x} and
     * {@code y} are final, and the screen hides a closed drawer's slots from both rendering and
     * hit-testing instead.
     */
    public static final int DRAWER_X = 193;
    public static final int AUGMENT_DRAWER_Y = 16;
    public static final int FUEL_DRAWER_Y = 44;

    private static final int FIRST_AUGMENT_SLOT_X = DRAWER_X + 9;
    private static final int AUGMENT_SLOT_Y = AUGMENT_DRAWER_Y + 21;
    private static final int FUEL_SLOT_X = DRAWER_X + 9;
    private static final int FUEL_SLOT_Y = FUEL_DRAWER_Y + 21;
    private static final int INVENTORY_X = 17;
    private static final int INVENTORY_Y = 173;
    private static final int HOTBAR_Y = 231;

    public static final int DRAWER_AUGMENTS = 0;
    public static final int DRAWER_FUEL = 1;
    public static final int DRAWER_NONE = 2;

    /**
     * Stands in for "the pack worn as a curio" where an inventory slot index is expected.
     *
     * <p>Negative so it can never collide with a real index. The pack's own inventory slot is
     * frozen while its menu is open, to stop the stack being moved out from under it; a worn pack
     * needs no such treatment, because it is not one of the slots this menu draws.
     */
    public static final int CURIO_SLOT = -1;

    private final Player player;
    private final int packSlotIndex;
    /**
     * Which side drawer the screen is showing, or -1 on the server where nothing is hidden.
     *
     * <p>Vanilla skips both rendering and hover for a slot whose {@link Slot#isActive()} is false,
     * which is the only supported way to hide one - the screen's {@code isHovering(Slot, ...)}
     * overload is private.
     */
    private int visibleDrawer = -1;
    /** Only used to back the augment and fuel slots; never read for state - see {@link #pack()}. */
    private final ItemStack slotBackingStack;
    /** Varies with the fuel config, so shift-clicking cannot assume a fixed boundary. */
    private final int packSlotCount;

    public BeaconPackMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readVarInt());
    }

    public BeaconPackMenu(int containerId, Inventory playerInventory, int packSlotIndex) {
        super(BPMenus.BEACON_PACK.get(), containerId);
        this.player = playerInventory.player;
        this.packSlotIndex = packSlotIndex;
        this.slotBackingStack = packSlotIndex == CURIO_SLOT
                ? CuriosCompat.findPack(playerInventory.player)
                : playerInventory.getItem(packSlotIndex);

        IItemHandler handler = new LivePackHandler();
        for (int i = 0; i < BeaconPackItem.AUGMENT_SLOTS; i++) {
            addSlot(new AugmentSlot(handler, i, FIRST_AUGMENT_SLOT_X + i * 18, AUGMENT_SLOT_Y));
        }
        // No fuel slot at all when fuel is switched off, rather than a slot that refuses
        // everything. Both sides read the same synced config, so the slot counts agree.
        if (BPConfig.fuelEnabled()) {
            addSlot(new FuelSlot(handler, BeaconPackItem.FUEL_SLOT, FUEL_SLOT_X, FUEL_SLOT_Y));
        }
        this.packSlotCount = slots.size();

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

    /**
     * Resolved from the inventory on every call rather than cached.
     *
     * <p>Syncing a slot replaces the client's {@link ItemStack} instance, so a cached reference goes
     * stale the moment the server answers - which shows up as a screen that only refreshes when it
     * is closed and reopened.
     */
    public ItemStack pack() {
        return packSlotIndex == CURIO_SLOT
                ? CuriosCompat.findPack(player)
                : player.getInventory().getItem(packSlotIndex);
    }

    public PackState state() {
        return BeaconPackItem.stateOf(pack());
    }

    /**
     * Resolved stats. Both sides compute this from the same synced registries and the same synced
     * stack, so the figure the GUI prints is the figure the ticker charges.
     */
    public PackStats stats() {
        RegistryAccess access = player.level().registryAccess();
        PackTierDef tier = tierDef();
        if (tier == null) {
            return new PackStats(0, 0, 0.0, 0, 0, 1.0,
                    java.util.EnumSet.of(AuraMode.SELF), false, false);
        }
        return PackResolver.resolve(
                tier, BPLookups.installedAugments(pack()), BPLookups.augments(access));
    }

    public PackTierDef tierDef() {
        if (!(pack().getItem() instanceof BeaconPackItem item)) {
            return null;
        }
        return BPLookups.tier(player.level().registryAccess(), item);
    }

    @Override
    public boolean stillValid(Player who) {
        // Compared against the instance captured at open time: a same-looking pack swapped into the
        // slot is not this pack. The server never replaces the instance, it mutates it in place.
        if (who != player) {
            return false;
        }
        if (packSlotIndex == CURIO_SLOT) {
            // "Still wearing a pack", not "still wearing this exact instance". The inventory case
            // compares instances to stop the stack being moved out from under an open menu, but a
            // worn pack is not one of the slots this menu draws, so there is no such move to guard
            // against - and Curios is free to hand back a different instance for the same slot.
            return !CuriosCompat.findPack(who).isEmpty();
        }
        return packSlotIndex < who.getInventory().getContainerSize()
                && who.getInventory().getItem(packSlotIndex) == slotBackingStack;
    }

    /**
     * Applies one configuration change. Called from {@code PackActionPayload} rather than
     * {@code clickMenuButton}, whose id is a single byte on the wire.
     */
    public boolean applyAction(int action, int slotIndex, int value) {
        Player who = player;
        // Both arrive from the client as var-ints, which carry negatives perfectly well. Every
        // guard below this point is an upper bound - `slotIndex < effects.size()` and friends - so
        // a negative index sailed past all of them and reached List.set/remove, throwing on the
        // server thread from a packet any modified client can send at will.
        if (slotIndex < 0 || value < 0) {
            return false;
        }
        PackStats stats = stats();
        PackTierDef tier = tierDef();
        if (tier == null) {
            return false;
        }
        if (isReconfiguration(action) && !canReconfigure()) {
            who.displayClientMessage(
                    Component.translatable("beaconpack.msg.needs_beacon"), true);
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
        BeaconPackItem.setState(pack(), PackResolver.sanitize(updated, stats, lookup, tier));
        if (action == ACTION_TOGGLE_ACTIVE) {
            // Audible confirmation, because the only other signal is a small label changing colour.
            who.level().playSound(null, who.blockPosition(),
                    state().active() ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 0.4F, 1.0F);
        }
        broadcastChanges();
        return true;
    }

    /**
     * Turning the pack or a single effect off must always work - a player caught out in the field
     * has to be able to stop the drain. Only changes to what the pack projects are gated.
     */
    private static boolean isReconfiguration(int action) {
        return action == ACTION_SET_EFFECT
                || action == ACTION_CLEAR_EFFECT
                || action == ACTION_CYCLE_AMPLIFIER
                || action == ACTION_CYCLE_AURA;
    }

    private boolean canReconfigure() {
        return !BPConfig.INSTANCE.requireBeaconToConfigure.get()
                || BeaconProximity.activeBeaconNear(player);
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
        if (def.isEmpty() || def.get().minTier() > tier.level() || !tier.allows(key)) {
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
        int packSlots = packSlotCount;

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

    /**
     * Resolves the pack's inventory on every call instead of holding a handler bound to one
     * {@link ItemStack}.
     *
     * <p>Any change to the pack makes the server resend its slot, which replaces the client's stack
     * instance - leaving a bound handler writing into an orphaned copy. The augment and fuel slots
     * would then show contents that no longer exist.
     */
    private class LivePackHandler implements IItemHandlerModifiable {

        /**
         * Modifiable, not just {@link IItemHandler}: {@code SlotItemHandler#set} casts to
         * {@link IItemHandlerModifiable} without checking, which is how the client applies the
         * server's container contents.
         */
        private IItemHandlerModifiable delegate() {
            IItemHandler handler = pack().getCapability(Capabilities.ItemHandler.ITEM);
            return handler instanceof IItemHandlerModifiable modifiable ? modifiable : null;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            IItemHandlerModifiable handler = delegate();
            if (handler != null) {
                handler.setStackInSlot(slot, stack);
            }
        }

        @Override
        public int getSlots() {
            IItemHandlerModifiable handler = delegate();
            return handler == null ? BeaconPackItem.CONTAINER_SIZE : handler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            IItemHandlerModifiable handler = delegate();
            return handler == null ? ItemStack.EMPTY : handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            IItemHandlerModifiable handler = delegate();
            return handler == null ? stack : handler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            IItemHandlerModifiable handler = delegate();
            return handler == null ? ItemStack.EMPTY : handler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            IItemHandlerModifiable handler = delegate();
            return handler == null ? 64 : handler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IItemHandlerModifiable handler = delegate();
            return handler == null || handler.isItemValid(slot, stack);
        }
    }

    public void setVisibleDrawer(int drawer) {
        this.visibleDrawer = drawer;
    }

    private boolean drawerVisible(int drawer) {
        return visibleDrawer < 0 || visibleDrawer == drawer;
    }

    /** Rejects a second augment of a type already installed, so the rule is visible, not hidden. */
    private class AugmentSlot extends SlotItemHandler {
        AugmentSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean isActive() {
            return drawerVisible(DRAWER_AUGMENTS);
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
            return BPLookups.installedAugments(pack()).stream()
                    .noneMatch(other -> other.type().equals(instance.type()));
        }
    }

    private class FuelSlot extends SlotItemHandler {
        FuelSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean isActive() {
            return drawerVisible(DRAWER_FUEL);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return BPLookups.fuelValue(player.level().registryAccess(), stack.getItem()) > 0;
        }
    }
}
