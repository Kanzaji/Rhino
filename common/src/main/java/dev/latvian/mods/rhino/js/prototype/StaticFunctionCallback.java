package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;

@FunctionalInterface
public interface StaticFunctionCallback {
	Object invoke(ContextJS cx, Object[] args);
}