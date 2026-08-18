package dev.drimoz.portablebeacons.datagen;

import dev.drimoz.portablebeacons.PortableBeacons;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Generates the repetitive resources.
 *
 * <p>Deliberately not everything. The datapack registry entries stay hand-written: they are the
 * mod's content rather than boilerplate, they read better as the JSON a beacon author will copy, and
 * a builtin-entries provider is a lot of machinery for twenty small files. Translations stay
 * hand-written too, since a generator only ever produces {@code en_us} and translators work on the
 * files directly.
 */
@EventBusSubscriber(modid = PortableBeacons.MOD_ID)
public final class BPDataGen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> registries = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new BPItemModelProvider(output, helper));
        generator.addProvider(event.includeServer(), new BPRecipeProvider(output, registries));
        generator.addProvider(event.includeServer(),
                new BPItemTagProvider(output, registries, helper));
        generator.addProvider(event.includeServer(), new net.neoforged.neoforge.common.data
                .AdvancementProvider(output, registries, helper,
                java.util.List.of(new BPAdvancementProvider())));
    }

    private BPDataGen() {}
}
