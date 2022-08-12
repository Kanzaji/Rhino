package dev.latvian.mods.rhino.js.prototype;

@FunctionalInterface
public interface AsNumber {
	AsNumber DEFAULT = value -> Double.NaN;

	double asNumber(Object value);
}
