package dev.lunex.client.render;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;

public final class Render3D {
	private Render3D() {
	}

	public static void outlinedEntityBox(MatrixStack matrices, Camera camera, Entity entity, float tickDelta, int color) {
		Vec3d position = entity.getLerpedPos(tickDelta);
		Vec3d cameraPosition = camera.getPos();
		Box box = entity.getBoundingBox()
				.offset(-entity.getX(), -entity.getY(), -entity.getZ())
				.offset(position)
				.expand(0.04D)
				.offset(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		outlinedBox(matrices, box, color);
	}

	public static void outlinedBox(MatrixStack matrices, Box box, int color) {
		RenderSystem.enableBlend();
		RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
		RenderSystem.lineWidth(2.0F);

		BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
		Matrix4f matrix = matrices.peek().getPositionMatrix();
		line(buffer, matrix, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, color);
		line(buffer, matrix, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, color);
		line(buffer, matrix, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, color);
		line(buffer, matrix, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, color);
		line(buffer, matrix, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, color);
		line(buffer, matrix, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
		line(buffer, matrix, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
		line(buffer, matrix, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, color);
		line(buffer, matrix, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, color);
		line(buffer, matrix, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, color);
		line(buffer, matrix, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, color);
		line(buffer, matrix, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
		BufferRenderer.drawWithGlobalProgram(buffer.end());

		RenderSystem.lineWidth(1.0F);
		RenderSystem.disableBlend();
	}

	private static void line(BufferBuilder buffer, Matrix4f matrix, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
		buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).color(color);
		buffer.vertex(matrix, (float) x2, (float) y2, (float) z2).color(color);
	}
}
