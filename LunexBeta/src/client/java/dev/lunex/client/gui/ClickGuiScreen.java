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
	private static final int GUI_WIDTH = 514;
	private static final int GUI_HEIGHT = 318;
	private static final int SIDEBAR_WIDTH = 126;
	private static final int PANEL_MARGIN = 14;
	private static final int CARD_HEIGHT = 82;
	private static final int CARD_EXPANDED_HEIGHT = 126;
	private static final int CARD_GAP = 10;
	private static final int CONTENT_TOP = 64;
	private static final int BACKGROUND = 0xF20A0B0F;
	private static final int PANEL = 0xF015171F;
	private static final int PANEL_DARK = 0xF006070A;
	private static final int PANEL_HOVER = 0xFF222632;
	private static final int FIELD = 0xEE101119;
	private static final int ACCENT = 0xFF3C414B;
	private static final int ACCENT_SOFT = 0xFF262A34;
	private static final int TEXT = 0xFFF4F1E8;
	private static final int MUTED = 0xFF9B9FAA;
	private static final int DIM = 0xFF666B76;
	private static final int OFF = 0xFF4C4F58;

	private final ModuleManager moduleManager;
	private final Map<String, Animation> hoverAnimations = new HashMap<>();
	private final Map<String, Animation> toggleAnimations = new HashMap<>();
	private final Animation openAnimation = new Animation(0.0F);
	private final Animation categorySlide = new Animation(0.0F);
	private final Animation scrollAnimation = new Animation(0.0F);
	private Category selectedCategory = Category.COMBAT;
	private int targetScroll;
	private int ticks;
	private Module expandedModule;
	private Module bindingModule;

	public ClickGuiScreen(ModuleManager moduleManager) {
		super(Text.literal("Lunex ClickGUI"));
		this.moduleManager = moduleManager;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		ticks++;
		openAnimation.animate(1.0F, 0.17F);
		targetScroll = clampScroll(targetScroll);
		scrollAnimation.animate(targetScroll, 0.24F);
		categorySlide.animate(categoryIndex(selectedCategory), 0.21F);
		renderBackground(context, mouseX, mouseY, delta);

		float open = Animation.easeOutBack(Render2D.clamp(openAnimation.get(), 0.0F, 1.0F));
		int x = (width - GUI_WIDTH) / 2;
		int y = (height - GUI_HEIGHT) / 2;
		MatrixStack matrices = context.getMatrices();
		matrices.push();
		matrices.translate(x + GUI_WIDTH / 2.0F, y + GUI_HEIGHT / 2.0F, 0.0F);
		matrices.scale(0.86F + open * 0.14F, 0.86F + open * 0.14F, 1.0F);
		matrices.translate(-(x + GUI_WIDTH / 2.0F), -(y + GUI_HEIGHT / 2.0F), 0.0F);

		int alpha = (int) (242 * Render2D.clamp(openAnimation.get(), 0.0F, 1.0F));
		Render2D.roundedGlow(context, x, y, GUI_WIDTH, GUI_HEIGHT, 20, ACCENT, 5);
		Render2D.roundedVerticalGradient(context, x, y, GUI_WIDTH, GUI_HEIGHT, 20, Render2D.alpha(BACKGROUND, alpha), Render2D.alpha(PANEL_DARK, alpha));
		Render2D.roundedBorder(context, x, y, GUI_WIDTH, GUI_HEIGHT, 20, Render2D.alpha(0xFFB9BEC8, 70));
		Render2D.roundedRect(context, x + 18, y + 10, GUI_WIDTH - 36, 2, 1, Render2D.alpha(0xFFB9BEC8, 70));

		renderDecor(context, x, y);
		renderSidebar(context, mouseX, mouseY, x, y);
		renderModulePanel(context, mouseX, mouseY, x + SIDEBAR_WIDTH, y + PANEL_MARGIN, GUI_WIDTH - SIDEBAR_WIDTH - PANEL_MARGIN, GUI_HEIGHT - PANEL_MARGIN * 2);
		matrices.pop();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int x = (width - GUI_WIDTH) / 2;
		int y = (height - GUI_HEIGHT) / 2;

		int categoryY = y + 76;
		for (Category category : Category.values()) {
			if (Render2D.hovered(mouseX, mouseY, x + 14, categoryY, 98, 28)) {
				selectedCategory = category;
				targetScroll = 0;
				expandedModule = null;
				bindingModule = null;
				return true;
			}
			categoryY += 34;
		}

		int panelX = x + SIDEBAR_WIDTH;
		int panelY = y + PANEL_MARGIN;
		int panelWidth = GUI_WIDTH - SIDEBAR_WIDTH - PANEL_MARGIN;
		int panelHeight = GUI_HEIGHT - PANEL_MARGIN * 2;
		if (Render2D.hovered(mouseX, mouseY, panelX + panelWidth - 42, panelY + 38, 15, 15)) {
			targetScroll = clampScroll(targetScroll + 42);
			return true;
		}
		if (Render2D.hovered(mouseX, mouseY, panelX + panelWidth - 23, panelY + 38, 15, 15)) {
			targetScroll = clampScroll(targetScroll - 42);
			return true;
		}

		int contentX = panelX + 20;
		int contentY = panelY + CONTENT_TOP + Math.round(scrollAnimation.get());
		int cardWidth = panelWidth - 42;
		for (Module module : moduleManager.getModules(selectedCategory)) {
			int height = cardHeight(module);
			if (contentY + height < panelY + CONTENT_TOP) {
				contentY += height + CARD_GAP;
				continue;
			}
			if (contentY > panelY + panelHeight - 12) {
				break;
			}
			if (Render2D.hovered(mouseX, mouseY, contentX + cardWidth - 62, contentY + 14, 48, 18)) {
				module.toggle();
				return true;
			}
			if (Render2D.hovered(mouseX, mouseY, contentX + 12, contentY + 58, 72, 16)) {
				expandedModule = expandedModule == module ? null : module;
				if (expandedModule != module) {
					bindingModule = null;
				}
				return true;
			}
			if (Render2D.hovered(mouseX, mouseY, contentX + cardWidth - 82, contentY + 58, 68, 16)) {
				bindingModule = module;
				expandedModule = module;
				return true;
			}
			contentY += height + CARD_GAP;
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int x = (width - GUI_WIDTH) / 2 + SIDEBAR_WIDTH;
		int y = (height - GUI_HEIGHT) / 2 + PANEL_MARGIN;
		if (!Render2D.hovered(mouseX, mouseY, x, y, GUI_WIDTH - SIDEBAR_WIDTH - PANEL_MARGIN, GUI_HEIGHT - PANEL_MARGIN * 2)) {
			return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		}

		targetScroll = clampScroll(targetScroll + (int) (verticalAmount * 32.0D));
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (bindingModule != null && isBindableKey(keyCode)) {
			bindingModule.setKeybind(keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE ? 0 : keyCode);
			bindingModule = null;
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private void renderDecor(DrawContext context, int x, int y) {
		float pulse = pulse();
		Render2D.roundedRect(context, x + GUI_WIDTH - 78, y + 18, 42, 42, 21, Render2D.alpha(ACCENT, (int) (8 + pulse * 10)));
		Render2D.roundedBorder(context, x + GUI_WIDTH - 72, y + 24, 30, 30, 15, Render2D.alpha(0xFFB9BEC8, 32));
		Render2D.roundedRect(context, x + 166, y + GUI_HEIGHT - 36, 78, 4, 2, Render2D.alpha(0xFFB9BEC8, 10));
		Render2D.roundedRect(context, x + 254, y + GUI_HEIGHT - 38, 42, 4, 2, Render2D.alpha(0xFFB9BEC8, 8));
	}

	private void renderSidebar(DrawContext context, int mouseX, int mouseY, int x, int y) {
		Render2D.roundedRect(context, x + 8, y + 10, SIDEBAR_WIDTH - 16, GUI_HEIGHT - 20, 17, 0xEA11131A);
		Render2D.roundedBorder(context, x + 8, y + 10, SIDEBAR_WIDTH - 16, GUI_HEIGHT - 20, 17, 0x24FFFFFF);
		Render2D.roundedGlow(context, x + 19, y + 20, 28, 28, 10, ACCENT, 4);
		Render2D.roundedRect(context, x + 18, y + 19, 30, 30, 10, 0xFF2C3038);
		Render2D.centeredText(context, "L", x + 33, y + 29, TEXT);
		Render2D.scaledText(context, Lunex.NAME, x + 56, y + 21, 1.12F, TEXT);
		Render2D.roundedRect(context, x + 18, y + 60, 96, 2, 1, Render2D.alpha(0xFFB9BEC8, 54));

		int indicatorY = y + 76 + Math.round(categorySlide.get() * 34.0F);
		Render2D.roundedGlow(context, x + 14, indicatorY, 98, 28, 11, ACCENT, 2);
		Render2D.roundedRect(context, x + 14, indicatorY, 98, 28, 11, 0xFF323640);

		int categoryY = y + 76;
		for (Category category : Category.values()) {
			boolean selected = category == selectedCategory;
			boolean hovered = Render2D.hovered(mouseX, mouseY, x + 14, categoryY, 98, 28);
			float hover = animation("category:" + category.name(), hovered || selected, 0.18F);
			if (!selected && hover > 0.02F) {
				Render2D.roundedRect(context, x + 14, categoryY, 98, 28, 11, Render2D.alpha(PANEL_HOVER, (int) (122 * hover)));
			}

			int color = selected ? TEXT : Render2D.lerpColor(MUTED, TEXT, hover);
			Render2D.roundedRect(context, x + 26 + Math.round(2.0F * hover), categoryY + 12, 5, 5, 2, selected ? TEXT : Render2D.alpha(color, 150));
			Render2D.text(context, category.getTitle(), x + 42 + Math.round(2.0F * hover), categoryY + 9, color);
			categoryY += 34;
		}
	}

	private void renderModulePanel(DrawContext context, int mouseX, int mouseY, int x, int y, int panelWidth, int panelHeight) {
		Render2D.roundedRect(context, x, y, panelWidth, panelHeight, 18, 0x86101017);
		Render2D.roundedBorder(context, x, y, panelWidth, panelHeight, 18, 0x28FFFFFF);
		String title = selectedCategory.getTitle() + " Modules";
		Render2D.text(context, title, x + 20, y + 18, TEXT);
		renderSearch(context, x + panelWidth - 154, y + 13);
		renderScrollControls(context, x + panelWidth - 42, y + 38);
		renderCategoryStats(context, x + 20, y + 40);

		context.enableScissor(x + 12, y + 60, x + panelWidth - 10, y + panelHeight - 12);
		int cardWidth = panelWidth - 42;
		int moduleX = x + 20;
		int moduleY = y + CONTENT_TOP + Math.round(scrollAnimation.get());
		int index = 0;
		for (Module module : moduleManager.getModules(selectedCategory)) {
			float delay = Render2D.clamp(openAnimation.get() + index * 0.055F, 0.0F, 1.0F);
			int animatedY = moduleY + Math.round((1.0F - Animation.easeOutCubic(delay)) * 16.0F);
			renderModuleCard(context, mouseX, mouseY, module, moduleX, animatedY, cardWidth, index);
			moduleY += cardHeight(module) + CARD_GAP;
			index++;
		}
		context.disableScissor();
	}

	private void renderSearch(DrawContext context, int x, int y) {
		float shine = pulse(1.7F);
		Render2D.roundedGlow(context, x, y, 132, 28, 11, ACCENT, 1);
		Render2D.roundedRect(context, x, y, 132, 28, 11, FIELD);
		Render2D.roundedBorder(context, x, y, 132, 28, 11, Render2D.alpha(ACCENT, 58));
		Render2D.text(context, "Search", x + 15, y + 10, DIM);
		Render2D.roundedRect(context, x + 108, y + 11, 8, 6, 3, Render2D.alpha(ACCENT, (int) (70 + shine * 35)));
	}

	private void renderScrollControls(DrawContext context, int x, int y) {
		Render2D.roundedRect(context, x, y, 15, 15, 5, FIELD);
		Render2D.roundedRect(context, x + 19, y, 15, 15, 5, FIELD);
		Render2D.centeredText(context, "^", x + 7, y + 3, MUTED);
		Render2D.centeredText(context, "v", x + 26, y + 3, MUTED);
	}

	private void renderCategoryStats(DrawContext context, int x, int y) {
		List<Module> modules = moduleManager.getModules(selectedCategory);
		long active = modules.stream().filter(Module::isEnabled).count();
		String label = active + "/" + modules.size() + " enabled";
		Render2D.roundedRect(context, x, y, 94, 18, 8, 0xA60D0D14);
		Render2D.roundedBorder(context, x, y, 94, 18, 8, Render2D.alpha(ACCENT, 42));
		Render2D.text(context, label, x + 10, y + 5, MUTED);
	}

	private void renderModuleCard(DrawContext context, int mouseX, int mouseY, Module module, int x, int y, int width, int index) {
		int height = cardHeight(module);
		boolean hovered = Render2D.hovered(mouseX, mouseY, x, y, width, height);
		float hover = animation("hover:" + module.getId(), hovered, 0.2F);
		float enabled = animation("toggle:" + module.getId(), module.isEnabled(), 0.18F);
		float wave = module.isEnabled() ? pulse(index * 0.45F) : hover;
		int top = Render2D.lerpColor(Render2D.lerpColor(PANEL, PANEL_HOVER, hover), 0xFF242832, enabled * 0.42F);
		int bottom = Render2D.lerpColor(PANEL_DARK, 0xFF14171D, enabled * 0.55F);
		int drawY = y - Math.round(hover * 3.0F);
		int glowColor = Render2D.alpha(ACCENT, (int) (135 + wave * 40));
		Render2D.roundedGlow(context, x, drawY, width, height, 15, glowColor, (int) (hover * 2.0F + enabled * 3.0F));
		Render2D.roundedVerticalGradient(context, x, drawY, width, height, 15, top, bottom);
		Render2D.roundedBorder(context, x, drawY, width, height, 15, Render2D.alpha(TEXT, (int) (28 + enabled * 70 + hover * 18)));

		Render2D.text(context, module.getName(), x + 14, drawY + 13, TEXT);
		Render2D.text(context, Render2D.trimToWidth(module.getDescription(), width - 120), x + 14, drawY + 30, Render2D.lerpColor(OFF, 0xFFE9E8F5, enabled));
		renderStateButton(context, x + width - 62, drawY + 14, module, enabled);
		Render2D.text(context, "Key " + keyName(module.getKeybind()), x + 14, drawY + 48, MUTED);
		renderSmallButton(context, expandedModule == module ? "Hide" : "Settings", x + 14, drawY + 58, 72, expandedModule == module);
		renderSmallButton(context, bindingModule == module ? "..." : "Bind", x + width - 82, drawY + 58, 68, bindingModule == module);

		if (expandedModule == module) {
			renderSettings(context, module, x, drawY, width, height);
		}
	}

	private void renderStateButton(DrawContext context, int x, int y, Module module, float enabled) {
		int color = module.isEnabled() ? 0xFF303642 : 0xFF191B22;
		Render2D.roundedRect(context, x, y, 48, 18, 8, Render2D.lerpColor(color, ACCENT_SOFT, enabled));
		Render2D.roundedBorder(context, x, y, 48, 18, 8, Render2D.alpha(TEXT, (int) (24 + enabled * 70)));
		Render2D.centeredText(context, module.isEnabled() ? "ON" : "OFF", x + 24, y + 5, module.isEnabled() ? TEXT : MUTED);
	}

	private void renderSmallButton(DrawContext context, String text, int x, int y, int width, boolean active) {
		Render2D.roundedRect(context, x, y, width, 16, 6, active ? 0xFF303640 : 0xFF171922);
		Render2D.roundedBorder(context, x, y, width, 16, 6, Render2D.alpha(0xFFB9BEC8, active ? 74 : 34));
		Render2D.centeredText(context, text, x + width / 2, y + 4, active ? TEXT : MUTED);
	}

	private void renderSettings(DrawContext context, Module module, int x, int y, int width, int height) {
		int settingsY = y + 82;
		Render2D.roundedRect(context, x + 10, settingsY, width - 20, 36, 9, 0xA00B0D12);
		Render2D.text(context, "State", x + 22, settingsY + 8, DIM);
		Render2D.text(context, module.isEnabled() ? "Enabled" : "Disabled", x + 72, settingsY + 8, MUTED);
		Render2D.text(context, "Bind", x + 22, settingsY + 22, DIM);
		Render2D.text(context, bindingModule == module ? "..." : keyName(module.getKeybind()), x + 72, settingsY + 22, TEXT);
		Render2D.text(context, "LMB Bind, press key", x + width - 140, settingsY + 15, MUTED);
	}

	private float animation(String id, boolean target, float speed) {
		Animation animation = id.startsWith("toggle:") ? toggleAnimations.computeIfAbsent(id, key -> new Animation(0.0F)) : hoverAnimations.computeIfAbsent(id, key -> new Animation(0.0F));
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

	private int cardHeight(Module module) {
		return expandedModule == module ? CARD_EXPANDED_HEIGHT : CARD_HEIGHT;
	}

	private int contentHeight() {
		int height = 0;
		List<Module> modules = moduleManager.getModules(selectedCategory);
		for (Module module : modules) {
			height += cardHeight(module) + CARD_GAP;
		}
		return Math.max(0, height - CARD_GAP);
	}

	private int clampScroll(int scroll) {
		int panelHeight = GUI_HEIGHT - PANEL_MARGIN * 2;
		int viewHeight = panelHeight - CONTENT_TOP - 12;
		int min = -Math.max(0, contentHeight() - viewHeight);
		return Math.min(0, Math.max(scroll, min));
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
