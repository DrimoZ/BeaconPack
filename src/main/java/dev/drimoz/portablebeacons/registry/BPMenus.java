package dev.drimoz.portablebeacons.registry;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.menu.PortableBeaconMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BPMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, PortableBeacons.MOD_ID);

    /** Extra data is just the pack's inventory slot index; the client reads the stack from there. */
    public static final DeferredHolder<MenuType<?>, MenuType<PortableBeaconMenu>> BEACON_PACK =
            MENUS.register("beacon",
                    () -> IMenuTypeExtension.create(PortableBeaconMenu::new));

    private BPMenus() {}
}
