package dev.lunex.client.gui;

import dev.lunex.Lunex;
import dev.lunex.client.module.Category;
import dev.lunex.client.module.Module;
import dev.lunex.client.module.ModuleManager;
import dev.lunex.client.render.Render2D;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClickGuiScreen extends Screen {
	private static final int PREFERRED_WIDTH = 760;
	private static final int PREFERRED_HEIGHT = 480;
	private static final int CARD_GAP = 14;
	private static final int CARD_HEIGHT = 92;
	private static final int CARD_EXPANDED_HEIGHT = 132;
	private static final int BACKGROUND = 0xD807080C;
	private static final int GLASS = 0x8C4A4A4A;
	private static final int GLASS_DARK = 0x9E22242C;
	private static final int GLASS_LIGHT = 0xA25E626E;
	private static final int FIELD = 0x9C1A1C23;
	private static final int ACCENT = 0xFF8E96A8;
	private static final int TEXT = 0xFFF8F8F8;
	private static final int MUTED = 0xFFB2B6C1;
	private static final int DIM = 0xFF747985;
	private static final int GREEN = 0xFF65F28B;

	private final ModuleManager moduleManager;
	private final Map<String, Animation> animations = new HashMap<>();
	private final Animation openAnimation = new Animation(0.0F);
	private final Animation categorySlide = new Animation(0.0F);
	private final Animation scrollAnimation = new Animation(0.0F);
	private Category selectedCategory = Category.COMBAT;
	private Module expandedModule;
	private Module bindingModule;
	private String searchText = "";
	private boolean searchFocused;
	private int targetScroll;
	private int ticks;

	public ClickGuiScreen(ModuleManager moduleManager) {
		super(Text.literal("Lunex ClickGUI"));
		this.moduleManager = moduleManager;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		ticks++;
		openAnimation.animate(1.0F, 0.16F);
		categorySlide.animate(categoryIndex(selectedCategory), 0.18F);
		targetScroll = clampScroll(targetScroll);
		scrollAnimation.animate(targetScroll, 0.22F);

		renderBackground(context, mouseX, mouseY, delta);
		renderGlassBackground(context);

		int guiWidth = guiWidth();
		int guiHeight = guiHeight();
		int x = (width - guiWidth) / 2;
		int y = (height - guiHeight) / 2;
		float open = Animation.easeOutBack(Render2D.clamp(openAnimation.get(), 0.0F, 1.0F));
		MatrixStack matrices = context.getMatrices();
		matrices.push();
		matrices.translate(x + guiWidth / 2.0F, y + guiHeight / 2.0F, 0.0F);
		matrices.scale(0.88F + open * 0.12F, 0.88F + open * 0.12F, 1.0F);
		matrices.translate(-(x + guiWidth / 2.0F), -(y + guiHeight / 2.0F), 0.0F);

		int sidebarWidth = sidebarWidth(guiWidth);
		renderSidebar(context, mouseX, mouseY, x, y, sidebarWidth, guiHeight);
		renderMainPanel(context, mouseX, mouseY, x + sidebarWidth + 18, y, guiWidth - sidebarWidth - 18, guiHeight);
		matrices.pop();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int guiWidth = guiWidth();
		int guiHeight = guiHeight();
		int x = (width - guiWidth) / 2;
		int y = (height - guiHeight) / 2;
		int sidebarWidth = sidebarWidth(guiWidth);

		if (button == 0) {
			searchFocused = Render2D.hovered(mouseX, mouseY, x + 16, y + 76, sidebarWidth - 32, 30);
			if (searchFocused) {
				return true;
			}
		}

		int categoryY = y + 122;
		for (Category category : Category.values()) {
			if (Render2D.hovered(mouseX, mouseY, x + 12, categoryY, sidebarWidth - 24, 45)) {
				selectedCategory = category;
				expandedModule = null;
				bindingModule = null;
				targetScroll = 0;
				return true;
			}
			categoryY += 52;
		}

		int panelX = x + sidebarWidth + 18;
		if (Render2D.hovered(mouseX, mouseY, panelX, y, guiWidth - sidebarWidth - 18, guiHeight)) {
			return handleModuleClick(mouseX, mouseY, button, panelX, y, guiWidth - sidebarWidth - 18, guiHeight);
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int guiWidth = guiWidth();
		int guiHeight = guiHeight();
		int x = (width - guiWidth) / 2;
		int y = (height - guiHeight) / 2;
		int sidebarWidth = sidebarWidth(guiWidth);
		int panelX = x + sidebarWidth + 18;
		int panelWidth = guiWidth - sidebarWidth - 18;
		if (!Render2D.hovered(mouseX, mouseY, panelX, y + 70, panelWidth, guiHeight - 84)) {
			return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		}

		targetScroll = clampScroll(targetScroll + (int) (verticalAmount * 36.0D));
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (bindingModule != null && isBindableKey(keyCode)) {
			bindingModule.setKeybind(keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE ? 0 : keyCode);
			bindingModule = null;
			return true;
		}

		if (searchFocused) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
				searchFocused = false;
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
				searchText = searchText.substring(0, searchText.length() - 1);
				targetScroll = 0;
				return true;
			}
			return false;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (!searchFocused || chr < 32 || chr > 126 || searchText.length() >= 28) {
			return super.charTyped(chr, modifiers);
		}

		searchText += chr;
		targetScroll = 0;
		return true;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private void renderGlassBackground(DrawContext context) {
		context.fill(0, 0, width, height, BACKGROUND);
		float pulse = pulse();
		int spotA = Render2D.alpha(0xFF7D8799, (int) (18 + pulse * 12));
		int spotB = Render2D.alpha(0xFF4F5868, (int) (14 + pulse(1.7F) * 9));
		Render2D.roundedGlow(context, width / 5 + Math.round((float) Math.sin(ticks * 0.025D) * 40.0F), height / 4, 170, 170, 85, spotA, 12);
		Render2D.roundedGlow(context, width * 2 / 3 + Math.round((float) Math.cos(ticks * 0.02D) * 32.0F), height * 2 / 3, 150, 150, 75, spotB, 10);
	}

	private void renderSidebar(DrawContext context, int mouseX, int mouseY, int x, int y, int sidebarWidth, int guiHeight) {
		Render2D.roundedGlow(context, x - 6, y - 6, sidebarWidth + 12, guiHeight + 12, 18, 0xAA000000, 8);
		Render2D.roundedVerticalGradient(context, x, y, sidebarWidth, guiHeight, 18, GLASS_LIGHT, GLASS_DARK);
		Render2D.roundedBorder(context, x, y, sidebarWidth, guiHeight, 18, 0x70FFFFFF);

		Render2D.roundedGlow(context, x + 18, y + 18, 38, 38, 14, ACCENT, 3);
		Render2D.roundedVerticalGradient(context, x + 18, y + 18, 38, 38, 14, 0xFF707787, 0xFF3F4653);
		Render2D.centeredText(context, "L", x + 37, y + 31, TEXT);
		Render2D.scaledText(context, Lunex.NAME, x + 68, y + 21, 1.25F, TEXT);
		Render2D.text(context, "private client", x + 68, y + 42, MUTED);

		renderSearch(context, x + 16, y + 76, sidebarWidth - 32);
		renderCategories(context, mouseX, mouseY, x, y, sidebarWidth);
		renderUser(context, x, y + guiHeight - 66, sidebarWidth);
	}

	private void renderSearch(DrawContext context, int x, int y, int searchWidth) {
		int border = searchFocused ? Render2D.alpha(ACCENT, 170) : 0x66FFFFFF;
		Render2D.roundedGlow(context, x, y, searchWidth, 30, 11, searchFocused ? ACCENT : 0x55000000, searchFocused ? 2 : 1);
		Render2D.roundedRect(context, x, y, searchWidth, 30, 11, FIELD);
		Render2D.roundedBorder(context, x, y, searchWidth, 30, 11, border);
		String value = searchText.isEmpty() && !searchFocused ? "Search..." : searchText;
		Render2D.text(context, value, x + 13, y + 11, searchText.isEmpty() ? DIM : TEXT);
		if (searchFocused && ticks / 18 % 2 == 0) {
			int cursorX = x + 14 + Render2D.width(searchText);
			Render2D.line(context, cursorX, y + 8, cursorX, y + 22, TEXT);
		}
	}

	private void renderCategories(DrawContext context, int mouseX, int mouseY, int x, int y, int sidebarWidth) {
		int indicatorY = y + 122 + Math.round(categorySlide.get() * 52.0F);
		Render2D.roundedGlow(context, x + 10, indicatorY - 2, sidebarWidth - 20, 49, 14, ACCENT, 3);
		int categoryY = y + 122;
		for (Category category : Category.values()) {
			boolean selected = category == selectedCategory;
			boolean hovered = Render2D.hovered(mouseX, mouseY, x + 12, categoryY, sidebarWidth - 24, 45);
			float hover = animation("category:" + category.name(), hovered || selected, 0.16F);
			int top = selected ? Render2D.alpha(ACCENT, 150) : Render2D.lerpColor(0x785E626E, 0xA5757A86, hover);
			int bottom = selected ? 0x8C4E5665 : Render2D.lerpColor(0x70414650, 0x965B606C, hover);
			Render2D.roundedVerticalGradient(context, x + 12, categoryY, sidebarWidth - 24, 45, 12, top, bottom);
			Render2D.roundedBorder(context, x + 12, categoryY, sidebarWidth - 24, 45, 12, selected ? Render2D.alpha(ACCENT, 220) : 0x70FFFFFF);

			String title = category.getTitle().toUpperCase(Locale.ROOT);
			Render2D.text(context, categoryIcon(category), x + 26 + Math.round(hover * 3.0F), categoryY + 17, selected ? TEXT : categoryColor(category));
			Render2D.centeredText(context, title, x + sidebarWidth / 2 + 8, categoryY + 17, TEXT);
			categoryY += 52;
		}
	}

	private void renderUser(DrawContext context, int x, int y, int sidebarWidth) {
		Render2D.roundedGlow(context, x + 8, y - 2, sidebarWidth - 16, 54, 12, 0xAA000000, 3);
		Render2D.roundedRect(context, x + 10, y, sidebarWidth - 20, 50, 12, GLASS);
		Render2D.roundedBorder(context, x + 10, y, sidebarWidth - 20, 50, 12, 0x70FFFFFF);
		Render2D.roundedRect(context, x + 20, y + 10, 30, 30, 15, 0x995F6674);
		Render2D.roundedBorder(context, x + 20, y + 10, 30, 30, 15, 0xAAFFFFFF);
		Render2D.centeredText(context, "U", x + 35, y + 19, TEXT);
		Render2D.text(context, username(), x + 58, y + 12, TEXT);
		Render2D.text(context, "Online", x + 58, y + 28, GREEN);
	}

	private void renderMainPanel(DrawContext context, int mouseX, int mouseY, int x, int y, int panelWidth, int guiHeight) {
		Render2D.roundedGlow(context, x - 6, y - 6, panelWidth + 12, guiHeight + 12, 18, 0xAA000000, 8);
		Render2D.roundedVerticalGradient(context, x, y, panelWidth, guiHeight, 18, GLASS, 0x981D2028);
		Render2D.roundedBorder(context, x, y, panelWidth, guiHeight, 18, 0x70FFFFFF);

		String title = selectedCategory.getTitle();
		Render2D.scaledText(context, title, x + 24, y + 18, 1.35F, TEXT);
		Render2D.roundedRect(context, x + 24, y + 50, 180, 2, 1, Render2D.alpha(ACCENT, 120));
		renderStats(context, x + panelWidth - 128, y + 20);
		renderModules(context, mouseX, mouseY, x, y, panelWidth, guiHeight);
	}

	private void renderStats(DrawContext context, int x, int y) {
		List<Module> modules = moduleManager.getModules(selectedCategory);
		long enabled = modules.stream().filter(Module::isEnabled).count();
		Render2D.roundedRect(context, x, y, 104, 24, 9, FIELD);
		Render2D.roundedBorder(context, x, y, 104, 24, 9, Render2D.alpha(ACCENT, 80));
		Render2D.centeredText(context, enabled + "/" + modules.size() + " enabled", x + 52, y + 8, MUTED);
	}

	private void renderModules(DrawContext context, int mouseX, int mouseY, int panelX, int panelY, int panelWidth, int guiHeight) {
		int contentX = panelX + 20;
		int contentY = panelY + 76 + Math.round(scrollAnimation.get());
		int viewTop = panelY + 70;
		int viewBottom = panelY + guiHeight - 14;
		int columns = columnCount(panelWidth);
		int columnGap = 16;
		int columnWidth = (panelWidth - 40 - (columns - 1) * columnGap) / columns;
		int[] columnHeights = new int[columns];
		context.enableScissor(panelX + 10, viewTop, panelX + panelWidth - 10, viewBottom);
		for (Module module : visibleModules()) {
			int column = shortestColumn(columnHeights);
			int cardX = contentX + column * (columnWidth + columnGap);
			int cardY = contentY + columnHeights[column];
			int cardHeight = cardHeight(module);
			if (cardY + cardHeight >= viewTop && cardY <= viewBottom) {
				renderModuleCard(context, mouseX, mouseY, module, cardX, cardY, columnWidth, cardHeight);
			}
			columnHeights[column] += cardHeight + CARD_GAP;
		}
		context.disableScissor();
	}

	private void renderModuleCard(DrawContext context, int mouseX, int mouseY, Module module, int x, int y, int cardWidth, int cardHeight) {
		boolean hovered = Render2D.hovered(mouseX, mouseY, x, y, cardWidth, cardHeight);
		float hover = animation("hover:" + module.getId(), hovered, 0.18F);
		float enabled = animation("enabled:" + module.getId(), module.isEnabled(), 0.16F);
		int drawY = y - Math.round(hover * 3.0F);
		int top = Render2D.lerpColor(0xA02A2A2A, 0xB66B6F7B, hover);
		int bottom = Render2D.lerpColor(0xA01B1D24, 0xA7474D5C, enabled);

		Render2D.roundedGlow(context, x - 2, drawY - 2, cardWidth + 4, cardHeight + 4, 13, module.isEnabled() ? Render2D.alpha(ACCENT, 150) : 0xAA000000, module.isEnabled() ? 4 : 2);
		Render2D.roundedVerticalGradient(context, x, drawY, cardWidth, cardHeight, 12, top, bottom);
		Render2D.roundedBorder(context, x, drawY, cardWidth, cardHeight, 12, module.isEnabled() ? Render2D.alpha(ACCENT, 180) : 0x66FFFFFF);

		Render2D.text(context, module.getName(), x + 14, drawY + 14, TEXT);
		Render2D.text(context, Render2D.trimToWidth(module.getDescription(), cardWidth - 118), x + 14, drawY + 33, MUTED);
		renderKeyBadge(context, module, x + cardWidth - 76, drawY + 13);
		renderStatePill(context, module, x + cardWidth - 66, drawY + 52, enabled);
		renderSmallButton(context, expandedModule == module ? "Hide" : "Settings", x + 14, drawY + 58, 78, expandedModule == module);
		renderSmallButton(context, bindingModule == module ? "..." : "Bind", x + 100, drawY + 58, 58, bindingModule == module);

		if (expandedModule == module) {
			renderSettings(context, module, x, drawY + 88, cardWidth);
		}
	}

	private void renderKeyBadge(DrawContext context, Module module, int x, int y) {
		String key = bindingModule == module ? "..." : keyName(module.getKeybind());
		int width = Math.max(42, Render2D.width(key) + 18);
		Render2D.roundedRect(context, x + 62 - width, y, width, 20, 8, 0x86171920);
		Render2D.roundedBorder(context, x + 62 - width, y, width, 20, 8, 0x55FFFFFF);
		Render2D.centeredText(context, key, x + 62 - width / 2, y + 7, TEXT);
	}

	private void renderStatePill(DrawContext context, Module module, int x, int y, float enabled) {
		Render2D.roundedRect(context, x, y, 52, 22, 9, Render2D.lerpColor(0x99191B22, Render2D.alpha(ACCENT, 140), enabled));
		Render2D.roundedBorder(context, x, y, 52, 22, 9, module.isEnabled() ? Render2D.alpha(ACCENT, 210) : 0x55FFFFFF);
		Render2D.centeredText(context, module.isEnabled() ? "ON" : "OFF", x + 26, y + 8, module.isEnabled() ? TEXT : MUTED);
	}

	private void renderSmallButton(DrawContext context, String label, int x, int y, int width, boolean active) {
		Render2D.roundedRect(context, x, y, width, 20, 7, active ? Render2D.alpha(ACCENT, 120) : 0x78191B22);
		Render2D.roundedBorder(context, x, y, width, 20, 7, active ? Render2D.alpha(ACCENT, 190) : 0x55FFFFFF);
		Render2D.centeredText(context, label, x + width / 2, y + 7, active ? TEXT : MUTED);
	}

	private void renderSettings(DrawContext context, Module module, int x, int y, int width) {
		Render2D.roundedRect(context, x + 10, y, width - 20, 34, 9, 0x8C11131A);
		Render2D.roundedBorder(context, x + 10, y, width - 20, 34, 9, 0x44FFFFFF);
		Render2D.text(context, "State", x + 22, y + 7, DIM);
		Render2D.text(context, module.isEnabled() ? "Enabled" : "Disabled", x + 78, y + 7, MUTED);
		Render2D.text(context, "Bind", x + 22, y + 21, DIM);
		Render2D.text(context, bindingModule == module ? "press key..." : keyName(module.getKeybind()), x + 78, y + 21, TEXT);
	}

	private boolean handleModuleClick(double mouseX, double mouseY, int button, int panelX, int panelY, int panelWidth, int guiHeight) {
		int contentX = panelX + 20;
		int contentY = panelY + 76 + Math.round(scrollAnimation.get());
		int columns = columnCount(panelWidth);
		int columnGap = 16;
		int columnWidth = (panelWidth - 40 - (columns - 1) * columnGap) / columns;
		int[] columnHeights = new int[columns];
		for (Module module : visibleModules()) {
			int column = shortestColumn(columnHeights);
			int cardX = contentX + column * (columnWidth + columnGap);
			int cardY = contentY + columnHeights[column];
			int height = cardHeight(module);
			if (Render2D.hovered(mouseX, mouseY, cardX, cardY, columnWidth, height)) {
				if (button == 2 || Render2D.hovered(mouseX, mouseY, cardX + 100, cardY + 58, 58, 20)) {
					bindingModule = bindingModule == module ? null : module;
					expandedModule = module;
					return true;
				}
				if (button == 0 && Render2D.hovered(mouseX, mouseY, cardX + 14, cardY + 58, 78, 20)) {
					expandedModule = expandedModule == module ? null : module;
					if (expandedModule != module) {
						bindingModule = null;
					}
					return true;
				}
				if (button == 0) {
					module.toggle();
					return true;
				}
			}
			columnHeights[column] += height + CARD_GAP;
		}
		return true;
	}

	private List<Module> visibleModules() {
		String query = searchText.strip().toLowerCase(Locale.ROOT);
		List<Module> modules = new ArrayList<>();
		for (Module module : moduleManager.getModules(selectedCategory)) {
			if (query.isEmpty()
					|| module.getName().toLowerCase(Locale.ROOT).contains(query)
					|| module.getDescription().toLowerCase(Locale.ROOT).contains(query)) {
				modules.add(module);
			}
		}
		return modules;
	}

	private int contentHeight() {
		int columns = columnCount(guiWidth() - sidebarWidth(guiWidth()) - 18);
		int[] columnHeights = new int[columns];
		for (Module module : visibleModules()) {
			int column = shortestColumn(columnHeights);
			columnHeights[column] += cardHeight(module) + CARD_GAP;
		}
		int height = 0;
		for (int columnHeight : columnHeights) {
			height = Math.max(height, columnHeight);
		}
		return Math.max(0, height - CARD_GAP);
	}

	private int clampScroll(int scroll) {
		int viewHeight = guiHeight() - 90;
		int min = -Math.max(0, contentHeight() - viewHeight);
		return Math.min(0, Math.max(scroll, min));
	}

	private int shortestColumn(int[] columnHeights) {
		int column = 0;
		for (int index = 1; index < columnHeights.length; index++) {
			if (columnHeights[index] < columnHeights[column]) {
				column = index;
			}
		}
		return column;
	}

	private int cardHeight(Module module) {
		return expandedModule == module ? CARD_EXPANDED_HEIGHT : CARD_HEIGHT;
	}

	private int columnCount(int panelWidth) {
		return panelWidth >= 460 ? 2 : 1;
	}

	private int guiWidth() {
		return Math.max(420, Math.min(PREFERRED_WIDTH, width - 28));
	}

	private int guiHeight() {
		return Math.max(320, Math.min(PREFERRED_HEIGHT, height - 28));
	}

	private int sidebarWidth(int guiWidth) {
		return Math.max(170, Math.min(210, guiWidth / 3));
	}

	private float animation(String id, boolean target, float speed) {
		Animation animation = animations.computeIfAbsent(id, key -> new Animation(0.0F));
		animation.animate(target ? 1.0F : 0.0F, speed);
		return animation.get();
	}

	private float pulse() {
		return pulse(0.0F);
	}

	private float pulse(float offset) {
		return (float) ((Math.sin((ticks + offset) * 0.08D) + 1.0D) * 0.5D);
	}

	private int categoryIndex(Category category) {
		Category[] categories = Category.values();
		for (int index = 0; index < categories.length; index++) {
			if (categories[index] == category) {
				return index;
			}
		}
		return 0;
	}

	private String categoryIcon(Category category) {
		return switch (category) {
			case COMBAT -> "C";
			case MOVEMENT -> "M";
			case RENDER -> "R";
			case MISC -> "X";
		};
	}

	private int categoryColor(Category category) {
		return switch (category) {
			case COMBAT -> 0xFFFF6A6A;
			case MOVEMENT -> 0xFF6AFF88;
			case RENDER -> 0xFF7D9CFF;
			case MISC -> 0xFFFF76E2;
		};
	}

	private String username() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null) {
			return client.player.getGameProfile().getName();
		}
		return client.getSession().getUsername();
	}

	private String keyName(int key) {
		if (key <= 0) {
			return "None";
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

	private boolean isBindableKey(int keyCode) {
		return keyCode == GLFW.GLFW_KEY_ESCAPE
				|| keyCode == GLFW.GLFW_KEY_BACKSPACE
				|| keyCode != GLFW.GLFW_KEY_RIGHT_SHIFT
				&& keyCode != GLFW.GLFW_KEY_LEFT_SHIFT
				&& keyCode != GLFW.GLFW_KEY_LEFT_CONTROL
				&& keyCode != GLFW.GLFW_KEY_RIGHT_CONTROL
				&& keyCode != GLFW.GLFW_KEY_LEFT_ALT
				&& keyCode != GLFW.GLFW_KEY_RIGHT_ALT;
	}
}
