import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * Writes the game tests' structure template.
 *
 * <p>Vanilla's game test framework has no "no structure" option: every test names a template that
 * must exist as {@code data/<ns>/structure/<name>.nbt}. NeoForge ships no empty-template helper on
 * 1.21.1 either, so the file has to come from somewhere - and an NBT is a gzipped binary, which is
 * not something to keep as an opaque committed blob nobody can read or change.
 *
 * <p>So it is generated, like the textures. The format is small enough to write directly: a tag is
 * a type byte, a name, and a payload, and {@code DataOutputStream.writeUTF} already emits NBT's
 * length-prefixed modified UTF-8.
 *
 * <pre>java tools/GenerateTestStructure.java</pre>
 */
public final class GenerateTestStructure {

    private static final String OUT = "src/main/resources/data/beaconpack/structure";

    // Room for a carrier, a second player and a pet, well inside a tier IV aura.
    private static final int SIZE_X = 9;
    private static final int SIZE_Y = 4;
    private static final int SIZE_Z = 9;

    /** 1.21.1. A template without one is read as ancient and silently run through DataFixerUpper. */
    private static final int DATA_VERSION = 3955;

    private static final byte TAG_END = 0;
    private static final byte TAG_INT = 3;
    private static final byte TAG_STRING = 8;
    private static final byte TAG_LIST = 9;
    private static final byte TAG_COMPOUND = 10;

    private static final int STONE = 0;
    private static final int AIR = 1;

    public static void main(String[] args) throws IOException {
        new File(OUT).mkdirs();
        File file = new File(OUT + "/platform.nbt");

        try (DataOutputStream out = new DataOutputStream(
                new GZIPOutputStream(new FileOutputStream(file)))) {
            out.writeByte(TAG_COMPOUND);
            out.writeUTF("");

            intTag(out, "DataVersion", DATA_VERSION);
            size(out);
            emptyList(out, "entities");
            palette(out);
            blocks(out);

            out.writeByte(TAG_END);
        }
        System.out.println("Wrote " + file + " (" + file.length() + " bytes)");
    }

    private static void size(DataOutputStream out) throws IOException {
        out.writeByte(TAG_LIST);
        out.writeUTF("size");
        out.writeByte(TAG_INT);
        out.writeInt(3);
        out.writeInt(SIZE_X);
        out.writeInt(SIZE_Y);
        out.writeInt(SIZE_Z);
    }

    private static void palette(DataOutputStream out) throws IOException {
        out.writeByte(TAG_LIST);
        out.writeUTF("palette");
        out.writeByte(TAG_COMPOUND);
        out.writeInt(2);
        for (String name : new String[]{"minecraft:stone", "minecraft:air"}) {
            out.writeByte(TAG_STRING);
            out.writeUTF("Name");
            out.writeUTF(name);
            out.writeByte(TAG_END);
        }
    }

    /**
     * Every position is written, air included.
     *
     * <p>A template only overwrites the positions it lists, so leaving the air implicit would let
     * whatever terrain the test area happens to sit on stay standing inside the test.
     */
    private static void blocks(DataOutputStream out) throws IOException {
        out.writeByte(TAG_LIST);
        out.writeUTF("blocks");
        out.writeByte(TAG_COMPOUND);
        out.writeInt(SIZE_X * SIZE_Y * SIZE_Z);

        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    out.writeByte(TAG_LIST);
                    out.writeUTF("pos");
                    out.writeByte(TAG_INT);
                    out.writeInt(3);
                    out.writeInt(x);
                    out.writeInt(y);
                    out.writeInt(z);

                    intTag(out, "state", y == 0 ? STONE : AIR);
                    out.writeByte(TAG_END);
                }
            }
        }
    }

    private static void emptyList(DataOutputStream out, String name) throws IOException {
        out.writeByte(TAG_LIST);
        out.writeUTF(name);
        out.writeByte(TAG_END);
        out.writeInt(0);
    }

    private static void intTag(DataOutputStream out, String name, int value) throws IOException {
        out.writeByte(TAG_INT);
        out.writeUTF(name);
        out.writeInt(value);
    }

    private GenerateTestStructure() {}
}
