package dev.lunex.client.gui;

import dev.lunex.client.module.Category;
import dev.lunex.client.module.Module;
import dev.lunex.client.module.ModuleManager;
import dev.lunex.client.render.Render2D;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClickGuiScreen extends Screen {
	private static final int GUI_WIDTH = 760;
	private static final int GUI_HEIGHT = 312;
	private static final int COLUMN_COUNT = 5;
	private static final int COLUMN_WIDTH = 104;
	private static final int COLUMN_GAP = 6;
	private static final int COLUMN_AREA_HEIGHT = 250;
	private static final int MODULE_HEIGHT = 18;
	private static final int SETTING_HEIGHT = 16;
	private static final int SEARCH_WIDTH = 300;
	private static final int SEARCH_HEIGHT = 24;
	private static final int BACKDROP = 0xB8000000;
	private static final int PANEL = 0xE6141016;
	private static final int CARD = 0xAA1C161C;
	private static final int CARD_ENABLED = 0xAA3C243C;
	private static final int OUTLINE = 0x2DFFFFFF;
	private static final int TEXT = 0xFFE0E0E0;
	private static final int MUTED = 0xFFB4B4B4;
	private static final int DIM = 0xFF858085;
	private static final int ACCENT = 0xFF5A243C;

	private final ModuleManager moduleManager;
	private final Map<String, Animation> hoverAnimations = new HashMap<>();
	private final Map<String, Animation> heightAnimations = new HashMap<>();
	private final Map<Category, Animation> scrollAnimations = new EnumMap<>(Category.class);
	private final Map<Category, Integer> targetScrolls = new EnumMap<>(Category.class);
	private final Animation openAnimation = new Animation(0.0F);
	private final Animation closeAnimation = new Animation(1.0F);
	private final Animation searchAnimation = new Animation(0.0F);
	private boolean closing;
	private String searchText = "";
	private boolean searchFocused;
	private Module bindingModule;
	private Module expandedModule;

	public ClickGuiScreen(ModuleManager moduleManager) {
		super(Text.literal("Lunex ClickGUI"));
		this.moduleManager = moduleManager;
		for (Category category : columns()) {
			scrollAnimations.put(category, new Animation(0.0F));
			targetScrolls.put(category, 0);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if (closing) {
			closeAnimation.animate(0.0F, 0.22F);
			if (closeAnimation.get() <= 0.01F) {
				client.setScreen(null);
				return;
			}
		} else {
			openAnimation.animate(1.0F, 0.18F);
		}

		float alpha = closing ? closeAnimation.get() : Render2D.clamp(openAnimation.get(), 0.0F, 1.0F);
		searchAnimation.animate(searchFocused ? 1.0F : 0.0F, 0.20F);
		for (Category category : columns()) {
			int target = clampScroll(category, targetScrolls.getOrDefault(category, 0));
			targetScrolls.put(category, target);
			scrollAnimations.get(category).animate(target, 0.12F);
		}

		context.fill(0, 0, width, height, Render2D.alpha(BACKDROP, (int) (184 * alpha)));
		int x = (width - GUI_WIDTH) / 2;
		int y = (height - GUI_HEIGHT) / 2;
		float eased = Animation.easeOutBack(alpha);
		MatrixStack matrices = context.getMatrices();
		matrices.push();
		matrices.translate(x + GUI_WIDTH / 2.0F, y + GUI_HEIGHT / 2.0F, 0.0F);
		matrices.scale(0.92F + eased * 0.08F, 0.92F + eased * 0.08F, 1.0F);
		matrices.translate(-(x + GUI_WIDTH / 2.0F), -(y + GUI_HEIGHT / 2.0F), 0.0F);
		renderColumns(context, mouseX, mouseY, x, y, alpha);
		renderSearch(context, mouseX, mouseY, x + (GUI_WIDTH - SEARCH_WIDTH) / 2, y + GUI_HEIGHT - 5, alpha);
		if (bindingModule != null) {
			renderBindOverlay(context, alpha);
		}
		matrices.pop();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (bindingModule != null) {
			return true;
		}
		int x = (width - GUI_WIDTH) / 2;
		int y = (height - GUI_HEIGHT) / 2;
		int searchX = x + (GUI_WIDTH - SEARCH_WIDTH) / 2;
		int searchY = y + GUI_HEIGHT - 5;
		if (button == 0 && Render2D.hovered(mouseX, mouseY, searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT)) {
			searchFocused = true;
			return true;
		}
		searchFocused = false;

		for (int column = 0; column < columns().length; column++) {
			Category category = columns()[column];
			ColumnBounds bounds = columnBounds(x, y, column);
			if (!Render2D.hovered(mouseX, mouseY, bounds.x(), bounds.listY() - 6, COLUMN_WIDTH, COLUMN_AREA_HEIGHT + 32)) {
				continue;
			}
			int moduleY = bounds.contentY() + Math.round(scrollAnimations.get(category).get());
			for (Module module : filteredModules(category)) {
				int height = animatedModuleHeight(module);
				if (moduleY + height >= bounds.contentY() - 4 && moduleY <= bounds.listY() - 6 + COLUMN_AREA_HEIGHT + 32) {
					if (Render2D.hovered(mouseX, mouseY, bounds.x() + 6, moduleY, COLUMN_WIDTH - 12, height)) {
						if (button == 0) {
							module.toggle();
							return true;
						}
						if (button == 1) {
							expandedModule = expandedModule == module ? null : module;
							return true;
						}
						if (button == 2) {
							bindingModule = module;
							expandedModule = module;
							return true;
						}
					}
				}
				moduleY += height + 2;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int x = (width - GUI_WIDTH) / 2;
		int y = (height - GUI_HEIGHT) / 2;
		for (int column = 0; column < columns().length; column++) {
			Category category = columns()[column];
			ColumnBounds bounds = columnBounds(x, y, column);
			if (Render2D.hovered(mouseX, mouseY, bounds.x(), bounds.listY() - 6, COLUMN_WIDTH, COLUMN_AREA_HEIGHT + 32)) {
				int target = targetScrolls.getOrDefault(category, 0) + (int) (verticalAmount * 15.0D);
				targetScrolls.put(category, clampScroll(category, target));
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (bindingModule != null) {
			bindingModule.setKeybind(keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE ? 0 : keyCode);
			bindingModule = null;
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			closeWithAnimation();
			return true;
		}
		if (searchFocused) {
			if (keyCode == GLFW.GLFW_KEY_ENTER) {
				searchFocused = false;
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
				searchText = searchText.substring(0, searchText.length() - 1);
				resetScrolls();
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (searchFocused && !Character.isISOControl(chr) && vanillaWidth(searchText + chr) < SEARCH_WIDTH - 20) {
			searchText += chr;
			resetScrolls();
			return true;
		}
		return super.charTyped(chr, modifiers);
	}

	@Override
	public void close() {
		closeWithAnimation();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private void renderColumns(DrawContext context, int mouseX, int mouseY, int x, int y, float alpha) {
		for (int column = 0; column < columns().length; column++) {
			Category category = columns()[column];
			ColumnBounds bounds = columnBounds(x, y, column);
			int panelColor = Render2D.alpha(PANEL, (int) (230 * alpha));
			int outlineColor = Render2D.alpha(OUTLINE, (int) (45 * alpha));
			Render2D.roundedGlow(context, bounds.x(), bounds.listY() - 6, COLUMN_WIDTH, COLUMN_AREA_HEIGHT + 32, 6, 0xFF000000, 4);
			Render2D.roundedRect(context, bounds.x(), bounds.listY() - 6, COLUMN_WIDTH, COLUMN_AREA_HEIGHT + 32, 6, panelColor);
			Render2D.roundedBorder(context, bounds.x(), bounds.listY() - 6, COLUMN_WIDTH, COLUMN_AREA_HEIGHT + 32, 6, outlineColor);
			vanillaCenteredText(context, categoryTitle(category), bounds.x() + COLUMN_WIDTH / 2, bounds.listY() + 8, Render2D.alpha(TEXT, (int) (255 * alpha)));
			renderModuleList(context, mouseX, mouseY, category, bounds, alpha);
		}
	}

	private void renderModuleList(DrawContext context, int mouseX, int mouseY, Category category, ColumnBounds bounds, float alpha) {
		int clipTop = bounds.contentY() - 4;
		int clipBottom = bounds.listY() - 6 + COLUMN_AREA_HEIGHT + 32;
		context.enableScissor(bounds.x(), clipTop, bounds.x() + COLUMN_WIDTH, clipBottom);
		int moduleY = bounds.contentY() + Math.round(scrollAnimations.get(category).get());
		for (Module module : filteredModules(category)) {
			int height = animatedModuleHeight(module);
			if (moduleY + height >= clipTop && moduleY <= clipBottom) {
				renderModule(context, mouseX, mouseY, module, bounds.x(), moduleY, COLUMN_WIDTH, height, alpha);
			}
			moduleY += height + 2;
		}
		context.disableScissor();
		renderScrollBar(context, category, bounds, alpha);
	}

	private void renderModule(DrawContext context, int mouseX, int mouseY, Module module, int x, int y, int width, int height, float alpha) {
		boolean hovered = Render2D.hovered(mouseX, mouseY, x + 6, y, width - 12, height);
		float hover = animation("hover:" + module.getId(), hovered, 0.18F);
		int base = module.isEnabled() ? CARD_ENABLED : CARD;
		int fill = Render2D.lerpColor(base, Render2D.alpha(0xFF3D303D, 190), hover);
		Render2D.roundedBorder(context, x + 6, y, width - 12, height, 4, Render2D.alpha(0xFFFFFFFF, (int) (45 * alpha)));
		Render2D.roundedGlow(context, x + 6, y, width - 12, height, 4, 0xFF000000, 2);
		Render2D.roundedRect(context, x + 6, y, width - 12, height, 4, Render2D.alpha(fill, (int) (170 * alpha)));
		vanillaText(context, trimVanilla(module.getName(), width - 36), x + 10, y + 5, Render2D.alpha(TEXT, (int) (255 * alpha)));
		if (module.getSettings().length > 0) {
			vanillaCenteredText(context, "...", x + width - 20, y + 5, Render2D.alpha(MUTED, (int) (255 * alpha)));
		}
		if (bindingModule == module) {
			vanillaCenteredText(context, "bind", x + width / 2, y + 5, Render2D.alpha(TEXT, (int) (255 * alpha)));
		}
		if (height > MODULE_HEIGHT + 2) {
			renderSettings(context, module, x, y + MODULE_HEIGHT - 2, width, alpha);
		}
	}

	private void renderSettings(DrawContext context, Module module, int x, int y, int width, float alpha) {
		Module.Setting[] settings = module.getSettings();
		if (settings.length == 0) {
			vanillaText(context, "No settings", x + 10, y + 4, Render2D.alpha(DIM, (int) (255 * alpha)));
			return;
		}
		for (int index = 0; index < settings.length; index++) {
			Module.Setting setting = settings[index];
			int settingY = y + index * SETTING_HEIGHT;
			vanillaText(context, trimVanilla(setting.name(), 52), x + 10, settingY + 4, Render2D.alpha(TEXT, (int) (230 * alpha)));
			vanillaText(context, trimVanilla(setting.value(), 34), x + width - 46, settingY + 4, Render2D.alpha(MUTED, (int) (220 * alpha)));
		}
	}

	private void renderSearch(DrawContext context, int mouseX, int mouseY, int x, int y, float alpha) {
		float active = Math.max(searchAnimation.get(), animation("search", Render2D.hovered(mouseX, mouseY, x, y, SEARCH_WIDTH, SEARCH_HEIGHT), 0.20F));
		Render2D.roundedRect(context, x, y, SEARCH_WIDTH, SEARCH_HEIGHT, 6, Render2D.alpha(Render2D.lerpColor(PANEL, 0xFF201820, active), (int) (230 * alpha)));
		Render2D.roundedBorder(context, x, y, SEARCH_WIDTH, SEARCH_HEIGHT, 6, Render2D.alpha(0xFFC8C8C8, (int) ((12 + active * 45) * alpha)));
		String display = searchText.isEmpty() && !searchFocused ? "Поиск" : searchText;
		vanillaText(context, display, x + 10, y + 8, Render2D.alpha(MUTED, (int) (255 * alpha)));
	}

	private void renderScrollBar(DrawContext context, Category category, ColumnBounds bounds, float alpha) {
		int contentHeight = contentHeight(category);
		int visible = COLUMN_AREA_HEIGHT + 12;
		int maxScroll = Math.max(0, contentHeight - visible);
		if (maxScroll <= 0) {
			return;
		}
		int thumbHeight = Math.max(15, visible * visible / contentHeight);
		float scroll = scrollAnimations.get(category).get();
		int thumbY = bounds.contentY() + Math.round((-scroll / Math.max(1, maxScroll)) * (visible - thumbHeight));
		Render2D.roundedRect(context, bounds.x() + COLUMN_WIDTH - 4, thumbY, 2, thumbHeight, 1, Render2D.alpha(0xFFFFFFFF, (int) (120 * alpha)));
	}

	private void renderBindOverlay(DrawContext context, float alpha) {
		context.fill(0, 0, width, height, Render2D.alpha(0xFF000000, (int) (120 * alpha)));
		vanillaCenteredText(context, "Press any key to bind...", width / 2, height / 2, Render2D.alpha(TEXT, (int) (255 * alpha)));
	}

	private int animatedModuleHeight(Module module) {
		int target = expandedModule == module ? expandedHeight(module) : MODULE_HEIGHT;
		Animation animation = heightAnimations.computeIfAbsent(module.getId(), key -> new Animation(MODULE_HEIGHT));
		animation.animate(target, 0.12F);
		return Math.round(animation.get());
	}

	private int expandedHeight(Module module) {
		return MODULE_HEIGHT + Math.max(1, module.getSettings().length) * SETTING_HEIGHT + 2;
	}

	private int contentHeight(Category category) {
		int height = 0;
		for (Module module : filteredModules(category)) {
			height += animatedModuleHeight(module) + 2;
		}
		return Math.max(0, height - 2);
	}

	private int clampScroll(Category category, int scroll) {
		int max = Math.max(0, contentHeight(category) - (COLUMN_AREA_HEIGHT + 12));
		return Math.min(0, Math.max(scroll, -max));
	}

	private List<Module> filteredModules(Category category) {
		String filter = searchText.toLowerCase(Locale.ROOT);
		return moduleManager.getModules(category).stream()
				.filter(module -> filter.isEmpty()
						|| module.getName().toLowerCase(Locale.ROOT).contains(filter)
						|| module.getDescription().toLowerCase(Locale.ROOT).contains(filter))
				.toList();
	}

	private ColumnBounds columnBounds(int x, int y, int column) {
		int startX = x + 20;
		int headerY = y + 2;
		int colX = startX + column * (COLUMN_WIDTH + COLUMN_GAP) + 60;
		int listY = headerY + 22;
		int contentY = listY + 20;
		return new ColumnBounds(colX, listY, contentY);
	}

	private Category[] columns() {
		return new Category[] {Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.PLAYER, Category.MISC};
	}

	private String categoryTitle(Category category) {
		return category == Category.RENDER ? "Visual" : category.getTitle();
	}

	private void resetScrolls() {
		for (Category category : columns()) {
			targetScrolls.put(category, 0);
		}
	}

	private void closeWithAnimation() {
		if (!closing) {
			closing = true;
			closeAnimation.animate(openAnimation.get(), 1.0F);
		}
	}

	private float animation(String id, boolean target, float speed) {
		Animation animation = hoverAnimations.computeIfAbsent(id, key -> new Animation(0.0F));
		animation.animate(target ? 1.0F : 0.0F, speed);
		return animation.get();
	}

	private void vanillaText(DrawContext context, String text, int x, int y, int color) {
		context.drawText(textRenderer(), text, x, y, color, false);
	}

	private void vanillaCenteredText(DrawContext context, String text, int x, int y, int color) {
		context.drawText(textRenderer(), text, x - vanillaWidth(text) / 2, y, color, false);
	}

	private int vanillaWidth(String text) {
		return textRenderer().getWidth(text);
	}

	private String trimVanilla(String text, int maxWidth) {
		String value = text;
		while (!value.isEmpty() && vanillaWidth(value + "...") > maxWidth) {
			value = value.substring(0, value.length() - 1);
		}
		return value.length() == text.length() ? text : value + "...";
	}

	private TextRenderer textRenderer() {
		return client.textRenderer;
	}

	private int categoryIndex(Category category) {
		Category[] categories = columns();
		for (int index = 0; index < categories.length; index++) {
			if (categories[index] == category) {
				return index;
			}
		}
		return 0;
	}

	private String keyName(int key) {
		if (key <= 0) {
			return "NONE";
		}
		String name = GLFW.glfwGetKeyName(key, 0);
		if (name == null) {
			name = InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
		}
		if (name.startsWith("key.keyboard.")) {
			name = name.substring("key.keyboard.".length());
		}
		return name.toUpperCase(Locale.ROOT);
	}

	private record ColumnBounds(int x, int listY, int contentY) {
	}
}
