package dev.lunex.client.gui;

public final class Animation {
	private float value;

	public Animation(float initialValue) {
		value = initialValue;
	}

	public void animate(float target, float speed) {
		value += (target - value) * speed;
		if (Math.abs(target - value) < 0.001F) {
			value = target;
		}
	}

	public float get() {
		return value;
	}

	public static float easeOutBack(float progress) {
		float value = progress - 1.0F;
		return 1.0F + value * value * (2.70158F * value + 1.70158F);
	}

	public static float easeOutCubic(float progress) {
		float value = 1.0F - progress;
		return 1.0F - value * value * value;
	}
}
