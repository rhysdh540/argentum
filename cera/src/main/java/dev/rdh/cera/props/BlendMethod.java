package dev.rdh.cera.props;

/**
 * A texture blending operation, as described in the "Blending methods" section of
 * {@code properties_files.txt}.
 */
public enum BlendMethod {
	REPLACE,
	ALPHA,
	OVERLAY,
	ADD,
	SUBTRACT,
	MULTIPLY,
	DODGE,
	BURN,
	SCREEN;

	public static BlendMethod byName(String name) {
		if (name.equalsIgnoreCase("color")) {
			return OVERLAY;
		}
		for (BlendMethod method : values()) {
			if (method.name().equalsIgnoreCase(name)) {
				return method;
			}
		}
		return null;
	}
}
