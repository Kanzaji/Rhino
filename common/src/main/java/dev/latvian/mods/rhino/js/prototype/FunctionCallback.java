package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;

@FunctionalInterface
public interface FunctionCallback {
	Object invoke(ContextJS cx, Object self, Object[] args);
}