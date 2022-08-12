package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;

@FunctionalInterface
public interface StaticFunctionCallbackNoArgs extends StaticFunctionCallback {
	Object invoke(ContextJS cx);

	@Override
	default Object invoke(ContextJS cx, Object[] args) {
		return invoke(cx);
	}
}