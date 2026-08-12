import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Generates the mod's placeholder textures.
 *
 * <p>Kept as a script rather than committed-only PNGs so the GUI background stays in sync with the
 * slot coordinates in BeaconPackMenu: change a constant there, change it here, re-run.
 *
 * <pre>java tools/GenerateTextures.java</pre>
 */
public final class GenerateTextures {

    private static final String GUI_DIR = "src/main/resources/assets/beaconpack/textures/gui";
    private static final String ITEM_DIR = "src/main/resources/assets/beaconpack/textures/item";

    // Vanilla container palette, so the panel does not clash with the player inventory below it.
    private static final int FACE = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int DARK = 0xFF555555;
    private static final int SLOT = 0xFF8B8B8B;

    private static final int WIDTH = 248;
    private static final int HEIGHT = 294;

    public static void main(String[] args) throws IOException {
        new File(GUI_DIR).mkdirs();
        new File(ITEM_DIR).mkdirs();

        writeGui();
        writeItems();
        System.out.println("Textures written.");
    }

    private static void writeGui() throws IOException {
        BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);

        panel(image, 0, 0, WIDTH, HEIGHT);

        // Effect cases: 24x24 so the level indicator has room in the corner.
        for (int i = 0; i < 3; i++) {
            recess(image, 28 + i * 30, 38, 26, 26);
        }
        // Info panel.
        recess(image, 28, 76, 192, 76);
        // Augment slots + fuel slot, matching BeaconPackMenu's coordinates minus the 1px border.
        for (int i = 0; i < 3; i++) {
            recess(image, 28 + i * 18, 172, 18, 18);
        }
        recess(image, 120, 172, 18, 18);
        // Fuel gauge.
        recess(image, 142, 174, 72, 14);

        // Player inventory + hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                recess(image, 43 + col * 18, 210 + row * 18, 18, 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            recess(image, 43 + col * 18, 268, 18, 18);
        }

        ImageIO.write(image, "PNG", new File(GUI_DIR + "/beacon_pack.png"));
    }

    private static void writeItems() throws IOException {
        int[] tierColours = {0xFF9AA7B0, 0xFF62C2E0, 0xFF6BE07F, 0xFFE0C24A};
        String[] names = {"beacon_pack_i", "beacon_pack_ii", "beacon_pack_iii", "beacon_pack_iv"};
        for (int i = 0; i < names.length; i++) {
            ImageIO.write(packIcon(tierColours[i]), "PNG", new File(ITEM_DIR + "/" + names[i] + ".png"));
        }
        // Greyscale on purpose: the augment item is tinted at render time from its registry entry.
        ImageIO.write(augmentIcon(null), "PNG", new File(ITEM_DIR + "/augment.png"));

        // One glyph per built-in augment, selected by a model override. A datapack-added augment
        // declares no model_data and falls back to the plain gem above.
        String[] glyphs = {"range", "focus", "amplification", "efficiency", "capacity", "attunement"};
        for (String glyph : glyphs) {
            ImageIO.write(augmentIcon(glyph), "PNG",
                    new File(ITEM_DIR + "/augment_" + glyph + ".png"));
        }
    }

    private static BufferedImage packIcon(int accent) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(image, 3, 4, 10, 10, 0xFF3B3B44);
        fill(image, 4, 5, 8, 8, 0xFF54545F);
        fill(image, 6, 2, 4, 3, 0xFF2A2A31);
        fill(image, 6, 7, 4, 4, accent);
        fill(image, 7, 8, 2, 2, 0xFFFFFFFF);
        return image;
    }

    /** Gem body plus an optional glyph. Everything stays greyscale so the tint does the colouring. */
    private static BufferedImage augmentIcon(String glyph) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        fill(image, 5, 2, 6, 1, 0xFF7A7A7A);
        fill(image, 4, 3, 8, 10, 0xFF7A7A7A);
        fill(image, 5, 3, 6, 9, 0xFFD8D8D8);
        fill(image, 5, 13, 6, 1, 0xFF7A7A7A);
        if (glyph == null) {
            return image;
        }

        int ink = 0xFF3A3A3A;
        switch (glyph) {
            // Outward arrow: reach.
            case "range" -> {
                fill(image, 6, 7, 5, 2, ink);
                fill(image, 9, 5, 2, 2, ink);
                fill(image, 9, 9, 2, 2, ink);
            }
            // Plus: one more effect slot.
            case "focus" -> {
                fill(image, 7, 5, 2, 6, ink);
                fill(image, 5, 7, 6, 2, ink);
            }
            // Chevron up: stronger.
            case "amplification" -> {
                fill(image, 7, 4, 2, 2, ink);
                fill(image, 5, 6, 2, 2, ink);
                fill(image, 9, 6, 2, 2, ink);
                fill(image, 7, 8, 2, 3, ink);
            }
            // Chevron down: less fuel.
            case "efficiency" -> {
                fill(image, 7, 5, 2, 3, ink);
                fill(image, 5, 8, 2, 2, ink);
                fill(image, 9, 8, 2, 2, ink);
                fill(image, 7, 10, 2, 2, ink);
            }
            // Battery bars: buffer size.
            case "capacity" -> {
                fill(image, 5, 5, 6, 2, ink);
                fill(image, 5, 8, 6, 2, ink);
                fill(image, 5, 11, 6, 1, ink);
            }
            // Two linked rings: who the aura reaches.
            case "attunement" -> {
                fill(image, 5, 6, 3, 1, ink);
                fill(image, 5, 9, 3, 1, ink);
                fill(image, 5, 7, 1, 2, ink);
                fill(image, 8, 7, 1, 2, ink);
                fill(image, 8, 6, 3, 1, ink);
                fill(image, 8, 9, 3, 1, ink);
                fill(image, 10, 7, 1, 2, ink);
            }
            default -> { }
        }
        return image;
    }

    private static void panel(BufferedImage image, int x, int y, int w, int h) {
        fill(image, x, y, w, h, FACE);
        fill(image, x, y, w, 1, LIGHT);
        fill(image, x, y, 1, h, LIGHT);
        fill(image, x, y + h - 1, w, 1, DARK);
        fill(image, x + w - 1, y, 1, h, DARK);
    }

    private static void recess(BufferedImage image, int x, int y, int w, int h) {
        fill(image, x, y, w, h, SLOT);
        fill(image, x, y, w, 1, DARK);
        fill(image, x, y, 1, h, DARK);
        fill(image, x, y + h - 1, w, 1, LIGHT);
        fill(image, x + w - 1, y, 1, h, LIGHT);
    }

    private static void fill(BufferedImage image, int x, int y, int w, int h, int argb) {
        for (int px = x; px < x + w; px++) {
            for (int py = y; py < y + h; py++) {
                if (px >= 0 && py >= 0 && px < image.getWidth() && py < image.getHeight()) {
                    image.setRGB(px, py, argb);
                }
            }
        }
    }

    private GenerateTextures() {}
}
