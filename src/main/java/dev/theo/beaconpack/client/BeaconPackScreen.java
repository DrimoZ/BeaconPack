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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

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
    private static final int TEXTURE_SIZE = 512;

    static final int IMAGE_W = 248;
    static final int IMAGE_H = 290;

    private static final int TOGGLE_X = 176;
    private static final int TOGGLE_Y = 6;
    private static final int TOGGLE_W = 64;
    private static final int TOGGLE_H = 18;

    private static final int CASE_X = 36;
    private static final int CASE_Y = 38;
    private static final int CASE_SIZE = 26;
    private static final int CASE_SPACING = 32;
    private static final int MAX_CASES = 3;

    /** Right of the effect cases: what the pack as a whole currently is. */
    private static final int SUMMARY_X = 150;
    private static final int SUMMARY_Y = 40;

    private static final int INFO_X = 28;
    private static final int INFO_Y = 72;
    private static final int INFO_W = 192;
    private static final int INFO_H = 76;
    private static final int BTN_H = 16;
    private static final int ROW_CHANGE = INFO_Y + 36;
    private static final int ROW_SETTINGS = INFO_Y + 56;

    private static final int LEVEL_X = INFO_X + 6;
    private static final int LEVEL_W = 50;
    private static final int ENABLED_X = INFO_X + 62;
    private static final int ENABLED_W = 54;
    private static final int AURA_X = INFO_X + 120;
    private static final int AURA_W = 70;

    private static final int SECTION_LABEL_Y = 156;
    private static final int GAUGE_X = 172;
    private static final int GAUGE_Y = 170;
    private static final int GAUGE_W = 64;
    private static final int GAUGE_H = 14;

    private static final int SELECTOR_X = 40;
    private static final int SELECTOR_Y = 34;
    private static final int SELECTOR_W = 168;
    private static final int SELECTOR_H = 140;
    private static final int ROW_H = 20;
    private static final int VISIBLE_ROWS = 6;

    private static final int TEXT = 0x404040;
    private static final int TEXT_DIM = 0x707070;

    private int focusedCase = 0;
    private boolean selectorOpen;
    private int selectorSlot;
    private int scroll;
    private String search = "";

    public BeaconPackScreen(BeaconPackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = IMAGE_W;
        this.imageHeight = IMAGE_H;
        this.titleLabelY = 8;
        this.inventoryLabelX = 43;
        this.inventoryLabelY = 194;
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
                CASE_X, CASE_Y - 12, TEXT, false);
        graphics.drawString(font, Component.translatable("beaconpack.gui.augments"),
                CASE_X, SECTION_LABEL_Y, TEXT, false);
        graphics.drawString(font, Component.translatable("beaconpack.gui.fuel"),
                150, SECTION_LABEL_Y, TEXT, false);

        drawToggle(graphics, state, localX, localY);
        drawCases(graphics, state, stats, localX, localY);
        drawSummary(graphics, state, stats);
        drawFuel(graphics, state, stats);

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

    /** The pack-wide figures, so the effect of an augment is visible without opening anything. */
    private void drawSummary(GuiGraphics graphics, PackState state, PackStats stats) {
        double perSecond = PackResolver.fuelPerSecond(state, stats, effectLookup());
        graphics.drawString(font, Component.translatable("beaconpack.gui.range",
                        String.format(Locale.ROOT, "%.0f", stats.range())),
                SUMMARY_X, SUMMARY_Y, TEXT_DIM, false);
        graphics.drawString(font, Component.translatable("beaconpack.gui.cost",
                        String.format(Locale.ROOT, "%.1f", perSecond)),
                SUMMARY_X, SUMMARY_Y + 12, TEXT_DIM, false);
        graphics.drawString(font, Component.translatable("beaconpack.gui.slots",
                        state.effects().size(), stats.effectSlots()),
                SUMMARY_X, SUMMARY_Y + 24, TEXT_DIM, false);
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

        double cost = PackResolver.fuelPerSecond(slot, stats, effectLookup()) * stats.fuelMultiplier();
        graphics.drawString(font, String.format(Locale.ROOT, "%.1f u/s  ·  %s",
                        cost, slot.aura().isAura()
                                ? String.format(Locale.ROOT, "%.0f m", stats.range())
                                : Component.translatable("beaconpack.aura.self").getString()),
                INFO_X + 8, INFO_Y + 21, TEXT_DIM, false);

        // Explicit rather than "click the case again": re-clicking the case is how you focus it,
        // and overloading that click with "open the picker" made every attempt to read a second
        // effect's details pop the selector instead.
        drawButton(graphics, INFO_X + 6, ROW_CHANGE, INFO_W - 12, BTN_H,
                Component.translatable("beaconpack.gui.change_effect"),
                within(mouseX, mouseY, INFO_X + 6, ROW_CHANGE, INFO_W - 12, BTN_H), true);

        drawButton(graphics, LEVEL_X, ROW_SETTINGS, LEVEL_W, BTN_H,
                Component.literal("< " + roman(slot.amplifier() + 1) + " >"),
                within(mouseX, mouseY, LEVEL_X, ROW_SETTINGS, LEVEL_W, BTN_H),
                canAmplify(def, stats));
        drawButton(graphics, ENABLED_X, ROW_SETTINGS, ENABLED_W, BTN_H,
                Component.translatable(slot.enabled()
                        ? "beaconpack.gui.active" : "beaconpack.gui.inactive"),
                within(mouseX, mouseY, ENABLED_X, ROW_SETTINGS, ENABLED_W, BTN_H), slot.enabled());
        drawButton(graphics, AURA_X, ROW_SETTINGS, AURA_W, BTN_H,
                Component.translatable("beaconpack.aura." + slot.aura().getSerializedName()),
                within(mouseX, mouseY, AURA_X, ROW_SETTINGS, AURA_W, BTN_H),
                stats.allowedAuraModes().size() > 1);
    }

    private void drawFuel(GuiGraphics graphics, PackState state, PackStats stats) {
        int capacity = Math.max(1, stats.fuelCapacity());
        int filled = (int) ((GAUGE_W - 4) * Math.min(1.0, state.fuel() / (double) capacity));
        graphics.fill(GAUGE_X + 2, GAUGE_Y + 2, GAUGE_X + 2 + filled, GAUGE_Y + GAUGE_H - 2,
                0xFF3FA34D);

        double perSecond = PackResolver.fuelPerSecond(state, stats, effectLookup());
        String autonomy = perSecond <= 0.0 ? "-" : formatDuration((int) (state.fuel() / perSecond));
        String label = state.fuel() + " u  ·  " + autonomy;
        graphics.drawString(font, label,
                GAUGE_X + (GAUGE_W - font.width(label)) / 2, GAUGE_Y + 4, 0xFFFFFFFF, true);
    }

    private void drawSelector(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(SELECTOR_X - 3, SELECTOR_Y - 3,
                SELECTOR_X + SELECTOR_W + 3, SELECTOR_Y + SELECTOR_H + 3, 0xFF1F1F1F);
        graphics.fill(SELECTOR_X, SELECTOR_Y, SELECTOR_X + SELECTOR_W, SELECTOR_Y + SELECTOR_H,
                0xFF3C3C3C);
        graphics.fill(SELECTOR_X, SELECTOR_Y, SELECTOR_X + SELECTOR_W, SELECTOR_Y + 20, 0xFF2E2E2E);

        graphics.drawString(font, search.isEmpty()
                        ? Component.translatable("beaconpack.gui.search").getString() + "..."
                        : search + "_",
                SELECTOR_X + 6, SELECTOR_Y + 6, search.isEmpty() ? 0xFF777777 : 0xFFFFFFFF, false);

        List<ResourceKey<BeaconEffectDef>> rows = visibleRows();
        int tierLevel = tierLevel();
        for (int i = 0; i < VISIBLE_ROWS && i + scroll < rows.size(); i++) {
            ResourceKey<BeaconEffectDef> key = rows.get(i + scroll);
            Optional<BeaconEffectDef> maybeDef = effectLookup().get(key);
            if (maybeDef.isEmpty()) {
                continue;
            }
            BeaconEffectDef def = maybeDef.get();
            int y = SELECTOR_Y + 22 + i * ROW_H;
            boolean locked = def.minTier() > tierLevel;
            if (within(mouseX, mouseY, SELECTOR_X, y, SELECTOR_W, ROW_H)) {
                graphics.fill(SELECTOR_X, y, SELECTOR_X + SELECTOR_W, y + ROW_H, 0xFF555555);
            }

            drawEffectIcon(graphics, key, SELECTOR_X + 3, y + 2);
            String right = locked
                    ? Component.translatable("beaconpack.gui.locked_tier",
                            roman(def.minTier())).getString()
                    : String.format(Locale.ROOT, "%.1f u/s", def.cost());
            graphics.drawString(font, right,
                    SELECTOR_X + SELECTOR_W - font.width(right) - 6, y + 6,
                    locked ? 0xFFAA5555 : 0xFFBBBBBB, false);

            // Truncated against the space the cost leaves, otherwise long effect names run
            // straight through it.
            int available = SELECTOR_W - 24 - font.width(right) - 12;
            String name = font.plainSubstrByWidth(
                    def.effect().value().getDisplayName().getString(), available);
            graphics.drawString(font, name,
                    SELECTOR_X + 24, y + 6, locked ? 0xFF888888 : 0xFFFFFFFF, false);
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

    // ------------------------------------------------------------------ tooltips

    /** Nothing on this screen is self-explanatory without these. */
    private List<Component> tooltipAt(int x, int y) {
        if (within(x, y, TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
            return List.of(Component.translatable("beaconpack.tip.master"));
        }
        if (within(x, y, GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H)) {
            return List.of(
                    Component.translatable("beaconpack.gui.fuel"),
                    Component.translatable("beaconpack.tip.fuel",
                            menu.state().fuel(), menu.stats().fuelCapacity()));
        }
        List<Component> caseTip = caseTooltip(x, y);
        if (!caseTip.isEmpty()) {
            return caseTip;
        }
        // Guarded on the panel actually having buttons: they are only drawn for a focused case
        // holding an effect, and a tooltip over blank panel space was pure noise.
        if (focusedCase >= menu.state().effects().size()) {
            return List.of();
        }
        if (within(x, y, INFO_X + 6, ROW_CHANGE, INFO_W - 12, BTN_H)) {
            return List.of(Component.translatable("beaconpack.tip.change_effect"));
        }
        if (within(x, y, LEVEL_X, ROW_SETTINGS, LEVEL_W, BTN_H)) {
            return List.of(Component.translatable("beaconpack.tip.level"));
        }
        if (within(x, y, ENABLED_X, ROW_SETTINGS, ENABLED_W, BTN_H)) {
            return List.of(Component.translatable("beaconpack.tip.effect_toggle"));
        }
        if (within(x, y, AURA_X, ROW_SETTINGS, AURA_W, BTN_H)) {
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
        if (within(x, y, INFO_X + 6, ROW_CHANGE, INFO_W - 12, BTN_H)) {
            openSelector(focusedCase);
            return true;
        }
        if (within(x, y, LEVEL_X, ROW_SETTINGS, LEVEL_W, BTN_H)) {
            send(BeaconPackMenu.ACTION_CYCLE_AMPLIFIER, focusedCase, 0);
            return true;
        }
        if (within(x, y, ENABLED_X, ROW_SETTINGS, ENABLED_W, BTN_H)) {
            send(BeaconPackMenu.ACTION_TOGGLE_EFFECT, focusedCase, 0);
            return true;
        }
        if (within(x, y, AURA_X, ROW_SETTINGS, AURA_W, BTN_H)) {
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
            int rowY = SELECTOR_Y + 22 + i * ROW_H;
            if (!within(x, y, SELECTOR_X, rowY, SELECTOR_W, ROW_H)) {
                continue;
            }
            ResourceKey<BeaconEffectDef> key = rows.get(i + scroll);
            if (effectLookup().get(key).map(def -> def.minTier() > tierLevel()).orElse(true)) {
                return;
            }
            send(BeaconPackMenu.ACTION_SET_EFFECT, selectorSlot, allKeys().indexOf(key));
            focusedCase = selectorSlot;
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
