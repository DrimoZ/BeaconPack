package dev.drimoz.portablebeacons.core;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fuel entry accepts an item or a tag, and exactly one of them.
 *
 * <p>The rejection cases matter more than the acceptance ones: an entry naming both or neither
 * parses into something that can never match anything, and a fuel that silently prices nothing is
 * indistinguishable from a fuel that is simply never used.
 */
class FuelDefTest {

    @Test
    void anItemEntryParses() {
        assertTrue(parse("{\"item\": \"minecraft:iron_ingot\", \"units\": 300}").result().isPresent());
    }

    @Test
    void aTagEntryParses() {
        assertTrue(parse("{\"tag\": \"c:ingots/steel\", \"units\": 450}").result().isPresent());
    }

    @Test
    void namingBothIsRejected() {
        assertTrue(parse("{\"item\": \"minecraft:iron_ingot\", \"tag\": \"c:ingots\", \"units\": 1}")
                .error().isPresent(), "an entry naming an item and a tag should not parse");
    }

    @Test
    void namingNeitherIsRejected() {
        assertTrue(parse("{\"units\": 300}").error().isPresent(),
                "an entry naming neither an item nor a tag should not parse");
    }

    @Test
    void zeroUnitsIsRejected() {
        // A fuel worth nothing would be consumed for no gain, which reads in game as the beacon
        // eating items at random.
        assertTrue(parse("{\"item\": \"minecraft:iron_ingot\", \"units\": 0}").error().isPresent());
    }

    @Test
    void anItemEntryMatchesOnlyThatItem() {
        FuelDef def = parse("{\"item\": \"minecraft:iron_ingot\", \"units\": 300}")
                .result().orElseThrow().getFirst();
        assertTrue(def.matches(net.minecraft.world.item.Items.IRON_INGOT));
        assertEquals(false, def.matches(net.minecraft.world.item.Items.GOLD_INGOT));
    }

    private static com.mojang.serialization.DataResult<com.mojang.datafixers.util.Pair<FuelDef,
            com.google.gson.JsonElement>> parse(String json) {
        return FuelDef.CODEC.decode(JsonOps.INSTANCE, JsonParser.parseString(json));
    }
}
