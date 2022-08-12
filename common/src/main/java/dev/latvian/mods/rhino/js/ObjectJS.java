package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.js.prototype.PrototypeJS;

/**
 * New experimental replacement for {@link dev.latvian.mods.rhino.Scriptable} classes
 */
public class ObjectJS {
	public static PrototypeJS PROTOTYPE = PrototypeJS.DEFAULT.create("Object")
			.staticFunction("getPrototypeOf", ObjectJS::unimpl)
			.staticFunction("setPrototypeOf", ObjectJS::unimpl)
			.staticFunction("keys", ObjectJS::unimpl)
			.staticFunction("entries", ObjectJS::unimpl)
			.staticFunction("values", ObjectJS::unimpl)
			.staticFunction("getOwnPropertyNames", ObjectJS::unimpl)
			.staticFunction("getOwnPropertySymbols", ObjectJS::unimpl)
			.staticFunction("getOwnPropertyDescriptor", ObjectJS::unimpl)
			.staticFunction("defineProperty", ObjectJS::unimpl)
			.staticFunction("isExtensible", ObjectJS::unimpl)
			.staticFunction("preventExtensions", ObjectJS::unimpl)
			.staticFunction("defineProperties", ObjectJS::unimpl)
			.staticFunction("create", ObjectJS::unimpl)
			.staticFunction("isSealed", ObjectJS::unimpl)
			.staticFunction("isFrozen", ObjectJS::unimpl)
			.staticFunction("seal", ObjectJS::unimpl)
			.staticFunction("freeze", ObjectJS::unimpl)
			.staticFunction("assign", ObjectJS::unimpl)
			.staticFunction("is", ObjectJS::unimpl);

	private static String unimpl(ContextJS cx, Object[] args) {
		throw new IllegalStateException("This function is not yet implemented!");
	}

	private static String unimpl(ContextJS cx, Object self) {
		throw new IllegalStateException("This function is not yet implemented!");
	}
}
