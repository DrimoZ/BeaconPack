package dev.theo.beaconpack.registry;

import dev.theo.beaconpack.BeaconPack;
import dev.theo.beaconpack.menu.BeaconPackMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BPMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, BeaconPack.MOD_ID);

    /** Extra data is just the pack's inventory slot index; the client reads the stack from there. */
    public static final DeferredHolder<MenuType<?>, MenuType<BeaconPackMenu>> BEACON_PACK =
            MENUS.register("beacon_pack",
                    () -> IMenuTypeExtension.create(BeaconPackMenu::new));

    private BPMenus() {}
}
