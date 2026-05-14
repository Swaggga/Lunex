package dev.lunex.client.gui;

import dev.lunex.Lunex;
import dev.lunex.client.module.Category;
import dev.lunex.client.module.Module;
import dev.lunex.client.module.ModuleManager;
import dev.lunex.client.render.Render2D;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClickGuiScreen extends Screen {
	private static final int GUI_WIDTH = 560;
	private static final int GUI_HEIGHT = 520;
	private static final int SIDEBAR_WIDTH = 152;
	private static final int CONTENT_GAP = 14;
	private static final int HEADER_HEIGHT = 72;
	private static final int MODULE_WIDTH = 168;
	private static final int MODULE_BASE_HEIGHT = 66;
	private static final int MODULE_EXPANDED_HEIGHT = 108;
	private static final int MODULE_GAP = 12;
	private static final int BACKDROP = 0x52000000;
	private static final int GLASS = 0xEA08080B;
	private static final int GLASS_LIGHT = 0xFF17171D;
	private static final int GLASS_DARK = 0xF20D0D11;
	private static final int OUTLINE = 0x705A5A60;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int MUTED = 0xFFB8B8B8;
	private static final int DIM = 0xFF727278;
	private static final int ACCENT = 0xFF8E7BFF;
	private static final int ACCENT_2 = 0xFF54D6FF;
	private static final int SUCCESS = 0xFF65FFB5;

	private final ModuleManager moduleManager;
	private final Map<String, Animation> hoverAnimations = new HashMap<>();
	private final Map<String, Animation> toggleAnimations = new HashMap<>();
	private final Animation openAnimation = new Animation(0.0F);
	private final Animation categoryAnimation = new Animation(0.0F);
	private final Animation scrollAnimation = new Animation(0.0F);
	private final Animation searchFocusAnimation = new Animation(0.0F);
	private Category selectedCategory = Category.COMBAT;
	private int targetScroll;
	private int ticks;
	private Module expandedModule;
	private Module bindingModule;
	private String searchText = "";
	private boolean searchFocused;

	public ClickGuiScreen(ModuleManager moduleManager) {
		super(Text.literal("Lunex ClickGUI"));
		this.moduleManager = moduleManager;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		ticks++;
		openAnimation.animate(1.0F, 0.16F);
		categoryAnimation.animate(categoryIndex(selectedCategory), 0.14F);
		searchFocusAnimation.animate(searchFocused ? 1.0F : 0.0F, 0.18F);
		targetScroll = clampScroll(targetScroll);
		scrollAnimation.animate(targetScroll, 0.22F);
		renderBackground(context);

		float open = Animation.easeOutBack(Render2D.clamp(openAnimation.get(), 0.0F, 1.0F));
		int x = (width - GUI_WIDTH) / 2;
		int y = (height - GUI_HEIGHT) / 2;
		MatrixStack matrices = context.getMatrices();
		matrices.push();
		matrices.translate(x + GUI_WIDTH / 2.0F, y + GUI_HEIGHT / 2.0F, 0.0F);
		matrices.scale(0.88F + open * 0.12F, 0.88F + open * 0.12F, 1.0F);
		matrices.translate(-(x + GUI_WIDTH / 2.0F), -(y + GUI_HEIGHT / 2.0F), 0.0F);

		renderSidebar(context, mouseX, mouseY, x, y);
		renderMainArea(context, mouseX, mouseY, x + SIDEBAR_WIDTH + CONTENT_GAP, y);
		matrices.pop();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int x = (width - GUI_WIDTH) / 2;
		int y = (height - GUI_HEIGHT) / 2;
		if (button == 0 && Render2D.hovered(mouseX, mouseY, x + 12, y + 66, SIDEBAR_WIDTH - 24, 28)) {
			searchFocused = true;
			bindingModule = null;
			return true;
		}
		searchFocused = false;

		int categoryY = y + 112;
		for (Category category : Category.values()) {
			if (Render2D.hovered(mouseX, mouseY, x + 8, categoryY, SIDEBAR_WIDTH - 16, 38)) {
				selectedCategory = category;
				targetScroll = 0;
				expandedModule = null;
				bindingModule = null;
				return true;
			}
			categoryY += 46;
		}

		int contentX = x + SIDEBAR_WIDTH + CONTENT_GAP;
		List<Module> modules = filteredModules();
		int contentY = y + HEADER_HEIGHT + Math.round(scrollAnimation.get());
		for (int index = 0; index < modules.size(); index++) {
			Module module = modules.get(index);
			int column = index % 2;
			int itemX = contentX + column * (MODULE_WIDTH + MODULE_GAP);
			if (column == 0 && index > 0) {
				contentY += maxPairHeight(modules, index - 2) + MODULE_GAP;
			}
			int moduleHeight = moduleHeight(module);
			if (contentY + moduleHeight < y + HEADER_HEIGHT) {
				continue;
			}
			if (contentY > y + GUI_HEIGHT - 26) {
				break;
			}
			if (Render2D.hovered(mouseX, mouseY, itemX, contentY, MODULE_WIDTH, 40)) {
				if (button == 0) {
					module.toggle();
					return true;
				}
				if (button == 1) {
					expandedModule = expandedModule == module ? null : module;
					bindingModule = null;
					return true;
				}
				if (button == 2) {
					bindingModule = bindingModule == module ? null : module;
					expandedModule = module;
					return true;
				}
			}
			if (Render2D.hovered(mouseX, mouseY, itemX + MODULE_WIDTH - 62, contentY + 45, 50, 16)) {
				bindingModule = bindingModule == module ? null : module;
				expandedModule = module;
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int x = (width - GUI_WIDTH) / 2 + SIDEBAR_WIDTH + CONTENT_GAP;
		int y = (height - GUI_HEIGHT) / 2 + HEADER_HEIGHT;
		if (!Render2D.hovered(mouseX, mouseY, x, y - 10, GUI_WIDTH - SIDEBAR_WIDTH - CONTENT_GAP, GUI_HEIGHT - HEADER_HEIGHT)) {
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
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				searchFocused = false;
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
				searchText = searchText.substring(0, searchText.length() - 1);
				targetScroll = 0;
				return true;
			}
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (searchFocused && chr >= 32 && chr <= 126 && Render2D.width(searchText + chr) < SIDEBAR_WIDTH - 64) {
			searchText += chr;
			targetScroll = 0;
			return true;
		}

		return super.charTyped(chr, modifiers);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private void renderBackground(DrawContext context) {
		context.fill(0, 0, width, height, BACKDROP);
		int alpha = (int) (10.0F * Render2D.clamp(openAnimation.get(), 0.0F, 1.0F));
		float wave = pulse();
		renderSoftSpot(context, Math.round(width * 0.24F + wave * 36.0F), Math.round(height * 0.30F), 150, Render2D.alpha(ACCENT, alpha));
		renderSoftSpot(context, Math.round(width * 0.66F - wave * 30.0F), Math.round(height * 0.58F), 130, Render2D.alpha(ACCENT_2, alpha - 3));
	}

	private void renderSoftSpot(DrawContext context, int x, int y, int size, int color) {
		for (int layer = 0; layer < 9; layer++) {
			int inset = layer * 10;
			int alpha = Math.max(0, (color >>> 24) - layer * 3);
			Render2D.roundedRect(context, x + inset, y + inset, size - inset * 2, size - inset * 2, Math.max(0, (size - inset * 2) / 2), Render2D.alpha(color, alpha));
		}
	}

	private void renderSidebar(DrawContext context, int mouseX, int mouseY, int x, int y) {
		Render2D.roundedGlow(context, x, y, SIDEBAR_WIDTH, GUI_HEIGHT, 12, 0xFF000000, 5);
		Render2D.roundedRect(context, x, y, SIDEBAR_WIDTH, GUI_HEIGHT, 12, GLASS);
		Render2D.roundedBorder(context, x, y, SIDEBAR_WIDTH, GUI_HEIGHT, 12, OUTLINE);

		Render2D.roundedGlow(context, x + 14, y + 18, 28, 28, 9, ACCENT, 2);
		Render2D.roundedRect(context, x + 14, y + 18, 28, 28, 9, 0xFF15151B);
		Render2D.centeredText(context, "L", x + 28, y + 27, ACCENT_2);
		Render2D.scaledText(context, Lunex.NAME, x + 52, y + 19, 1.05F, TEXT);
		Render2D.text(context, "1.21.4", x + 53, y + 37, MUTED);

		renderSearch(context, x + 12, y + 66, mouseX, mouseY);
		renderCategories(context, mouseX, mouseY, x, y);
		renderUserCard(context, x + 12, y + GUI_HEIGHT - 62);
	}

	private void renderSearch(DrawContext context, int x, int y, int mouseX, int mouseY) {
		boolean hovered = Render2D.hovered(mouseX, mouseY, x, y, SIDEBAR_WIDTH - 24, 28);
		float active = Math.max(searchFocusAnimation.get(), animation("search-hover", hovered, 0.2F));
		Render2D.roundedRect(context, x, y, SIDEBAR_WIDTH - 24, 28, 8, Render2D.lerpColor(0xFF0E0E13, 0xFF171720, active));
		Render2D.roundedBorder(context, x, y, SIDEBAR_WIDTH - 24, 28, 8, Render2D.alpha(ACCENT, (int) (42 + active * 84)));
		Render2D.text(context, searchText.isEmpty() && !searchFocused ? "Search..." : searchText, x + 10, y + 10, searchText.isEmpty() && !searchFocused ? DIM : TEXT);
		if (searchFocused && ticks / 12 % 2 == 0) {
			int cursorX = x + 11 + Render2D.width(searchText);
			Render2D.rect(context, cursorX, y + 7, 1, 14, TEXT);
		}
	}

	private void renderCategories(DrawContext context, int mouseX, int mouseY, int x, int y) {
		int indicatorY = y + 112 + Math.round(categoryAnimation.get() * 46.0F);
		Render2D.roundedGlow(context, x + 8, indicatorY, SIDEBAR_WIDTH - 16, 38, 10, ACCENT, 3);

		int categoryY = y + 112;
		for (Category category : Category.values()) {
			boolean selected = category == selectedCategory;
			boolean hovered = Render2D.hovered(mouseX, mouseY, x + 8, categoryY, SIDEBAR_WIDTH - 16, 38);
			float hover = animation("category:" + category.name(), hovered || selected, 0.18F);
			Render2D.roundedGlow(context, x + 7, categoryY - 1, SIDEBAR_WIDTH - 14, 40, 10, 0xFF000000, 1);
			if (selected) {
				Render2D.roundedHorizontalGradient(context, x + 8, categoryY, SIDEBAR_WIDTH - 16, 38, 10, Render2D.alpha(ACCENT, 132), Render2D.alpha(ACCENT_2, 82));
				Render2D.roundedBorder(context, x + 8, categoryY, SIDEBAR_WIDTH - 16, 38, 10, Render2D.alpha(TEXT, 96));
			} else {
				Render2D.roundedRect(context, x + 8, categoryY, SIDEBAR_WIDTH - 16, 38, 10, Render2D.lerpColor(GLASS_DARK, GLASS_LIGHT, hover * 0.5F));
				Render2D.roundedBorder(context, x + 8, categoryY, SIDEBAR_WIDTH - 16, 38, 10, Render2D.alpha(0xFF8A8A8A, 44 + (int) (hover * 38)));
			}
			int dotColor = selected ? TEXT : Render2D.lerpColor(MUTED, ACCENT_2, hover);
			Render2D.roundedRect(context, x + 22 + Math.round(hover * 3.0F), categoryY + 15, 8, 8, 3, dotColor);
			Render2D.text(context, category.getTitle(), x + 42 + Math.round(hover * 3.0F), categoryY + 14, selected ? TEXT : Render2D.lerpColor(MUTED, TEXT, hover));
			categoryY += 46;
		}
	}

	private void renderUserCard(DrawContext context, int x, int y) {
		Render2D.roundedRect(context, x, y, SIDEBAR_WIDTH - 24, 48, 12, 0xFF0E0E13);
		Render2D.roundedBorder(context, x, y, SIDEBAR_WIDTH - 24, 48, 12, 0x35FFFFFF);
		Render2D.roundedRect(context, x + 10, y + 10, 28, 28, 9, Render2D.alpha(ACCENT, 122));
		Render2D.centeredText(context, "U", x + 24, y + 19, TEXT);
		Render2D.text(context, "User", x + 46, y + 10, TEXT);
		Render2D.text(context, "Lunex", x + 46, y + 27, MUTED);
	}

	private void renderMainArea(DrawContext context, int mouseX, int mouseY, int x, int y) {
		int contentWidth = GUI_WIDTH - SIDEBAR_WIDTH - CONTENT_GAP;
		Render2D.roundedGlow(context, x - 8, y - 8, contentWidth + 16, GUI_HEIGHT + 16, 14, 0xFF000000, 4);
		Render2D.roundedRect(context, x, y, contentWidth, GUI_HEIGHT, 14, 0xF0050507);
		Render2D.roundedBorder(context, x, y, contentWidth, GUI_HEIGHT, 14, 0x5966666C);

		Render2D.scaledText(context, selectedCategory.getTitle(), x + 18, y + 18, 1.22F, TEXT);
		List<Module> modules = filteredModules();
		long active = modules.stream().filter(Module::isEnabled).count();
		String subtitle = modules.size() + " modules / " + active + " active";
		Render2D.text(context, subtitle, x + 20, y + 43, MUTED);
		Render2D.roundedRect(context, x + contentWidth - 112, y + 20, 92, 22, 8, 0xFF101016);
		Render2D.text(context, "RMB settings", x + contentWidth - 102, y + 28, MUTED);

		context.enableScissor(x + 12, y + HEADER_HEIGHT, x + contentWidth - 10, y + GUI_HEIGHT - 14);
		int moduleX = x + 14;
		int moduleY = y + HEADER_HEIGHT + Math.round(scrollAnimation.get());
		for (int index = 0; index < modules.size(); index++) {
			Module module = modules.get(index);
			int row = index % 2;
			int columnOffset = row * (MODULE_WIDTH + 18);
			int itemX = moduleX + columnOffset;
			if (row == 0 && index > 0) {
				moduleY += maxPairHeight(modules, index - 2) + MODULE_GAP;
			}
			if (moduleY + moduleHeight(module) >= y + HEADER_HEIGHT && moduleY <= y + GUI_HEIGHT - 16) {
				renderModule(context, mouseX, mouseY, module, itemX, moduleY, MODULE_WIDTH, index);
			}
		}
		context.disableScissor();
	}

	private void renderModule(DrawContext context, int mouseX, int mouseY, Module module, int x, int y, int w, int index) {
		int h = moduleHeight(module);
		boolean hovered = Render2D.hovered(mouseX, mouseY, x, y, w, h);
		float hover = animation("module-hover:" + module.getId(), hovered, 0.18F);
		float enabled = animation("module-toggle:" + module.getId(), module.isEnabled(), 0.2F);
		int drawY = y - Math.round(hover * 3.0F);
		Render2D.roundedGlow(context, x - 3, drawY - 3, w + 6, h + 6, 10, 0xFF000000, 3);
		Render2D.roundedRect(context, x, drawY, w, h, 9, Render2D.lerpColor(0xFF111116, 0xFF191922, hover));
		Render2D.roundedBorder(context, x, drawY, w, h, 9, Render2D.alpha(module.isEnabled() ? ACCENT : 0xFF555555, (int) (86 + enabled * 74 + hover * 28)));
		if (enabled > 0.05F) {
			Render2D.roundedBorder(context, x - 1, drawY - 1, w + 2, h + 2, 10, Render2D.alpha(ACCENT_2, (int) (enabled * 105)));
		}

		Render2D.text(context, Render2D.trimToWidth(module.getName(), w - 80), x + 10, drawY + 11, TEXT);
		Render2D.text(context, Render2D.trimToWidth(module.getDescription(), w - 22), x + 10, drawY + 28, Render2D.lerpColor(MUTED, TEXT, enabled * 0.35F));
		renderKeyBadge(context, bindingModule == module ? "..." : keyName(module.getKeybind()), x + w - 62, drawY + 10, module.isEnabled());
		renderTogglePill(context, x + 10, drawY + 45, module, enabled);
		Render2D.text(context, "MMB bind", x + 98, drawY + 50, DIM);

		if (expandedModule == module) {
			renderExpanded(context, module, x, drawY, w);
		}
	}

	private void renderKeyBadge(DrawContext context, String text, int x, int y, boolean active) {
		Render2D.roundedRect(context, x, y, 50, 18, 7, active ? 0x65344152 : 0xFF0E0E13);
		Render2D.roundedBorder(context, x, y, 50, 18, 7, Render2D.alpha(active ? ACCENT_2 : 0xFF777777, 76));
		Render2D.centeredText(context, Render2D.trimToWidth(text, 40), x + 25, y + 5, active ? TEXT : MUTED);
	}

	private void renderTogglePill(DrawContext context, int x, int y, Module module, float enabled) {
		Render2D.roundedHorizontalGradient(context, x, y, 76, 16, 8, Render2D.lerpColor(0xFF111116, Render2D.alpha(ACCENT, 130), enabled), Render2D.lerpColor(0xFF171720, Render2D.alpha(ACCENT_2, 120), enabled));
		Render2D.roundedBorder(context, x, y, 76, 16, 8, Render2D.alpha(TEXT, 34 + (int) (enabled * 60)));
		Render2D.roundedRect(context, x + 4 + Math.round(enabled * 44.0F), y + 3, 10, 10, 5, module.isEnabled() ? TEXT : MUTED);
		Render2D.centeredText(context, module.isEnabled() ? "ON" : "OFF", x + 38, y + 4, TEXT);
	}

	private void renderExpanded(DrawContext context, Module module, int x, int y, int w) {
		int boxY = y + 70;
		Render2D.roundedRect(context, x + 8, boxY, w - 16, 30, 8, 0xFF0B0B10);
		Render2D.roundedBorder(context, x + 8, boxY, w - 16, 30, 8, 0x33FFFFFF);
		Render2D.text(context, "Desc", x + 16, boxY + 7, DIM);
		Render2D.text(context, Render2D.trimToWidth(module.getDescription(), w - 74), x + 52, boxY + 7, MUTED);
		Render2D.text(context, "Bind", x + 16, boxY + 20, DIM);
		Render2D.text(context, bindingModule == module ? "Press key" : keyName(module.getKeybind()), x + 52, boxY + 20, TEXT);
	}

	private float animation(String id, boolean target, float speed) {
		Animation animation = id.contains("toggle") ? toggleAnimations.computeIfAbsent(id, key -> new Animation(0.0F)) : hoverAnimations.computeIfAbsent(id, key -> new Animation(0.0F));
		animation.animate(target ? 1.0F : 0.0F, speed);
		return animation.get();
	}

	private float pulse() {
		return pulse(0.0F);
	}

	private float pulse(float offset) {
		return (float) ((Math.sin((ticks + offset) * 0.05D) + 1.0D) * 0.5D);
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

	private int moduleHeight(Module module) {
		return expandedModule == module ? MODULE_EXPANDED_HEIGHT : MODULE_BASE_HEIGHT;
	}

	private int maxPairHeight(List<Module> modules, int startIndex) {
		int first = startIndex >= 0 && startIndex < modules.size() ? moduleHeight(modules.get(startIndex)) : 0;
		int second = startIndex + 1 >= 0 && startIndex + 1 < modules.size() ? moduleHeight(modules.get(startIndex + 1)) : 0;
		return Math.max(first, second);
	}

	private int contentHeight() {
		List<Module> modules = filteredModules();
		int height = 0;
		for (int index = 0; index < modules.size(); index += 2) {
			height += maxPairHeight(modules, index) + MODULE_GAP;
		}
		return Math.max(0, height - MODULE_GAP);
	}

	private int clampScroll(int scroll) {
		int viewHeight = GUI_HEIGHT - HEADER_HEIGHT - 20;
		int min = -Math.max(0, contentHeight() - viewHeight);
		return Math.min(0, Math.max(scroll, min));
	}

	private List<Module> filteredModules() {
		String filter = searchText.toLowerCase(Locale.ROOT);
		return moduleManager.getModules(selectedCategory).stream()
				.filter(module -> filter.isEmpty()
						|| module.getName().toLowerCase(Locale.ROOT).contains(filter)
						|| module.getDescription().toLowerCase(Locale.ROOT).contains(filter))
				.toList();
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
