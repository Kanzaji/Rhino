package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;

@FunctionalInterface
public interface StaticPropertyCallback {
	Object get(ContextJS cx);

	record Fixed(Object value) implements StaticPropertyCallback {
		@Override
		public Object get(ContextJS cx) {
			return value;
		}
	}
}