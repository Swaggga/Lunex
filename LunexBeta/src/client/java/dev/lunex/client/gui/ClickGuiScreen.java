package dev.lunex.client.gui;

import dev.lunex.Lunex;
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClickGuiScreen extends Screen {
	private static final int GUI_SIZE = 380;
	private static final int SIDEBAR_WIDTH = 92;
	private static final int HEADER_HEIGHT = 46;
	private static final int MODULE_WIDTH = 252;
	private static final int MODULE_BASE_HEIGHT = 50;
	private static final int MODULE_SETTING_HEIGHT = 15;
	private static final int MODULE_GAP = 8;
	private static final int BACKDROP = 0x66000000;
	private static final int WINDOW = 0xF5050507;
	private static final int PANEL = 0xFF0B0B10;
	private static final int CARD = 0xFF111116;
	private static final int CARD_HOVER = 0xFF181820;
	private static final int OUTLINE = 0xFF2B2B32;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int MUTED = 0xFFB9B9C2;
	private static final int DIM = 0xFF74747C;
	private static final int ACCENT = 0xFF8E7BFF;
	private static final int ACCENT_2 = 0xFF45D7FF;
	private static final int ACTIVE = 0xFF58FFB3;

	private final ModuleManager moduleManager;
	private final Map<String, Animation> hoverAnimations = new HashMap<>();
	private final Map<String, Animation> stateAnimations = new HashMap<>();
	private final Animation openAnimation = new Animation(0.0F);
	private final Animation closeAnimation = new Animation(1.0F);
	private final Animation tabAnimation = new Animation(0.0F);
	private final Animation scrollAnimation = new Animation(0.0F);
	private final Animation searchAnimation = new Animation(0.0F);
	private Category selectedCategory = Category.COMBAT;
	private int targetScroll;
	private int ticks;
	private boolean closing;
	private String searchText = "";
	private boolean searchFocused;
	private Module bindingModule;
	private Module expandedModule;

	public ClickGuiScreen(ModuleManager moduleManager) {
		super(Text.literal("Lunex ClickGUI"));
		this.moduleManager = moduleManager;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		ticks++;
		if (closing) {
			closeAnimation.animate(0.0F, 0.24F);
			if (closeAnimation.get() <= 0.01F) {
				client.setScreen(null);
				return;
			}
		} else {
			openAnimation.animate(1.0F, 0.18F);
		}

		float open = closing ? closeAnimation.get() : Render2D.clamp(openAnimation.get(), 0.0F, 1.0F);
		tabAnimation.animate(categoryIndex(selectedCategory), 0.20F);
		searchAnimation.animate(searchFocused ? 1.0F : 0.0F, 0.20F);
		targetScroll = clampScroll(targetScroll);
		scrollAnimation.animate(targetScroll, 0.25F);

		context.fill(0, 0, width, height, Render2D.alpha(BACKDROP, (int) (102 * open)));
		int x = (width - GUI_SIZE) / 2;
		int y = (height - GUI_SIZE) / 2;
		float eased = Animation.easeOutBack(open);
		MatrixStack matrices = context.getMatrices();
		matrices.push();
		matrices.translate(x + GUI_SIZE / 2.0F, y + GUI_SIZE / 2.0F, 0.0F);
		matrices.scale(0.88F + eased * 0.12F, 0.88F + eased * 0.12F, 1.0F);
		matrices.translate(-(x + GUI_SIZE / 2.0F), -(y + GUI_SIZE / 2.0F), 0.0F);

		renderWindow(context, x, y);
		renderSidebar(context, mouseX, mouseY, x, y);
		renderContent(context, mouseX, mouseY, x + SIDEBAR_WIDTH, y);
		matrices.pop();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int x = (width - GUI_SIZE) / 2;
		int y = (height - GUI_SIZE) / 2;
		if (!Render2D.hovered(mouseX, mouseY, x, y, GUI_SIZE, GUI_SIZE)) {
			return super.mouseClicked(mouseX, mouseY, button);
		}

		if (button == 0 && Render2D.hovered(mouseX, mouseY, x + SIDEBAR_WIDTH + 12, y + 12, 120, 22)) {
			searchFocused = true;
			bindingModule = null;
			return true;
		}
		searchFocused = false;

		int categoryY = y + 52;
		for (Category category : Category.values()) {
			if (Render2D.hovered(mouseX, mouseY, x + 8, categoryY, SIDEBAR_WIDTH - 16, 34)) {
				selectedCategory = category;
				targetScroll = 0;
				expandedModule = null;
				bindingModule = null;
				return true;
			}
			categoryY += 40;
		}

		int contentX = x + SIDEBAR_WIDTH + 12;
		int contentY = y + HEADER_HEIGHT + Math.round(scrollAnimation.get());
		List<Module> modules = filteredModules();
		for (int index = 0; index < modules.size(); index++) {
			Module module = modules.get(index);
			if (index > 0) {
				contentY += moduleHeight(modules.get(index - 1)) + MODULE_GAP;
			}
			int height = moduleHeight(module);
			if (contentY + height < y + HEADER_HEIGHT || contentY > y + GUI_SIZE - 10) {
				continue;
			}
			if (Render2D.hovered(mouseX, mouseY, contentX, contentY, MODULE_WIDTH, height)) {
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
					bindingModule = module;
					expandedModule = module;
					return true;
				}
			}
		}

		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int x = (width - GUI_SIZE) / 2 + SIDEBAR_WIDTH;
		int y = (height - GUI_SIZE) / 2 + HEADER_HEIGHT;
		if (!Render2D.hovered(mouseX, mouseY, x, y, GUI_SIZE - SIDEBAR_WIDTH, GUI_SIZE - HEADER_HEIGHT)) {
			return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		}

		targetScroll = clampScroll(targetScroll + (int) (verticalAmount * 34.0D));
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
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			closeWithAnimation();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (searchFocused && chr >= 32 && chr <= 126 && vanillaWidth(searchText + chr) < 100) {
			searchText += chr;
			targetScroll = 0;
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

	private void closeWithAnimation() {
		if (!closing) {
			closing = true;
			closeAnimation.animate(openAnimation.get(), 1.0F);
		}
	}

	private void renderWindow(DrawContext context, int x, int y) {
		Render2D.roundedGlow(context, x, y, GUI_SIZE, GUI_SIZE, 14, 0xFF000000, 6);
		Render2D.roundedRect(context, x, y, GUI_SIZE, GUI_SIZE, 14, WINDOW);
		Render2D.roundedBorder(context, x, y, GUI_SIZE, GUI_SIZE, 14, 0x50FFFFFF);
		Render2D.roundedRect(context, x + SIDEBAR_WIDTH, y + 10, 1, GUI_SIZE - 20, 0, OUTLINE);
		Render2D.roundedRect(context, x + SIDEBAR_WIDTH + 12, y + HEADER_HEIGHT - 6, GUI_SIZE - SIDEBAR_WIDTH - 24, 1, 0, OUTLINE);
	}

	private void renderSidebar(DrawContext context, int mouseX, int mouseY, int x, int y) {
		Render2D.roundedRect(context, x + 8, y + 10, SIDEBAR_WIDTH - 16, 30, 9, PANEL);
		Render2D.roundedBorder(context, x + 8, y + 10, SIDEBAR_WIDTH - 16, 30, 9, 0x36FFFFFF);
		vanillaCenteredText(context, Lunex.NAME, x + SIDEBAR_WIDTH / 2, y + 21, TEXT);

		int indicatorY = y + 52 + Math.round(tabAnimation.get() * 40.0F);
		Render2D.roundedGlow(context, x + 8, indicatorY, SIDEBAR_WIDTH - 16, 34, 9, ACCENT, 2);
		int categoryY = y + 52;
		for (Category category : Category.values()) {
			boolean selected = selectedCategory == category;
			boolean hovered = Render2D.hovered(mouseX, mouseY, x + 8, categoryY, SIDEBAR_WIDTH - 16, 34);
			float hover = animation("tab:" + category.name(), hovered || selected, 0.20F);
			int fill = selected ? Render2D.lerpColor(Render2D.alpha(ACCENT, 150), Render2D.alpha(ACCENT_2, 110), pulse(category.ordinal() * 0.4F)) : Render2D.lerpColor(PANEL, CARD_HOVER, hover);
			Render2D.roundedRect(context, x + 8, categoryY, SIDEBAR_WIDTH - 16, 34, 9, fill);
			Render2D.roundedBorder(context, x + 8, categoryY, SIDEBAR_WIDTH - 16, 34, 9, selected ? Render2D.alpha(TEXT, 95) : 0x32FFFFFF);
			vanillaCenteredText(context, category.getTitle(), x + SIDEBAR_WIDTH / 2, categoryY + 13, selected ? TEXT : Render2D.lerpColor(MUTED, TEXT, hover));
			categoryY += 40;
		}
	}

	private void renderContent(DrawContext context, int mouseX, int mouseY, int x, int y) {
		renderSearch(context, x + 12, y + 12, mouseX, mouseY);
		vanillaText(context, selectedCategory.getTitle(), x + 148, y + 12, TEXT);
		List<Module> modules = filteredModules();
		long active = modules.stream().filter(Module::isEnabled).count();
		vanillaText(context, active + "/" + modules.size() + " on", x + 148, y + 28, MUTED);

		context.enableScissor(x + 9, y + HEADER_HEIGHT, x + GUI_SIZE - SIDEBAR_WIDTH - 8, y + GUI_SIZE - 10);
		int moduleX = x + 12;
		int moduleY = y + HEADER_HEIGHT + Math.round(scrollAnimation.get());
		for (int index = 0; index < modules.size(); index++) {
			Module module = modules.get(index);
			if (index > 0) {
				moduleY += moduleHeight(modules.get(index - 1)) + MODULE_GAP;
			}
			if (moduleY + moduleHeight(module) >= y + HEADER_HEIGHT && moduleY <= y + GUI_SIZE - 10) {
				renderModule(context, mouseX, mouseY, module, moduleX, moduleY, MODULE_WIDTH);
			}
		}
		context.disableScissor();
		renderScrollBar(context, x + GUI_SIZE - SIDEBAR_WIDTH - 14, y + HEADER_HEIGHT, GUI_SIZE - HEADER_HEIGHT - 18);
	}

	private void renderSearch(DrawContext context, int x, int y, int mouseX, int mouseY) {
		boolean hovered = Render2D.hovered(mouseX, mouseY, x, y, 120, 22);
		float active = Math.max(searchAnimation.get(), animation("search", hovered, 0.20F));
		Render2D.roundedRect(context, x, y, 120, 22, 7, Render2D.lerpColor(PANEL, CARD_HOVER, active));
		Render2D.roundedBorder(context, x, y, 120, 22, 7, Render2D.alpha(ACCENT, (int) (44 + active * 90)));
		String value = searchText.isEmpty() && !searchFocused ? "Search..." : searchText;
		vanillaText(context, value, x + 8, y + 7, searchText.isEmpty() && !searchFocused ? DIM : TEXT);
		if (searchFocused && ticks / 12 % 2 == 0) {
			Render2D.rect(context, x + 9 + vanillaWidth(searchText), y + 5, 1, 12, TEXT);
		}
	}

	private void renderModule(DrawContext context, int mouseX, int mouseY, Module module, int x, int y, int width) {
		int height = moduleHeight(module);
		boolean hovered = Render2D.hovered(mouseX, mouseY, x, y, width, height);
		float hover = animation("module:hover:" + module.getId(), hovered, 0.18F);
		float enabled = animation("module:state:" + module.getId(), module.isEnabled(), 0.22F);
		int drawY = y - Math.round(hover * 2.0F);
		int fill = Render2D.lerpColor(Render2D.lerpColor(CARD, CARD_HOVER, hover), Render2D.alpha(ACCENT, 120), enabled * 0.55F);
		Render2D.roundedGlow(context, x - 2, drawY - 2, width + 4, height + 4, 9, module.isEnabled() ? ACCENT : 0xFF000000, module.isEnabled() ? 3 : 2);
		Render2D.roundedRect(context, x, drawY, width, height, 9, fill);
		Render2D.roundedBorder(context, x, drawY, width, height, 9, Render2D.lerpColor(0x42FFFFFF, Render2D.alpha(ACTIVE, 150), enabled));
		vanillaText(context, trimVanilla(module.getName(), width - 66), x + 10, drawY + 8, module.isEnabled() ? TEXT : MUTED);
		vanillaText(context, trimVanilla(module.getDescription(), width - 20), x + 10, drawY + 23, module.isEnabled() ? 0xFFE0E0E8 : DIM);
		renderBindBadge(context, module, x + width - 56, drawY + 8);
		Render2D.roundedRect(context, x + 10, drawY + 41, width - 20, 3, 2, 0xFF09090C);
		Render2D.roundedRect(context, x + 10, drawY + 41, Math.round((width - 20) * Math.max(0.08F, enabled)), 3, 2, module.isEnabled() ? ACTIVE : DIM);
		if (expandedModule == module) {
			renderSettings(context, module, x, drawY + MODULE_BASE_HEIGHT, width);
		}
	}

	private void renderBindBadge(DrawContext context, Module module, int x, int y) {
		boolean binding = bindingModule == module;
		Render2D.roundedRect(context, x, y, 46, 17, 6, binding ? Render2D.alpha(ACCENT_2, 120) : PANEL);
		Render2D.roundedBorder(context, x, y, 46, 17, 6, binding ? Render2D.alpha(TEXT, 100) : 0x36FFFFFF);
		vanillaCenteredText(context, binding ? "..." : trimVanilla(keyName(module.getKeybind()), 34), x + 23, y + 4, binding ? TEXT : MUTED);
	}

	private void renderSettings(DrawContext context, Module module, int x, int y, int width) {
		Module.Setting[] settings = module.getSettings();
		if (settings.length == 0) {
			vanillaText(context, "No settings", x + 10, y + 7, DIM);
			return;
		}
		for (int index = 0; index < settings.length; index++) {
			Module.Setting setting = settings[index];
			int settingY = y + 5 + index * MODULE_SETTING_HEIGHT;
			Render2D.roundedRect(context, x + 10, settingY, width - 20, 13, 4, PANEL);
			vanillaText(context, trimVanilla(setting.name(), 80), x + 16, settingY + 3, DIM);
			vanillaText(context, trimVanilla(setting.value(), 82), x + width - 96, settingY + 3, MUTED);
		}
	}

	private void renderScrollBar(DrawContext context, int x, int y, int height) {
		int content = contentHeight();
		if (content <= height) {
			return;
		}
		int thumbHeight = Math.max(22, height * height / content);
		int maxScroll = Math.max(1, content - height);
		int thumbY = y + Math.round((-scrollAnimation.get() / maxScroll) * (height - thumbHeight));
		Render2D.roundedRect(context, x, y, 3, height, 2, PANEL);
		Render2D.roundedRect(context, x, thumbY, 3, thumbHeight, 2, ACCENT);
	}

	private float animation(String id, boolean target, float speed) {
		Animation animation = id.contains(":state:") ? stateAnimations.computeIfAbsent(id, key -> new Animation(0.0F)) : hoverAnimations.computeIfAbsent(id, key -> new Animation(0.0F));
		animation.animate(target ? 1.0F : 0.0F, speed);
		return animation.get();
	}

	private float pulse(float offset) {
		return (float) ((Math.sin((ticks + offset) * 0.075D) + 1.0D) * 0.5D);
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
		if (expandedModule != module) {
			return MODULE_BASE_HEIGHT;
		}
		return MODULE_BASE_HEIGHT + Math.max(1, module.getSettings().length) * MODULE_SETTING_HEIGHT + 10;
	}

	private int contentHeight() {
		List<Module> modules = filteredModules();
		int height = 0;
		for (Module module : modules) {
			height += moduleHeight(module) + MODULE_GAP;
		}
		return Math.max(0, height - MODULE_GAP);
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

	private int clampScroll(int scroll) {
		int viewHeight = GUI_SIZE - HEADER_HEIGHT - 18;
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
