package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.DToA;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.Undefined;
import dev.latvian.mods.rhino.js.prototype.PrototypeJS;

public class NumberJS {
	public static final Double NaN = Double.NaN;
	public static final Double ZERO = 0.0;
	public static final Double MINUS_ZERO = -0.0;
	public static final Double ONE = 1.0;
	public static final Double MINUS_ONE = -1.0;
	public static final double MAX_SAFE_INTEGER = 9007199254740991.0;
	public static final double MIN_SAFE_INTEGER = -MAX_SAFE_INTEGER;

	public static PrototypeJS PROTOTYPE = PrototypeJS.DEFAULT.create(TypeJS.NUMBER, "Number")
			.constructor(NumberJS::construct)
			.staticPropertyValue("NaN", NaN)
			.staticPropertyValue("POSITIVE_INFINITY", Double.POSITIVE_INFINITY)
			.staticPropertyValue("NEGATIVE_INFINITY", Double.NEGATIVE_INFINITY)
			.staticPropertyValue("MAX_VALUE", Double.MAX_VALUE)
			.staticPropertyValue("MIN_VALUE", Double.MIN_VALUE)
			.staticPropertyValue("MAX_SAFE_INTEGER", MAX_SAFE_INTEGER)
			.staticPropertyValue("MIN_SAFE_INTEGER", MIN_SAFE_INTEGER)
			.staticPropertyValue("EPSILON", Math.pow(2D, -52D))
			.staticFunction("isFinite", NumberJS::isFinite)
			.staticFunction("isNaN", NumberJS::isNaN)
			.staticFunction("isInteger", NumberJS::isInteger)
			.staticFunction("isSafeInteger", NumberJS::isSafeInteger)
			.staticFunction("parseFloat", NumberJS::parseFloat)
			.staticFunction("parseInt", NumberJS::parseInt)
			.function("toFixed", NumberJS::toFixed)
			.function("toExponential", NumberJS::toExponential)
			.function("toPrecision", NumberJS::toPrecision)
			.asNumber(value -> ((Number) value).doubleValue())
			.asBoolean(value -> ((Number) value).doubleValue() != 0.0);

	public static Double of(double number) {
		if (Double.isNaN(number)) {
			return NaN;
		} else if (number == 0.0) {
			return ZERO;
		} else if (number == 1.0) {
			return ONE;
		} else if (number == -1.0) {
			return MINUS_ONE;
		} else {
			return number;
		}
	}

	private static Object construct(ContextJS cx, Object[] objects) {
		if (objects.length == 0) {
			return NaN;
		} else if (objects[0] instanceof Number) {
			return objects[0];
		}

		return cx.asNumber(objects, 0);
	}

	private static Object isFinite(ContextJS cx, Object[] args) {
		if (args.length == 0) {
			return Boolean.FALSE;
		} else if (args[0] instanceof Double n) {
			return !n.isInfinite() && !n.isNaN();
		} else if (args[0] instanceof Float n) {
			return !n.isInfinite() && !n.isNaN();
		}

		double d = cx.asNumber(args, 0);
		return !Double.isInfinite(d) && !Double.isNaN(d);
	}

	private static Object isNaN(ContextJS cx, Object[] args) {
		if (args.length == 0) {
			return Boolean.TRUE;
		} else if (args[0] instanceof Double n) {
			return n.isNaN();
		} else if (args[0] instanceof Float n) {
			return n.isNaN();
		} else {
			double d = cx.asNumber(args, 0);
			return Double.isNaN(d);
		}
	}

	private static Object isInteger(ContextJS cx, Object[] args) {
		if (args.length == 0) {
			return false;
		}

		double d = cx.asNumber(args, 0);
		return !Double.isInfinite(d) && !Double.isNaN(d) && (Math.floor(d) == d);
	}

	private static Object isSafeInteger(ContextJS cx, Object[] args) {
		if (args.length == 0) {
			return false;
		}

		double d = cx.asNumber(args, 0);
		return !Double.isInfinite(d) && !Double.isNaN(d) && (d <= MAX_SAFE_INTEGER) && (d >= MIN_SAFE_INTEGER) && (Math.floor(d) == d);
	}

	private static Object parseFloat(ContextJS cx, Object[] args) {
		throw new IllegalStateException("This function is not yet implemented!");
	}

	private static Object parseInt(ContextJS cx, Object[] args) {
		throw new IllegalStateException("This function is not yet implemented!");
	}

	private static Object toFixed(ContextJS cx, Object self, Object[] args) {
		double value = ((Number) self).doubleValue();
		return num_to(cx, value, args, DToA.DTOSTR_FIXED, DToA.DTOSTR_FIXED, 0, 0);
	}

	private static Object toExponential(ContextJS cx, Object self, Object[] args) {
		double value = ((Number) self).doubleValue();
		// Handle special values before range check
		if (Double.isNaN(value)) {
			return "NaN";
		}
		if (Double.isInfinite(value)) {
			if (value >= 0) {
				return "Infinity";
			}
			return "-Infinity";
		}
		// General case
		return num_to(cx, value, args, DToA.DTOSTR_STANDARD_EXPONENTIAL, DToA.DTOSTR_EXPONENTIAL, 0, 1);
	}

	private static Object toPrecision(ContextJS cx, Object self, Object[] args) {
		double value = ((Number) self).doubleValue();

		// Undefined precision, fall back to ToString()
		if (args.length == 0 || args[0] == Undefined.instance) {
			return ScriptRuntime.numberToString(cx.context, value, 10);
		}
		// Handle special values before range check
		if (Double.isNaN(value)) {
			return "NaN";
		}
		if (Double.isInfinite(value)) {
			if (value >= 0) {
				return "Infinity";
			}
			return "-Infinity";
		}
		return num_to(cx, value, args, DToA.DTOSTR_STANDARD, DToA.DTOSTR_PRECISION, 1, 0);
	}

	private static Object num_to(ContextJS cx, double val, Object[] args, int zeroArgMode, int oneArgMode, int precisionMin, int precisionOffset) {
		int precision;
		if (args.length == 0) {
			precision = 0;
			oneArgMode = zeroArgMode;
		} else {
            /* We allow a larger range of precision than
               ECMA requires; this is permitted by ECMA. */
			double p = cx.asInteger(args[0]);
			if (p < precisionMin || p > 100) {
				String msg = ScriptRuntime.getMessage1("msg.bad.precision", cx.asString(args[0]));
				throw ScriptRuntime.rangeError(msg);
			}
			precision = ScriptRuntime.toInt32(p);
		}
		StringBuilder sb = new StringBuilder();
		DToA.JS_dtostr(sb, oneArgMode, precision + precisionOffset, val);
		return sb.toString();
	}
}
