package dev.rdh.cera.props;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.resource.loader.api.resource.Resource;

import net.minecraft.resource.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Wrapper for a {@link Properties} object with typed parsers for OptiFine's types.
 * @param id the location of the resource this properties was loaded from
 * @param properties the backing map of properties
 */
public record Props(NamespacedIdentifier id, Properties properties) {

	public Props(Resource resource) throws IOException {
		this(resource.location(), load(resource));
	}

	private static Properties load(Resource resource) throws IOException {
		Properties p = new Properties();
		try (resource; InputStream stream = resource.open()) {
			p.load(stream);
		}
		return p;
	}

	public boolean contains(String key) {
		return properties.containsKey(key);
	}

	public String get(String key) {
		return properties.getProperty(key);
	}

	public String get(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	public Result<Integer> getInt(String key, int defaultValue) {
		String value = properties.getProperty(key);
		if (value == null) {
			return Result.success(defaultValue);
		}
		try {
			return Result.success(Integer.parseInt(value.trim()));
		} catch (NumberFormatException e) {
			return Result.failure("Failed to parse int from property " + key + "=" + value + " (" + this.id + ")");
		}
	}

	public Result<Float> getFloat(String key, float defaultValue) {
		String value = properties.getProperty(key);
		if (value == null) {
			return Result.success(defaultValue);
		}
		try {
			return Result.success(Float.parseFloat(value.trim()));
		} catch (NumberFormatException e) {
			return Result.failure("Failed to parse float from property " + key + "=" + value + " (" + this.id + ")");
		}
	}

	public Result<Boolean> getBoolean(String key, boolean defaultValue) {
		String value = properties.getProperty(key);
		if (value == null) {
			return Result.success(defaultValue);
		}
		String trimmed = value.trim();
		if (trimmed.equalsIgnoreCase("true")) {
			return Result.success(true);
		} else if (trimmed.equalsIgnoreCase("false")) {
			return Result.success(false);
		} else {
			return Result.failure("Failed to parse boolean from property " + key + "=" + value + " (" + this.id + ")");
		}
	}

	public Result<Integer> getColor(String key) {
		String value = properties.getProperty(key);
		if (value == null || value.isBlank()) {
			return Result.failure("Missing color property: " + key);
		}
		return parseColor(key, value);
	}

	public Result<Integer> getColor(String key, int defaultValue) {
		String value = properties.getProperty(key);
		if (value == null || value.isBlank()) {
			return Result.success(defaultValue);
		}
		return parseColor(key, value);
	}

	private Result<Integer> parseColor(String key, String value) {
		String hex = value.trim();
		if (hex.startsWith("#")) {
			hex = hex.substring(1);
		} else if (hex.startsWith("0x") || hex.startsWith("0X")) {
			hex = hex.substring(2);
		}
		try {
			return Result.success(Integer.parseInt(hex, 16));
		} catch (NumberFormatException e) {
			return Result.failure("Failed to parse color from property " + key + "=" + value + " (" + this.id + ")");
		}
	}

	public Result<BlendMethod> getBlendMethod(String key, BlendMethod defaultValue) {
		String value = properties.getProperty(key);
		if (value == null || value.isBlank()) {
			return Result.success(defaultValue);
		}
		BlendMethod method = BlendMethod.byName(value.trim());
		if (method == null) {
			return Result.failure("Failed to parse blend method from property " + key + "=" + value + " (" + this.id + ")");
		}
		return Result.success(method);
	}

	public Result<List<Identifier>> getIdentifiers(String key) {
		String value = properties.getProperty(key);
		if (value == null || value.isBlank()) {
			return Result.success(List.of());
		}
		
		List<Identifier> ids = new ArrayList<>();
		for (String part : value.split("\\s+")) {
			if (part.isEmpty()) continue;
			ids.add(parseId(part, this.id));
		}
		return Result.success(List.copyOf(ids));
	}

	public Result<NumberList> getNumberList(String key) {
		String value = properties.getProperty(key);
		if (value == null) {
			return Result.success(null);
		}
		return NumberList.parse(value);
	}

	public static Identifier parseId(String value, NamespacedIdentifier current) {
		String path = current.identifier();
		int lastSlash = path.lastIndexOf('/');
		String dir = lastSlash == -1 ? "" : path.substring(0, lastSlash + 1);

		if (value.startsWith("~/")) {
			int firstSlash = path.indexOf('/');
			String root = firstSlash == -1 ? path : path.substring(0, firstSlash);
			return new Identifier(current.namespace(), root + "/" + value.substring(2));
		} else if (value.startsWith("./")) {
			return new Identifier(current.namespace(), dir + value.substring(2));
		} else if (!value.contains(":") && !value.contains("/")) {
			return new Identifier(current.namespace(), dir + value);
		}
		return new Identifier(value);
	}
}
