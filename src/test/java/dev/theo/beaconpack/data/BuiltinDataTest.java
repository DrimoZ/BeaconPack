package dev.theo.beaconpack.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.theo.beaconpack.core.AugmentDef;
import dev.theo.beaconpack.core.BeaconEffectDef;
import dev.theo.beaconpack.core.FuelDef;
import dev.theo.beaconpack.core.PackTierDef;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Parses every JSON the mod ships through the codec that will read it in game.
 *
 * <p>A typo in one of these files makes the entry silently vanish from its registry rather than
 * crash, which is close to impossible to diagnose from inside the game - the symptom is an empty
 * list in the GUI with nothing in the log.
 */
class BuiltinDataTest {

    /** Resolved through the classpath: the test's working directory is the run folder, not the
     *  project root, so a relative source path finds nothing. */
    private static final Path DATA = resourceRoot();

    private static Path resourceRoot() {
        try {
            return Path.of(BuiltinDataTest.class
                    .getResource("/data/beaconpack/beaconpack").toURI());
        } catch (Exception e) {
            throw new IllegalStateException("built-in data not on the test classpath", e);
        }
    }

    // No bootstrap here: ModDevGradle's junit run type already brings Minecraft up, and calling
    // SharedConstants.setVersion a second time throws.

    @Test
    void effectsParse() throws IOException {
        assertAllParse("effect", BeaconEffectDef.CODEC);
    }

    @Test
    void augmentsParse() throws IOException {
        assertAllParse("augment", AugmentDef.CODEC);
    }

    @Test
    void tiersParse() throws IOException {
        assertAllParse("tier", PackTierDef.CODEC);
    }

    @Test
    void fuelParses() throws IOException {
        assertAllParse("fuel", FuelDef.CODEC);
    }

    /**
     * Recipes are hand-written, and a malformed one is dropped at load time with only a line in the
     * log - in practice it just goes missing from the crafting book. Full validation needs registry
     * access, so this checks the part that actually breaks: well-formed JSON, and augment results
     * carrying the component that gives them their identity.
     */
    @Test
    void recipesAreWellFormed() throws IOException {
        Path recipes = DATA.getParent().resolve("recipe");
        List<Path> files;
        try (Stream<Path> stream = Files.list(recipes)) {
            files = stream.filter(path -> path.toString().endsWith(".json")).toList();
        }
        assertFalse(files.isEmpty(), "no recipes found");

        for (Path file : files) {
            try (Reader reader = Files.newBufferedReader(file)) {
                var root = JsonParser.parseReader(reader).getAsJsonObject();
                var result = root.getAsJsonObject("result");
                if (!"beaconpack:augment".equals(result.get("id").getAsString())) {
                    continue;
                }
                var component = result.getAsJsonObject("components")
                        .getAsJsonObject("beaconpack:augment");
                assertFalse(component.get("type").getAsString().isEmpty(),
                        file + ": augment result without a type");
            }
        }
    }

    private static void assertAllParse(String folder, Codec<?> codec) throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.list(DATA.resolve(folder))) {
            files = stream.filter(path -> path.toString().endsWith(".json")).toList();
        }
        assertFalse(files.isEmpty(), "no built-in entries found for " + folder);

        for (Path file : files) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonElement json = JsonParser.parseReader(reader);
                codec.parse(JsonOps.INSTANCE, json)
                        .getOrThrow(error -> new AssertionError(file + ": " + error));
            }
        }
    }
}
