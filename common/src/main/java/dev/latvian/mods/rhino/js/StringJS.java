package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;

public class StringJS extends ObjectJS {
	public static final PrototypeJS PROTOTYPE = PrototypeJS.create("String")
			.constructor(StringJS::construct)
			.property("length", StringJS::length)
			.function("trim", StringJS::trim);

	public static final StringJS EMPTY = new StringJS("");

	public static StringJS of(CharSequence string) {
		return string.isEmpty() ? EMPTY : new StringJS(string);
	}

	private static StringJS construct(ContextJS cx, Object[] args) {
		return args[0] instanceof StringJS s ? s : of(args[0].toString());
	}

	public final CharSequence string;

	private StringJS(CharSequence string) {
		super(PROTOTYPE);
		this.string = string;
	}

	@Override
	public Object unwrap() {
		return string.toString();
	}

	@Override
	public double asNumber() {
		return Double.parseDouble(string.toString());
	}

	@Override
	public String asString() {
		return string.toString();
	}

	private static ObjectJS length(ContextJS cx, ObjectJS self) {
		return NumberJS.of(self.asString().length());
	}

	private static StringJS trim(ContextJS cx, ObjectJS self) {
		return new StringJS(self.asString().trim());
	}
}
