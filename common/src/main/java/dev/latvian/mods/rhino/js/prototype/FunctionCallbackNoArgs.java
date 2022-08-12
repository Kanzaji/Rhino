package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;

@FunctionalInterface
public interface FunctionCallbackNoArgs extends FunctionCallback {
	Object invoke(ContextJS cx, Object self);

	@Override
	default Object invoke(ContextJS cx, Object self, Object[] args) {
		return invoke(cx, self);
	}
}