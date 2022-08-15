package dev.latvian.mods.rhino.js.prototype;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextJS;

@FunctionalInterface
public interface CastType {
	CastType NONE = (cx, value) -> value;
	CastType WRAP = (cx, value) -> Context.javaToJS(cx.context, cx.getScope(), value);
	CastType UNWRAP = (cx, value) -> Context.jsToJava(cx.context, cx.getScope(), value, null);

	Object cast(ContextJS cx, Object value);
}