package dev.latvian.mods.rhino.js.prototype;

@FunctionalInterface
public interface AsBoolean {
	AsBoolean DEFAULT = value -> true;

	boolean asBoolean(Object value);
}
