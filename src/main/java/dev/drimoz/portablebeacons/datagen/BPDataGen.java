package dev.drimoz.portablebeacons.datagen;

import dev.drimoz.portablebeacons.PortableBeacons;
import net.minecraft.data.advancements.AdvancementProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;

/**
 * Generates the repetitive resources.
 *
 * <p>Split in two since 1.21.4: the client run owns anything that needs assets, the server run owns
 * recipes, tags and advancements. There is no single event with an {@code includeClient()} flag any
 * more — they are separate events, and a provider registered on the wrong one simply never runs.
 *
 * <p>Deliberately not everything. The datapack registry entries stay hand-written: they are the
 * mod's content rather than boilerplate, they read better as the JSON a pack author will copy, and
 * a builtin-entries provider is a lot of machinery for twenty small files. Translations stay
 * hand-written too, since a generator only ever produces {@code en_us} and translators work on the
 * files directly.
 */
@EventBusSubscriber(modid = PortableBeacons.MOD_ID)
public final class BPDataGen {

    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent.Client event) {
        event.createProvider(BPItemModelProvider::new);
    }

    @SubscribeEvent
    public static void gatherServerData(GatherDataEvent.Server event) {
        event.createProvider(BPRecipeProvider.Runner::new);
        event.createProvider(BPItemTagProvider::new);
        event.createProvider((output, registries) ->
                new AdvancementProvider(output, registries, List.of(new BPAdvancementProvider())));
    }

    private BPDataGen() {}
}
