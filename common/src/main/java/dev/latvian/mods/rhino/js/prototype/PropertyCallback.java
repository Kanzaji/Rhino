package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;

@FunctionalInterface
public interface PropertyCallback {
	Object get(ContextJS cx, Object self);
}