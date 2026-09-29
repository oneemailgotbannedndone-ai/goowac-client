package dev.goowac.client;

import dev.goowac.client.module.Module;
import dev.goowac.client.module.ModuleCategory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ClientMenuScreen extends Screen {
    private static final int ACCENT = 0xFFE0B6FF;
    private static final int BG = 0xE90A0B10;
    private static final int PANEL = 0xF0191A24;
    private static final int PANEL_2 = 0xF0222330;
    private static final int MUTED = 0xFF9595A7;
    private static final int TEXT = 0xFFE7E4EF;

    private ModuleCategory category = ModuleCategory.RENDER;
    private int page;
    private String search = "";
    private String selected = "";
    private TextFieldWidget searchBox;

    public ClientMenuScreen() {
        super(Text.literal("Goowac Client"));
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearChildren();

        int left = Math.max(18, (width - 980) / 2);
        int top = Math.max(18, (height - 560) / 2);
        int right = left + 980;
        int bottom = top + 560;

        searchBox = new TextFieldWidget(
            textRenderer, left + 220, top + 64, 330, 22, Text.literal("Search"));
        searchBox.setText(search);
        searchBox.setChangedListener(value -> {
            search = value;
            page = 0;
            rebuild();
        });
        addDrawableChild(searchBox);

        int sideY = top + 112;
        for (ModuleCategory c : ModuleCategory.values()) {
            final ModuleCategory target = c;
            String label = pretty(c) + "  " + GoowacClient.MODULES.category(c).size();
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                category = target;
                page = 0;
                selected = "";
                rebuild();
            }).dimensions(left + 22, sideY, 175, 28).build());
            sideY += 34;
        }

        List<Module> list = filtered();
        int perPage = 18;
        int pages = Math.max(1, (list.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);

        int start = page * perPage;
        int gridX = left + 220;
        int gridY = top + 112;

        for (int i = 0; i < perPage && start + i < list.size(); i++) {
            Module module = list.get(start + i);
            int col = i % 3;
            int row = i / 3;
            int bx = gridX + col * 240;
            int by = gridY + row * 52;

            String state = !module.functional()
                ? "  •"
                : (module.enabled() ? "  ON" : "  OFF");

            addDrawableChild(ButtonWidget.builder(
                Text.literal(module.name() + state),
                b -> {
                    selected = module.name();
                    if (module.functional()) {
                        module.toggle();
                        GoowacClient.CONFIG.save();
                    }
                    b.setMessage(Text.literal(module.name() + (!module.functional()
                        ? "  •"
                        : (module.enabled() ? "  ON" : "  OFF"))));
                }).dimensions(bx, by, 225, 32).build());
        }

        int navY = bottom - 64;
        addDrawableChild(ButtonWidget.builder(Text.literal("< Prev"), b -> {
            if (page > 0) {
                page--;
                rebuild();
            }
        }).dimensions(gridX, navY, 90, 26).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Next >"), b -> {
            if (page < pages - 1) {
                page++;
                rebuild();
            }
        }).dimensions(gridX + 100, navY, 90, 26).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Save Config"), b -> {
            GoowacClient.CONFIG.save();
        }).dimensions(right - 250, navY, 110, 26).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
            .dimensions(right - 132, navY, 110, 26).build());
    }

    private List<Module> filtered() {
        String query = search.trim().toLowerCase();
        List<Module> out = new ArrayList<>();

        for (Module module : GoowacClient.MODULES.category(category)) {
            if (query.isEmpty()
                || module.name().toLowerCase().contains(query)
                || module.description().toLowerCase().contains(query)) {
                out.add(module);
            }
        }

        out.sort(Comparator.comparing(Module::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int left = Math.max(18, (width - 980) / 2);
        int top = Math.max(18, (height - 560) / 2);
        int right = left + 980;
        int bottom = top + 560;

        context.fill(0, 0, width, height, BG);
        context.fill(left, top, right, bottom, PANEL);
        context.fill(left, top, right, top + 4, ACCENT);
        context.drawBorder(left, top, 980, 560, 0xFF3B3C49);

        context.drawTextWithShadow(
            textRenderer, "GOOWAC", left + 22, top + 18, ACCENT);
        context.drawTextWithShadow(
            textRenderer, "1.21.11  •  client menu", left + 22, top + 36, MUTED);

        context.drawTextWithShadow(
            textRenderer, pretty(category), left + 220, top + 100, TEXT);

        int infoX = right - 225;
        context.fill(infoX, top + 52, right - 20, bottom - 86, PANEL_2);

        Module current = GoowacClient.MODULES.find(selected);
        if (current != null) {
            context.drawTextWithShadow(textRenderer, current.name(), infoX + 14, top + 70, ACCENT);
            context.drawTextWithShadow(
                textRenderer,
                current.functional()
                    ? (current.enabled() ? "Enabled" : "Disabled")
                    : "Catalogue only",
                infoX + 14, top + 90, current.functional() && current.enabled() ? 0xFFA4E3B1 : MUTED
            );

            drawWrapped(context, current.description(), infoX + 14, top + 116, 175, 0xFFD0CDD8);
        } else {
            context.drawTextWithShadow(textRenderer, "Select a module", infoX + 14, top + 70, ACCENT);
            drawWrapped(
                context,
                "Search, select a category, then click a module. Your enabled modules are saved to config/goowac.json.",
                infoX + 14, top + 98, 175, 0xFFD0CDD8);
        }

        context.drawTextWithShadow(
            textRenderer,
            "Right Shift menu  •  H HUD  •  G Sprint  •  C copy coords  •  D Detection Test",
            left + 22, bottom - 28, MUTED);

        super.render(context, mouseX, mouseY, delta);
    }

    private static String pretty(ModuleCategory c) {
        return c.name().replace('_', ' ');
    }

    private void drawWrapped(
        DrawContext context, String text, int x, int y, int maxChars, int color) {
        String[] words = text.split("\s+");
        String line = "";
        int lineY = y;

        for (String word : words) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (next.length() > maxChars) {
                context.drawTextWithShadow(textRenderer, line, x, lineY, color);
                line = word;
                lineY += 12;
            } else {
                line = next;
            }
        }

        if (!line.isEmpty()) {
            context.drawTextWithShadow(textRenderer, line, x, lineY, color);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
