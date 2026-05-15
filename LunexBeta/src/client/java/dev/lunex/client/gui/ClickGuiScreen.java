package dev.lunex.client.gui;

import dev.lunex.client.module.Category;
import dev.lunex.client.module.Module;
import dev.lunex.client.module.ModuleManager;
import dev.lunex.client.render.Render2D;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClickGuiScreen extends Screen {
	private static final int GUI_WIDTH = 560;
	private static final int GUI_HEIGHT = 360;
	private static final int SIDEBAR_WIDTH = 150;
	private static final int MODULE_WIDTH = 142;
	private static final int SETTINGS_WIDTH = 238;
	private static final int GAP = 14;
	private static final int MODULE_CARD_HEIGHT = 54;
	private static final int SETTING_CARD_HEIGHT = 32;
	private static final int BACKDROP = 0x77000000;
	private static final int WINDOW = 0xF309090B;
	private static final int PANEL = 0xF70E0E11;
	private static final int PANEL_LIGHT = 0xFF151518;
	private static final int CARD = 0xF8141417;
	private static final int CARD_HOVER = 0xFF1B1B20;
	private static final int CARD_ACTIVE = 0xFF181A22;
	private static final int OUTLINE = 0x18FFFFFF;
	private static final int TEXT = 0xFFF4F4F6;
	private static final int MUTED = 0xFFB3B3B9;
	private static final int DIM = 0xFF76767D;
	private static final int ACCENT = 0xFF8B95FF;

	private final ModuleManager moduleManager;
	private final Map<String, Animation> animations = new HashMap<>();
	private final Animation openAnimation = new Animation(0.0F);
	private final Animation closeAnimation = new Animation(1.0F);
	private final Animation moduleScrollAnimation = new Animation(0.0F);
	private final Animation settingsScrollAnimation = new Animation(0.0F);
	private Category selectedCategory = Category.RENDER;
	private Module selectedModule;
	private Module bindingModule;
	private int targetModuleScroll;
	private int targetSettingsScroll;
	private boolean closing;

	public ClickGuiScreen(ModuleManager moduleManager) {
		super(Text.literal("Lunex ClickGUI"));
		this.moduleManager = moduleManager;
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
		List<Module> modules = modulesForSelectedCategory();
		if (selectedModule == null || selectedModule.getCategory() != selectedCategory) {
			selectedModule = modules.isEmpty() ? null : modules.get(0);
		}
		targetModuleScroll = clampModuleScroll(targetModuleScroll);
		targetSettingsScroll = clampSettingsScroll(targetSettingsScroll);
		moduleScrollAnimation.animate(targetModuleScroll, 0.16F);
		settingsScrollAnimation.animate(targetSettingsScroll, 0.16F);

		context.fill(0, 0, width, height, Render2D.alpha(BACKDROP, (int) (119 * alpha)));
		int x = (width - GUI_WIDTH) / 2;
		int y = (height - GUI_HEIGHT) / 2;
		float eased = Animation.easeOutBack(alpha);
		MatrixStack matrices = context.getMatrices();
		matrices.push();
		matrices.translate(x + GUI_WIDTH / 2.0F, y + GUI_HEIGHT / 2.0F, 0.0F);
		matrices.scale(0.94F + eased * 0.06F, 0.94F + eased * 0.06F, 1.0F);
		matrices.translate(-(x + GUI_WIDTH / 2.0F), -(y + GUI_HEIGHT / 2.0F), 0.0F);

		renderWindow(context, x, y, alpha);
		renderSidebar(context, mouseX, mouseY, x, y, alpha);
		renderModulePanel(context, mouseX, mouseY, x + SIDEBAR_WIDTH + GAP, y, alpha);
		renderSettingsPanel(context, mouseX, mouseY, x + SIDEBAR_WIDTH + GAP + MODULE_WIDTH + GAP, y, alpha);
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
		if (!Render2D.hovered(mouseX, mouseY, x, y, GUI_WIDTH, GUI_HEIGHT)) {
			return super.mouseClicked(mouseX, mouseY, button);
		}

		int categoryY = y + 66;
		for (Category category : categories()) {
			if (Render2D.hovered(mouseX, mouseY, x + 18, categoryY, SIDEBAR_WIDTH - 36, 34)) {
				selectedCategory = category;
				selectedModule = firstModule(category);
				targetModuleScroll = 0;
				targetSettingsScroll = 0;
				return true;
			}
			categoryY += 34;
		}

		int moduleX = x + SIDEBAR_WIDTH + GAP;
		int moduleY = y + 18 + Math.round(moduleScrollAnimation.get());
		for (Module module : modulesForSelectedCategory()) {
			if (Render2D.hovered(mouseX, mouseY, moduleX, moduleY, MODULE_WIDTH, MODULE_CARD_HEIGHT)) {
				selectedModule = module;
				targetSettingsScroll = 0;
				if (button == 0) {
					module.toggle();
					return true;
				}
				if (button == 1) {
					return true;
				}
				if (button == 2) {
					bindingModule = module;
					return true;
				}
			}
			moduleY += MODULE_CARD_HEIGHT + 10;
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int x = (width - GUI_WIDTH) / 2;
		int y = (height - GUI_HEIGHT) / 2;
		int moduleX = x + SIDEBAR_WIDTH + GAP;
		int settingsX = moduleX + MODULE_WIDTH + GAP;
		if (Render2D.hovered(mouseX, mouseY, moduleX, y + 14, MODULE_WIDTH, GUI_HEIGHT - 28)) {
			targetModuleScroll = clampModuleScroll(targetModuleScroll + (int) (verticalAmount * 34.0D));
			return true;
		}
		if (Render2D.hovered(mouseX, mouseY, settingsX, y + 14, SETTINGS_WIDTH, GUI_HEIGHT - 28)) {
			targetSettingsScroll = clampSettingsScroll(targetSettingsScroll + (int) (verticalAmount * 34.0D));
			return true;
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
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void close() {
		closeWithAnimation();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private void renderWindow(DrawContext context, int x, int y, float alpha) {
		Render2D.roundedGlow(context, x, y, GUI_WIDTH, GUI_HEIGHT, 12, 0xFF000000, 6);
		Render2D.roundedRect(context, x, y, GUI_WIDTH, GUI_HEIGHT, 12, Render2D.alpha(WINDOW, (int) (243 * alpha)));
		Render2D.roundedBorder(context, x, y, GUI_WIDTH, GUI_HEIGHT, 12, Render2D.alpha(0xFFFFFFFF, (int) (22 * alpha)));
		Render2D.roundedRect(context, x + SIDEBAR_WIDTH, y + 12, 1, GUI_HEIGHT - 24, 0, Render2D.alpha(0xFFFFFFFF, (int) (9 * alpha)));
	}

	private void renderSidebar(DrawContext context, int mouseX, int mouseY, int x, int y, float alpha) {
		Render2D.roundedRect(context, x + 14, y + 14, SIDEBAR_WIDTH - 28, 36, 8, Render2D.alpha(PANEL, (int) (247 * alpha)));
		Render2D.roundedBorder(context, x + 14, y + 14, SIDEBAR_WIDTH - 28, 36, 8, Render2D.alpha(OUTLINE, (int) (55 * alpha)));
		Render2D.roundedRect(context, x + 22, y + 24, 10, 10, 5, Render2D.alpha(ACCENT, (int) (255 * alpha)));
		text(context, "Lunex", x + 38, y + 20, TEXT, alpha);
		text(context, "Build 1.0.0", x + 88, y + 20, MUTED, alpha);
		Render2D.roundedRect(context, x + 18, y + 56, SIDEBAR_WIDTH - 36, 1, 0, Render2D.alpha(0xFFFFFFFF, (int) (8 * alpha)));

		int categoryY = y + 66;
		for (Category category : categories()) {
			boolean selected = category == selectedCategory;
			boolean hovered = Render2D.hovered(mouseX, mouseY, x + 18, categoryY, SIDEBAR_WIDTH - 36, 30);
			float active = Math.max(animation("cat:" + category.name(), hovered, 0.18F), selected ? 1.0F : 0.0F);
			int fill = Render2D.lerpColor(0x00000000, PANEL_LIGHT, active);
			if (selected || active > 0.05F) {
				Render2D.roundedRect(context, x + 18, categoryY, SIDEBAR_WIDTH - 36, 30, 7, Render2D.alpha(fill, (int) ((55 + active * 130) * alpha)));
			}
			text(context, categoryIcon(category), x + 26, categoryY + 10, selected ? ACCENT : DIM, alpha);
			text(context, categoryTitle(category), x + 44, categoryY + 10, selected ? TEXT : MUTED, alpha);
			categoryY += 34;
		}
	}

	private void renderModulePanel(DrawContext context, int mouseX, int mouseY, int x, int y, float alpha) {
		context.enableScissor(x - 3, y + 12, x + MODULE_WIDTH + 3, y + GUI_HEIGHT - 12);
		int moduleY = y + 18 + Math.round(moduleScrollAnimation.get());
		for (Module module : modulesForSelectedCategory()) {
			if (moduleY + MODULE_CARD_HEIGHT >= y + 12 && moduleY <= y + GUI_HEIGHT - 12) {
				renderModuleCard(context, mouseX, mouseY, module, x, moduleY, alpha);
			}
			moduleY += MODULE_CARD_HEIGHT + 10;
		}
		context.disableScissor();
		renderScrollbar(context, x + MODULE_WIDTH - 3, y + 18, GUI_HEIGHT - 36, moduleContentHeight(), -moduleScrollAnimation.get(), alpha);
	}

	private void renderModuleCard(DrawContext context, int mouseX, int mouseY, Module module, int x, int y, float alpha) {
		boolean selected = module == selectedModule;
		boolean hovered = Render2D.hovered(mouseX, mouseY, x, y, MODULE_WIDTH, MODULE_CARD_HEIGHT);
		float hover = animation("module:" + module.getId(), hovered || selected, 0.18F);
		int fill = selected ? CARD_ACTIVE : Render2D.lerpColor(CARD, CARD_HOVER, hover);
		Render2D.roundedRect(context, x, y, MODULE_WIDTH, MODULE_CARD_HEIGHT, 7, Render2D.alpha(fill, (int) (248 * alpha)));
		Render2D.roundedBorder(context, x, y, MODULE_WIDTH, MODULE_CARD_HEIGHT, 7, Render2D.alpha(selected ? ACCENT : OUTLINE, (int) ((selected ? 80 : 42) * alpha)));
		Render2D.roundedRect(context, x + 10, y + 14, 8, 8, 4, Render2D.alpha(module.isEnabled() ? ACCENT : 0xFF2D2D34, (int) (255 * alpha)));
		text(context, trim(module.getName(), MODULE_WIDTH - 42), x + 25, y + 10, TEXT, alpha);
		text(context, trim(module.getDescription(), MODULE_WIDTH - 30), x + 12, y + 30, MUTED, alpha * 0.82F);
		Render2D.roundedRect(context, x + MODULE_WIDTH - 18, y + 13, 10, 10, 4, Render2D.alpha(PANEL_LIGHT, (int) (200 * alpha)));
		text(context, module.isEnabled() ? "✓" : "○", x + MODULE_WIDTH - 16, y + 14, module.isEnabled() ? ACCENT : DIM, alpha);
	}

	private void renderSettingsPanel(DrawContext context, int mouseX, int mouseY, int x, int y, float alpha) {
		if (selectedModule == null) {
			return;
		}
		context.enableScissor(x - 3, y + 12, x + SETTINGS_WIDTH + 3, y + GUI_HEIGHT - 12);
		int settingY = y + 16 + Math.round(settingsScrollAnimation.get());
		renderHeaderSetting(context, selectedModule, x, settingY, alpha);
		settingY += SETTING_CARD_HEIGHT + 8;
		int index = 0;
		for (Module.Setting setting : selectedModule.getSettings()) {
			renderSettingCard(context, setting, index++, x, settingY, alpha);
			settingY += SETTING_CARD_HEIGHT + 8;
		}
		renderStaticSetting(context, "Уведомлять об эффектах", true, x, settingY, alpha);
		settingY += SETTING_CARD_HEIGHT + 8;
		renderStaticSetting(context, "Уведомлять о модулях", selectedModule.isEnabled(), x, settingY, alpha);
		context.disableScissor();
		renderScrollbar(context, x + SETTINGS_WIDTH - 3, y + 18, GUI_HEIGHT - 36, settingsContentHeight(), -settingsScrollAnimation.get(), alpha);
	}

	private void renderHeaderSetting(DrawContext context, Module module, int x, int y, float alpha) {
		Render2D.roundedRect(context, x, y, SETTINGS_WIDTH, SETTING_CARD_HEIGHT, 7, Render2D.alpha(CARD, (int) (248 * alpha)));
		Render2D.roundedBorder(context, x, y, SETTINGS_WIDTH, SETTING_CARD_HEIGHT, 7, Render2D.alpha(OUTLINE, (int) (44 * alpha)));
		text(context, "Лого клиента", x + 12, y + 12, TEXT, alpha);
		renderCheck(context, x + SETTINGS_WIDTH - 22, y + 8, module.isEnabled(), alpha);
	}

	private void renderSettingCard(DrawContext context, Module.Setting setting, int index, int x, int y, float alpha) {
		Render2D.roundedRect(context, x, y, SETTINGS_WIDTH, SETTING_CARD_HEIGHT, 7, Render2D.alpha(CARD, (int) (248 * alpha)));
		Render2D.roundedBorder(context, x, y, SETTINGS_WIDTH, SETTING_CARD_HEIGHT, 7, Render2D.alpha(OUTLINE, (int) (44 * alpha)));
		boolean slider = isNumber(setting.value());
		text(context, trim(settingLabel(setting), SETTINGS_WIDTH - 70), x + 12, y + 9, TEXT, alpha);
		if (slider) {
			text(context, setting.value(), x + SETTINGS_WIDTH - 35, y + 9, MUTED, alpha);
			renderSlider(context, x + 12, y + 24, SETTINGS_WIDTH - 36, numberRatio(setting.value(), index), alpha);
		} else {
			renderCheck(context, x + SETTINGS_WIDTH - 22, y + 8, index % 3 != 1, alpha);
		}
	}

	private void renderStaticSetting(DrawContext context, String name, boolean checked, int x, int y, float alpha) {
		Render2D.roundedRect(context, x, y, SETTINGS_WIDTH, SETTING_CARD_HEIGHT, 7, Render2D.alpha(CARD, (int) (248 * alpha)));
		Render2D.roundedBorder(context, x, y, SETTINGS_WIDTH, SETTING_CARD_HEIGHT, 7, Render2D.alpha(OUTLINE, (int) (44 * alpha)));
		text(context, trim(name, SETTINGS_WIDTH - 48), x + 12, y + 11, TEXT, alpha);
		renderCheck(context, x + SETTINGS_WIDTH - 22, y + 8, checked, alpha);
	}

	private void renderCheck(DrawContext context, int x, int y, boolean checked, float alpha) {
		Render2D.roundedRect(context, x, y, 14, 14, 4, Render2D.alpha(checked ? ACCENT : PANEL_LIGHT, (int) (255 * alpha)));
		Render2D.roundedBorder(context, x, y, 14, 14, 4, Render2D.alpha(0xFFFFFFFF, (int) ((checked ? 80 : 24) * alpha)));
		if (checked) {
			text(context, "✓", x + 3, y + 3, TEXT, alpha);
		}
	}

	private void renderSlider(DrawContext context, int x, int y, int width, float ratio, float alpha) {
		Render2D.roundedRect(context, x, y, width, 3, 2, Render2D.alpha(0xFF2B2B31, (int) (255 * alpha)));
		Render2D.roundedRect(context, x, y, Math.max(5, Math.round(width * ratio)), 3, 2, Render2D.alpha(ACCENT, (int) (255 * alpha)));
	}

	private void renderScrollbar(DrawContext context, int x, int y, int height, int contentHeight, float scroll, float alpha) {
		if (contentHeight <= height) {
			return;
		}
		int thumbHeight = Math.max(20, height * height / contentHeight);
		int maxScroll = Math.max(1, contentHeight - height);
		int thumbY = y + Math.round((scroll / maxScroll) * (height - thumbHeight));
		Render2D.roundedRect(context, x, thumbY, 2, thumbHeight, 1, Render2D.alpha(ACCENT, (int) (120 * alpha)));
	}

	private void renderBindOverlay(DrawContext context, float alpha) {
		context.fill(0, 0, width, height, Render2D.alpha(0xFF000000, (int) (125 * alpha)));
		centeredText(context, "Нажмите любую клавишу для бинда", width / 2, height / 2, TEXT, alpha);
	}

	private List<Module> modulesForSelectedCategory() {
		return moduleManager.getModules(selectedCategory);
	}

	private Module firstModule(Category category) {
		List<Module> modules = moduleManager.getModules(category);
		return modules.isEmpty() ? null : modules.get(0);
	}

	private Category[] categories() {
		return new Category[] {Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.PLAYER, Category.MISC};
	}

	private String categoryTitle(Category category) {
		return switch (category) {
			case COMBAT -> "Combat";
			case MOVEMENT -> "Movement";
			case RENDER -> "Render";
			case PLAYER -> "Player";
			default -> "Other";
		};
	}

	private String categoryIcon(Category category) {
		return switch (category) {
			case COMBAT -> "◢";
			case MOVEMENT -> "➜";
			case RENDER -> "◉";
			case PLAYER -> "▣";
			default -> "✣";
		};
	}

	private int moduleContentHeight() {
		return Math.max(0, modulesForSelectedCategory().size() * (MODULE_CARD_HEIGHT + 10) - 10);
	}

	private int settingsContentHeight() {
		int count = selectedModule == null ? 0 : selectedModule.getSettings().length + 3;
		return Math.max(0, count * (SETTING_CARD_HEIGHT + 8) - 8);
	}

	private int clampModuleScroll(int scroll) {
		int max = Math.max(0, moduleContentHeight() - (GUI_HEIGHT - 28));
		return Math.min(0, Math.max(scroll, -max));
	}

	private int clampSettingsScroll(int scroll) {
		int max = Math.max(0, settingsContentHeight() - (GUI_HEIGHT - 28));
		return Math.min(0, Math.max(scroll, -max));
	}

	private String settingLabel(Module.Setting setting) {
		return switch (setting.name().toLowerCase(Locale.ROOT)) {
			case "gamma" -> "Прозрачность";
			case "range" -> "Расстояние";
			case "mode" -> "Режим";
			case "delay" -> "Задержка";
			case "speed" -> "Скорость";
			default -> setting.name();
		};
	}

	private boolean isNumber(String value) {
		try {
			Double.parseDouble(value.replace("%", "").replace("ms", ""));
			return true;
		} catch (NumberFormatException ignored) {
			return false;
		}
	}

	private float numberRatio(String value, int index) {
		try {
			double parsed = Double.parseDouble(value.replace("%", "").replace("ms", ""));
			return (float) Render2D.clamp((float) (parsed / (parsed > 10 ? 100.0D : 10.0D)), 0.08F, 1.0F);
		} catch (NumberFormatException ignored) {
			return 0.35F + index % 4 * 0.12F;
		}
	}

	private float animation(String id, boolean target, float speed) {
		Animation animation = animations.computeIfAbsent(id, key -> new Animation(0.0F));
		animation.animate(target ? 1.0F : 0.0F, speed);
		return animation.get();
	}

	private void closeWithAnimation() {
		if (!closing) {
			closing = true;
			closeAnimation.animate(openAnimation.get(), 1.0F);
		}
	}

	private void text(DrawContext context, String text, int x, int y, int color, float alpha) {
		Render2D.text(context, text, x, y, Render2D.alpha(color, (int) (255 * alpha)), true);
	}

	private void centeredText(DrawContext context, String text, int x, int y, int color, float alpha) {
		Render2D.centeredText(context, text, x, y, Render2D.alpha(color, (int) (255 * alpha)), true);
	}

	private String trim(String text, int maxWidth) {
		return Render2D.trimToWidth(text, maxWidth);
	}

}
