import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Generates the mod's placeholder textures.
 *
 * <p>Kept as a script rather than committed-only PNGs so the GUI background stays in sync with the
 * slot coordinates in PortableBeaconMenu: change a constant there, change it here, re-run.
 *
 * <pre>java tools/GenerateTextures.java</pre>
 */
public final class GenerateTextures {

    private static final String GUI_DIR = "src/main/resources/assets/portablebeacons/textures/gui";
    private static final String ITEM_DIR = "src/main/resources/assets/portablebeacons/textures/item";
    /** The mod list logo lives at the jar root, not under assets/. */
    private static final String ROOT_DIR = "src/main/resources";

    // Vanilla container palette, so the panel does not clash with the player inventory below it.
    private static final int FACE = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int DARK = 0xFF555555;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_SHADE = 0xFF373737;
    private static final int OUTLINE = 0xFF1B1B1B;

    private static final int WIDTH = 194;
    private static final int HEIGHT = 256;

    public static void main(String[] args) throws IOException {
        new File(GUI_DIR).mkdirs();
        new File(ITEM_DIR).mkdirs();

        writeGui();
        writeItems();
        writeLogo();
        System.out.println("Textures written.");
    }

    private static void writeGui() throws IOException {
        BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);

        panel(image, 0, 0, WIDTH, HEIGHT);

