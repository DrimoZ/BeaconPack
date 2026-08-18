package dev.drimoz.portablebeacons.data;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Locates the mod's own resources for the tests that read them.
 *
 * <p>From the source tree rather than the classpath. These tests check the files that ship, and
 * reading them where they are written removes a dependency on how the build happens to assemble a
 * test classpath — which changed under ModDevGradle 2.0.144 and took the classpath-based lookups
 * with it.
 *
 * <p>Not the working directory either: MDG runs the unit tests from {@code build/minecraft-junit}
 * so they see a Minecraft-shaped game folder.
 */
final class ProjectFiles {

    static Path resources() {
        return root().resolve(Path.of("src", "main", "resources"));
    }

    static Path root() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve(Path.of("src", "main", "java")))
                    && Files.isRegularFile(dir.resolve("gradle.properties"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("no project root above " + Path.of("").toAbsolutePath());
    }

    private ProjectFiles() {}
}
