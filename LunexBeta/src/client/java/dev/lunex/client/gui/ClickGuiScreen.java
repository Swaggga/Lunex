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
	private static final int GUI_WIDTH = 900;
	private static final int GUI_HEIGHT = 600;
	private static final int SIDEBAR_WIDTH = 250;
	private static final int CONTENT_GAP = 22;
	private static final int HEADER_HEIGHT = 86;
	private static final int MODULE_WIDTH = 286;
	private static final int MODULE_BASE_HEIGHT = 72;
	private static final int MODULE_EXPANDED_HEIGHT = 118;
	private static final int MODULE_GAP = 16;
	private static final int BACKDROP = 0xA8000000;
	private static final int GLASS = 0x8F4A4A4A;
	private static final int GLASS_LIGHT = 0x9E5A5A5A;
	private static final int GLASS_DARK = 0x902A2A2A;
	private static final int OUTLINE = 0x78777777;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int MUTED = 0xFFB8B8B8;
	private static final int DIM = 0xFF777777;
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
		if (button == 0 && Render2D.hovered(mouseX, mouseY, x + 18, y + 72, SIDEBAR_WIDTH - 36, 30)) {
			searchFocused = true;
			bindingModule = null;
			return true;
		}
		searchFocused = false;

		int categoryY = y + 120;
		for (Category category : Category.values()) {
			if (Render2D.hovered(mouseX, mouseY, x + 10, categoryY, SIDEBAR_WIDTH - 20, 45)) {
				selectedCategory = category;
				targetScroll = 0;
				expandedModule = null;
				bindingModule = null;
				return true;
			}
			categoryY += 58;
		}

		int contentX = x + SIDEBAR_WIDTH + CONTENT_GAP;
		int contentY = y + HEADER_HEIGHT + Math.round(scrollAnimation.get());
		for (Module module : filteredModules()) {
			int moduleHeight = moduleHeight(module);
			if (contentY + moduleHeight < y + HEADER_HEIGHT) {
				contentY += moduleHeight + MODULE_GAP;
				continue;
			}
			if (contentY > y + GUI_HEIGHT - 26) {
				break;
			}
			if (Render2D.hovered(mouseX, mouseY, contentX, contentY, MODULE_WIDTH, 40)) {
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
			if (Render2D.hovered(mouseX, mouseY, contentX + MODULE_WIDTH - 82, contentY + 48, 66, 18)) {
				bindingModule = bindingModule == module ? null : module;
				expandedModule = module;
				return true;
			}
			contentY += moduleHeight + MODULE_GAP;
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
		int alpha = (int) (26.0F * Render2D.clamp(openAnimation.get(), 0.0F, 1.0F));
		float wave = pulse();
		renderSoftSpot(context, Math.round(width * 0.20F + wave * 80.0F), Math.round(height * 0.28F), 210, Render2D.alpha(ACCENT, alpha));
		renderSoftSpot(context, Math.round(width * 0.72F - wave * 70.0F), Math.round(height * 0.62F), 185, Render2D.alpha(ACCENT_2, alpha - 4));
		renderSoftSpot(context, Math.round(width * 0.15F + pulse(1.4F) * 50.0F), Math.round(height * 0.82F), 155, Render2D.alpha(SUCCESS, alpha - 8));
	}

	private void renderSoftSpot(DrawContext context, int x, int y, int size, int color) {
		for (int layer = 0; layer < 9; layer++) {
			int inset = layer * 10;
			int alpha = Math.max(0, (color >>> 24) - layer * 3);
			Render2D.roundedRect(context, x + inset, y + inset, size - inset * 2, size - inset * 2, Math.max(0, (size - inset * 2) / 2), Render2D.alpha(color, alpha));
		}
	}

	private void renderSidebar(DrawContext context, int mouseX, int mouseY, int x, int y) {
		Render2D.roundedGlow(context, x, y, SIDEBAR_WIDTH, GUI_HEIGHT, 15, 0xFF000000, 6);
		Render2D.roundedRect(context, x, y, SIDEBAR_WIDTH, GUI_HEIGHT, 15, GLASS);
		Render2D.roundedBorder(context, x, y, SIDEBAR_WIDTH, GUI_HEIGHT, 15, OUTLINE);

		Render2D.roundedGlow(context, x + 16, y + 18, 40, 40, 13, ACCENT, 3);
		Render2D.roundedRect(context, x + 16, y + 18, 40, 40, 13, 0xC93D3D46);
		Render2D.centeredText(context, "L", x + 36, y + 33, ACCENT_2);
		Render2D.scaledText(context, Lunex.NAME, x + 70, y + 25, 1.36F, TEXT);
		Render2D.text(context, "Minecraft 1.21.4", x + 71, y + 45, MUTED);

		renderSearch(context, x + 15, y + 72, mouseX, mouseY);
		renderCategories(context, mouseX, mouseY, x, y);
		renderUserCard(context, x + 15, y + GUI_HEIGHT - 72);
	}

	private void renderSearch(DrawContext context, int x, int y, int mouseX, int mouseY) {
		boolean hovered = Render2D.hovered(mouseX, mouseY, x, y, SIDEBAR_WIDTH - 30, 30);
		float active = Math.max(searchFocusAnimation.get(), animation("search-hover", hovered, 0.2F));
		Render2D.roundedRect(context, x, y, SIDEBAR_WIDTH - 30, 30, 9, Render2D.lerpColor(0x4A1D1D22, 0x6D34343C, active));
		Render2D.roundedBorder(context, x, y, SIDEBAR_WIDTH - 30, 30, 9, Render2D.alpha(ACCENT, (int) (54 + active * 96)));
		Render2D.text(context, searchText.isEmpty() && !searchFocused ? "Search..." : searchText, x + 12, y + 11, searchText.isEmpty() && !searchFocused ? DIM : TEXT);
		if (searchFocused && ticks / 12 % 2 == 0) {
			int cursorX = x + 13 + Render2D.width(searchText);
			Render2D.rect(context, cursorX, y + 8, 1, 15, TEXT);
		}
	}

	private void renderCategories(DrawContext context, int mouseX, int mouseY, int x, int y) {
		int indicatorY = y + 120 + Math.round(categoryAnimation.get() * 58.0F);
		Render2D.roundedGlow(context, x + 10, indicatorY, SIDEBAR_WIDTH - 20, 45, 12, ACCENT, 4);

		int categoryY = y + 120;
		for (Category category : Category.values()) {
			boolean selected = category == selectedCategory;
			boolean hovered = Render2D.hovered(mouseX, mouseY, x + 10, categoryY, SIDEBAR_WIDTH - 20, 45);
			float hover = animation("category:" + category.name(), hovered || selected, 0.18F);
			Render2D.roundedGlow(context, x + 8, categoryY - 2, SIDEBAR_WIDTH - 16, 49, 12, 0xFF000000, 2);
			if (selected) {
				Render2D.roundedHorizontalGradient(context, x + 10, categoryY, SIDEBAR_WIDTH - 20, 45, 12, Render2D.alpha(ACCENT, 148), Render2D.alpha(ACCENT_2, 104));
				Render2D.roundedBorder(context, x + 10, categoryY, SIDEBAR_WIDTH - 20, 45, 12, Render2D.alpha(TEXT, 122));
			} else {
				Render2D.roundedRect(context, x + 10, categoryY, SIDEBAR_WIDTH - 20, 45, 12, Render2D.lerpColor(GLASS_DARK, GLASS_LIGHT, hover * 0.48F));
				Render2D.roundedBorder(context, x + 10, categoryY, SIDEBAR_WIDTH - 20, 45, 12, Render2D.alpha(0xFF8A8A8A, 54 + (int) (hover * 46)));
			}
			int dotColor = selected ? TEXT : Render2D.lerpColor(MUTED, ACCENT_2, hover);
			Render2D.roundedRect(context, x + 26 + Math.round(hover * 4.0F), categoryY + 17, 10, 10, 4, dotColor);
			Render2D.scaledText(context, category.getTitle(), x + 48 + Math.round(hover * 4.0F), categoryY + 14, 1.16F, selected ? TEXT : Render2D.lerpColor(MUTED, TEXT, hover));
			categoryY += 58;
		}
	}

	private void renderUserCard(DrawContext context, int x, int y) {
		Render2D.roundedRect(context, x, y, SIDEBAR_WIDTH - 30, 55, 14, 0x5E1B1B21);
		Render2D.roundedBorder(context, x, y, SIDEBAR_WIDTH - 30, 55, 14, 0x4BFFFFFF);
		Render2D.roundedRect(context, x + 12, y + 12, 30, 30, 10, Render2D.alpha(ACCENT, 132));
		Render2D.centeredText(context, "U", x + 27, y + 22, TEXT);
		Render2D.text(context, "User", x + 52, y + 13, TEXT);
		Render2D.text(context, "Lunex Client", x + 52, y + 30, MUTED);
	}

	private void renderMainArea(DrawContext context, int mouseX, int mouseY, int x, int y) {
		int contentWidth = GUI_WIDTH - SIDEBAR_WIDTH - CONTENT_GAP;
		Render2D.roundedGlow(context, x - 10, y - 10, contentWidth + 20, GUI_HEIGHT + 20, 18, 0xFF000000, 5);
		Render2D.roundedRect(context, x, y, contentWidth, GUI_HEIGHT, 18, 0x664A4A4A);
		Render2D.roundedBorder(context, x, y, contentWidth, GUI_HEIGHT, 18, 0x5E777777);

		Render2D.scaledText(context, selectedCategory.getTitle(), x + 24, y + 22, 1.55F, TEXT);
		List<Module> modules = filteredModules();
		long active = modules.stream().filter(Module::isEnabled).count();
		String subtitle = modules.size() + " modules / " + active + " active";
		Render2D.text(context, subtitle, x + 26, y + 51, MUTED);
		Render2D.roundedRect(context, x + contentWidth - 170, y + 24, 140, 26, 10, 0x57373740);
		Render2D.text(context, "RMB settings", x + contentWidth - 154, y + 33, MUTED);

		context.enableScissor(x + 12, y + HEADER_HEIGHT, x + contentWidth - 10, y + GUI_HEIGHT - 16);
		int moduleX = x + 18;
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
		Render2D.roundedGlow(context, x - 3, drawY - 3, w + 6, h + 6, 12, 0xFF000000, 4);
		Render2D.roundedRect(context, x, drawY, w, h, 10, Render2D.lerpColor(0x852A2A2A, 0xA43A3A44, hover));
		Render2D.roundedBorder(context, x, drawY, w, h, 10, Render2D.alpha(module.isEnabled() ? ACCENT : 0xFF555555, (int) (100 + enabled * 84 + hover * 30)));
		if (enabled > 0.05F) {
			Render2D.roundedBorder(context, x - 1, drawY - 1, w + 2, h + 2, 11, Render2D.alpha(ACCENT_2, (int) (enabled * 120)));
		}

		Render2D.text(context, module.getName(), x + 12, drawY + 14, TEXT);
		Render2D.text(context, Render2D.trimToWidth(module.getDescription(), w - 34), x + 12, drawY + 32, Render2D.lerpColor(MUTED, TEXT, enabled * 0.35F));
		renderKeyBadge(context, bindingModule == module ? "..." : keyName(module.getKeybind()), x + w - 82, drawY + 14, module.isEnabled());
		renderTogglePill(context, x + 12, drawY + 49, module, enabled);
		Render2D.text(context, "Middle: bind", x + 114, drawY + 54, DIM);

		if (expandedModule == module) {
			renderExpanded(context, module, x, drawY, w);
		}
	}

	private void renderKeyBadge(DrawContext context, String text, int x, int y, boolean active) {
		Render2D.roundedRect(context, x, y, 66, 20, 8, active ? 0x65344152 : 0x4A1D1D22);
		Render2D.roundedBorder(context, x, y, 66, 20, 8, Render2D.alpha(active ? ACCENT_2 : 0xFF777777, 88));
		Render2D.centeredText(context, text, x + 33, y + 6, active ? TEXT : MUTED);
	}

	private void renderTogglePill(DrawContext context, int x, int y, Module module, float enabled) {
		Render2D.roundedHorizontalGradient(context, x, y, 84, 18, 9, Render2D.lerpColor(0x5D1D1D22, Render2D.alpha(ACCENT, 140), enabled), Render2D.lerpColor(0x5D262630, Render2D.alpha(ACCENT_2, 132), enabled));
		Render2D.roundedBorder(context, x, y, 84, 18, 9, Render2D.alpha(TEXT, 42 + (int) (enabled * 70)));
		Render2D.roundedRect(context, x + 4 + Math.round(enabled * 48.0F), y + 4, 10, 10, 5, module.isEnabled() ? TEXT : MUTED);
		Render2D.centeredText(context, module.isEnabled() ? "ON" : "OFF", x + 42, y + 5, TEXT);
	}

	private void renderExpanded(DrawContext context, Module module, int x, int y, int w) {
		int boxY = y + 76;
		Render2D.roundedRect(context, x + 10, boxY, w - 20, 34, 9, 0x4D16161C);
		Render2D.roundedBorder(context, x + 10, boxY, w - 20, 34, 9, 0x43FFFFFF);
		Render2D.text(context, "Description", x + 22, boxY + 8, DIM);
		Render2D.text(context, Render2D.trimToWidth(module.getDescription(), w - 120), x + 94, boxY + 8, MUTED);
		Render2D.text(context, "Bind", x + 22, boxY + 22, DIM);
		Render2D.text(context, bindingModule == module ? "Press key" : keyName(module.getKeybind()), x + 94, boxY + 22, TEXT);
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
