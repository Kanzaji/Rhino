package dev.latvian.mods.rhino.js.prototype;

@FunctionalInterface
public interface AsString {
	AsString DEFAULT = String::valueOf;

	String asString(Object value);
}
