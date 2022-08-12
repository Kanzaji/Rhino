package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.js.prototype.PrototypeJS;

public class StringJS {
	public static final PrototypeJS PROTOTYPE = PrototypeJS.DEFAULT.create(TypeJS.STRING, "String")
			.constructor(StringJS::construct)
			.staticFunction("fromCharCode", StringJS::fromCharCode)
			.staticFunction("fromCodePoint", StringJS::fromCodePoint)
			.staticFunction("raw", StringJS::raw)
			.property("length", StringJS::length)
			.property("namespace", StringJS::namespace)
			.property("path", StringJS::path)
			.function("charAt", StringJS::unimpl)
			.function("charCodeAt", StringJS::unimpl)
			.function("indexOf", StringJS::unimpl)
			.function("lastIndexOf", StringJS::unimpl)
			.function("split", StringJS::unimpl)
			.function("substring", StringJS::unimpl)
			.function("toLowerCase", StringJS::unimpl)
			.function("toUpperCase", StringJS::unimpl)
			.function("substr", StringJS::unimpl)
			.function("concat", StringJS::unimpl)
			.function("slice", StringJS::unimpl)
			.function("equalsIgnoreCase", StringJS::unimpl)
			.function("match", StringJS::unimpl)
			.function("search", StringJS::unimpl)
			.function("replace", StringJS::unimpl)
			.function("localeCompare", StringJS::unimpl)
			.function("toLocaleLowerCase", StringJS::unimpl)
			.function("trim", StringJS::unimpl)
			.function("trimLeft", StringJS::unimpl)
			.function("trimRight", StringJS::unimpl)
			.function("includes", StringJS::unimpl)
			.function("startsWith", StringJS::unimpl)
			.function("endsWith", StringJS::unimpl)
			.function("normalize", StringJS::unimpl)
			.function("repeat", StringJS::unimpl)
			.function("codePointAt", StringJS::unimpl)
			.function("padStart", StringJS::unimpl)
			.function("padEnd", StringJS::unimpl)
			.function("trimStart", StringJS::unimpl)
			.function("trimEnd", StringJS::unimpl)
			.asNumber(value -> Double.parseDouble(value.toString()))
			.asBoolean(value -> !value.toString().isEmpty());

	private static String construct(ContextJS cx, Object[] args) {
		return String.valueOf(args[0]);
	}

	private static String fromCharCode(ContextJS cx, Object[] args) {
		int n = args.length;

		if (n < 1) {
			return "";
		}

		char[] chars = new char[n];

		for (int i = 0; i != n; ++i) {
			chars[i] = ScriptRuntime.toUint16(cx.context, args[i]);
		}

		return new String(chars);
	}

	private static String fromCodePoint(ContextJS cx, Object[] args) {
		int n = args.length;

		if (n < 1) {
			return "";
		}

		int[] codePoints = new int[n];

		for (int i = 0; i != n; i++) {
			Object arg = args[i];
			int codePoint = ScriptRuntime.toInt32(cx.context, arg);
			double num = cx.asNumber(arg);

			if (!ScriptRuntime.eqNumber(cx.context, num, codePoint) || !Character.isValidCodePoint(codePoint)) {
				throw ScriptRuntime.rangeError("Invalid code point " + cx.asString(arg));
			}

			codePoints[i] = codePoint;
		}

		return new String(codePoints, 0, n);
	}

	private static String raw(ContextJS cx, Object[] args) {
		return cx.asString(args[0]);
	}

	private static CharSequence asCS(Object value) {
		return value instanceof CharSequence c ? c : value.toString();
	}

	private static int length(ContextJS cx, Object self) {
		return asCS(self).length();
	}

	private static String namespace(ContextJS cx, Object self) {
		String str = cx.asString(self);
		int colon = str.indexOf(':');
		return colon == -1 ? "minecraft" : str.substring(0, colon);
	}

	private static String path(ContextJS cx, Object self) {
		String str = cx.asString(self);
		int colon = str.indexOf(':');
		return colon == -1 ? str : str.substring(colon + 1);
	}

	private static String unimpl(ContextJS cx, Object self) {
		throw new IllegalStateException("This function is not yet implemented!");
	}

	private static String trim(ContextJS cx, Object self) {
		return self.toString().trim();
	}
}
