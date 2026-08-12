package dev.theo.beaconpack.client;

import dev.theo.beaconpack.core.BeaconEffectDef;
import dev.theo.beaconpack.core.EffectSlotConfig;
import dev.theo.beaconpack.core.PackResolver;
import dev.theo.beaconpack.core.PackState;
import dev.theo.beaconpack.core.PackStats;
import dev.theo.beaconpack.core.PackTierDef;
import dev.theo.beaconpack.item.BeaconPackItem;
import dev.theo.beaconpack.menu.BeaconPackMenu;
import dev.theo.beaconpack.net.PackActionPayload;
import dev.theo.beaconpack.registry.BPLookups;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
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
public class BeaconPackScreen extends AbstractContainerScreen<BeaconPackMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("beaconpack", "textures/gui/beacon_pack.png");
    private static final int TEXTURE_SIZE = 512;

    static final int IMAGE_W = 248;
    static final int IMAGE_H = 294;

    /** The single column everything aligns to. */
    private static final int CONTENT_LEFT = 28;
    private static final int CONTENT_RIGHT = 220;

    private static final int TOGGLE_W = 64;
    private static final int TOGGLE_H = 18;
    private static final int TOGGLE_X = CONTENT_RIGHT - TOGGLE_W;
    private static final int TOGGLE_Y = 6;

    private static final int CASE_X = CONTENT_LEFT;
    private static final int CASE_Y = 38;
    private static final int CASE_SIZE = 26;
    private static final int CASE_SPACING = 30;
    private static final int MAX_CASES = 3;

    private static final int INFO_X = CONTENT_LEFT;
    private static final int INFO_Y = 76;
    private static final int INFO_W = CONTENT_RIGHT - CONTENT_LEFT;

    private static final int BTN_H = 16;
    private static final int BTN_GAP = 3;
    private static final int ROW_X = INFO_X + 6;
    private static final int ROW_W = INFO_W - 12;
    private static final int BTN_W = (ROW_W - 2 * BTN_GAP) / 3;
    private static final int ROW_CHANGE = INFO_Y + 36;
    private static final int ROW_SETTINGS = INFO_Y + 56;

    private static final int SECTION_LABEL_Y = 160;
    private static final int SLOT_ROW_Y = 172;
    private static final int SLOT_SIZE = 18;
    private static final int AUGMENT_SLOT_X = CONTENT_LEFT;
    private static final int FUEL_SLOT_X = 120;
    private static final int GAUGE_X = 142;
    private static final int GAUGE_Y = 174;
    private static final int GAUGE_W = CONTENT_RIGHT - GAUGE_X - 6;
    private static final int GAUGE_H = 14;

    private static final int SELECTOR_W = 152;
    private static final int SEARCH_H = 22;
    private static final int ROW_H = 20;
    private static final int VISIBLE_ROWS = 6;
    private static final int FOOTER_H = 14;
    private static final int SELECTOR_H = SEARCH_H + VISIBLE_ROWS * ROW_H + FOOTER_H;

    private static final int TEXT = 0x404040;
    private static final int TEXT_DIM = 0x707070;
    private static final int PANEL = 0xFF313131;
    private static final int PANEL_EDGE = 0xFF1B1B1B;
    private static final int PANEL_HEADER = 0xFF262626;
    private static final int ROW_ALT = 0xFF383838;
    private static final int ROW_FOCUS = 0xFF3E6899;

    private int focusedCase = 0;
    private boolean selectorOpen;
    private int selectorSlot;
    private int selectorX;
    private int selectorY;
    private int scroll;
    /** Index into the filtered rows; driven by both the mouse and the arrow keys. */
    private int highlighted;
    private String search = "";

    public BeaconPackScreen(BeaconPackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = IMAGE_W;
        this.imageHeight = IMAGE_H;
        this.titleLabelX = CONTENT_LEFT;
        this.titleLabelY = 10;
        this.inventoryLabelX = 43;
        this.inventoryLabelY = 198;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight,
                TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (selectorOpen) {
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
        PackStats stats = menu.stats();

        graphics.drawString(font, Component.translatable("beaconpack.gui.effects"),
                CONTENT_LEFT, CASE_Y - 12, TEXT, false);
        graphics.drawString(font, Component.translatable("beaconpack.gui.augments"),
                CONTENT_LEFT, SECTION_LABEL_Y, TEXT, false);
        graphics.drawString(font, Component.translatable("beaconpack.gui.fuel"),
                FUEL_SLOT_X, SECTION_LABEL_Y, TEXT, false);

        drawToggle(graphics, state, localX, localY);
        drawCases(graphics, state, stats, localX, localY);
        drawSummary(graphics, state, stats);
        drawFuel(graphics, state, stats);
        drawLockedAugmentSlots(graphics, stats);

        if (selectorOpen) {
            drawSelector(graphics, localX, localY);
        } else {
            drawInfoPanel(graphics, state, stats, localX, localY);
        }
    }

    private void drawToggle(GuiGraphics graphics, PackState state, int mouseX, int mouseY) {
        boolean hovered = within(mouseX, mouseY, TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H);
        Component label = Component.translatable(
                state.active() ? "beaconpack.gui.active" : "beaconpack.gui.inactive");
        drawButton(graphics, TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H, label, hovered, state.active());
    }

    private void drawCases(GuiGraphics graphics, PackState state, PackStats stats,
                           int mouseX, int mouseY) {
        List<EffectSlotConfig> effects = state.effects();
        for (int i = 0; i < MAX_CASES; i++) {
            int x = CASE_X + i * CASE_SPACING;

            if (i >= stats.effectSlots()) {
                // Locked cases stay visible rather than hidden: the player should see what a higher
                // tier would give them.
                graphics.fill(x + 2, CASE_Y + 2, x + CASE_SIZE - 2, CASE_Y + CASE_SIZE - 2,
                        0x60000000);
                graphics.drawCenteredString(font, "?", x + CASE_SIZE / 2, CASE_Y + 9, TEXT_DIM);
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
        }
    }

    /**
     * The pack-wide figures, right-aligned to the content column.
     *
     * <p>Right-aligned rather than placed at a fixed x: a longer translation used to run under the
     * frame. No fuel units either - they are an implementation detail of the datapack format, and a
     * rate in points per second is not something a player can act on. Time is.
     */
    private void drawSummary(GuiGraphics graphics, PackState state, PackStats stats) {
        drawRightAligned(graphics, Component.translatable("beaconpack.gui.range",
                String.format(Locale.ROOT, "%.0f", stats.range())), CASE_Y + 2);
        drawRightAligned(graphics, Component.translatable("beaconpack.gui.runtime", totalRuntime()),
                CASE_Y + 14);
        drawRightAligned(graphics, Component.translatable("beaconpack.gui.slots",
                state.effects().size(), stats.effectSlots()), CASE_Y + 26);
    }

    private void drawRightAligned(GuiGraphics graphics, Component text, int y) {
        graphics.drawString(font, text, CONTENT_RIGHT - font.width(text), y, TEXT_DIM, false);
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

        graphics.drawString(font, Component.empty()
                        .append(def.effect().value().getDisplayName())
                        .append(" ")
                        .append(roman(slot.amplifier() + 1)),
                INFO_X + 8, INFO_Y + 8, TEXT, false);

        // This effect's share of the total drain, rather than a raw rate: it answers "which of my
        // effects is draining the pack" without asking the player to compare two decimals.
        double cost = PackResolver.fuelPerSecond(slot, stats, effectLookup()) * stats.fuelMultiplier();
        double total = PackResolver.fuelPerSecond(state, stats, effectLookup());
        int share = total <= 0.0 ? 0 : (int) Math.round(cost / total * 100.0);
        String reach = slot.aura().isAura()
                ? String.format(Locale.ROOT, "%.0f m", stats.range())
                : Component.translatable("beaconpack.aura.self").getString();
        graphics.drawString(font,
                Component.translatable("beaconpack.gui.share", share).getString() + "  ·  " + reach,
                INFO_X + 8, INFO_Y + 21, TEXT_DIM, false);

        // Explicit rather than "click the case again": re-clicking the case is how you focus it,
        // and overloading that click with "open the picker" made every attempt to read a second
        // effect's details pop the selector instead.
        drawButton(graphics, ROW_X, ROW_CHANGE, ROW_W, BTN_H,
                Component.translatable("beaconpack.gui.change_effect"),
                within(mouseX, mouseY, ROW_X, ROW_CHANGE, ROW_W, BTN_H), true);

        drawButton(graphics, buttonX(0), ROW_SETTINGS, BTN_W, BTN_H,
                Component.literal("< " + roman(slot.amplifier() + 1) + " >"),
                within(mouseX, mouseY, buttonX(0), ROW_SETTINGS, BTN_W, BTN_H),
                canAmplify(def, stats));
        drawButton(graphics, buttonX(1), ROW_SETTINGS, BTN_W, BTN_H,
                Component.translatable(slot.enabled()
                        ? "beaconpack.gui.active" : "beaconpack.gui.inactive"),
                within(mouseX, mouseY, buttonX(1), ROW_SETTINGS, BTN_W, BTN_H), slot.enabled());
        drawButton(graphics, buttonX(2), ROW_SETTINGS, BTN_W, BTN_H,
                Component.translatable("beaconpack.aura." + slot.aura().getSerializedName()),
                within(mouseX, mouseY, buttonX(2), ROW_SETTINGS, BTN_W, BTN_H),
                stats.allowedAuraModes().size() > 1);
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

    private void drawLockedAugmentSlots(GuiGraphics graphics, PackStats stats) {
        for (int i = stats.augmentSlots(); i < BeaconPackItem.AUGMENT_SLOTS; i++) {
            int x = AUGMENT_SLOT_X + i * SLOT_SIZE;
            graphics.fill(x + 1, SLOT_ROW_Y + 1, x + SLOT_SIZE - 1, SLOT_ROW_Y + SLOT_SIZE - 1,
                    0x80000000);
            graphics.drawCenteredString(font, "?", x + SLOT_SIZE / 2, SLOT_ROW_Y + 5, TEXT_DIM);
        }
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

        int anchorX = CASE_X + caseIndex * CASE_SPACING + CASE_SIZE;
        selectorX = Mth.clamp(anchorX, 4, IMAGE_W - SELECTOR_W - 4);
        selectorY = Mth.clamp(CASE_Y, 4, IMAGE_H - SELECTOR_H - 4);
    }

    private void drawSelector(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = selectorX;
        int y = selectorY;
        graphics.fill(x - 1, y - 1, x + SELECTOR_W + 1, y + SELECTOR_H + 1, PANEL_EDGE);
        graphics.fill(x, y, x + SELECTOR_W, y + SELECTOR_H, PANEL);
        graphics.fill(x, y, x + SELECTOR_W, y + SEARCH_H, PANEL_HEADER);

        drawSearchField(graphics, x, y);

        List<ResourceKey<BeaconEffectDef>> rows = visibleRows();
        if (rows.isEmpty()) {
            graphics.drawCenteredString(font,
                    Component.translatable("beaconpack.gui.no_results"),
                    x + SELECTOR_W / 2, y + SEARCH_H + 16, 0xFF999999);
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
            if (index == highlighted) {
                graphics.fill(x, rowY, x + SELECTOR_W, rowY + ROW_H, locked ? ROW_ALT : ROW_FOCUS);
            } else if (index % 2 == 1) {
                graphics.fill(x, rowY, x + SELECTOR_W, rowY + ROW_H, ROW_ALT);
            }

            drawEffectIcon(graphics, key, x + 4, rowY + 2);

            if (locked) {
                String tag = Component.translatable("beaconpack.gui.locked_tier",
                        roman(def.minTier())).getString();
                graphics.drawString(font, tag, x + SELECTOR_W - font.width(tag) - 5, rowY + 6,
                        0xFFAA5555, false);
                drawName(graphics, def, x, rowY, font.width(tag) + 10, 0xFF888888);
            } else {
                drawCostMeter(graphics, x + SELECTOR_W - 30, rowY + 7, def.cost() / maxCost);
                drawName(graphics, def, x, rowY, 36, 0xFFFFFFFF);
            }
        }

        drawScrollbar(graphics, x, y, rows.size());
        String count = Component.translatable("beaconpack.gui.result_count", rows.size()).getString();
        graphics.drawString(font, count, x + 5, y + SELECTOR_H - 11, 0xFF888888, false);
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

    private void drawSearchField(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 4, y + 4, x + SELECTOR_W - 4, y + SEARCH_H - 4, 0xFF1A1A1A);
        boolean empty = search.isEmpty();
        String shown = empty
                ? Component.translatable("beaconpack.gui.search").getString()
                : search;
        graphics.drawString(font, shown, x + 8, y + 8, empty ? 0xFF6A6A6A : 0xFFFFFFFF, false);
        if (!empty || (System.currentTimeMillis() / 500) % 2 == 0) {
            int caret = x + 8 + (empty ? 0 : font.width(search));
            graphics.fill(caret + 1, y + 7, caret + 2, y + 16, 0xFFCCCCCC);
        }
    }

    private void drawScrollbar(GuiGraphics graphics, int x, int y, int total) {
        if (total <= VISIBLE_ROWS) {
            return;
        }
        int trackTop = y + SEARCH_H;
        int trackHeight = VISIBLE_ROWS * ROW_H;
        int thumbHeight = Math.max(12, trackHeight * VISIBLE_ROWS / total);
        int travel = trackHeight - thumbHeight;
        int thumbTop = trackTop + travel * scroll / Math.max(1, total - VISIBLE_ROWS);
        graphics.fill(x + SELECTOR_W - 3, trackTop, x + SELECTOR_W - 1, trackTop + trackHeight,
                0xFF262626);
        graphics.fill(x + SELECTOR_W - 3, thumbTop, x + SELECTOR_W - 1, thumbTop + thumbHeight,
                0xFF7A7A7A);
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

    private void drawButton(GuiGraphics graphics, int x, int y, int w, int h,
                            Component label, boolean hovered, boolean lit) {
        int background = !lit ? 0xFF5A5A5A : hovered ? 0xFF7FA7D8 : 0xFF6E6E6E;
        graphics.fill(x, y, x + w, y + h, background);
        graphics.renderOutline(x, y, w, h, 0xFF2B2B2B);
        // Truncated defensively: a translated label that overflows used to run past the button and
        // under the frame.
        String text = font.plainSubstrByWidth(label.getString(), w - 6);
        graphics.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2,
                0xFFFFFFFF, false);
    }

    // ------------------------------------------------------------------ tooltips

    private boolean renderFuelSlotTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!(hoveredSlot instanceof SlotItemHandler handler)
                || handler.getSlotIndex() != BeaconPackItem.FUEL_SLOT
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
        PackStats stats = menu.stats();
        double perSecond = PackResolver.fuelPerSecond(menu.state(), stats, effectLookup());
        if (perSecond > 0.0) {
            lines.add(Component.translatable("beaconpack.tip.fuel_worth",
                            formatDuration((int) (perItem / perSecond)),
                            formatDuration((int) (perItem * stack.getCount() / perSecond)))
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

    /** Nothing on this screen is self-explanatory without these. */
    private List<Component> tooltipAt(int x, int y) {
        if (within(x, y, TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
            return List.of(Component.translatable("beaconpack.tip.master"));
        }
        if (within(x, y, GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H)) {
            PackStats stats = menu.stats();
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
        for (int i = menu.stats().augmentSlots(); i < BeaconPackItem.AUGMENT_SLOTS; i++) {
            if (within(x, y, AUGMENT_SLOT_X + i * SLOT_SIZE, SLOT_ROW_Y, SLOT_SIZE, SLOT_SIZE)) {
                return List.of(Component.translatable("beaconpack.tip.augment_locked"));
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
        for (int i = 0; i < MAX_CASES; i++) {
            if (!within(x, y, CASE_X + i * CASE_SPACING, CASE_Y, CASE_SIZE, CASE_SIZE)) {
                continue;
            }
            if (i >= menu.stats().effectSlots()) {
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
        if (within(x, y, TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
            send(BeaconPackMenu.ACTION_TOGGLE_ACTIVE, 0, 0);
            return true;
        }
        if (handleCaseClick(x, y, button) || handleInfoClick(x, y)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleCaseClick(int x, int y, int button) {
        for (int i = 0; i < MAX_CASES; i++) {
            if (!within(x, y, CASE_X + i * CASE_SPACING, CASE_Y, CASE_SIZE, CASE_SIZE)) {
                continue;
            }
            if (i >= menu.stats().effectSlots()) {
                return true;
            }
            focusedCase = i;
            if (button == 1) {
                send(BeaconPackMenu.ACTION_CLEAR_EFFECT, i, 0);
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
            send(BeaconPackMenu.ACTION_CYCLE_AMPLIFIER, focusedCase, 0);
            return true;
        }
        if (within(x, y, buttonX(1), ROW_SETTINGS, BTN_W, BTN_H)) {
            send(BeaconPackMenu.ACTION_TOGGLE_EFFECT, focusedCase, 0);
            return true;
        }
        if (within(x, y, buttonX(2), ROW_SETTINGS, BTN_W, BTN_H)) {
            send(BeaconPackMenu.ACTION_CYCLE_AURA, focusedCase, 0);
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
        send(BeaconPackMenu.ACTION_SET_EFFECT, selectorSlot, allKeys().indexOf(key));
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
        PacketDistributor.sendToServer(new PackActionPayload(action, slot, value));
    }

    private PackResolver.Lookup<BeaconEffectDef> effectLookup() {
        return BPLookups.effects(Minecraft.getInstance().level.registryAccess());
    }

    private List<ResourceKey<BeaconEffectDef>> allKeys() {
        return BPLookups.sortedEffectKeys(Minecraft.getInstance().level.registryAccess());
    }

    /** Filtered by the search box, but locked entries are kept so progress stays visible. */
    private List<ResourceKey<BeaconEffectDef>> visibleRows() {
        if (search.isEmpty()) {
            return allKeys();
        }
        String needle = search.toLowerCase(Locale.ROOT);
        return allKeys().stream()
                .filter(key -> effectLookup().get(key)
                        .map(def -> def.effect().value().getDisplayName().getString()
                                .toLowerCase(Locale.ROOT).contains(needle))
                        .orElse(false))
                .toList();
    }

    private String totalRuntime() {
        double perSecond = PackResolver.fuelPerSecond(menu.state(), menu.stats(), effectLookup());
        return atCurrentDraw(menu.state().fuel() + reserveUnits(), perSecond);
    }

    private static String atCurrentDraw(int units, double perSecond) {
        return perSecond <= 0.0 ? "-" : formatDuration((int) (units / perSecond));
    }

    /** Fuel units still sitting in the fuel slot, not yet drawn into the buffer. */
    private int reserveUnits() {
        ItemStack fuel = fuelSlotStack();
        if (fuel.isEmpty()) {
            return 0;
        }
        int perItem = BPLookups.fuelValue(
                Minecraft.getInstance().level.registryAccess(), fuel.getItem());
        return perItem * fuel.getCount();
    }

    private ItemStack fuelSlotStack() {
        for (var slot : menu.slots) {
            if (slot instanceof SlotItemHandler handler
                    && handler.getSlotIndex() == BeaconPackItem.FUEL_SLOT) {
                return slot.getItem();
            }
        }
        return ItemStack.EMPTY;
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

    static String formatDuration(int seconds) {
        if (seconds >= 3600) {
            return (seconds / 3600) + " h";
        }
        if (seconds >= 60) {
            return (seconds / 60) + " min";
        }
        return seconds + " s";
    }
}
