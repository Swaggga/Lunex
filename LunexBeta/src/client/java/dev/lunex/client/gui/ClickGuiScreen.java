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
	private static final int GUI_SIZE = 540;
	private static final int SIDEBAR_WIDTH = 86;
	private static final int HEADER_HEIGHT = 58;
	private static final int MODULE_WIDTH = 196;
	private static final int MODULE_BASE_HEIGHT = 60;
	private static final int MODULE_SETTING_HEIGHT = 18;
	private static final int MODULE_GAP = 12;
	private static final int BACKDROP = 0x66000000;
	private static final int WINDOW = 0xF5050507;
	private static final int PANEL = 0xFF0B0B0F;
	private static final int CARD = 0xFF111117;
	private static final int CARD_HOVER = 0xFF181820;
	private static final int OUTLINE = 0xFF24242B;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int MUTED = 0xFFB5B5BE;
	private static final int DIM = 0xFF707079;
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
			closeAnimation.animate(0.0F, 0.22F);
			if (closeAnimation.get() <= 0.01F) {
				client.setScreen(null);
				return;
			}
		} else {
			openAnimation.animate(1.0F, 0.18F);
		}

		float open = closing ? closeAnimation.get() : Render2D.clamp(openAnimation.get(), 0.0F, 1.0F);
		tabAnimation.animate(categoryIndex(selectedCategory), 0.18F);
		searchAnimation.animate(searchFocused ? 1.0F : 0.0F, 0.2F);
		targetScroll = clampScroll(targetScroll);
		scrollAnimation.animate(targetScroll, 0.24F);

		context.fill(0, 0, width, height, Render2D.alpha(BACKDROP, (int) (102 * open)));
		int x = (width - GUI_SIZE) / 2;
		int y = (height - GUI_SIZE) / 2;
		float eased = Animation.easeOutBack(open);
		MatrixStack matrices = context.getMatrices();
		matrices.push();
		matrices.translate(x + GUI_SIZE / 2.0F, y + GUI_SIZE / 2.0F, 0.0F);
		matrices.scale(0.84F + eased * 0.16F, 0.84F + eased * 0.16F, 1.0F);
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

		if (button == 0 && Render2D.hovered(mouseX, mouseY, x + SIDEBAR_WIDTH + 18, y + 18, 170, 28)) {
			searchFocused = true;
			bindingModule = null;
			return true;
		}
		searchFocused = false;

		int categoryY = y + 78;
		for (Category category : Category.values()) {
			if (Render2D.hovered(mouseX, mouseY, x + 14, categoryY, 58, 44)) {
				selectedCategory = category;
				targetScroll = 0;
				expandedModule = null;
				bindingModule = null;
				return true;
			}
			categoryY += 52;
		}

		int contentX = x + SIDEBAR_WIDTH + 18;
		int contentY = y + HEADER_HEIGHT + Math.round(scrollAnimation.get());
		List<Module> modules = filteredModules();
		for (int index = 0; index < modules.size(); index++) {
			Module module = modules.get(index);
			int column = index % 2;
			int itemX = contentX + column * (MODULE_WIDTH + MODULE_GAP);
			if (column == 0 && index > 0) {
				contentY += maxPairHeight(modules, index - 2) + MODULE_GAP;
			}
			int height = moduleHeight(module);
			if (contentY + height < y + HEADER_HEIGHT || contentY > y + GUI_SIZE - 14) {
				continue;
			}
			if (Render2D.hovered(mouseX, mouseY, itemX, contentY, MODULE_WIDTH, height)) {
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

		targetScroll = clampScroll(targetScroll + (int) (verticalAmount * 38.0D));
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
		if (searchFocused && chr >= 32 && chr <= 126 && Render2D.width(searchText + chr) < 148) {
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
		Render2D.roundedGlow(context, x, y, GUI_SIZE, GUI_SIZE, 16, 0xFF000000, 7);
		Render2D.roundedRect(context, x, y, GUI_SIZE, GUI_SIZE, 16, WINDOW);
		Render2D.roundedBorder(context, x, y, GUI_SIZE, GUI_SIZE, 16, 0x50FFFFFF);
		Render2D.roundedRect(context, x + SIDEBAR_WIDTH, y + 12, 1, GUI_SIZE - 24, 0, OUTLINE);
		Render2D.roundedRect(context, x + SIDEBAR_WIDTH + 16, y + HEADER_HEIGHT - 7, GUI_SIZE - SIDEBAR_WIDTH - 32, 1, 0, OUTLINE);
	}

	private void renderSidebar(DrawContext context, int mouseX, int mouseY, int x, int y) {
		Render2D.roundedRect(context, x + 12, y + 12, 62, 48, 13, PANEL);
		Render2D.roundedBorder(context, x + 12, y + 12, 62, 48, 13, 0x36FFFFFF);
		Render2D.centeredText(context, "L", x + 43, y + 22, ACCENT_2);
		Render2D.text(context, Lunex.NAME, x + 24, y + 42, MUTED);

		int indicatorY = y + 78 + Math.round(tabAnimation.get() * 52.0F);
		Render2D.roundedGlow(context, x + 14, indicatorY, 58, 44, 12, ACCENT, 3);
		int categoryY = y + 78;
		for (Category category : Category.values()) {
			boolean selected = selectedCategory == category;
			boolean hovered = Render2D.hovered(mouseX, mouseY, x + 14, categoryY, 58, 44);
			float hover = animation("tab:" + category.name(), hovered || selected, 0.2F);
			int fill = selected ? Render2D.lerpColor(Render2D.alpha(ACCENT, 150), Render2D.alpha(ACCENT_2, 120), pulse(category.ordinal() * 0.4F)) : Render2D.lerpColor(PANEL, CARD_HOVER, hover);
			Render2D.roundedRect(context, x + 14, categoryY, 58, 44, 12, fill);
			Render2D.roundedBorder(context, x + 14, categoryY, 58, 44, 12, selected ? Render2D.alpha(TEXT, 110) : 0x32FFFFFF);
			Render2D.centeredText(context, category.getIcon(), x + 43, categoryY + 8, selected ? TEXT : Render2D.lerpColor(MUTED, TEXT, hover));
			Render2D.centeredText(context, shortName(category), x + 43, categoryY + 27, selected ? TEXT : MUTED);
			categoryY += 52;
		}
	}

	private void renderContent(DrawContext context, int mouseX, int mouseY, int x, int y) {
		renderSearch(context, x + 18, y + 18, mouseX, mouseY);
		Render2D.scaledText(context, selectedCategory.getTitle(), x + 206, y + 17, 1.18F, TEXT);
		List<Module> modules = filteredModules();
		long active = modules.stream().filter(Module::isEnabled).count();
		Render2D.text(context, active + "/" + modules.size() + " enabled", x + 208, y + 39, MUTED);

		context.enableScissor(x + 12, y + HEADER_HEIGHT, x + GUI_SIZE - SIDEBAR_WIDTH - 8, y + GUI_SIZE - 12);
		int moduleX = x + 18;
		int moduleY = y + HEADER_HEIGHT + Math.round(scrollAnimation.get());
		for (int index = 0; index < modules.size(); index++) {
			Module module = modules.get(index);
			int column = index % 2;
			int itemX = moduleX + column * (MODULE_WIDTH + MODULE_GAP);
			if (column == 0 && index > 0) {
				moduleY += maxPairHeight(modules, index - 2) + MODULE_GAP;
			}
			if (moduleY + moduleHeight(module) >= y + HEADER_HEIGHT && moduleY <= y + GUI_SIZE - 12) {
				renderModule(context, mouseX, mouseY, module, itemX, moduleY, MODULE_WIDTH);
			}
		}
		context.disableScissor();
		renderScrollBar(context, x + GUI_SIZE - SIDEBAR_WIDTH - 16, y + HEADER_HEIGHT, GUI_SIZE - HEADER_HEIGHT - 22);
	}

	private void renderSearch(DrawContext context, int x, int y, int mouseX, int mouseY) {
		boolean hovered = Render2D.hovered(mouseX, mouseY, x, y, 170, 28);
		float active = Math.max(searchAnimation.get(), animation("search", hovered, 0.2F));
		Render2D.roundedRect(context, x, y, 170, 28, 9, Render2D.lerpColor(PANEL, CARD_HOVER, active));
		Render2D.roundedBorder(context, x, y, 170, 28, 9, Render2D.alpha(ACCENT, (int) (44 + active * 90)));
		Render2D.text(context, searchText.isEmpty() && !searchFocused ? "Search modules..." : searchText, x + 11, y + 10, searchText.isEmpty() && !searchFocused ? DIM : TEXT);
		if (searchFocused && ticks / 12 % 2 == 0) {
			Render2D.rect(context, x + 12 + Render2D.width(searchText), y + 7, 1, 14, TEXT);
		}
	}

	private void renderModule(DrawContext context, int mouseX, int mouseY, Module module, int x, int y, int width) {
		int height = moduleHeight(module);
		boolean hovered = Render2D.hovered(mouseX, mouseY, x, y, width, height);
		float hover = animation("module:hover:" + module.getId(), hovered, 0.18F);
		float enabled = animation("module:state:" + module.getId(), module.isEnabled(), 0.22F);
		int drawY = y - Math.round(hover * 3.0F);
		int fill = Render2D.lerpColor(Render2D.lerpColor(CARD, CARD_HOVER, hover), Render2D.alpha(ACCENT, 120), enabled * 0.55F);
		Render2D.roundedGlow(context, x - 2, drawY - 2, width + 4, height + 4, 10, module.isEnabled() ? ACCENT : 0xFF000000, module.isEnabled() ? 3 : 2);
		Render2D.roundedRect(context, x, drawY, width, height, 10, fill);
		Render2D.roundedBorder(context, x, drawY, width, height, 10, Render2D.lerpColor(0x42FFFFFF, Render2D.alpha(ACTIVE, 150), enabled));
		Render2D.text(context, Render2D.trimToWidth(module.getName(), width - 66), x + 10, drawY + 10, module.isEnabled() ? TEXT : MUTED);
		Render2D.text(context, Render2D.trimToWidth(module.getDescription(), width - 20), x + 10, drawY + 27, module.isEnabled() ? 0xFFE0E0E8 : DIM);
		renderBindBadge(context, module, x + width - 56, drawY + 9);
		Render2D.roundedRect(context, x + 10, drawY + 45, width - 20, 4, 2, 0xFF09090C);
		Render2D.roundedRect(context, x + 10, drawY + 45, Math.round((width - 20) * Math.max(0.08F, enabled)), 4, 2, module.isEnabled() ? ACTIVE : DIM);
		if (expandedModule == module) {
			renderSettings(context, module, x, drawY + MODULE_BASE_HEIGHT, width);
		}
	}

	private void renderBindBadge(DrawContext context, Module module, int x, int y) {
		boolean binding = bindingModule == module;
		Render2D.roundedRect(context, x, y, 46, 18, 7, binding ? Render2D.alpha(ACCENT_2, 120) : PANEL);
		Render2D.roundedBorder(context, x, y, 46, 18, 7, binding ? Render2D.alpha(TEXT, 100) : 0x36FFFFFF);
		Render2D.centeredText(context, binding ? "..." : Render2D.trimToWidth(keyName(module.getKeybind()), 34), x + 23, y + 5, binding ? TEXT : MUTED);
	}

	private void renderSettings(DrawContext context, Module module, int x, int y, int width) {
		Module.Setting[] settings = module.getSettings();
		if (settings.length == 0) {
			Render2D.text(context, "No settings", x + 10, y + 8, DIM);
			return;
		}
		for (int index = 0; index < settings.length; index++) {
			Module.Setting setting = settings[index];
			int settingY = y + 6 + index * MODULE_SETTING_HEIGHT;
			Render2D.roundedRect(context, x + 10, settingY, width - 20, 14, 5, PANEL);
			Render2D.text(context, Render2D.trimToWidth(setting.name(), 72), x + 16, settingY + 4, DIM);
			Render2D.text(context, Render2D.trimToWidth(setting.value(), 76), x + width - 88, settingY + 4, MUTED);
		}
	}

	private void renderScrollBar(DrawContext context, int x, int y, int height) {
		int content = contentHeight();
		if (content <= height) {
			return;
		}
		int thumbHeight = Math.max(24, height * height / content);
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

	private String shortName(Category category) {
		String title = category.getTitle();
		return title.length() <= 6 ? title : title.substring(0, 6);
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
		return MODULE_BASE_HEIGHT + Math.max(1, module.getSettings().length) * MODULE_SETTING_HEIGHT + 12;
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
		int viewHeight = GUI_SIZE - HEADER_HEIGHT - 22;
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
