package dev.drimoz.portablebeacons.client;

import dev.drimoz.portablebeacons.BPConfig;
import dev.drimoz.portablebeacons.core.Durations;
import dev.drimoz.portablebeacons.core.BeaconEffectDef;
import dev.drimoz.portablebeacons.core.EffectSlotConfig;
import dev.drimoz.portablebeacons.core.PackResolver;
import dev.drimoz.portablebeacons.core.PackState;
import dev.drimoz.portablebeacons.core.PackStats;
import dev.drimoz.portablebeacons.core.PackTierDef;
import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import dev.drimoz.portablebeacons.menu.PortableBeaconMenu;
import dev.drimoz.portablebeacons.net.PackActionPayload;
import dev.drimoz.portablebeacons.registry.BPLookups;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The pack's screen.
 *
 * <p>Everything is drawn by hand instead of using vanilla widgets. The effect selector is an
 * overlay, and vanilla widgets always render underneath {@code renderLabels}, so a mixed approach
 * would put the search field behind the panel it belongs to.
 *
 * <p>Layout rule: every panel spans the same {@link #CONTENT_LEFT}..{@link #CONTENT_RIGHT} column
 * and every button row is divided into equal thirds. Text is measured against the space it has
 * rather than assumed to fit — a long translation used to run under the frame.
 */
public class PortableBeaconScreen extends AbstractContainerScreen<PortableBeaconMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("beaconpack", "textures/gui/beacon_pack.png");
    private static final int TEXTURE_SIZE = 512;

    static final int IMAGE_W = 194;
    static final int IMAGE_H = 256;

    /** The single column everything aligns to. */
    private static final int CONTENT_LEFT = 16;
    private static final int CONTENT_RIGHT = 178;

    /**
     * Side tabs, the way Mekanism and Thermal arrange theirs.
     *
     * <p>Augments and fuel moved out of the main frame entirely: they are configured once and then
     * left alone, so keeping them permanently on screen crowded the panel the player actually reads.
     * Power stays a tab too - the one control that decides whether the pack runs should not compete
     * with the settings it governs.
     */
    private static final int TAB_W = 30;
    private static final int TAB_H = 28;
    /** Content clears the tab's own glyph, which stays put when the panel grows around it. */
    private static final int PANEL_TEXT_INSET = TAB_W + 6;

    /**
     * Two columns, not one.
     *
     * <p>Four tabs stacked down one edge left the window visibly heavier on that side. Split, each
     * side carries what belongs together: the two controls that govern the pack on the left, the
     * two containers you load on the right.
     */
    private static final int LEFT_TAB_X = 1;
    private static final int RIGHT_TAB_X = IMAGE_W - 1;

    private static final int POWER_TAB_Y = 16;
    private static final int STATS_TAB_Y = 50;
    private static final int AUGMENT_TAB_Y = 16;
    private static final int FUEL_TAB_Y = 50;

    /**
     * An open tab grows into its panel in place and pushes the tabs below it down, the way Thermal
     * does it, rather than a detached square appearing alongside.
     *
     * <p>This works only because one drawer is open at a time: a tab is displaced only by a panel
     * above it, and if a panel above is open then this one is shut, so every panel is always drawn
     * at its tab's resting position. That is what lets the slots inside keep fixed coordinates -
     * {@code Slot.x} is final and cannot follow a moving panel.
     */
    private static final int PANEL_W = 120;
    private static final int PANEL_H = 46;
    private static final int STATS_PANEL_H = 62;
    private static final int PANEL_GAP = 4;
    /** Short enough that the slots appearing at the end of it does not read as a lag. */
    private static final long DRAWER_ANIM_MS = 130L;

    /** One accent per tab, so the four are told apart by colour and not only by a grey glyph. */
    private static final int ACCENT_POWER = 0xFF4BC46A;
    /** Glyph colour on a shut tab: dark enough to read, quiet enough not to compete. */
    private static final int GLYPH_OFF = 0xFF4A4A4A;
    private static final int ACCENT_STATS = 0xFF5B9BD5;
    private static final int ACCENT_AUGMENTS = 0xFFB07CD8;
    private static final int ACCENT_FUEL = 0xFFE0913A;

    private static final int DRAWER_X = PortableBeaconMenu.DRAWER_X;
    private static final int AUGMENT_DRAWER_Y = PortableBeaconMenu.AUGMENT_DRAWER_Y;
    private static final int FUEL_DRAWER_Y = PortableBeaconMenu.FUEL_DRAWER_Y;
    private static final int SLOT_SIZE = 18;

    private static final int CASE_X = CONTENT_LEFT;
    private static final int CASE_Y = 44;
    private static final int CASE_SIZE = 26;
    private static final int CASE_SPACING = 30;
    /**
     * Five, now that the stats moved to a drawer and freed the whole row. The pack itself still
     * decides how many are unlocked; this is only how many the screen can lay out.
     */
    private static final int MAX_CASES = 5;

    private static final int INFO_X = CONTENT_LEFT;
    private static final int INFO_Y = 78;
    private static final int INFO_W = CONTENT_RIGHT - CONTENT_LEFT;

    private static final int BTN_H = 16;
    private static final int BTN_GAP = 3;
    private static final int ROW_X = INFO_X + 6;
    private static final int ROW_W = INFO_W - 12;
    private static final int BTN_W = (ROW_W - 2 * BTN_GAP) / 3;
    private static final int ROW_CHANGE = INFO_Y + 30;
    private static final int ROW_SETTINGS = INFO_Y + 48;

    private static final int AUGMENT_SLOT_X = DRAWER_X + 8;
    private static final int AUGMENT_SLOT_Y = AUGMENT_DRAWER_Y + 24;
    private static final int FUEL_SLOT_X = DRAWER_X + 8;
    private static final int FUEL_SLOT_Y = FUEL_DRAWER_Y + 24;
    private static final int GAUGE_X = DRAWER_X + 30;
    private static final int GAUGE_Y = FUEL_DRAWER_Y + 26;
    private static final int GAUGE_W = 70;
    private static final int GAUGE_H = 14;

    private static final int SELECTOR_W = 138;
    private static final int SEARCH_H = 20;
    private static final int ROW_H = 18;
    private static final int VISIBLE_ROWS = 5;
    private static final int FOOTER_H = 12;
    private static final int SELECTOR_H = SEARCH_H + VISIBLE_ROWS * ROW_H + FOOTER_H;
    /** Gap between the case and the popup, so the two read as related but distinct. */
    private static final int SELECTOR_OFFSET = 6;
    /** Above the item layer, which renders around z=150 and otherwise punches through the popup. */
    private static final int SELECTOR_Z = 300;
    private static final long OPEN_ANIM_MS = 110L;

    private static final int TEXT = 0x404040;
    private static final int TEXT_DIM = 0x707070;

    private int focusedCase = 0;
    private boolean selectorOpen;
    private int selectorSlot;
    private int selectorX;
    private int selectorY;
    private int scroll;
    /** Index into the filtered rows; driven by both the mouse and the arrow keys. */
    private int highlighted;
    private String search = "";
    private long openedAt;
    private List<ResourceKey<BeaconEffectDef>> allKeysCache;
    private List<ResourceKey<BeaconEffectDef>> rowsCache;
    private String rowsCacheKey;
    /** Cleared at the top of every frame; see {@link #stats()}. */
    private PackStats frameStats;

    /**
     * The pack's resolved stats, computed at most once per frame.
     *
     * <p>Each call walks the augment slots through a capability lookup and re-applies every
     * operation. Rendering asked for it around six times a frame - the labels, the drawer, the
     * cases, and several tooltip branches - which is six times more often than it can change.
     */
    private PackStats stats() {
        if (frameStats == null) {
            frameStats = menu.stats();
        }
        return frameStats;
    }

    public PortableBeaconScreen(PortableBeaconMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = IMAGE_W;
        this.imageHeight = IMAGE_H;
        this.titleLabelX = CONTENT_LEFT;
        this.titleLabelY = 10;
        this.inventoryLabelX = CONTENT_LEFT;
        this.inventoryLabelY = 160;
    }

    /** Only one drawer at a time, so the side of the screen never becomes a second panel. */
    private enum Drawer { NONE, STATS, AUGMENTS, FUEL }

    /**
     * One drawer per side, not one in total.
     *
     * <p>They open away from each other, so nothing stops both being out at once - and comparing
     * the pack's figures against the augments producing them is exactly when you want both.
     */
    private Drawer leftDrawer = Drawer.NONE;
    private Drawer rightDrawer = Drawer.AUGMENTS;

    private long leftAnimStart = Long.MIN_VALUE;
    private long rightAnimStart = Long.MIN_VALUE;
    /** What the menu was last told, so slots are only revealed once the panel has finished opening. */
    private Drawer syncedRightDrawer = Drawer.NONE;

    // ------------------------------------------------------------------ rendering

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight,
                TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        frameStats = null;
        super.render(graphics, mouseX, mouseY, partialTick);
        if (selectorOpen) {
            renderSelectorTooltip(graphics, mouseX, mouseY);
            return;
        }
        if (renderFuelSlotTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        List<Component> tooltip = tooltipAt(mouseX - leftPos, mouseY - topPos);
        if (tooltip.isEmpty()) {
            renderTooltip(graphics, mouseX, mouseY);
        } else {
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;

        PackState state = menu.state();
        PackStats stats = stats();

        graphics.drawString(font, Component.translatable("beaconpack.gui.effects"),
                CONTENT_LEFT, CASE_Y - 12, TEXT, false);

        updateSlotVisibility();
        drawTabs(graphics, state, localX, localY);
        drawDrawer(graphics, state, stats, localX, localY);
        drawCases(graphics, state, stats, localX, localY);

        if (selectorOpen) {
            drawSelector(graphics, localX, localY);
        } else {
            drawInfoPanel(graphics, state, stats, localX, localY);
        }
    }

    private void drawTabs(GuiGraphics graphics, PackState state, int mouseX, int mouseY) {
        // Left column: the two controls that govern the pack.
        boolean powerHovered = hitTab(mouseX, mouseY, false, POWER_TAB_Y);
        // Never expanded - power is a switch, not a drawer. Passing "is the pack on" as the
        // expanded flag is what drew this tab a full panel wide whenever the pack was running.
        drawTab(graphics, false, POWER_TAB_Y, TAB_H, 0.0F, powerHovered);
        int powerCx = glyphCentre(false);
        drawPowerGlyph(graphics, powerCx, POWER_TAB_Y + TAB_H / 2 - 2,
                state.active() ? ACCENT_POWER : GLYPH_OFF);
        // A lit pip rather than a whole coloured tab: the state stays legible without the control
        // shouting louder than everything else on the screen.
        graphics.fill(powerCx - 7, POWER_TAB_Y + TAB_H - 8, powerCx + 7, POWER_TAB_Y + TAB_H - 5,
                state.active() ? ACCENT_POWER : 0xFF8A8A8A);

        float leftP = progress(leftAnimStart, leftDrawer != Drawer.NONE);
        drawTab(graphics, false, STATS_TAB_Y, lerp(TAB_H, STATS_PANEL_H, leftP), leftP, hitTab(mouseX, mouseY, false, STATS_TAB_Y));
        drawTabBars(graphics, glyphCentre(false), STATS_TAB_Y + TAB_H / 2,
                leftDrawer == Drawer.STATS ? ACCENT_STATS : GLYPH_OFF);

        // Right column: the two containers you load.
        float rightP = progress(rightAnimStart, rightDrawer != Drawer.NONE);
        float augP = rightDrawer == Drawer.AUGMENTS ? rightP : 0.0F;
        drawTab(graphics, true, AUGMENT_TAB_Y, lerp(TAB_H, PANEL_H, augP), augP, hitTab(mouseX, mouseY, true, AUGMENT_TAB_Y));
        drawTabGem(graphics, glyphCentre(true), AUGMENT_TAB_Y + TAB_H / 2,
                rightDrawer == Drawer.AUGMENTS ? ACCENT_AUGMENTS : GLYPH_OFF);

        if (BPConfig.fuelEnabled()) {
            float fuelP = rightDrawer == Drawer.FUEL ? rightP : 0.0F;
            int fuelY = fuelTabY();
            drawTab(graphics, true, fuelY, lerp(TAB_H, PANEL_H, fuelP), fuelP, hitTab(mouseX, mouseY, true, fuelY));
            drawTabFlame(graphics, glyphCentre(true), fuelY + TAB_H / 2,
                    rightDrawer == Drawer.FUEL ? ACCENT_FUEL : GLYPH_OFF);
        }
    }

    /**
     * Where the fuel tab currently sits: pushed down by the augment panel above it, and following
     * that panel's animation rather than jumping once it finishes.
     */
    private int fuelTabY() {
        float augP = rightDrawer == Drawer.AUGMENTS
                ? progress(rightAnimStart, true) : 0.0F;
        return FUEL_TAB_Y + Math.round(augP * (AUGMENT_TAB_Y + PANEL_H + PANEL_GAP - FUEL_TAB_Y));
    }

    /**
     * 0 shut, 1 fully out. Eased so the panel arrives rather than stops dead.
     *
     * <p>Slots are not animated - {@code Slot.x} is final - so they are revealed only once this
     * reaches 1, which is why the panel has to finish quickly.
     */
    private static float progress(long startedAt, boolean opening) {
        if (startedAt == Long.MIN_VALUE) {
            return opening ? 1.0F : 0.0F;
        }
        float t = Mth.clamp((System.currentTimeMillis() - startedAt) / (float) DRAWER_ANIM_MS,
                0.0F, 1.0F);
        float eased = 1.0F - (1.0F - t) * (1.0F - t);
        return opening ? eased : 1.0F - eased;
    }

    private static int lerp(int from, int to, float t) {
        return from + Math.round((to - from) * t);
    }

    /** Icons keep to the closed tab's centre, so they do not slide about as the panel grows. */
    private static int glyphCentre(boolean right) {
        int width = TAB_W;
        return right ? RIGHT_TAB_X + 2 + width / 2 : LEFT_TAB_X - 2 - width / 2;
    }

    private static boolean hitTab(int mouseX, int mouseY, boolean right, int y) {
        int x = right ? RIGHT_TAB_X : LEFT_TAB_X - TAB_W;
        return within(mouseX, mouseY, x, y, TAB_W, TAB_H);
    }

    /**
     * A tab welded to the frame, which grows into its own panel when opened.
     *
     * <p>The edge against the frame carries no outline and no bevel, so tab and panel read as one
     * piece hinged on the window rather than as a square parked next to it. An open tab keeps the
     * frame's own face colour for the same reason: it is the same surface, pulled out.
     *
     * <p>The accent stripe on the outer edge is what tells the four apart at a glance; the icons
     * alone are small and all the same grey.
     */
    private void drawTab(GuiGraphics graphics, boolean right, int y, int height,
                         float openness, boolean hovered) {
        int width = lerp(TAB_W, PANEL_W, openness);
        boolean open = openness > 0.99F;
        int face = open ? 0xFFC6C6C6 : hovered ? 0xFFBDBDBD : 0xFFA8A8A8;
        // Everything is written for the right-hand column and mirrored for the left, so the two
        // cannot drift apart.
        int near = right ? RIGHT_TAB_X : LEFT_TAB_X;
        int far = right ? near + width : near - width;
        int outerLo = Math.min(near, far);
        int outerHi = Math.max(near, far);

        graphics.fill(outerLo, y - 1, outerHi + 1, y, 0xFF1B1B1B);
        graphics.fill(outerLo, y + height, outerHi + 1, y + height + 1, 0xFF1B1B1B);
        graphics.fill(right ? far : far - 1, y - 1, right ? far + 1 : far, y + height + 1,
                0xFF1B1B1B);
        graphics.fill(outerLo, y, outerHi, y + height, face);

        // Clipped outer corners, so a column of them reads as tabs and not as bricks.
        int cornerLo = right ? far - 1 : far;
        graphics.fill(cornerLo, y, cornerLo + 1, y + 1, 0xFF1B1B1B);
        graphics.fill(cornerLo, y + height - 1, cornerLo + 1, y + height, 0xFF1B1B1B);

        graphics.fill(outerLo, y + 1, outerHi - 1, y + 2, 0x40FFFFFF);
        graphics.fill(outerLo, y + height - 2, outerHi - 1, y + height - 1, 0x30000000);
    }

    /**
     * Ascending bars, drawn on a baseline so they read as a chart and not as three loose blocks.
     *
     * <p>Every glyph is a silhouette in one colour with a single darker shadow. The previous ones
     * mixed a mid grey with a near-white highlight, which at this size just looked muddy.
     */
    private static void drawTabBars(GuiGraphics graphics, int cx, int cy, int c) {
        int shadow = shade(c);
        graphics.fill(cx - 9, cy + 7, cx + 10, cy + 9, shadow);
        graphics.fill(cx - 8, cy + 1, cx - 3, cy + 7, c);
        graphics.fill(cx - 2, cy - 3, cx + 3, cy + 7, c);
        graphics.fill(cx + 4, cy - 7, cx + 9, cy + 7, c);
    }

    /** A cut gem: wide shoulders, tapered foot, with one facet picked out. */
    private static void drawTabGem(GuiGraphics graphics, int cx, int cy, int c) {
        int shadow = shade(c);
        graphics.fill(cx - 6, cy - 7, cx + 6, cy - 4, c);
        graphics.fill(cx - 8, cy - 4, cx + 8, cy + 1, c);
        graphics.fill(cx - 5, cy + 1, cx + 5, cy + 4, c);
        graphics.fill(cx - 2, cy + 4, cx + 2, cy + 7, c);
        graphics.fill(cx - 5, cy - 4, cx - 2, cy + 1, shadow);
    }

    /** A flame: narrow tip, full body, with a hollow core so it is not a solid blob. */
    private static void drawTabFlame(GuiGraphics graphics, int cx, int cy, int c) {
        int shadow = shade(c);
        graphics.fill(cx - 2, cy - 8, cx + 2, cy - 5, c);
        graphics.fill(cx - 3, cy - 5, cx + 3, cy - 2, c);
        graphics.fill(cx - 6, cy - 2, cx + 6, cy + 4, c);
        graphics.fill(cx - 4, cy + 4, cx + 4, cy + 7, c);
        graphics.fill(cx - 3, cy, cx + 3, cy + 4, shadow);
    }

    /** The same hue, darkened - one colour per glyph keeps the four consistent. */
    private static int shade(int argb) {
        int r = (argb >> 16 & 0xFF) * 55 / 100;
        int g = (argb >> 8 & 0xFF) * 55 / 100;
        int b = (argb & 0xFF) * 55 / 100;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /**
     * The open drawer, drawn to the right of the tabs.
     *
     * <p>The slots inside it sit at fixed coordinates; a closed drawer hides them from rendering and
     * from hit-testing instead of moving them, because {@code Slot.x} is final.
     */
    private void drawDrawer(GuiGraphics graphics, PackState state, PackStats stats,
                            int mouseX, int mouseY) {
        // The two sides are independent, so each is drawn on its own terms. Contents appear only
        // once the panel holding them has finished growing, or they would be drawn outside it.
        if (leftDrawer == Drawer.STATS && progress(leftAnimStart, true) > 0.99F) {
            drawStatsDrawer(graphics, state, stats);
        }
        if (rightDrawer == Drawer.NONE || (rightDrawer == Drawer.FUEL && !BPConfig.fuelEnabled())
                || progress(rightAnimStart, true) <= 0.99F) {
            return;
        }
        // The panel itself is the open tab, drawn by drawTabs; only its contents belong here.
        int y = rightDrawer == Drawer.AUGMENTS ? AUGMENT_DRAWER_Y : FUEL_DRAWER_Y;
        graphics.drawString(font, Component.translatable(rightDrawer == Drawer.AUGMENTS
                        ? "beaconpack.gui.augments" : "beaconpack.gui.fuel"),
                DRAWER_X + PANEL_TEXT_INSET, y + 6, TEXT, false);

        if (rightDrawer == Drawer.AUGMENTS) {
            for (int i = 0; i < PortableBeaconItem.AUGMENT_SLOTS; i++) {
                int x = AUGMENT_SLOT_X + i * SLOT_SIZE;
                slotFrame(graphics, x, AUGMENT_SLOT_Y);
                if (i >= stats.augmentSlots()) {
                    graphics.fill(x + 1, AUGMENT_SLOT_Y + 1, x + SLOT_SIZE - 1,
                            AUGMENT_SLOT_Y + SLOT_SIZE - 1, 0x80000000);
                    drawPadlock(graphics, x + SLOT_SIZE / 2, AUGMENT_SLOT_Y + 8, 0xFF8A8A8A);
                } else if (slotStack(i).isEmpty()) {
                    graphics.drawCenteredString(font, "+", x + SLOT_SIZE / 2, AUGMENT_SLOT_Y + 5,
                            0xFFA0A0A0);
                }
            }
        } else {
            slotFrame(graphics, FUEL_SLOT_X, FUEL_SLOT_Y);
            graphics.fill(GAUGE_X, GAUGE_Y, GAUGE_X + GAUGE_W, GAUGE_Y + GAUGE_H, 0xFF8B8B8B);
            graphics.renderOutline(GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H, 0xFF373737);
            drawFuel(graphics, state, stats);
        }
    }

    /**
     * The pack's figures, moved off the main panel.
     *
     * <p>They were three lines of small text wedged beside the effect cases, competing with them
     * for the same row. In a drawer they get labels, room to breathe, and the case row gets the
     * whole width back.
     */
    private void drawStatsDrawer(GuiGraphics graphics, PackState state, PackStats stats) {
        // Opens leftward, so its text is laid out from the panel's far edge inwards.
        int x = LEFT_TAB_X - PANEL_W + 8;
        // Stops short of the tab glyph, which sits at the panel's inner edge.
        int textW = PANEL_W - PANEL_TEXT_INSET - 8;
        graphics.drawString(font, Component.translatable("beaconpack.gui.stats"),
                x, STATS_TAB_Y + 6, TEXT, false);

        int y = STATS_TAB_Y + 20;
        for (Component line : summaryTooltip(state, stats)) {
            graphics.drawString(font, font.plainSubstrByWidth(line.getString(), textW),
                    x, y, TEXT_DIM, false);
            y += 11;
        }
    }

    /**
     * Raised panel matching the frame. Still needed by the effect picker, which floats over the
     * screen wherever its case happens to be; the drawers no longer use it, because an open tab
     * draws its own body.
     */
    private static void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y - 1, x + w + 1, y + h + 1, 0xFF1B1B1B);
        graphics.fill(x, y, x + w, y + h, 0xFFC6C6C6);
        graphics.fill(x, y, x + w - 1, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y, x + 1, y + h - 1, 0xFFFFFFFF);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
        graphics.fill(x + w - 1, y, x + w, y + h, 0xFF555555);
    }

    private static void slotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF8B8B8B);
        graphics.fill(x, y, x + SLOT_SIZE, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + SLOT_SIZE, 0xFF373737);
        graphics.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, 0xFFFFFFFF);
        graphics.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFFFFFFFF);
    }

    /** The universal power mark: a broken ring with a stroke through the gap. */
    private static void drawPowerGlyph(GuiGraphics graphics, int cx, int cy, int colour) {
        graphics.fill(cx - 1, cy - 9, cx + 2, cy - 1, colour);
        graphics.fill(cx - 7, cy - 5, cx - 4, cy + 4, colour);
        graphics.fill(cx + 4, cy - 5, cx + 7, cy + 4, colour);
        graphics.fill(cx - 6, cy + 4, cx + 6, cy + 7, colour);
        graphics.fill(cx - 7, cy - 6, cx - 3, cy - 3, colour);
        graphics.fill(cx + 3, cy - 6, cx + 7, cy - 3, colour);
    }

    private void drawCases(GuiGraphics graphics, PackState state, PackStats stats,
                           int mouseX, int mouseY) {
        List<EffectSlotConfig> effects = state.effects();
        for (int i = 0; i < visibleCases(stats); i++) {
            int x = CASE_X + i * CASE_SPACING;

            if (i >= stats.effectSlots()) {
                // Locked cases stay visible rather than hidden: the player should see what a higher
                // tier would give them.
                graphics.fill(x + 2, CASE_Y + 2, x + CASE_SIZE - 2, CASE_Y + CASE_SIZE - 2,
                        0x60000000);
                drawPadlock(graphics, x + CASE_SIZE / 2, CASE_Y + 11, 0xFF8A8A8A);
                continue;
            }
            if (i == focusedCase) {
                graphics.renderOutline(x - 1, CASE_Y - 1, CASE_SIZE + 2, CASE_SIZE + 2, 0xFFFFDD55);
            }
            if (i >= effects.size()) {
                graphics.drawCenteredString(font, "+", x + CASE_SIZE / 2, CASE_Y + 9, TEXT_DIM);
                continue;
            }

            EffectSlotConfig slot = effects.get(i);
            drawEffectIcon(graphics, slot.effect(), x + 5, CASE_Y + 5);
            if (!slot.enabled()) {
                graphics.fill(x + 2, CASE_Y + 2, x + CASE_SIZE - 2, CASE_Y + CASE_SIZE - 2,
                        0x90303030);
            }
            graphics.drawString(font, roman(slot.amplifier() + 1),
                    x + CASE_SIZE - 9, CASE_Y + CASE_SIZE - 10, 0xFFFFFF, true);
            // A shared effect looked identical to a private one, which hid the single most
            // expensive setting on the screen.
            if (slot.aura().isAura()) {
                graphics.fill(x + 3, CASE_Y + 3, x + 7, CASE_Y + 7, 0xFF6FA8DC);
                graphics.renderOutline(x + 3, CASE_Y + 3, 4, 4, 0xFF20364C);
            }
        }
    }

    /**
     * Unlocked cases plus a single locked preview.
     *
     * <p>Drawing the full five turned a tier-IV pack into two slots and three padlocks, which reads
     * as a broken screen rather than as progression.
     */
    private int visibleCases(PackStats stats) {
        return Math.min(MAX_CASES, stats.effectSlots() + 1);
    }

    /**
     * The pack-wide figures, right-aligned to the content column.
     *
     * <p>Right-aligned rather than placed at a fixed x: a longer translation used to run under the
     * frame. No fuel units either - they are an implementation detail of the datapack format, and a
     * rate in points per second is not something a player can act on. Time is.
     */
    private List<Component> summaryTooltip(PackState state, PackStats stats) {
        List<Component> lines = new ArrayList<>(3);
        lines.add(Component.translatable("beaconpack.gui.range",
                String.format(Locale.ROOT, "%.0f", stats.range())));
        if (BPConfig.fuelEnabled()) {
            lines.add(Component.translatable("beaconpack.gui.runtime", totalRuntime())
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("beaconpack.gui.slots",
                        state.effects().size(), stats.effectSlots())
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private void drawInfoPanel(GuiGraphics graphics, PackState state, PackStats stats,
                               int mouseX, int mouseY) {
        List<EffectSlotConfig> effects = state.effects();
        if (focusedCase >= effects.size()) {
            graphics.drawString(font, Component.translatable("beaconpack.gui.empty_slot"),
                    INFO_X + 8, INFO_Y + 10, TEXT_DIM, false);
            return;
        }
        EffectSlotConfig slot = effects.get(focusedCase);
        Optional<BeaconEffectDef> maybeDef = effectLookup().get(slot.effect());
        if (maybeDef.isEmpty()) {
            return;
        }
        BeaconEffectDef def = maybeDef.get();

        // The icon repeated beside the name ties the panel to the case it describes; without it,
        // nothing said which of the cases above these controls belonged to.
        drawEffectIcon(graphics, slot.effect(), INFO_X + 7, INFO_Y + 6);
        graphics.drawString(font, Component.empty()
                        .append(def.effect().value().getDisplayName())
                        .append(" ")
                        .append(roman(slot.amplifier() + 1)),
                INFO_X + 28, INFO_Y + 8, TEXT, false);

        // This effect's share of the total drain, rather than a raw rate: it answers "which of my
        // effects is draining the pack" without asking the player to compare two decimals.
        double cost = PackResolver.fuelPerSecond(slot, stats, effectLookup()) * stats.fuelMultiplier();
        double total = PackResolver.fuelPerSecond(state, stats, effectLookup());
        int share = total <= 0.0 ? 0 : (int) Math.round(cost / total * 100.0);
        // Labelled: the bare word "Self" next to a percentage read as if the two were related.
        String reach = slot.aura().isAura()
                ? String.format(Locale.ROOT, "%.0f m", stats.range())
                : Component.translatable("beaconpack.aura.self").getString();
        // Two independent facts, so they are placed independently: share from the left, reach
        // against the right edge, and the share truncated if the two would meet. Concatenating them
        // with spaces meant the pair ran past the panel as soon as either string grew - which is
        // every language whose words are longer than English's.
        String shareText = Component.translatable("beaconpack.gui.share", share).getString();
        String reachText = Component.translatable("beaconpack.gui.reach", reach).getString();
        int reachX = INFO_X + INFO_W - 6 - font.width(reachText);
        int shareX = INFO_X + 28;
        graphics.drawString(font,
                font.plainSubstrByWidth(shareText, Math.max(0, reachX - shareX - 6)),
                shareX, INFO_Y + 19, TEXT_DIM, false);
        graphics.drawString(font, reachText, reachX, INFO_Y + 19, TEXT_DIM, false);

        // Explicit rather than "click the case again": re-clicking the case is how you focus it,
        // and overloading that click with "open the picker" made every attempt to read a second
        // effect's details pop the selector instead.
        drawButton(graphics, ROW_X, ROW_CHANGE, ROW_W, BTN_H,
                Component.translatable("beaconpack.gui.change_effect"),
                within(mouseX, mouseY, ROW_X, ROW_CHANGE, ROW_W, BTN_H), true, false);

        drawButton(graphics, buttonX(0), ROW_SETTINGS, BTN_W, BTN_H,
                Component.literal("< " + roman(slot.amplifier() + 1) + " >"),
                within(mouseX, mouseY, buttonX(0), ROW_SETTINGS, BTN_W, BTN_H),
                canAmplify(def, stats), false);
        drawButton(graphics, buttonX(1), ROW_SETTINGS, BTN_W, BTN_H,
                Component.translatable(slot.enabled()
                        ? "beaconpack.gui.active" : "beaconpack.gui.inactive"),
                within(mouseX, mouseY, buttonX(1), ROW_SETTINGS, BTN_W, BTN_H), true, slot.enabled());
        drawButton(graphics, buttonX(2), ROW_SETTINGS, BTN_W, BTN_H,
                Component.translatable("beaconpack.aura." + slot.aura().getSerializedName()),
                within(mouseX, mouseY, buttonX(2), ROW_SETTINGS, BTN_W, BTN_H),
                stats.allowedAuraModes().size() > 1, slot.aura().isAura());
    }

    private static int buttonX(int index) {
        return ROW_X + index * (BTN_W + BTN_GAP);
    }

    private void drawFuel(GuiGraphics graphics, PackState state, PackStats stats) {
        int capacity = Math.max(1, stats.fuelCapacity());
        int filled = (int) ((GAUGE_W - 4) * Math.min(1.0, state.fuel() / (double) capacity));
        graphics.fill(GAUGE_X + 2, GAUGE_Y + 2, GAUGE_X + 2 + filled, GAUGE_Y + GAUGE_H - 2,
                0xFF3FA34D);

        String label = totalRuntime();
        graphics.drawString(font, label,
                GAUGE_X + (GAUGE_W - font.width(label)) / 2, GAUGE_Y + 4, 0xFFFFFFFF, true);
    }

    /**
     * Tells the menu which drawer is open.
     *
     * <p>Vanilla guards both slot rendering and hover on {@code Slot#isActive()}, and the {@code
     * isHovering(Slot, ...)} overload is private, so this is the supported way to hide a slot rather
     * than overriding the screen.
     */
    private void setLeftDrawer(Drawer next) {
        if (next != leftDrawer) {
            click();
            leftAnimStart = System.currentTimeMillis();
        }
        leftDrawer = next;
    }

    private void setRightDrawer(Drawer next) {
        if (next != rightDrawer) {
            click();
            rightAnimStart = System.currentTimeMillis();
            // Hide the slots for the whole animation, in both directions: they cannot move with the
            // panel, so showing them early leaves items floating outside it.
            syncSlots(Drawer.NONE);
        }
        rightDrawer = next;
    }

    /**
     * Reveals the open drawer's slots once its panel has finished growing.
     *
     * <p>Called every frame rather than once on click, because the reveal is driven by the
     * animation finishing, not by the click that started it.
     */
    private void updateSlotVisibility() {
        Drawer wanted = progress(rightAnimStart, rightDrawer != Drawer.NONE) > 0.99F
                ? rightDrawer
                : Drawer.NONE;
        if (wanted != syncedRightDrawer) {
            syncSlots(wanted);
        }
    }

    private void syncSlots(Drawer drawer) {
        syncedRightDrawer = drawer;
        menu.setVisibleDrawer(switch (drawer) {
            case AUGMENTS -> PortableBeaconMenu.DRAWER_AUGMENTS;
            case FUEL -> PortableBeaconMenu.DRAWER_FUEL;
            case STATS, NONE -> PortableBeaconMenu.DRAWER_NONE;
        });
    }

    @Override
    protected void init() {
        super.init();
        syncSlots(rightDrawer);
    }

    /**
     * A padlock rather than a question mark: "?" reads as unknown content, when the slot is simply
     * not unlocked yet. A lock is the universally understood shape for that.
     */
    private static void drawPadlock(GuiGraphics graphics, int cx, int cy, int colour) {
        graphics.fill(cx - 2, cy - 5, cx + 2, cy - 4, colour);
        graphics.fill(cx - 3, cy - 4, cx - 2, cy - 1, colour);
        graphics.fill(cx + 1, cy - 4, cx + 2, cy - 1, colour);
        graphics.fill(cx - 4, cy - 1, cx + 3, cy + 4, colour);
    }

    private ItemStack slotStack(int handlerIndex) {
        for (var slot : menu.slots) {
            if (slot instanceof SlotItemHandler handler
                    && handler.getSlotIndex() == handlerIndex) {
                return slot.getItem();
            }
        }
        return ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------ effect selector

    /**
     * Anchored to the top-right corner of the case it belongs to, then clamped inside the screen.
     *
     * <p>Anchoring ties the popup to what opened it instead of dropping it in the middle of the
     * panel; clamping is what keeps the rightmost case from opening a list half off the frame.
     */
    private void openSelector(int caseIndex) {
        selectorOpen = true;
        selectorSlot = caseIndex;
        scroll = 0;
        highlighted = 0;
        search = "";

        int anchorX = CASE_X + caseIndex * CASE_SPACING + CASE_SIZE + SELECTOR_OFFSET;
        selectorX = Mth.clamp(anchorX, 4, IMAGE_W - SELECTOR_W - 4);
        selectorY = Mth.clamp(CASE_Y - 2, 4, IMAGE_H - SELECTOR_H - 4);
        openedAt = System.currentTimeMillis();
    }

    /** 0 to 1 over {@link #OPEN_ANIM_MS}; the popup unrolls instead of appearing from nowhere. */
    private float openProgress() {
        long elapsed = System.currentTimeMillis() - openedAt;
        return elapsed >= OPEN_ANIM_MS ? 1.0F : elapsed / (float) OPEN_ANIM_MS;
    }

    private void drawSelector(GuiGraphics graphics, int mouseX, int mouseY) {
        // Raised above the item layer: slot contents are drawn at a higher z than renderLabels, so
        // without this the inventory's items show straight through the popup.
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, SELECTOR_Z);
        drawSelectorBody(graphics, mouseX, mouseY);
        graphics.pose().popPose();
    }

    private void drawSelectorBody(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = selectorX;
        int y = selectorY;
        float progress = openProgress();
        int height = Math.max(4, Math.round(SELECTOR_H * progress));

        // Vanilla palette, like every other surface here. A dark popup inside a light container
        // reads as a foreign object, whatever its own merits.
        panel(graphics, x, y, SELECTOR_W, height);
        if (progress < 1.0F) {
            return;
        }
        drawSearchField(graphics, x, y);
        // The list is a sunken well, the way vanilla sinks anything scrollable.
        graphics.fill(x + 4, y + SEARCH_H, x + SELECTOR_W - 4, y + SELECTOR_H - FOOTER_H, 0xFF8B8B8B);
        graphics.fill(x + 4, y + SEARCH_H, x + SELECTOR_W - 4, y + SEARCH_H + 1, 0xFF373737);
        graphics.fill(x + 4, y + SEARCH_H, x + 5, y + SELECTOR_H - FOOTER_H, 0xFF373737);

        List<ResourceKey<BeaconEffectDef>> rows = visibleRows();
        if (rows.isEmpty()) {
            graphics.drawCenteredString(font,
                    Component.translatable("beaconpack.gui.no_results"),
                    x + SELECTOR_W / 2, y + SEARCH_H + 16, 0xFF5A5A5A);
            return;
        }

        highlighted = Mth.clamp(highlighted, 0, rows.size() - 1);
        int tierLevel = tierLevel();
        double maxCost = rows.stream()
                .map(key -> effectLookup().get(key).map(BeaconEffectDef::cost).orElse(0.0))
                .max(Double::compare).orElse(1.0);

        for (int i = 0; i < VISIBLE_ROWS && i + scroll < rows.size(); i++) {
            int index = i + scroll;
            ResourceKey<BeaconEffectDef> key = rows.get(index);
            Optional<BeaconEffectDef> maybeDef = effectLookup().get(key);
            if (maybeDef.isEmpty()) {
                continue;
            }
            BeaconEffectDef def = maybeDef.get();
            int rowY = y + SEARCH_H + i * ROW_H;
            boolean locked = def.minTier() > tierLevel;

            if (within(mouseX, mouseY, x, rowY, SELECTOR_W, ROW_H)) {
                // Hover drives the same highlight the arrow keys do, so mouse and keyboard never
                // disagree about what Enter would pick.
                highlighted = index;
            }
            int listLeft = x + 5;
            int listRight = x + SELECTOR_W - 5;
            if (index == highlighted) {
                graphics.fill(listLeft, rowY, listRight, rowY + ROW_H, 0xFF7B9FD6);
            } else if (index % 2 == 1) {
                graphics.fill(listLeft, rowY, listRight, rowY + ROW_H, 0x18000000);
            }

            drawEffectIcon(graphics, key, listLeft + 2, rowY + 2);

            if (locked) {
                // A padlock and the numeral, not the sentence: "Requires tier III" ate most of the
                // row and left the effect's own name truncated to nothing.
                String tag = roman(def.minTier());
                int tagX = listRight - font.width(tag) - 4;
                graphics.drawString(font, tag, tagX, rowY + 5, 0xFF8B3A3A, false);
                drawPadlock(graphics, tagX - 8, rowY + 9, 0xFF8B3A3A);
                drawName(graphics, def, x, rowY, font.width(tag) + 20, 0xFF6E6E6E);
            } else {
                drawCostMeter(graphics, listRight - 26, rowY + 6, def.cost() / maxCost);
                drawName(graphics, def, x, rowY, 34,
                        index == highlighted ? 0xFFFFFFFF : 0xFF2B2B2B);
            }
        }

        drawScrollbar(graphics, x, y, rows.size());
        String count = Component.translatable("beaconpack.gui.result_count", rows.size()).getString();
        graphics.drawString(font, count, x + 6, y + SELECTOR_H - 10, 0xFF5A5A5A, false);
    }

    /** Truncated against whatever the right-hand column leaves, never assumed to fit. */
    private void drawName(GuiGraphics graphics, BeaconEffectDef def, int x, int rowY,
                          int reserved, int colour) {
        int available = SELECTOR_W - 24 - reserved;
        String name = font.plainSubstrByWidth(
                def.effect().value().getDisplayName().getString(), available);
        graphics.drawString(font, name, x + 24, rowY + 6, colour, false);
    }

    /**
     * Relative cost as four segments instead of a number.
     *
     * <p>The player never needs the absolute figure here - only whether this effect is cheaper than
     * that one - and a comparison is what a meter reads as at a glance.
     */
    private void drawCostMeter(GuiGraphics graphics, int x, int y, double ratio) {
        int lit = Mth.clamp((int) Math.ceil(ratio * 4), 1, 4);
        for (int i = 0; i < 4; i++) {
            int colour = i < lit ? (lit >= 4 ? 0xFFD86A5A : lit >= 3 ? 0xFFD8B45A : 0xFF6ABF6A)
                    : 0xFF555555;
            graphics.fill(x + i * 6, y - i, x + i * 6 + 4, y + 6, colour);
        }
    }

    /** A sunken field, matching how vanilla renders anything you type into. */
    private void drawSearchField(GuiGraphics graphics, int x, int y) {
        int left = x + 4;
        int right = x + SELECTOR_W - 4;
        graphics.fill(left, y + 4, right, y + SEARCH_H - 2, 0xFF8B8B8B);
        graphics.fill(left, y + 4, right, y + 5, 0xFF373737);
        graphics.fill(left, y + 4, left + 1, y + SEARCH_H - 2, 0xFF373737);
        graphics.fill(left, y + SEARCH_H - 3, right, y + SEARCH_H - 2, 0xFFFFFFFF);

        boolean empty = search.isEmpty();
        String shown = empty
                ? Component.translatable("beaconpack.gui.search").getString()
                : search;
        graphics.drawString(font, shown, left + 4, y + 7, empty ? 0xFF6E6E6E : 0xFF2B2B2B, false);
        // No caret over the placeholder: it read as a stray character appended to the hint.
        if (!empty && (System.currentTimeMillis() / 500) % 2 == 0) {
            int caret = left + 5 + font.width(search);
            graphics.fill(caret, y + 6, caret + 1, y + SEARCH_H - 4, 0xFF2B2B2B);
        }
    }

    /** Sunken track, raised thumb - the same construction as vanilla's creative-tab scrollbar. */
    private void drawScrollbar(GuiGraphics graphics, int x, int y, int total) {
        if (total <= VISIBLE_ROWS) {
            return;
        }
        int trackLeft = x + SELECTOR_W - 9;
        int trackTop = y + SEARCH_H + 1;
        int trackHeight = VISIBLE_ROWS * ROW_H - 2;
        int thumbHeight = Math.max(14, trackHeight * VISIBLE_ROWS / total);
        int travel = trackHeight - thumbHeight;
        int thumbTop = trackTop + travel * scroll / Math.max(1, total - VISIBLE_ROWS);

        graphics.fill(trackLeft, trackTop, trackLeft + 4, trackTop + trackHeight, 0xFF6E6E6E);
        graphics.fill(trackLeft, thumbTop, trackLeft + 4, thumbTop + thumbHeight, 0xFFC6C6C6);
        graphics.fill(trackLeft, thumbTop, trackLeft + 3, thumbTop + 1, 0xFFFFFFFF);
        graphics.fill(trackLeft, thumbTop + thumbHeight - 1, trackLeft + 4, thumbTop + thumbHeight,
                0xFF555555);
    }

    private void drawEffectIcon(GuiGraphics graphics, ResourceKey<BeaconEffectDef> key, int x, int y) {
        effectLookup().get(key).ifPresent(def -> {
            // Straight from the vanilla effect atlas, so any registered effect - vanilla, another
            // mod's, or one added by a datapack - shows its own icon with no texture from us.
            TextureAtlasSprite sprite =
                    Minecraft.getInstance().getMobEffectTextures().get(def.effect());
            graphics.blit(x, y, 0, 16, 16, sprite);
        });
    }

    /**
     * Draws a button with <em>availability</em> and <em>state</em> as two separate inputs.
     *
     * <p>They used to share one flag, so a greyed-out button meant "you cannot press this" on one
     * control and "this setting is off" on the next. Colour now means state, and only a dimmed,
     * unhoverable face means unavailable.
     */
    private void drawButton(GuiGraphics graphics, int x, int y, int w, int h,
                            Component label, boolean hovered, boolean available, boolean on) {
        int background;
        int textColour = 0xFFFFFFFF;
        if (!available) {
            background = 0xFF4C4C4C;
            textColour = 0xFF9A9A9A;
        } else if (on) {
            background = hovered ? 0xFF57A268 : 0xFF3E7A4B;
        } else {
            background = hovered ? 0xFF8797AC : 0xFF6E6E6E;
        }
        graphics.fill(x, y, x + w, y + h, background);
        graphics.renderOutline(x, y, w, h, 0xFF2B2B2B);
        // Highlight along the top edge so the control reads as raised, i.e. as pressable.
        if (available) {
            graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0x33FFFFFF);
        }
        // Truncated defensively: a translated label that overflows used to run past the button and
        // under the frame.
        String text = font.plainSubstrByWidth(label.getString(), w - 6);
        graphics.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2,
                textColour, false);
    }

    // ------------------------------------------------------------------ tooltips

    private boolean renderFuelSlotTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!(hoveredSlot instanceof SlotItemHandler handler)
                || handler.getSlotIndex() != PortableBeaconItem.FUEL_SLOT
                || !hoveredSlot.hasItem()) {
            return false;
        }
        ItemStack stack = hoveredSlot.getItem();
        int perItem = BPLookups.fuelValue(
                Minecraft.getInstance().level.registryAccess(), stack.getItem());
        if (perItem <= 0) {
            return false;
        }

        List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(stack));
        PackStats stats = stats();
        double perSecond = PackResolver.fuelPerSecond(menu.state(), stats, effectLookup());
        if (perSecond > 0.0) {
            lines.add(Component.translatable("beaconpack.tip.fuel_worth",
                            Durations.format((int) (perItem / perSecond)),
                            Durations.format((int) (perItem * stack.getCount() / perSecond)))
                    .withStyle(ChatFormatting.GRAY));
        }
        // A denser fuel than the buffer can hold is never consumed, and that would otherwise look
        // like the pack ignoring it for no reason.
        if (perItem > stats.fuelCapacity()) {
            lines.add(Component.translatable("beaconpack.tip.fuel_too_dense")
                    .withStyle(ChatFormatting.RED));
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        return true;
    }

    /**
     * Explains the four-segment meter on the hovered row.
     *
     * <p>The meter compares effects at a glance, but nothing on screen said what it measured - a
     * row of bars with no legend is a puzzle, not information.
     */
    private void renderSelectorTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        List<ResourceKey<BeaconEffectDef>> rows = visibleRows();
        for (int i = 0; i < VISIBLE_ROWS && i + scroll < rows.size(); i++) {
            int rowY = selectorY + SEARCH_H + i * ROW_H;
            if (!within(x, y, selectorX, rowY, SELECTOR_W, ROW_H)) {
                continue;
            }
            effectLookup().get(rows.get(i + scroll)).ifPresent(def -> {
                List<Component> lines = new ArrayList<>(3);
                lines.add(def.effect().value().getDisplayName());
                if (def.minTier() > tierLevel()) {
                    lines.add(Component.translatable("beaconpack.gui.locked_tier",
                            roman(def.minTier())).withStyle(ChatFormatting.RED));
                } else {
                    lines.add(Component.translatable("beaconpack.tip.cost_meter",
                                    Component.translatable("beaconpack.cost." + costBand(def)))
                            .withStyle(ChatFormatting.GRAY));
                }
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            });
            return;
        }
    }

    private String costBand(BeaconEffectDef def) {
        double max = visibleRows().stream()
                .map(key -> effectLookup().get(key).map(BeaconEffectDef::cost).orElse(0.0))
                .max(Double::compare).orElse(1.0);
        return switch (Mth.clamp((int) Math.ceil(def.cost() / max * 4), 1, 4)) {
            case 1 -> "very_low";
            case 2 -> "low";
            case 3 -> "moderate";
            default -> "high";
        };
    }

    /** Nothing on this screen is self-explanatory without these. */
    private List<Component> tooltipAt(int x, int y) {
        if (hitTab(x, y, false, POWER_TAB_Y)) {
            return List.of(
                    Component.translatable(menu.state().active()
                            ? "beaconpack.gui.active" : "beaconpack.gui.inactive"),
                    Component.translatable("beaconpack.tip.master")
                            .withStyle(ChatFormatting.GRAY));
        }
        if (hitTab(x, y, false, STATS_TAB_Y)) {
            return List.of(Component.translatable("beaconpack.gui.stats"));
        }
        if (hitTab(x, y, true, AUGMENT_TAB_Y)) {
            return List.of(Component.translatable("beaconpack.gui.augments"));
        }
        if (BPConfig.fuelEnabled() && hitTab(x, y, true, fuelTabY())) {
            return List.of(Component.translatable("beaconpack.gui.fuel"));
        }
        if (BPConfig.fuelEnabled() && rightDrawer == Drawer.FUEL
                && within(x, y, GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H)) {
            PackStats stats = stats();
            double perSecond = PackResolver.fuelPerSecond(menu.state(), stats, effectLookup());
            return List.of(
                    Component.translatable("beaconpack.gui.fuel"),
                    Component.translatable("beaconpack.tip.fuel_stored",
                            atCurrentDraw(menu.state().fuel(), perSecond))
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("beaconpack.tip.fuel_reserve",
                            atCurrentDraw(reserveUnits(), perSecond))
                            .withStyle(ChatFormatting.GRAY));
        }
        List<Component> caseTip = caseTooltip(x, y);
        if (!caseTip.isEmpty()) {
            return caseTip;
        }
        if (rightDrawer == Drawer.AUGMENTS) {
            for (int i = stats().augmentSlots(); i < PortableBeaconItem.AUGMENT_SLOTS; i++) {
                if (within(x, y, AUGMENT_SLOT_X + i * SLOT_SIZE, AUGMENT_SLOT_Y,
                        SLOT_SIZE, SLOT_SIZE)) {
                    return List.of(Component.translatable("beaconpack.tip.augment_locked"));
                }
            }
        }
        // Guarded on the panel actually having buttons: they are only drawn for a focused case
        // holding an effect, and a tooltip over blank panel space was pure noise.
        if (focusedCase >= menu.state().effects().size()) {
            return List.of();
        }
        if (within(x, y, ROW_X, ROW_CHANGE, ROW_W, BTN_H)) {
            return List.of(Component.translatable("beaconpack.tip.change_effect"));
        }
        if (within(x, y, buttonX(0), ROW_SETTINGS, BTN_W, BTN_H)) {
            return List.of(Component.translatable("beaconpack.tip.level"));
        }
        if (within(x, y, buttonX(1), ROW_SETTINGS, BTN_W, BTN_H)) {
            return List.of(Component.translatable("beaconpack.tip.effect_toggle"));
        }
        if (within(x, y, buttonX(2), ROW_SETTINGS, BTN_W, BTN_H)) {
            return List.of(Component.translatable("beaconpack.tip.aura"));
        }
        return List.of();
    }

    private List<Component> caseTooltip(int x, int y) {
        for (int i = 0; i < visibleCases(stats()); i++) {
            if (!within(x, y, CASE_X + i * CASE_SPACING, CASE_Y, CASE_SIZE, CASE_SIZE)) {
                continue;
            }
            if (i >= stats().effectSlots()) {
                return List.of(Component.translatable("beaconpack.tip.case_locked"));
            }
            List<EffectSlotConfig> effects = menu.state().effects();
            if (i >= effects.size()) {
                return List.of(Component.translatable("beaconpack.gui.empty_slot"));
            }
            EffectSlotConfig slot = effects.get(i);
            return effectLookup().get(slot.effect())
                    .<List<Component>>map(def -> List.of(
                            def.effect().value().getDisplayName(),
                            Component.translatable("beaconpack.tip.case_clear")))
                    .orElse(List.of());
        }
        return List.of();
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;

        if (selectorOpen) {
            handleSelectorClick(x, y);
            return true;
        }
        if (hitTab(x, y, false, POWER_TAB_Y)) {
            send(PortableBeaconMenu.ACTION_TOGGLE_ACTIVE, 0, 0);
            return true;
        }
        if (hitTab(x, y, false, STATS_TAB_Y)) {
            setLeftDrawer(leftDrawer == Drawer.STATS ? Drawer.NONE : Drawer.STATS);
            return true;
        }
        if (hitTab(x, y, true, AUGMENT_TAB_Y)) {
            setRightDrawer(rightDrawer == Drawer.AUGMENTS ? Drawer.NONE : Drawer.AUGMENTS);
            return true;
        }
        // Against where the tab is now, not where it rests: the augment panel above pushes it down.
        if (BPConfig.fuelEnabled() && hitTab(x, y, true, fuelTabY())) {
            setRightDrawer(rightDrawer == Drawer.FUEL ? Drawer.NONE : Drawer.FUEL);
            return true;
        }
        if (handleCaseClick(x, y, button) || handleInfoClick(x, y)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleCaseClick(int x, int y, int button) {
        for (int i = 0; i < visibleCases(stats()); i++) {
            if (!within(x, y, CASE_X + i * CASE_SPACING, CASE_Y, CASE_SIZE, CASE_SIZE)) {
                continue;
            }
            if (i >= stats().effectSlots()) {
                return true;
            }
            focusedCase = i;
            if (button == 1) {
                send(PortableBeaconMenu.ACTION_CLEAR_EFFECT, i, 0);
            } else if (i >= menu.state().effects().size()) {
                // An empty case has nothing to inspect, so clicking it goes straight to the picker.
                // A filled one only takes focus, and is changed from the info panel below.
                openSelector(i);
            }
            return true;
        }
        return false;
    }

    private boolean handleInfoClick(int x, int y) {
        if (focusedCase >= menu.state().effects().size()) {
            return false;
        }
        if (within(x, y, ROW_X, ROW_CHANGE, ROW_W, BTN_H)) {
            openSelector(focusedCase);
            return true;
        }
        if (within(x, y, buttonX(0), ROW_SETTINGS, BTN_W, BTN_H)) {
            send(PortableBeaconMenu.ACTION_CYCLE_AMPLIFIER, focusedCase, 0);
            return true;
        }
        if (within(x, y, buttonX(1), ROW_SETTINGS, BTN_W, BTN_H)) {
            send(PortableBeaconMenu.ACTION_TOGGLE_EFFECT, focusedCase, 0);
            return true;
        }
        if (within(x, y, buttonX(2), ROW_SETTINGS, BTN_W, BTN_H)) {
            send(PortableBeaconMenu.ACTION_CYCLE_AURA, focusedCase, 0);
            return true;
        }
        return false;
    }

    private void handleSelectorClick(int x, int y) {
        if (!within(x, y, selectorX, selectorY, SELECTOR_W, SELECTOR_H)) {
            selectorOpen = false;
            return;
        }
        List<ResourceKey<BeaconEffectDef>> rows = visibleRows();
        for (int i = 0; i < VISIBLE_ROWS && i + scroll < rows.size(); i++) {
            if (within(x, y, selectorX, selectorY + SEARCH_H + i * ROW_H, SELECTOR_W, ROW_H)) {
                confirm(rows, i + scroll);
                return;
            }
        }
    }

    private void confirm(List<ResourceKey<BeaconEffectDef>> rows, int index) {
        if (index < 0 || index >= rows.size()) {
            return;
        }
        ResourceKey<BeaconEffectDef> key = rows.get(index);
        if (effectLookup().get(key).map(def -> def.minTier() > tierLevel()).orElse(true)) {
            return;
        }
        send(PortableBeaconMenu.ACTION_SET_EFFECT, selectorSlot, allKeys().indexOf(key));
        focusedCase = selectorSlot;
        selectorOpen = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (selectorOpen) {
            scroll = Mth.clamp(scroll - (int) Math.signum(deltaY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (selectorOpen && search.length() < 24) {
            search += codePoint;
            scroll = 0;
            highlighted = 0;
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!selectorOpen) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> selectorOpen = false;
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!search.isEmpty()) {
                    search = search.substring(0, search.length() - 1);
                    scroll = 0;
                    highlighted = 0;
                }
            }
            case GLFW.GLFW_KEY_DOWN -> moveHighlight(1);
            case GLFW.GLFW_KEY_UP -> moveHighlight(-1);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> confirm(visibleRows(), highlighted);
            default -> {
                // Everything else is swallowed so the inventory key does not close the whole screen
                // in the middle of typing a search.
            }
        }
        return true;
    }

    /** Keeps the highlighted row on screen, which is what makes arrow keys usable at all. */
    private void moveHighlight(int delta) {
        int size = visibleRows().size();
        if (size == 0) {
            return;
        }
        highlighted = Math.floorMod(highlighted + delta, size);
        if (highlighted < scroll) {
            scroll = highlighted;
        } else if (highlighted >= scroll + VISIBLE_ROWS) {
            scroll = highlighted - VISIBLE_ROWS + 1;
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private int maxScroll() {
        return Math.max(0, visibleRows().size() - VISIBLE_ROWS);
    }

    // ------------------------------------------------------------------ helpers

    private void send(int action, int slot, int value) {
        click();
        PacketDistributor.sendToServer(new PackActionPayload(action, slot, value));
    }

    /**
     * The click every vanilla button makes.
     *
     * <p>Hand-drawn controls get no audio for free, and a button that changes colour but makes no
     * sound reads as not having registered the press.
     */
    private static void click() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private PackResolver.Lookup<BeaconEffectDef> effectLookup() {
        return BPLookups.effects(Minecraft.getInstance().level.registryAccess());
    }

    /**
     * The whole registry, in the order both sides agree on.
     *
     * <p>This is what the wire index refers to, so it must never be filtered - the server resolves
     * the index against the same unfiltered list. Cached because the registry cannot change while
     * the screen is open, and building it means a stream and a sort.
     */
    private List<ResourceKey<BeaconEffectDef>> allKeys() {
        if (allKeysCache == null) {
            allKeysCache = BPLookups.sortedEffectKeys(Minecraft.getInstance().level.registryAccess());
        }
        return allKeysCache;
    }

    /**
     * The rows the picker shows: this pack's pool, narrowed by the search box.
     *
     * <p>A themed pack listing the standard beacon effects it will never accept would be a list of
     * dead ends, so the pool filters the picker rather than greying rows out. Locked entries are
     * kept, though - those are progress, not dead ends.
     *
     * <p>Recomputed only when the search text changes. Drawing one frame of the picker asks for
     * this list several times, and it used to re-sort the registry on every one of them.
     */
    private List<ResourceKey<BeaconEffectDef>> visibleRows() {
        if (rowsCache != null && search.equals(rowsCacheKey)) {
            return rowsCache;
        }
        PackTierDef tier = menu.tierDef();
        String needle = search.toLowerCase(Locale.ROOT);
        rowsCache = allKeys().stream()
                .filter(key -> tier == null || tier.allows(key))
                .filter(key -> needle.isEmpty() || effectLookup().get(key)
                        .map(def -> def.effect().value().getDisplayName().getString()
                                .toLowerCase(Locale.ROOT).contains(needle))
                        .orElse(false))
                .toList();
        rowsCacheKey = search;
        return rowsCache;
    }

    private String totalRuntime() {
        double perSecond = PackResolver.fuelPerSecond(menu.state(), stats(), effectLookup());
        return atCurrentDraw(menu.state().fuel() + reserveUnits(), perSecond);
    }

    /**
     * "Idle" rather than a dash when nothing is drawing.
     *
     * <p>A lone "-" reads as missing data or a bug; naming the state says the pack is fine and
     * simply has nothing running.
     */
    private static String atCurrentDraw(int units, double perSecond) {
        return perSecond <= 0.0
                ? Component.translatable("beaconpack.gui.idle").getString()
                : Durations.format((int) (units / perSecond));
    }

    /** Fuel units still sitting in the fuel slot, not yet drawn into the buffer. */
    private int reserveUnits() {
        ItemStack fuel = slotStack(PortableBeaconItem.FUEL_SLOT);
        if (fuel.isEmpty()) {
            return 0;
        }
        int perItem = BPLookups.fuelValue(
                Minecraft.getInstance().level.registryAccess(), fuel.getItem());
        return perItem * fuel.getCount();
    }

    private int tierLevel() {
        PackTierDef tier = menu.tierDef();
        return tier == null ? 1 : tier.level();
    }

    private static boolean canAmplify(BeaconEffectDef def, PackStats stats) {
        return Math.min(def.maxAmplifier(), stats.maxAmplifier()) > 0;
    }

    private static boolean within(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    static String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> String.valueOf(value);
        };
    }

}
