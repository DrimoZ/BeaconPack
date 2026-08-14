package dev.drimoz.portablebeacons.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every shipped locale must carry the same keys and the same format placeholders as {@code en_us}.
 *
 * <p>Both failures are invisible until a player using that language hits them: a missing key renders
 * as the raw translation id, and a dropped or reordered {@code %s} throws at format time, inside a
 * tooltip, on their machine only.
 */
class TranslationsTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("%%|%s");

    private static final Path LANG = resourceRoot();

    private static Path resourceRoot() {
        try {
            return Path.of(TranslationsTest.class.getResource("/assets/portablebeacons/lang").toURI());
        } catch (Exception e) {
            throw new IllegalStateException("lang files not on the test classpath", e);
        }
    }

    @Test
    void everyLocaleMatchesEnglish() throws IOException {
        JsonObject english = read(LANG.resolve("en_us.json"));
        Set<String> expected = english.keySet();

        List<Path> locales;
        try (Stream<Path> stream = Files.list(LANG)) {
            locales = stream.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
        assertTrue(locales.size() > 1, "no translations beyond en_us were found");

        List<String> problems = new ArrayList<>();
        for (Path locale : locales) {
            JsonObject json = read(locale);
            String name = locale.getFileName().toString();

            for (String key : expected) {
                if (!json.has(key)) {
                    problems.add(name + " is missing " + key);
                    continue;
                }
                String mine = placeholders(json.get(key).getAsString());
                String theirs = placeholders(english.get(key).getAsString());
                if (!mine.equals(theirs)) {
                    problems.add(name + " has placeholders " + mine + " for " + key
                            + ", expected " + theirs);
                }
            }
            for (String key : json.keySet()) {
                if (!expected.contains(key)) {
                    problems.add(name + " has an extra key " + key
                            + " - either en_us is missing it, or it is a typo");
                }
            }
        }
        assertEquals(List.of(), problems, "translation files are inconsistent");
    }

    @Test
    void noTranslationIsLeftEmpty() throws IOException {
        List<String> empty = new ArrayList<>();
        try (Stream<Path> stream = Files.list(LANG)) {
            for (Path locale : stream.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonObject json = read(locale);
                for (String key : json.keySet()) {
                    if (json.get(key).getAsString().isBlank()) {
                        empty.add(locale.getFileName() + ": " + key);
                    }
                }
            }
        }
        assertFalse(!empty.isEmpty(), "blank translations: " + empty);
    }

    /** The placeholders in order, which is what actually has to survive translation. */
    private static String placeholders(String value) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder found = new StringBuilder();
        while (matcher.find()) {
            found.append(matcher.group());
        }
        return found.toString();
    }

    private static JsonObject read(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