        // Effect cases: 24x24 so the level indicator has room in the corner.
        for (int i = 0; i < 3; i++) {
            recess(image, 16 + i * 30, 44, 26, 26);
        }
        // Info panel.
        recess(image, 16, 78, 162, 68);
        // Player inventory + hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                recess(image, 16 + col * 18, 172 + row * 18, 18, 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            recess(image, 16 + col * 18, 230, 18, 18);
        }

        ImageIO.write(image, "PNG", new File(GUI_DIR + "/beacon.png"));
    }

    private static void writeItems() throws IOException {
        int[] tierColours = {0xFF9AA7B0, 0xFF62C2E0, 0xFF6BE07F, 0xFFE0C24A};
        String[] names = {"beacon_i", "beacon_ii", "beacon_iii", "beacon_iv"};
        for (int i = 0; i < names.length; i++) {
            ImageIO.write(packIcon(tierColours[i], i + 1), "PNG",
                    new File(ITEM_DIR + "/" + names[i] + ".png"));
        }

        // Themed beacons share the silhouette but take a saturated core and a marked casing, so they
        // read as siblings of the numbered beacons rather than as a separate family.
        ImageIO.write(themedPackIcon(0xFFE0603A, 0xFF4A2A26), "PNG",
                new File(ITEM_DIR + "/cinder_beacon.png"));
        ImageIO.write(themedPackIcon(0xFFC48CE0, 0xFF2E2740), "PNG",
                new File(ITEM_DIR + "/void_beacon.png"));
        ImageIO.write(themedPackIcon(0xFF3FB6D8, 0xFF20404C), "PNG",
                new File(ITEM_DIR + "/tidal_beacon.png"));
        // Greyscale on purpose: the augment item is tinted at render time from its registry entry.
        ImageIO.write(augmentIcon(null), "PNG", new File(ITEM_DIR + "/augment.png"));

        // One glyph per built-in augment, selected by a model override. A datapack-added augment
        // declares no model_data and falls back to the plain gem above.
        String[] glyphs = {"range", "focus", "amplification", "efficiency", "capacity", "attunement",
                "discretion"};
        for (String glyph : glyphs) {
            ImageIO.write(augmentIcon(glyph), "PNG",
                    new File(ITEM_DIR + "/augment_" + glyph + ".png"));
        }
    }

    /**
     * The mod list logo: the tier IV icon, scaled up whole pixels onto a dark plate.
     *
     * <p>Nearest-neighbour by construction rather than by a scaling hint, and paired with
     * {@code logoBlur = false} in the mods.toml, because the interpolated version of a 16px icon
     * is mush. The plate exists because the icon is drawn for a grey slot; the mod list background
     * is dark and the outline would disappear into it.
     */
    private static void writeLogo() throws IOException {
        int scale = 6;
        int size = 128;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        fill(image, 0, 0, size, size, 0xFF23252E);
        fill(image, 0, 0, size, 2, 0xFF34384A);
        fill(image, 0, size - 2, size, 2, 0xFF15161C);

        BufferedImage icon = packIcon(0xFFE0C24A, 4);
        int origin = (size - icon.getWidth() * scale) / 2;
        for (int x = 0; x < icon.getWidth(); x++) {
            for (int y = 0; y < icon.getHeight(); y++) {
                int argb = icon.getRGB(x, y);
                if ((argb >>> 24) != 0) {
                    fill(image, origin + x * scale, origin + y * scale, scale, scale, argb);
                }
            }
        }
        ImageIO.write(image, "PNG", new File(ROOT_DIR + "/logo.png"));
    }

    /**
     * At 16x16 the silhouette carries the whole icon, so the shape is blocked out first and only
     * then shaded, with a hard outline to keep it readable against any inventory background.
     *
     * <p>The tier is shown by both colour and a count of pips: colour alone excludes anyone with a
     * colour vision deficiency, and four shades of "glowing gem" are hard to tell apart regardless.
     */
    private static BufferedImage packIcon(int accent, int tier) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int outline = 0xFF15161C;
        int bodyDark = 0xFF3A3D4C;
        int bodyLight = 0xFF565A6E;

        // Silhouette: a squat casing with a lid, outlined first.
        fill(image, 2, 3, 12, 11, outline);
        fill(image, 5, 1, 6, 2, outline);
        fill(image, 3, 4, 10, 9, bodyDark);
        fill(image, 6, 2, 4, 2, bodyLight);
        fill(image, 3, 4, 10, 1, bodyLight);
        fill(image, 3, 4, 1, 9, bodyLight);

        // Core: the one saturated area, so the eye lands there first. Bright enough to survive
        // being drawn at 16px over a grey slot.
        fill(image, 5, 5, 6, 5, outline);
        fill(image, 6, 6, 4, 3, accent);
        fill(image, 6, 6, 2, 1, 0xFFFFFFFF);

        // Tier as a pip count, readable without relying on the accent colour at all.
        for (int pip = 0; pip < tier; pip++) {
            fill(image, 4 + pip * 2, 11, 1, 1, 0xFFF2F2F2);
        }
        return image;
    }

    /** Same shape as a numbered beacon, with a themed casing and no tier pips. */
    private static BufferedImage themedPackIcon(int accent, int body) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int outline = 0xFF15161C;
        fill(image, 2, 3, 12, 11, outline);
        fill(image, 5, 1, 6, 2, outline);
        fill(image, 3, 4, 10, 9, body);
        fill(image, 6, 2, 4, 2, accent);
        fill(image, 3, 4, 10, 1, lighten(body));
        fill(image, 3, 4, 1, 9, lighten(body));

        fill(image, 5, 5, 6, 5, outline);
        fill(image, 6, 6, 4, 3, accent);
        fill(image, 6, 6, 2, 1, 0xFFFFFFFF);
        fill(image, 5, 11, 6, 1, accent);
        return image;
    }

    private static int lighten(int argb) {
        int r = Math.min(255, (argb >> 16 & 0xFF) + 28);
        int g = Math.min(255, (argb >> 8 & 0xFF) + 28);
        int b = Math.min(255, (argb & 0xFF) + 28);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /**
     * Gem body plus an optional glyph, outlined for contrast and left greyscale so the registry
     * tint does the colouring. The glyph is the identity here - two augments of similar hue must
     * still be distinguishable, so shape carries the meaning and colour only reinforces it.
     */
    private static BufferedImage augmentIcon(String glyph) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int outline = 0xFF2A2A2A;
        fill(image, 4, 2, 8, 12, outline);
        fill(image, 3, 4, 10, 8, outline);
        fill(image, 5, 3, 6, 10, 0xFFE4E4E4);
        fill(image, 4, 5, 8, 6, 0xFFE4E4E4);
        fill(image, 5, 4, 2, 2, 0xFFFFFFFF);
        fill(image, 9, 9, 2, 2, 0xFFB4B4B4);
        if (glyph == null) {
            return image;
        }

        int ink = 0xFF2F2F2F;
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
            // A closed eye: the effects are still there, they simply stop announcing themselves.
            case "discretion" -> {
                fill(image, 4, 7, 8, 1, ink);
                fill(image, 5, 8, 6, 1, ink);
                fill(image, 6, 9, 4, 1, ink);
                fill(image, 7, 5, 2, 1, ink);
                fill(image, 4, 5, 2, 1, ink);
                fill(image, 10, 5, 2, 1, ink);
            }
            default -> { }
        }
        return image;
    }

    /**
     * Two-step bevel plus an outer keyline.
     *
     * <p>A single-pixel border reads as flat and disappears against a bright world; the keyline is
     * what separates the panel from whatever is behind it.
     */
    private static void panel(BufferedImage image, int x, int y, int w, int h) {
        fill(image, x, y, w, h, OUTLINE);
        fill(image, x + 1, y + 1, w - 2, h - 2, FACE);
        fill(image, x + 1, y + 1, w - 2, 1, LIGHT);
        fill(image, x + 1, y + 1, 1, h - 2, LIGHT);
        fill(image, x + 1, y + h - 2, w - 2, 1, DARK);
        fill(image, x + w - 2, y + 1, 1, h - 2, DARK);
    }

    /** Sunken frame, shaded the opposite way to the panel so it reads as a hole, not a tile. */
    private static void recess(BufferedImage image, int x, int y, int w, int h) {
        fill(image, x, y, w, h, SLOT);
        fill(image, x, y, w, 1, SLOT_SHADE);
        fill(image, x, y, 1, h, SLOT_SHADE);
        fill(image, x, y + h - 1, w, 1, LIGHT);
        fill(image, x + w - 1, y, 1, h, LIGHT);
        // Corners left un-beveled, the way vanilla slots are, so a row of them reads as one strip.
        fill(image, x, y + h - 1, 1, 1, SLOT);
        fill(image, x + w - 1, y, 1, 1, SLOT);
    }

    /** Hairline rule separating one group of controls from the next. */
    private static void separator(BufferedImage image, int x, int y, int w) {
        fill(image, x, y, w, 1, DARK);
        fill(image, x, y + 1, w, 1, LIGHT);
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
