package dev.drimoz.portablebeacons.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every translation key named in Java has to exist in {@code en_us}.
 *
 * <p>{@link TranslationsTest} compares the locales against each other, which says nothing about
 * whether the code asks for keys any of them define. The rename to Portable Beacons shipped 44 keys
 * that no locale carried - the whole screen, every tooltip and the out-of-fuel message all rendered
 * as their raw ids - and every locale agreed with every other one the entire time.
 */
class TranslationKeyUsageTest {

    /**
     * A key literal: at least two dot-separated segments. Keys built by concatenation end at a dot
     * ({@code "portablebeacons.aura." + name}) so they never match - the prefix is real, the whole
     * key is not knowable from the source.
     */
    private static final Pattern KEY =
            Pattern.compile("\"([a-zA-Z][a-zA-Z0-9_]*(?:[.][a-zA-Z0-9_]+)+)\"");

    /**
     * Literals that look like keys but are not. Empty, and meant to stay that way.
     *
     * <p>Every dotted literal in the main sources is a translation key today, so the check needs no
     * namespace filter - which matters, because the first version of this test had one and passed
     * happily on {@code beaconpack.gui.effects}: a key under the wrong namespace is exactly what an
     * "is it one of ours?" filter waves through. Anything genuinely not a key goes here, by name,
     * with a reason.
     */
    private static final Set<String> NOT_KEYS = Set.of();

    @Test
    void everyKeyUsedInJavaIsTranslated() throws IOException {
        Path sources = ProjectFiles.root().resolve(Path.of("src", "main", "java"));

        JsonObject english = read(ProjectFiles.resources()
                .resolve(Path.of("assets", "portablebeacons", "lang", "en_us.json")));

        Set<String> missing = new TreeSet<>();
        List<Path> files;
        try (Stream<Path> stream = Files.walk(sources)) {
            files = stream.filter(p -> p.toString().endsWith(".java")).toList();
        }
        assertTrue(files.size() > 10, "found almost no sources to scan: " + files.size());

        for (Path file : files) {
            Matcher matcher = KEY.matcher(Files.readString(file));
            while (matcher.find()) {
                String key = matcher.group(1);
                if (!NOT_KEYS.contains(key) && !english.has(key)) {
                    missing.add(key + "  (" + file.getFileName() + ")");
                }
            }
        }
        assertEquals(List.of(), List.copyOf(missing), "translation keys used but never defined");
    }

    private static JsonObject read(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
