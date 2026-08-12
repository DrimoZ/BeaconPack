package dev.theo.beaconpack.client;

import dev.theo.beaconpack.core.BeaconEffectDef;
import dev.theo.beaconpack.core.EffectSlotConfig;
import dev.theo.beaconpack.core.PackResolver;
import dev.theo.beaconpack.core.PackState;
import dev.theo.beaconpack.core.PackStats;
import dev.theo.beaconpack.core.PackTierDef;
import dev.theo.beaconpack.menu.BeaconPackMenu;
import dev.theo.beaconpack.net.PackActionPayload;
import dev.theo.beaconpack.registry.BPLookups;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The pack's screen.
 *
 * <p>Everything is drawn by hand instead of using vanilla widgets. The effect selector is an
 * overlay, and vanilla widgets always render underneath {@code renderLabels}, so a mixed approach
 * would put the search field behind the panel it belongs to.
 */
public class BeaconPackScreen extends AbstractContainerScreen<BeaconPackMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("beaconpack", "textures/gui/beacon_pack.png");

    private static final int CASE_X = 35;
    private static final int CASE_Y = 28;
    private static final int CASE_SIZE = 24;
    private static final int CASE_SPACING = 28;
    private static final int MAX_CASES = 3;

    private static final int INFO_X = 30;
    private static final int INFO_Y = 58;
    private static final int INFO_W = 170;
    /** Row of the "change effect" button; the settings row sits below it. */
    private static final int INFO_ROW_CHANGE = INFO_Y + 27;
    private static final int INFO_ROW_SETTINGS = INFO_Y + 45;

    private static final int GAUGE_X = 162;
    private static final int GAUGE_Y = 133;

    private static final int TOGGLE_X = 165;
    private static final int TOGGLE_Y = 5;
    private static final int TOGGLE_W = 58;
    private static final int TOGGLE_H = 14;

    private static final int SELECTOR_X = 36;
    private static final int SELECTOR_Y = 24;
    private static final int SELECTOR_W = 158;
    private static final int SELECTOR_H = 118;
    private static final int ROW_H = 18;
    private static final int VISIBLE_ROWS = 5;

    private static final int TEXT = 0x404040;
    private static final int TEXT_DIM = 0x808080;

    private int focusedCase = 0;
    private boolean selectorOpen;
    private int selectorSlot;
    private int scroll;
    private String search = "";
    /** Row under the cursor in the selector, so the info panel previews before committing. */
    private int previewRow = -1;

    public BeaconPackScreen(BeaconPackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 230;
        this.imageHeight = 250;
        this.inventoryLabelY = 157;
        this.titleLabelY = 6;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (selectorOpen) {
            return;
        }
        List<Component> tooltip = tooltipAt(mouseX - leftPos, mouseY - topPos);
        if (tooltip.isEmpty()) {
            renderTooltip(graphics, mouseX, mouseY);
        } else {
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    /** Nothing on this screen is self-explanatory without these. */
    private List<Component> tooltipAt(int x, int y) {
        if (within(x, y, TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
            return List.of(Component.translatable("beaconpack.tip.master"));
        }
        if (within(x, y, GAUGE_X, GAUGE_Y, 60, 14)) {
            PackStats stats = menu.stats();
            return List.of(
                    Component.translatable("beaconpack.gui.fuel"),
                    Component.translatable("beaconpack.tip.fuel",
                            menu.state().fuel(), stats.fuelCapacity()));
        }
        for (int i = 0; i < MAX_CASES; i++) {
            if (!within(x, y, CASE_X + i * CASE_SPACING, CASE_Y, CASE_SIZE, CASE_SIZE)) {
                continue;
            }
            PackStats stats = menu.stats();
            if (i >= stats.effectSlots()) {
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
        int buttons = INFO_ROW_SETTINGS;
        if (within(x, y, INFO_X + 4, INFO_ROW_CHANGE, INFO_W - 8, TOGGLE_H)) {
            return List.of(Component.translatable("beaconpack.tip.change_effect"));
        }
        if (within(x, y, INFO_X + 4, buttons, 44, TOGGLE_H)) {
            return List.of(Component.translatable("beaconpack.tip.level"));
        }
        if (within(x, y, INFO_X + 52, buttons, 48, TOGGLE_H)) {
            return List.of(Component.translatable("beaconpack.tip.effect_toggle"));
        }
        if (within(x, y, INFO_X + 104, buttons, 60, TOGGLE_H)) {
            return List.of(Component.translatable("beaconpack.tip.aura"));
        }
        return List.of();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;

        PackState state = menu.state();
        PackStats stats = menu.stats();

        // Section labels: without them nothing on this screen says what the three groups of boxes
        // are for.
        graphics.drawString(font, Component.translatable("beaconpack.gui.effects"),
                CASE_X, CASE_Y - 10, TEXT, false);
        graphics.drawString(font, Component.translatable("beaconpack.gui.augments"),
                34, 122, TEXT, false);
        graphics.drawString(font, Component.translatable("beaconpack.gui.fuel"),
                139, 122, TEXT, false);

        drawToggle(graphics, state, localX, localY);
        drawCases(graphics, state, stats, localX, localY);
        drawFuel(graphics, state, stats);

        if (selectorOpen) {
            drawSelector(graphics, stats, localX, localY);
        } else {
            drawInfoPanel(graphics, state, stats, localX, localY);
        }
    }

    private void drawToggle(GuiGraphics graphics, PackState state, int mouseX, int mouseY) {
        boolean hovered = within(mouseX, mouseY, TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H);
        Component label = Component.translatable(
                state.active() ? "beaconpack.gui.active" : "beaconpack.gui.inactive");
        drawButton(graphics, TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H, label, hovered,
                state.active());
    }

    private void drawCases(GuiGraphics graphics, PackState state, PackStats stats,
                           int mouseX, int mouseY) {
        List<EffectSlotConfig> effects = state.effects();
        for (int i = 0; i < MAX_CASES; i++) {
            int x = CASE_X + i * CASE_SPACING;
            boolean unlocked = i < stats.effectSlots();

            if (!unlocked) {
                // Locked cases stay visible rather than hidden: the player should see what a higher
                // tier would give them.
                graphics.fill(x + 2, CASE_Y + 2, x + CASE_SIZE - 2, CASE_Y + CASE_SIZE - 2,
                        0x60000000);
                graphics.drawCenteredString(font, "⚠", x + CASE_SIZE / 2, CASE_Y + 8,
                        TEXT_DIM);
                continue;
            }
            if (i == focusedCase) {
                graphics.renderOutline(x - 1, CASE_Y - 1, CASE_SIZE + 2, CASE_SIZE + 2, 0xFFFFFFAA);
            }
            if (i >= effects.size()) {
                graphics.drawCenteredString(font, "+", x + CASE_SIZE / 2, CASE_Y + 8, TEXT_DIM);
                continue;
            }

            EffectSlotConfig slot = effects.get(i);
            drawEffectIcon(graphics, slot.effect(), x + 4, CASE_Y + 4);
            if (!slot.enabled()) {
                graphics.fill(x + 2, CASE_Y + 2, x + CASE_SIZE - 2, CASE_Y + CASE_SIZE - 2,
                        0x80303030);
            }
            graphics.drawString(font, roman(slot.amplifier() + 1),
                    x + CASE_SIZE - 9, CASE_Y + CASE_SIZE - 9, 0xFFFFFF, true);
        }
    }

    private void drawInfoPanel(GuiGraphics graphics, PackState state, PackStats stats,
                               int mouseX, int mouseY) {
        List<EffectSlotConfig> effects = state.effects();
        if (focusedCase >= effects.size()) {
            graphics.drawString(font, Component.translatable("beaconpack.gui.empty_slot"),
                    INFO_X + 6, INFO_Y + 6, TEXT_DIM, false);
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
                INFO_X + 6, INFO_Y + 5, TEXT, false);

        double cost = PackResolver.fuelPerSecond(slot, stats, effectLookup()) * stats.fuelMultiplier();
        String detail = String.format(Locale.ROOT, "%.1f u/s  ·  %s",
                cost, slot.aura().isAura()
                        ? String.format(Locale.ROOT, "%.0f m", stats.range())
                        : "—");
        graphics.drawString(font, detail, INFO_X + 6, INFO_Y + 17, TEXT_DIM, false);

        // Explicit rather than "click the case again": re-clicking the case is how you focus it,
        // and overloading that click with "open the selector" made every attempt to read a second
        // effect's details pop the picker instead.
        drawButton(graphics, INFO_X + 4, INFO_ROW_CHANGE, INFO_W - 8, TOGGLE_H,
                Component.translatable("beaconpack.gui.change_effect"),
                within(mouseX, mouseY, INFO_X + 4, INFO_ROW_CHANGE, INFO_W - 8, TOGGLE_H), true);

        int y = INFO_ROW_SETTINGS;
        drawButton(graphics, INFO_X + 4, y, 44, TOGGLE_H,
                Component.literal("‹ " + roman(slot.amplifier() + 1) + " ›"),
                within(mouseX, mouseY, INFO_X + 4, y, 44, TOGGLE_H),
                canAmplify(def, stats));
        drawButton(graphics, INFO_X + 52, y, 48, TOGGLE_H,
                Component.translatable(slot.enabled()
                        ? "beaconpack.gui.active" : "beaconpack.gui.inactive"),
                within(mouseX, mouseY, INFO_X + 52, y, 48, TOGGLE_H), slot.enabled());
        drawButton(graphics, INFO_X + 104, y, 60, TOGGLE_H,
                Component.translatable("beaconpack.aura." + slot.aura().getSerializedName()),
                within(mouseX, mouseY, INFO_X + 104, y, 60, TOGGLE_H),
                stats.allowedAuraModes().size() > 1);
    }

    private void drawFuel(GuiGraphics graphics, PackState state, PackStats stats) {
        int capacity = Math.max(1, stats.fuelCapacity());
        int filled = (int) (56.0 * Math.min(1.0, state.fuel() / (double) capacity));
        graphics.fill(GAUGE_X + 2, GAUGE_Y + 2, GAUGE_X + 2 + filled, GAUGE_Y + 12, 0xFF3FA34D);

        double perSecond = PackResolver.fuelPerSecond(state, stats, effectLookup());
        String autonomy = perSecond <= 0.0
                ? "∞"
                : formatDuration((int) (state.fuel() / perSecond));
        graphics.drawString(font, state.fuel() + " u  ·  " + autonomy, GAUGE_X + 2, GAUGE_Y + 16, TEXT_DIM, false);
    }

    private void drawSelector(GuiGraphics graphics, PackStats stats, int mouseX, int mouseY) {
        graphics.fill(SELECTOR_X - 2, SELECTOR_Y - 2,
                SELECTOR_X + SELECTOR_W + 2, SELECTOR_Y + SELECTOR_H + 2, 0xFF2B2B2B);
        graphics.fill(SELECTOR_X, SELECTOR_Y, SELECTOR_X + SELECTOR_W, SELECTOR_Y + SELECTOR_H,
                0xFF3C3C3C);

        List<ResourceKey<BeaconEffectDef>> rows = visibleRows();
        graphics.drawString(font, search.isEmpty()
                        ? Component.translatable("beaconpack.gui.search").getString() + "…"
                        : search + "_",
                SELECTOR_X + 5, SELECTOR_Y + 5, search.isEmpty() ? 0xFF777777 : 0xFFFFFFFF, false);

        previewRow = -1;
        int tierLevel = tierLevel();
        for (int i = 0; i < VISIBLE_ROWS && i + scroll < rows.size(); i++) {
            ResourceKey<BeaconEffectDef> key = rows.get(i + scroll);
            Optional<BeaconEffectDef> maybeDef = effectLookup().get(key);
            if (maybeDef.isEmpty()) {
                continue;
            }
            BeaconEffectDef def = maybeDef.get();
            int y = SELECTOR_Y + 18 + i * ROW_H;
            boolean locked = def.minTier() > tierLevel;
            boolean hovered = within(mouseX, mouseY, SELECTOR_X, y, SELECTOR_W, ROW_H);
            if (hovered) {
                graphics.fill(SELECTOR_X, y, SELECTOR_X + SELECTOR_W, y + ROW_H, 0xFF555555);
                previewRow = i + scroll;
            }

            drawEffectIcon(graphics, key, SELECTOR_X + 2, y + 1);
            String right = locked
                    ? Component.translatable("beaconpack.gui.locked_tier",
                            roman(def.minTier())).getString()
                    : String.format(Locale.ROOT, "%.1f u/s", def.cost());
            graphics.drawString(font, right,
                    SELECTOR_X + SELECTOR_W - font.width(right) - 4, y + 5,
                    locked ? 0xFFAA5555 : 0xFFBBBBBB, false);

            // Truncated against the space the cost leaves, otherwise long effect names run
            // straight through it.
            int available = SELECTOR_W - 22 - font.width(right) - 10;
            String name = font.plainSubstrByWidth(
                    def.effect().value().getDisplayName().getString(), available);
            graphics.drawString(font, name,
                    SELECTOR_X + 22, y + 5, locked ? 0xFF888888 : 0xFFFFFFFF, false);
        }
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
        graphics.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
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
            int caseX = CASE_X + i * CASE_SPACING;
            if (!within(x, y, caseX, CASE_Y, CASE_SIZE, CASE_SIZE)) {
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
        if (within(x, y, INFO_X + 4, INFO_ROW_CHANGE, INFO_W - 8, TOGGLE_H)) {
            openSelector(focusedCase);
            return true;
        }
        int y0 = INFO_ROW_SETTINGS;
        if (within(x, y, INFO_X + 4, y0, 44, TOGGLE_H)) {
            send(BeaconPackMenu.ACTION_CYCLE_AMPLIFIER, focusedCase, 0);
            return true;
        }
        if (within(x, y, INFO_X + 52, y0, 48, TOGGLE_H)) {
            send(BeaconPackMenu.ACTION_TOGGLE_EFFECT, focusedCase, 0);
            return true;
        }
        if (within(x, y, INFO_X + 104, y0, 60, TOGGLE_H)) {
            send(BeaconPackMenu.ACTION_CYCLE_AURA, focusedCase, 0);
            return true;
        }
        return false;
    }

    private void openSelector(int caseIndex) {
        selectorOpen = true;
        selectorSlot = caseIndex;
        scroll = 0;
        search = "";
    }

    private void handleSelectorClick(int x, int y) {
        if (!within(x, y, SELECTOR_X, SELECTOR_Y, SELECTOR_W, SELECTOR_H)) {
            selectorOpen = false;
            return;
        }
        List<ResourceKey<BeaconEffectDef>> rows = visibleRows();
        for (int i = 0; i < VISIBLE_ROWS && i + scroll < rows.size(); i++) {
            int rowY = SELECTOR_Y + 18 + i * ROW_H;
            if (!within(x, y, SELECTOR_X, rowY, SELECTOR_W, ROW_H)) {
                continue;
            }
            ResourceKey<BeaconEffectDef> key = rows.get(i + scroll);
            if (effectLookup().get(key).map(def -> def.minTier() > tierLevel()).orElse(true)) {
                return;
            }
            send(BeaconPackMenu.ACTION_SET_EFFECT, selectorSlot, allKeys().indexOf(key));
            selectorOpen = false;
            return;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (selectorOpen) {
            int max = Math.max(0, visibleRows().size() - VISIBLE_ROWS);
            scroll = Math.clamp(scroll - (int) Math.signum(deltaY), 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (selectorOpen && search.length() < 24) {
            search += codePoint;
            scroll = 0;
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectorOpen) {
            if (keyCode == 256) {
                selectorOpen = false;
                return true;
            }
            if (keyCode == 259 && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                scroll = 0;
                return true;
            }
            // Swallow the rest so the inventory key does not close the whole screen mid-search.
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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

    private static String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> String.valueOf(value);
        };
    }

    private static String formatDuration(int seconds) {
        if (seconds >= 3600) {
            return (seconds / 3600) + " h";
        }
        if (seconds >= 60) {
            return (seconds / 60) + " min";
        }
        return seconds + " s";
    }
}
