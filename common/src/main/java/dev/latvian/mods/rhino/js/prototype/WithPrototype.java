package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.ContextJS;

@FunctionalInterface
public interface WithPrototype {
	PrototypeJS getPrototype(ContextJS cx);
}
