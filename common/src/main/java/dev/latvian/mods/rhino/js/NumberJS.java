package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.DToA;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.Wrapper;

public class NumberJS extends ObjectJS {
	public static PrototypeJS PROTOTYPE = PrototypeJS.create("Number")
			.constructor(NumberJS::construct)
			.function("toFixed", NumberJS::toFixed);

	public static final NumberJS ZERO = new NumberJS(0.0);

	public static final NumberJS ONE = new NumberJS(1.0);
	public static final NumberJS MINUS_ONE = new NumberJS(-1.0);

	public static NumberJS of(double number) {
		if (number == 0.0) {
			return ZERO;
		} else if (number == 1.0) {
			return ONE;
		} else if (number == -1.0) {
			return MINUS_ONE;
		} else {
			return new NumberJS(number);
		}
	}

	private static NumberJS construct(ContextJS contextJS, Object[] objects) {
		return of(((Number) Wrapper.unwrapped(objects[0])).doubleValue());
	}

	public final double value;

	private NumberJS(double value) {
		this.value = value;
	}

	@Override
	public PrototypeJS getPrototype() {
		return PROTOTYPE;
	}

	@Override
	public TypeJS getType() {
		return TypeJS.NUMBER;
	}

	@Override
	public Object unwrap() {
		return value;
	}

	@Override
	public String asString() {
		return String.valueOf(value);
	}

	@Override
	public double asNumber() {
		return value;
	}

	@Override
	public Object cast(TypeJS type) {
		return type == TypeJS.NUMBER ? value : super.cast(type);
	}

	@Override
	public ObjectJS castJS(TypeJS type) {
		return type == TypeJS.NUMBER ? this : super.castJS(type);
	}

	private static ObjectJS toFixed(ContextJS cx, ObjectJS self, Object[] args) {
		int precision;

		if (args.length == 0) {
			precision = 0;
		} else {
			double p = ((Number) Wrapper.unwrapped(args[0])).doubleValue();

			if (p < 0 || p > 100) {
				String msg = ScriptRuntime.getMessage1("msg.bad.precision", p);
				throw ScriptRuntime.rangeError(msg);
			}

			precision = ScriptRuntime.toInt32(p);
		}

		StringBuilder sb = new StringBuilder();
		DToA.JS_dtostr(sb, DToA.DTOSTR_FIXED, precision, self.asNumber());
		return StringJS.of(sb.toString());
	}
}
