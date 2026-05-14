package dev.lunex.client.render;

import dev.lunex.Lunex;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class Render2D {
	private static final Identifier LUNEX_FONT = Identifier.of(Lunex.MOD_ID, "lunex");

	private Render2D() {
	}

	public static void rect(DrawContext context, int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + height, color);
	}

	public static void border(DrawContext context, int x, int y, int width, int height, int color) {
		context.drawBorder(x, y, width, height, color);
	}

	public static void roundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
		int roundness = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
		if (roundness == 0) {
			rect(context, x, y, width, height, color);
			return;
		}

		for (int row = 0; row < height; row++) {
			int inset = cornerInset(row, height, roundness);
			context.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
		}
	}

	public static void roundedBorder(DrawContext context, int x, int y, int width, int height, int radius, int color) {
		int roundness = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
		if (roundness == 0) {
			border(context, x, y, width, height, color);
			return;
		}

		for (int row = 0; row < height; row++) {
			int inset = cornerInset(row, height, roundness);
			int left = x + inset;
			int right = x + width - inset - 1;
			if (row == 0 || row == height - 1 || row < roundness || row >= height - roundness) {
				context.fill(left, y + row, right + 1, y + row + 1, color);
			} else {
				context.fill(left, y + row, left + 1, y + row + 1, color);
				context.fill(right, y + row, right + 1, y + row + 1, color);
			}
		}
	}

	public static void glow(DrawContext context, int x, int y, int width, int height, int color, int radius) {
		for (int layer = radius; layer > 0; layer--) {
			int alpha = Math.max(8, 42 - layer * 5);
			border(context, x - layer, y - layer, width + layer * 2, height + layer * 2, alpha(color, alpha));
		}
	}

	public static void roundedGlow(DrawContext context, int x, int y, int width, int height, int roundness, int color, int radius) {
		for (int layer = radius; layer > 0; layer--) {
			int alpha = Math.max(7, 46 - layer * 5);
			roundedBorder(context, x - layer, y - layer, width + layer * 2, height + layer * 2, roundness + layer, alpha(color, alpha));
		}
	}

	public static void horizontalGradient(DrawContext context, int x, int y, int width, int height, int leftColor, int rightColor) {
		for (int offset = 0; offset < width; offset++) {
			float progress = width <= 1 ? 1.0F : offset / (float) (width - 1);
			context.fill(x + offset, y, x + offset + 1, y + height, lerpColor(leftColor, rightColor, progress));
		}
	}

	public static void roundedHorizontalGradient(DrawContext context, int x, int y, int width, int height, int radius, int leftColor, int rightColor) {
		int roundness = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
		if (roundness == 0) {
			horizontalGradient(context, x, y, width, height, leftColor, rightColor);
			return;
		}

		for (int column = 0; column < width; column++) {
			float progress = width <= 1 ? 1.0F : column / (float) (width - 1);
			int color = lerpColor(leftColor, rightColor, progress);
			int topInset = cornerInset(column, width, roundness);
			context.fill(x + column, y + topInset, x + column + 1, y + height - topInset, color);
		}
	}

	public static void verticalGradient(DrawContext context, int x, int y, int width, int height, int topColor, int bottomColor) {
		context.fillGradient(x, y, x + width, y + height, topColor, bottomColor);
	}

	public static void roundedVerticalGradient(DrawContext context, int x, int y, int width, int height, int radius, int topColor, int bottomColor) {
		int roundness = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
		if (roundness == 0) {
			verticalGradient(context, x, y, width, height, topColor, bottomColor);
			return;
		}

		for (int row = 0; row < height; row++) {
			float progress = height <= 1 ? 1.0F : row / (float) (height - 1);
			int inset = cornerInset(row, height, roundness);
			context.fill(x + inset, y + row, x + width - inset, y + row + 1, lerpColor(topColor, bottomColor, progress));
		}
	}

	public static void text(DrawContext context, String text, int x, int y, int color) {
		text(context, text, x, y, color, false);
	}

	public static void text(DrawContext context, String text, int x, int y, int color, boolean shadow) {
		TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
		context.drawText(renderer, lunexText(sanitize(text)), x, y, color, shadow);
	}

	public static void centeredText(DrawContext context, String text, int x, int y, int color) {
		text(context, text, x - width(text) / 2, y, color);
	}

	public static void scaledText(DrawContext context, String text, int x, int y, float scale, int color) {
		MatrixStack matrices = context.getMatrices();
		matrices.push();
		matrices.translate(x, y, 0.0F);
		matrices.scale(scale, scale, 1.0F);
		context.drawText(MinecraftClient.getInstance().textRenderer, lunexText(sanitize(text)), 0, 0, color, false);
		matrices.pop();
	}

	public static int width(String text) {
		return MinecraftClient.getInstance().textRenderer.getWidth(lunexText(sanitize(text)));
	}

	public static String trimToWidth(String text, int maxWidth) {
		String sanitized = sanitize(text);
		String value = sanitized;
		while (!value.isEmpty() && width(value + "...") > maxWidth) {
			value = value.substring(0, value.length() - 1);
		}

		return value.length() == sanitized.length() ? sanitized : value + "...";
	}

	public static void line(DrawContext context, int x1, int y1, int x2, int y2, int color) {
		if (y1 == y2) {
			context.drawHorizontalLine(Math.min(x1, x2), Math.max(x1, x2), y1, color);
			return;
		}

		if (x1 == x2) {
			context.drawVerticalLine(x1, Math.min(y1, y2), Math.max(y1, y2), color);
		}
	}

	public static boolean hovered(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}

	public static int alpha(int color, int alpha) {
		return (alpha << 24) | (color & 0x00FFFFFF);
	}

	public static int lerpColor(int start, int end, float progress) {
		float value = clamp(progress, 0.0F, 1.0F);
		int a = (int) (channel(start, 24) + (channel(end, 24) - channel(start, 24)) * value);
		int r = (int) (channel(start, 16) + (channel(end, 16) - channel(start, 16)) * value);
		int g = (int) (channel(start, 8) + (channel(end, 8) - channel(start, 8)) * value);
		int b = (int) (channel(start, 0) + (channel(end, 0) - channel(start, 0)) * value);
		return a << 24 | r << 16 | g << 8 | b;
	}

	public static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static Text lunexText(String text) {
		return Text.literal(text).styled(style -> style.withFont(LUNEX_FONT));
	}

	private static String sanitize(String text) {
		StringBuilder builder = new StringBuilder(text.length());
		for (int index = 0; index < text.length(); index++) {
			char character = text.charAt(index);
			if ((character >= 32 && character <= 126) || character == '\u2026') {
				builder.append(character);
			} else {
				builder.append('?');
			}
		}

		return builder.toString();
	}

	private static int cornerInset(int position, int length, int radius) {
		if (position < radius) {
			float distance = radius - position - 0.5F;
			return Math.round(radius - (float) Math.sqrt(radius * radius - distance * distance));
		}

		if (position >= length - radius) {
			float distance = position - (length - radius) + 0.5F;
			return Math.round(radius - (float) Math.sqrt(radius * radius - distance * distance));
		}

		return 0;
	}

	private static int channel(int color, int shift) {
		return color >> shift & 0xFF;
	}
}
