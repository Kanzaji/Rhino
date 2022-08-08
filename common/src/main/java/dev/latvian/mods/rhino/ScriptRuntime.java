/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.ast.FunctionNode;
import dev.latvian.mods.rhino.regexp.RegExp;
import dev.latvian.mods.rhino.util.SpecialEquality;
import dev.latvian.mods.rhino.v8dtoa.DoubleConversion;
import dev.latvian.mods.rhino.v8dtoa.FastDtoa;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This is the class that implements the runtime.
 *
 * @author Norris Boyd
 */

public class ScriptRuntime {
	public static final Object[] EMPTY_ARGS = new Object[0];
	public static final String[] EMPTY_STRINGS = new String[0];

	/**
	 * No instances should be created.
	 */
	protected ScriptRuntime() {
	}

	/**
	 * Returns representation of the [[ThrowTypeError]] object.
	 * See ECMA 5 spec, 13.2.3
	 */
	public static BaseFunction typeErrorThrower(Context cx) {
		if (cx.typeErrorThrower == null) {
			BaseFunction thrower = new BaseFunction() {
				@Override
				public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
					throw typeError0("msg.op.not.allowed");
				}

				@Override
				public int getLength() {
					return 0;
				}
			};
			setFunctionProtoAndParent(cx, thrower, cx.topCallScope);
			thrower.preventExtensions();
			cx.typeErrorThrower = thrower;
		}
		return cx.typeErrorThrower;
	}

	static class NoSuchMethodShim implements Callable {
		String methodName;
		Callable noSuchMethodMethod;

		NoSuchMethodShim(Callable noSuchMethodMethod, String methodName) {
			this.noSuchMethodMethod = noSuchMethodMethod;
			this.methodName = methodName;
		}

		/**
		 * Perform the call.
		 *
		 * @param cx      the current Context for this thread
		 * @param scope   the scope to use to resolve properties.
		 * @param thisObj the JavaScript <code>this</code> object
		 * @param args    the array of arguments
		 * @return the result of the call
		 */
		@Override
		public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
			Object[] nestedArgs = new Object[2];

			nestedArgs[0] = methodName;
			nestedArgs[1] = newArrayLiteral(args, null, cx, scope);
			return noSuchMethodMethod.call(cx, scope, thisObj, nestedArgs);
		}

	}

	public final static Class<Boolean> BooleanClass = Boolean.class;
	public final static Class<Byte> ByteClass = Byte.class;
	public final static Class<Character> CharacterClass = Character.class;
	public final static Class<Class> ClassClass = Class.class;
	public final static Class<Double> DoubleClass = Double.class;
	public final static Class<Float> FloatClass = Float.class;
	public final static Class<Integer> IntegerClass = Integer.class;
	public final static Class<Long> LongClass = Long.class;
	public final static Class<Number> NumberClass = Number.class;
	public final static Class<Object> ObjectClass = Object.class;
	public final static Class<Short> ShortClass = Short.class;
	public final static Class<String> StringClass = String.class;
	public final static Class<Date> DateClass = Date.class;

	public final static Class<?> ContextClass = Context.class;
	public final static Class<ContextFactory> ContextFactoryClass = ContextFactory.class;
	public final static Class<Function> FunctionClass = Function.class;
	public final static Class<ScriptableObject> ScriptableObjectClass = ScriptableObject.class;
	public static final Class<Scriptable> ScriptableClass = Scriptable.class;

	private static final Object LIBRARY_SCOPE_KEY = "LIBRARY_SCOPE";

	public static boolean isRhinoRuntimeType(Class<?> cl) {
		if (cl.isPrimitive()) {
			return (cl != Character.TYPE);
		}
		return (cl == StringClass || cl == BooleanClass || NumberClass.isAssignableFrom(cl) || ScriptableClass.isAssignableFrom(cl));
	}

	public static ScriptableObject initSafeStandardObjects(Context cx, ScriptableObject scope, boolean sealed) {
		if (scope == null) {
			scope = new NativeObject();
		}
		scope.associateValue(LIBRARY_SCOPE_KEY, scope);
		(new ClassCache()).associate(scope);

		BaseFunction.init(cx, scope, sealed);
		NativeObject.init(cx, scope, sealed);

		Scriptable objectProto = ScriptableObject.getObjectPrototype(cx, scope);

		// Function.prototype.__proto__ should be Object.prototype
		Scriptable functionProto = ScriptableObject.getClassPrototype(cx, scope, "Function");
		functionProto.setPrototype(cx, objectProto);

		// Set the prototype of the object passed in if need be
		if (scope.getPrototype(cx) == null) {
			scope.setPrototype(cx, objectProto);
		}

		// must precede NativeGlobal since it's needed therein
		NativeError.init(cx, scope, sealed);
		NativeGlobal.init(cx, scope, sealed);

		NativeString.init(cx, scope, sealed);
		NativeBoolean.init(cx, scope, sealed);
		NativeNumber.init(cx, scope, sealed);
		NativeDate.init(cx, scope, sealed);
		NativeMath.init(cx, scope, sealed);
		NativeJSON.init(cx, scope, sealed);

		NativeWith.init(cx, scope, sealed);
		NativeCall.init(cx, scope, sealed);
		NativeScript.init(cx, scope, sealed);

		NativeIterator.init(cx, scope, sealed); // Also initializes NativeGenerator & ES6Generator

		NativeArrayIterator.init(cx, scope, sealed);
		NativeStringIterator.init(cx, scope, sealed);

		// define lazy-loaded properties using their class name
		new LazilyLoadedCtor(scope, "RegExp", "dev.latvian.mods.rhino.regexp.NativeRegExp", sealed, true);

		NativeSymbol.init(cx, scope, sealed);
		NativeCollectionIterator.init(cx, scope, NativeSet.ITERATOR_TAG, sealed);
		NativeCollectionIterator.init(cx, scope, NativeMap.ITERATOR_TAG, sealed);
		NativeMap.init(cx, scope, sealed);
		NativeSet.init(cx, scope, sealed);
		NativeWeakMap.init(cx, scope, sealed);
		NativeWeakSet.init(cx, scope, sealed);

		if (scope instanceof TopLevel) {
			((TopLevel) scope).cacheBuiltins(cx, scope, sealed);
		}

		return scope;
	}

	public static ScriptableObject initStandardObjects(Context cx, ScriptableObject scope, boolean sealed) {
		ScriptableObject s = initSafeStandardObjects(cx, scope, sealed);
		// new LazilyLoadedCtor(s, "Packages", "dev.latvian.mods.rhino.NativeJavaTopPackage", sealed, true);
		// new LazilyLoadedCtor(s, "getClass", "dev.latvian.mods.rhino.NativeJavaTopPackage", sealed, true);
		return s;
	}

	public static ScriptableObject getLibraryScopeOrNull(Context cx, Scriptable scope) {
		ScriptableObject libScope;
		libScope = (ScriptableObject) ScriptableObject.getTopScopeValue(cx, scope, LIBRARY_SCOPE_KEY);
		return libScope;
	}

	// It is public so NativeRegExp can access it.
	public static boolean isJSLineTerminator(int c) {
		// Optimization for faster check for eol character:
		// they do not have 0xDFD0 bits set
		if ((c & 0xDFD0) != 0) {
			return false;
		}
		return c == '\n' || c == '\r' || c == 0x2028 || c == 0x2029;
	}

	public static boolean isJSWhitespaceOrLineTerminator(int c) {
		return (isStrWhiteSpaceChar(c) || isJSLineTerminator(c));
	}

	/**
	 * Indicates if the character is a Str whitespace char according to ECMA spec:
	 * StrWhiteSpaceChar :::
	 * <TAB>
	 * <SP>
	 * <NBSP>
	 * <FF>
	 * <VT>
	 * <CR>
	 * <LF>
	 * <LS>
	 * <PS>
	 * <USP>
	 * <BOM>
	 */
	static boolean isStrWhiteSpaceChar(int c) {
		return switch (c) { // <SP>
			// <LF>
			// <CR>
			// <TAB>
			// <NBSP>
			// <FF>
			// <VT>
			// <LS>
			// <PS>
			case ' ', '\n', '\r', '\t', '\u00A0', '\u000C', '\u000B', '\u2028', '\u2029', '\uFEFF' -> // <BOM>
					true;
			default -> Character.getType(c) == Character.SPACE_SEPARATOR;
		};
	}

	public static Boolean wrapBoolean(boolean b) {
		return b;
	}

	public static Number wrapNumber(double x) {
		if (Double.isNaN(x)) {
			return NaNobj;
		}
		return x;
	}

	/**
	 * Convert the value to a boolean.
	 * <p>
	 * See ECMA 9.2.
	 */
	public static boolean toBoolean(Object val) {
		if (val instanceof Boolean) {
			return (Boolean) val;
		}
		if (val == null || val == Undefined.instance) {
			return false;
		}
		if (val instanceof CharSequence) {
			return ((CharSequence) val).length() != 0;
		}
		if (val instanceof Number) {
			double d = ((Number) val).doubleValue();
			return (!Double.isNaN(d) && d != 0.0);
		}
		if (val instanceof Scriptable) {
			return !(val instanceof ScriptableObject) || !((ScriptableObject) val).avoidObjectDetection();
		}
		warnAboutNonJSObject(val);
		return true;
	}

	/**
	 * Convert the value to a number.
	 * <p>
	 * See ECMA 9.3.
	 */
	public static double toNumber(Object val) {
		if (val instanceof Number) {
			return ((Number) val).doubleValue();
		}
		if (val == null) {
			return +0.0;
		}
		if (val == Undefined.instance) {
			return NaN;
		}
		if (val instanceof String) {
			return toNumber((String) val);
		}
		if (val instanceof CharSequence) {
			return toNumber(val.toString());
		}
		if (val instanceof Boolean) {
			return (Boolean) val ? 1 : +0.0;
		}
		if (val instanceof Symbol) {
			throw typeError0("msg.not.a.number");
		}
		if (val instanceof Scriptable) {
			var cx = Context.getContext();
			val = ((Scriptable) val).getDefaultValue(cx, NumberClass);
			if ((val instanceof Scriptable) && !isSymbol(val)) {
				throw errorWithClassName(cx, "msg.primitive.expected", val);
			}
			return toNumber(val);
		}
		warnAboutNonJSObject(val);
		return NaN;
	}

	public static double toNumber(Object[] args, int index) {
		return (index < args.length) ? toNumber(args[index]) : NaN;
	}

	public static final double NaN = Double.NaN;
	public static final Double NaNobj = NaN;

	// Preserve backward-compatibility with historical value of this.
	public static final double negativeZero = Double.longBitsToDouble(0x8000000000000000L);

	public static final Double zeroObj = 0.0;
	public static final Double negativeZeroObj = -0.0;

	static double stringPrefixToNumber(String s, int start, int radix) {
		return stringToNumber(s, start, s.length() - 1, radix, true);
	}

	static double stringToNumber(String s, int start, int end, int radix) {
		return stringToNumber(s, start, end, radix, false);
	}

	/*
	 * Helper function for toNumber, parseInt, and TokenStream.getToken.
	 */
	private static double stringToNumber(String source, int sourceStart, int sourceEnd, int radix, boolean isPrefix) {
		char digitMax = '9';
		char lowerCaseBound = 'a';
		char upperCaseBound = 'A';
		if (radix < 10) {
			digitMax = (char) ('0' + radix - 1);
		}
		if (radix > 10) {
			lowerCaseBound = (char) ('a' + radix - 10);
			upperCaseBound = (char) ('A' + radix - 10);
		}
		int end;
		double sum = 0.0;
		for (end = sourceStart; end <= sourceEnd; end++) {
			char c = source.charAt(end);
			int newDigit;
			if ('0' <= c && c <= digitMax) {
				newDigit = c - '0';
			} else if ('a' <= c && c < lowerCaseBound) {
				newDigit = c - 'a' + 10;
			} else if ('A' <= c && c < upperCaseBound) {
				newDigit = c - 'A' + 10;
			} else if (!isPrefix) {
				return NaN; // isn't a prefix but found unexpected char
			} else {
				break; // unexpected char
			}
			sum = sum * radix + newDigit;
		}
		if (sourceStart == end) { // stopped right at the beginning
			return NaN;
		}
		if (sum > NativeNumber.MAX_SAFE_INTEGER) {
			if (radix == 10) {
				/* If we're accumulating a decimal number and the number
				 * is >= 2^53, then the result from the repeated multiply-add
				 * above may be inaccurate.  Call Java to get the correct
				 * answer.
				 */
				try {
					return Double.parseDouble(source.substring(sourceStart, end));
				} catch (NumberFormatException nfe) {
					return NaN;
				}
			} else if (radix == 2 || radix == 4 || radix == 8 || radix == 16 || radix == 32) {
				/* The number may also be inaccurate for one of these bases.
				 * This happens if the addition in value*radix + digit causes
				 * a round-down to an even least significant mantissa bit
				 * when the first dropped bit is a one.  If any of the
				 * following digits in the number (which haven't been added
				 * in yet) are nonzero then the correct action would have
				 * been to round up instead of down.  An example of this
				 * occurs when reading the number 0x1000000000000081, which
				 * rounds to 0x1000000000000000 instead of 0x1000000000000100.
				 */
				int bitShiftInChar = 1;
				int digit = 0;

				final int SKIP_LEADING_ZEROS = 0;
				final int FIRST_EXACT_53_BITS = 1;
				final int AFTER_BIT_53 = 2;
				final int ZEROS_AFTER_54 = 3;
				final int MIXED_AFTER_54 = 4;

				int state = SKIP_LEADING_ZEROS;
				int exactBitsLimit = 53;
				double factor = 0.0;
				boolean bit53 = false;
				// bit54 is the 54th bit (the first dropped from the mantissa)
				boolean bit54 = false;
				int pos = sourceStart;

				for (; ; ) {
					if (bitShiftInChar == 1) {
						if (pos == end) {
							break;
						}
						digit = source.charAt(pos++);
						if ('0' <= digit && digit <= '9') {
							digit -= '0';
						} else if ('a' <= digit && digit <= 'z') {
							digit -= 'a' - 10;
						} else {
							digit -= 'A' - 10;
						}
						bitShiftInChar = radix;
					}
					bitShiftInChar >>= 1;
					boolean bit = (digit & bitShiftInChar) != 0;

					switch (state) {
						case SKIP_LEADING_ZEROS:
							if (bit) {
								--exactBitsLimit;
								sum = 1.0;
								state = FIRST_EXACT_53_BITS;
							}
							break;
						case FIRST_EXACT_53_BITS:
							sum *= 2.0;
							if (bit) {
								sum += 1.0;
							}
							--exactBitsLimit;
							if (exactBitsLimit == 0) {
								bit53 = bit;
								state = AFTER_BIT_53;
							}
							break;
						case AFTER_BIT_53:
							bit54 = bit;
							factor = 2.0;
							state = ZEROS_AFTER_54;
							break;
						case ZEROS_AFTER_54:
							if (bit) {
								state = MIXED_AFTER_54;
							}
							// fallthrough
						case MIXED_AFTER_54:
							factor *= 2;
							break;
					}
				}
				switch (state) {
					case SKIP_LEADING_ZEROS:
						sum = 0.0;
						break;
					case FIRST_EXACT_53_BITS:
					case AFTER_BIT_53:
						// do nothing
						break;
					case ZEROS_AFTER_54:
						// x1.1 -> x1 + 1 (round up)
						// x0.1 -> x0 (round down)
						if (bit54 & bit53) {
							sum += 1.0;
						}
						sum *= factor;
						break;
					case MIXED_AFTER_54:
						// x.100...1.. -> x + 1 (round up)
						// x.0anything -> x (round down)
						if (bit54) {
							sum += 1.0;
						}
						sum *= factor;
						break;
				}
			}
			/* We don't worry about inaccurate numbers for any other base. */
		}
		return sum;
	}

	/**
	 * ToNumber applied to the String type
	 * <p>
	 * See the #sec-tonumber-applied-to-the-string-type section of ECMA
	 */
	public static double toNumber(String s) {
		final int len = s.length();

		// Skip whitespace at the start
		int start = 0;
		char startChar;
		for (; ; ) {
			if (start == len) {
				// empty or contains only whitespace
				return +0.0;
			}
			startChar = s.charAt(start);
			if (!isStrWhiteSpaceChar(startChar)) {
				// found first non-whitespace character
				break;
			}
			start++;
		}

		// Skip whitespace at the end
		int end = len - 1;
		char endChar;
		while (isStrWhiteSpaceChar(endChar = s.charAt(end))) {
			end--;
		}

		// Do not break scripts relying on old non-compliant conversion
		// (see bug #368)
		// 1. makes ToNumber parse only a valid prefix in hex literals (similar to 'parseInt()')
		//    ToNumber('0x10 something') => 16
		// 2. allows plus and minus signs for hexadecimal numbers
		//    ToNumber('-0x10') => -16
		// 3. disables support for binary ('0b10') and octal ('0o13') literals
		//    ToNumber('0b1') => NaN
		//    ToNumber('0o5') => NaN
		final Context cx = Context.getCurrentContext();
		final boolean oldParsingMode = cx == null;

		// Handle non-base10 numbers
		if (startChar == '0') {
			if (start + 2 <= end) {
				final char radixC = s.charAt(start + 1);
				int radix = -1;
				if (radixC == 'x' || radixC == 'X') {
					radix = 16;
				} else if (!oldParsingMode && (radixC == 'o' || radixC == 'O')) {
					radix = 8;
				} else if (!oldParsingMode && (radixC == 'b' || radixC == 'B')) {
					radix = 2;
				}
				if (radix != -1) {
					if (oldParsingMode) {
						return stringPrefixToNumber(s, start + 2, radix);
					}
					return stringToNumber(s, start + 2, end, radix);
				}
			}
		} else if (oldParsingMode && (startChar == '+' || startChar == '-')) {
			// If in old parsing mode, check for a signed hexadecimal number
			if (start + 3 <= end && s.charAt(start + 1) == '0') {
				final char radixC = s.charAt(start + 2);
				if (radixC == 'x' || radixC == 'X') {
					double val = stringPrefixToNumber(s, start + 3, 16);
					return startChar == '-' ? -val : val;
				}
			}
		}

		if (endChar == 'y') {
			// check for "Infinity"
			if (startChar == '+' || startChar == '-') {
				start++;
			}
			if (start + 7 == end && s.regionMatches(start, "Infinity", 0, 8)) {
				return startChar == '-' ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
			}
			return NaN;
		}
		// A base10, non-infinity number:
		// just try a normal floating point conversion
		String sub = s.substring(start, end + 1);
		// Quick test to check string contains only valid characters because
		// Double.parseDouble() can be slow and accept input we want to reject
		for (int i = sub.length() - 1; i >= 0; i--) {
			char c = sub.charAt(i);
			if (('0' <= c && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
				continue;
			}
			return NaN;
		}
		try {
			return Double.parseDouble(sub);
		} catch (NumberFormatException ex) {
			return NaN;
		}
	}

	/**
	 * Helper function for builtin objects that use the varargs form.
	 * ECMA function formal arguments are undefined if not supplied;
	 * this function pads the argument array out to the expected
	 * length, if necessary.
	 */
	public static Object[] padArguments(Object[] args, int count) {
		if (count < args.length) {
			return args;
		}

		Object[] result = new Object[count];
		System.arraycopy(args, 0, result, 0, args.length);
		if (args.length < count) {
			Arrays.fill(result, args.length, count, Undefined.instance);
		}
		return result;
	}

	public static String escapeString(String s) {
		return escapeString(s, '"');
	}

	/**
	 * For escaping strings printed by object and array literals; not quite
	 * the same as 'escape.'
	 */
	public static String escapeString(String s, char escapeQuote) {
		if (!(escapeQuote == '"' || escapeQuote == '\'')) {
			throw Kit.codeBug();
		}
		StringBuilder sb = null;

		for (int i = 0, L = s.length(); i != L; ++i) {
			int c = s.charAt(i);

			if (' ' <= c && c <= '~' && c != escapeQuote && c != '\\') {
				// an ordinary print character (like C isprint()) and not "
				// or \ .
				if (sb != null) {
					sb.append((char) c);
				}
				continue;
			}
			if (sb == null) {
				sb = new StringBuilder(L + 3);
				sb.append(s);
				sb.setLength(i);
			}

			int escape = switch (c) {
				case '\b' -> 'b';
				case '\f' -> 'f';
				case '\n' -> 'n';
				case '\r' -> 'r';
				case '\t' -> 't';
				case 0xb -> 'v'; // Java lacks \v.
				case ' ' -> ' ';
				case '\\' -> '\\';
				default -> -1;
			};
			if (escape >= 0) {
				// an \escaped sort of character
				sb.append('\\');
				sb.append((char) escape);
			} else if (c == escapeQuote) {
				sb.append('\\');
				sb.append(escapeQuote);
			} else {
				int hexSize;
				if (c < 256) {
					// 2-digit hex
					sb.append("\\x");
					hexSize = 2;
				} else {
					// Unicode.
					sb.append("\\u");
					hexSize = 4;
				}
				// append hexadecimal form of c left-padded with 0
				for (int shift = (hexSize - 1) * 4; shift >= 0; shift -= 4) {
					int digit = 0xf & (c >> shift);
					int hc = (digit < 10) ? '0' + digit : 'a' - 10 + digit;
					sb.append((char) hc);
				}
			}
		}
		return (sb == null) ? s : sb.toString();
	}

	static boolean isValidIdentifierName(String s, Context cx, boolean isStrict) {
		int L = s.length();
		if (L == 0) {
			return false;
		}
		if (!Character.isJavaIdentifierStart(s.charAt(0))) {
			return false;
		}
		for (int i = 1; i != L; ++i) {
			if (!Character.isJavaIdentifierPart(s.charAt(i))) {
				return false;
			}
		}
		return !TokenStream.isKeyword(s, isStrict);
	}

	public static CharSequence toCharSequence(Object val) {
		if (val instanceof NativeString) {
			return ((NativeString) val).toCharSequence();
		}
		return val instanceof CharSequence ? (CharSequence) val : toString(val);
	}

	/**
	 * Convert the value to a string.
	 * <p>
	 * See ECMA 9.8.
	 */
	public static String toString(Object val) {
		if (val == null) {
			return "null";
		}
		if (val == Undefined.instance || val == Undefined.SCRIPTABLE_UNDEFINED) {
			return "undefined";
		}
		if (val instanceof String) {
			return (String) val;
		}
		if (val instanceof CharSequence) {
			return val.toString();
		}
		if (val instanceof Number) {
			// XXX should we just teach NativeNumber.stringValue()
			// about Numbers?
			return numberToString(((Number) val).doubleValue(), 10);
		}
		if (val instanceof Symbol) {
			throw typeError0("msg.not.a.string");
		}
		if (val instanceof Scriptable) {
			var cx = Context.getContext();
			val = ((Scriptable) val).getDefaultValue(cx, StringClass);
			if ((val instanceof Scriptable) && !isSymbol(val)) {
				throw errorWithClassName(cx, "msg.primitive.expected", val);
			}
			return toString(val);
		}
		return val.toString();
	}

	static String defaultObjectToString(Scriptable obj) {
		if (obj == null) {
			return "[object Null]";
		}
		if (Undefined.isUndefined(obj)) {
			return "[object Undefined]";
		}
		return "[object " + obj.getClassName() + ']';
	}

	public static String toString(Object[] args, int index) {
		return (index < args.length) ? toString(args[index]) : "undefined";
	}

	/**
	 * Optimized version of toString(Object) for numbers.
	 */
	public static String toString(double val) {
		return numberToString(val, 10);
	}

	public static String numberToString(double d, int base) {
		if ((base < 2) || (base > 36)) {
			throw Context.reportRuntimeError1(Context.getCurrentContext(), "msg.bad.radix", Integer.toString(base));
		}

		if (Double.isNaN(d)) {
			return "NaN";
		}
		if (d == Double.POSITIVE_INFINITY) {
			return "Infinity";
		}
		if (d == Double.NEGATIVE_INFINITY) {
			return "-Infinity";
		}
		if (d == 0.0) {
			return "0";
		}

		if (base != 10) {
			return DToA.JS_dtobasestr(base, d);
		}
		// V8 FastDtoa can't convert all numbers, so try it first but
		// fall back to old DToA in case it fails
		String result = FastDtoa.numberToString(d);
		if (result != null) {
			return result;
		}
		StringBuilder buffer = new StringBuilder();
		DToA.JS_dtostr(buffer, DToA.DTOSTR_STANDARD, 0, d);
		return buffer.toString();
	}

	static String uneval(Context cx, Scriptable scope, Object value) {
		if (value == null) {
			return "null";
		}
		if (value == Undefined.instance) {
			return "undefined";
		}
		if (value instanceof CharSequence) {
			String escaped = escapeString(value.toString());
			return '\"' + escaped + '\"';
		}
		if (value instanceof Number) {
			double d = ((Number) value).doubleValue();
			if (d == 0 && 1 / d < 0) {
				return "-0";
			}
			return toString(d);
		}
		if (value instanceof Boolean) {
			return toString(value);
		}
		if (value instanceof Scriptable obj) {
			// Wrapped Java objects won't have "toSource" and will report
			// errors for get()s of nonexistent name, so use has() first
			if (ScriptableObject.hasProperty(cx, obj, "toSource")) {
				Object v = ScriptableObject.getProperty(cx, obj, "toSource");
				if (v instanceof Function f) {
					return toString(f.call(cx, scope, obj, EMPTY_ARGS));
				}
			}
			return toString(value);
		}
		warnAboutNonJSObject(value);
		return value.toString();
	}

	static String defaultObjectToSource(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return "not_supported";
	}

	public static Scriptable toObject(Scriptable scope, Object val) {
		if (val instanceof Scriptable) {
			return (Scriptable) val;
		}
		return toObject(Context.getContext(), scope, val);
	}

	/**
	 * <strong>Warning</strong>: This doesn't allow to resolve primitive
	 * prototype properly when many top scopes are involved
	 *
	 * @deprecated Use {@link #toObjectOrNull(Context, Object, Scriptable)} instead
	 */
	@Deprecated
	public static Scriptable toObjectOrNull(Context cx, Object obj) {
		if (obj instanceof Scriptable) {
			return (Scriptable) obj;
		} else if (obj != null && obj != Undefined.instance) {
			return toObject(cx, getTopCallScope(cx), obj);
		}
		return null;
	}

	/**
	 * @param scope the scope that should be used to resolve primitive prototype
	 */
	public static Scriptable toObjectOrNull(Context cx, Object obj, Scriptable scope) {
		if (obj instanceof Scriptable) {
			return (Scriptable) obj;
		} else if (obj != null && obj != Undefined.instance) {
			return toObject(cx, scope, obj);
		}
		return null;
	}

	/**
	 * Convert the value to an object.
	 * <p>
	 * See ECMA 9.9.
	 */
	public static Scriptable toObject(Context cx, Scriptable scope, Object val) {
		if (val == null) {
			throw typeError0("msg.null.to.object");
		}
		if (Undefined.isUndefined(val)) {
			throw typeError0("msg.undef.to.object");
		}

		if (isSymbol(val)) {
			NativeSymbol result = new NativeSymbol((NativeSymbol) val);
			setBuiltinProtoAndParent(cx, result, scope, TopLevel.Builtins.Symbol);
			return result;
		}
		if (val instanceof Scriptable) {
			return (Scriptable) val;
		}
		if (val instanceof CharSequence) {
			// FIXME we want to avoid toString() here, especially for concat()
			NativeString result = new NativeString((CharSequence) val);
			setBuiltinProtoAndParent(cx, result, scope, TopLevel.Builtins.String);
			return result;
		}
		if (val instanceof Number) {
			NativeNumber result = new NativeNumber(((Number) val).doubleValue());
			setBuiltinProtoAndParent(cx, result, scope, TopLevel.Builtins.Number);
			return result;
		}
		if (val instanceof Boolean) {
			NativeBoolean result = new NativeBoolean((Boolean) val);
			setBuiltinProtoAndParent(cx, result, scope, TopLevel.Builtins.Boolean);
			return result;
		}

		// Extension: Wrap as a LiveConnect object.
		Object wrapped = cx.getWrapFactory().wrap(cx, scope, val, null);
		if (wrapped instanceof Scriptable) {
			return (Scriptable) wrapped;
		}
		throw errorWithClassName(cx, "msg.invalid.type", val);
	}

	public static Scriptable newObject(Context cx, Scriptable scope, String constructorName, Object[] args) {
		scope = ScriptableObject.getTopLevelScope(scope);
		Function ctor = getExistingCtor(cx, scope, constructorName);
		if (args == null) {
			args = EMPTY_ARGS;
		}
		return ctor.construct(cx, scope, args);
	}

	public static Scriptable newBuiltinObject(Context cx, Scriptable scope, TopLevel.Builtins type, Object[] args) {
		scope = ScriptableObject.getTopLevelScope(scope);
		Function ctor = TopLevel.getBuiltinCtor(cx, scope, type);
		if (args == null) {
			args = EMPTY_ARGS;
		}
		return ctor.construct(cx, scope, args);
	}

	static Scriptable newNativeError(Context cx, Scriptable scope, TopLevel.NativeErrors type, Object[] args) {
		scope = ScriptableObject.getTopLevelScope(scope);
		Function ctor = TopLevel.getNativeErrorCtor(cx, scope, type);
		if (args == null) {
			args = EMPTY_ARGS;
		}
		return ctor.construct(cx, scope, args);
	}

	/**
	 * See ECMA 9.4.
	 */
	public static double toInteger(Object val) {
		return toInteger(toNumber(val));
	}

	// convenience method
	public static double toInteger(double d) {
		// if it's NaN
		if (Double.isNaN(d)) {
			return +0.0;
		}

		if ((d == 0.0) || Double.isInfinite(d)) {
			return d;
		}

		if (d > 0.0) {
			return Math.floor(d);
		}

		return Math.ceil(d);
	}

	public static double toInteger(Object[] args, int index) {
		return (index < args.length) ? toInteger(args[index]) : +0.0;
	}

	public static long toLength(Object[] args, int index) {
		double len = toInteger(args, index);
		if (len <= +0.0) {
			return 0;
		}
		return (long) Math.min(len, NativeNumber.MAX_SAFE_INTEGER);
	}

	/**
	 * See ECMA 9.5.
	 */
	public static int toInt32(Object val) {
		// short circuit for common integer values
		if (val instanceof Integer) {
			return (Integer) val;
		}

		return toInt32(toNumber(val));
	}

	public static int toInt32(Object[] args, int index) {
		return (index < args.length) ? toInt32(args[index]) : 0;
	}

	public static int toInt32(double d) {
		return DoubleConversion.doubleToInt32(d);
	}

	/**
	 * See ECMA 9.6.
	 *
	 * @return long value representing 32 bits unsigned integer
	 */
	public static long toUint32(double d) {
		return DoubleConversion.doubleToInt32(d) & 0xffffffffL;
	}

	public static long toUint32(Object val) {
		return toUint32(toNumber(val));
	}

	/**
	 * See ECMA 9.7.
	 */
	public static char toUint16(Object val) {
		double d = toNumber(val);
		return (char) DoubleConversion.doubleToInt32(d);
	}

	public static Object getTopLevelProp(Context cx, Scriptable scope, String id) {
		scope = ScriptableObject.getTopLevelScope(scope);
		return ScriptableObject.getProperty(cx, scope, id);
	}

	static Function getExistingCtor(Context cx, Scriptable scope, String constructorName) {
		Object ctorVal = ScriptableObject.getProperty(cx, scope, constructorName);
		if (ctorVal instanceof Function) {
			return (Function) ctorVal;
		}
		if (ctorVal == Scriptable.NOT_FOUND) {
			throw Context.reportRuntimeError1(cx, "msg.ctor.not.found", constructorName);
		}
		throw Context.reportRuntimeError1(cx, "msg.not.ctor", constructorName);
	}

	/**
	 * Return -1L if str is not an index, or the index value as lower 32
	 * bits of the result. Note that the result needs to be cast to an int
	 * in order to produce the actual index, which may be negative.
	 */
	public static long indexFromString(String str) {
		// The length of the decimal string representation of
		//  Integer.MAX_VALUE, 2147483647
		final int MAX_VALUE_LENGTH = 10;

		int len = str.length();
		if (len > 0) {
			int i = 0;
			boolean negate = false;
			int c = str.charAt(0);
			if (c == '-') {
				if (len > 1) {
					c = str.charAt(1);
					if (c == '0') {
						return -1L; // "-0" is not an index
					}
					i = 1;
					negate = true;
				}
			}
			c -= '0';
			if (0 <= c && c <= 9 && len <= (negate ? MAX_VALUE_LENGTH + 1 : MAX_VALUE_LENGTH)) {
				// Use negative numbers to accumulate index to handle
				// Integer.MIN_VALUE that is greater by 1 in absolute value
				// then Integer.MAX_VALUE
				int index = -c;
				int oldIndex = 0;
				i++;
				if (index != 0) {
					// Note that 00, 01, 000 etc. are not indexes
					while (i != len && 0 <= (c = str.charAt(i) - '0') && c <= 9) {
						oldIndex = index;
						index = 10 * index - c;
						i++;
					}
				}
				// Make sure all characters were consumed and that it couldn't
				// have overflowed.
				if (i == len && (oldIndex > (Integer.MIN_VALUE / 10) || (oldIndex == (Integer.MIN_VALUE / 10) && c <= (negate ? -(Integer.MIN_VALUE % 10) : (Integer.MAX_VALUE % 10))))) {
					return 0xFFFFFFFFL & (negate ? index : -index);
				}
			}
		}
		return -1L;
	}

	/**
	 * If str is a decimal presentation of Uint32 value, return it as long.
	 * Othewise return -1L;
	 */
	public static long testUint32String(String str) {
		// The length of the decimal string representation of
		//  UINT32_MAX_VALUE, 4294967296
		final int MAX_VALUE_LENGTH = 10;

		int len = str.length();
		if (1 <= len && len <= MAX_VALUE_LENGTH) {
			int c = str.charAt(0);
			c -= '0';
			if (c == 0) {
				// Note that 00,01 etc. are not valid Uint32 presentations
				return (len == 1) ? 0L : -1L;
			}
			if (1 <= c && c <= 9) {
				long v = c;
				for (int i = 1; i != len; ++i) {
					c = str.charAt(i) - '0';
					if (!(0 <= c && c <= 9)) {
						return -1;
					}
					v = 10 * v + c;
				}
				// Check for overflow
				if ((v >>> 32) == 0) {
					return v;
				}
			}
		}
		return -1;
	}

	/**
	 * If s represents index, then return index value wrapped as Integer
	 * and othewise return s.
	 */
	static Object getIndexObject(String s) {
		long indexTest = indexFromString(s);
		if (indexTest >= 0) {
			return (int) indexTest;
		}
		return s;
	}

	/**
	 * If d is exact int value, return its value wrapped as Integer
	 * and othewise return d converted to String.
	 */
	static Object getIndexObject(double d) {
		int i = (int) d;
		if (i == d) {
			return i;
		}
		return toString(d);
	}

	/**
	 * Helper to return a string or an integer.
	 * Always use a null check on s.stringId to determine
	 * if the result is string or integer.
	 *
	 * @see ScriptRuntime#toStringIdOrIndex(Context, Object)
	 */
	static final class StringIdOrIndex {
		final String stringId;
		final int index;

		StringIdOrIndex(String stringId) {
			this.stringId = stringId;
			this.index = -1;
		}

		StringIdOrIndex(int index) {
			this.stringId = null;
			this.index = index;
		}
	}

	/**
	 * If toString(id) is a decimal presentation of int32 value, then id
	 * is index. In this case return null and make the index available
	 * as lastIndexResult(cx). Otherwise return toString(id).
	 */
	static StringIdOrIndex toStringIdOrIndex(Context cx, Object id) {
		if (id instanceof Number) {
			double d = ((Number) id).doubleValue();
			int index = (int) d;
			if (index == d) {
				return new StringIdOrIndex(index);
			}
			return new StringIdOrIndex(toString(id));
		}
		String s;
		if (id instanceof String) {
			s = (String) id;
		} else {
			s = toString(id);
		}
		long indexTest = indexFromString(s);
		if (indexTest >= 0) {
			return new StringIdOrIndex((int) indexTest);
		}
		return new StringIdOrIndex(s);
	}

	/**
	 * Call obj.[[Get]](id)
	 */
	public static Object getObjectElem(Context cx, Object obj, Object elem, Scriptable scope) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefReadError(obj, elem);
		}
		return getObjectElem(cx, sobj, elem);
	}

	public static Object getObjectElem(Context cx, Scriptable obj, Object elem) {
		Object result;

		if (isSymbol(elem)) {
			result = ScriptableObject.getProperty(cx, obj, (Symbol) elem);
		} else {
			StringIdOrIndex s = toStringIdOrIndex(cx, elem);
			if (s.stringId == null) {
				int index = s.index;
				result = ScriptableObject.getProperty(cx, obj, index);
			} else {
				result = ScriptableObject.getProperty(cx, obj, s.stringId);
			}
		}

		if (result == Scriptable.NOT_FOUND) {
			result = Undefined.instance;
		}

		return result;
	}

	/**
	 * Version of getObjectElem when elem is a valid JS identifier name.
	 *
	 * @param scope the scope that should be used to resolve primitive prototype
	 */
	public static Object getObjectProp(Context cx, Object obj, String property, Scriptable scope) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefReadError(obj, property);
		}
		return getObjectProp(cx, sobj, property);
	}

	public static Object getObjectProp(Context cx, Scriptable obj, String property) {

		Object result = ScriptableObject.getProperty(cx, obj, property);
		if (result == Scriptable.NOT_FOUND) {
			if (cx.hasFeature(Context.FEATURE_STRICT_MODE)) {
				Context.reportWarning(cx, getMessage1("msg.ref.undefined.prop", property));
			}
			result = Undefined.instance;
		}

		return result;
	}

	public static Object getObjectPropNoWarn(Context cx, Object obj, String property, Scriptable scope) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefReadError(obj, property);
		}
		Object result = ScriptableObject.getProperty(cx, sobj, property);
		if (result == Scriptable.NOT_FOUND) {
			return Undefined.instance;
		}
		return result;
	}

	public static Object getObjectPropOptional(Context cx, Object obj, String property, Scriptable scope) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			return Undefined.instance;
		}
		Object result = ScriptableObject.getProperty(cx, sobj, property);
		if (result == Scriptable.NOT_FOUND) {
			return Undefined.instance;
		}
		return result;
	}

	/**
	 * A cheaper and less general version of the above for well-known argument
	 * types.
	 */
	public static Object getObjectIndex(Context cx, Object obj, double dblIndex, Scriptable scope) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefReadError(obj, toString(dblIndex));
		}

		int index = (int) dblIndex;
		if (index == dblIndex) {
			return getObjectIndex(sobj, index, cx);
		}
		String s = toString(dblIndex);
		return getObjectProp(cx, sobj, s);
	}

	public static Object getObjectIndex(Scriptable obj, int index, Context cx) {
		Object result = ScriptableObject.getProperty(cx, obj, index);
		if (result == Scriptable.NOT_FOUND) {
			result = Undefined.instance;
		}

		return result;
	}

	/**
	 * Call obj.[[Put]](id, value)
	 */
	public static Object setObjectElem(Object obj, Object elem, Object value, Context cx, Scriptable scope) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefWriteError(obj, elem, value);
		}
		return setObjectElem(sobj, elem, value, cx);
	}

	public static Object setObjectElem(Scriptable obj, Object elem, Object value, Context cx) {
		if (isSymbol(elem)) {
			ScriptableObject.putProperty(cx, obj, (Symbol) elem, value);
		} else {
			StringIdOrIndex s = toStringIdOrIndex(cx, elem);
			if (s.stringId == null) {
				ScriptableObject.putProperty(cx, obj, s.index, value);
			} else {
				ScriptableObject.putProperty(cx, obj, s.stringId, value);
			}
		}

		return value;
	}

	/**
	 * Version of setObjectElem when elem is a valid JS identifier name.
	 */
	public static Object setObjectProp(Context cx, Object obj, String property, Object value, Scriptable scope) {
		if (!(obj instanceof Scriptable) && cx.isStrictMode()) {
			throw undefWriteError(obj, property, value);
		}

		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefWriteError(obj, property, value);
		}

		return setObjectProp(cx, sobj, property, value);
	}

	public static Object setObjectProp(Context cx, Scriptable obj, String property, Object value) {
		ScriptableObject.putProperty(cx, obj, property, value);
		return value;
	}

	/**
	 * A cheaper and less general version of the above for well-known argument
	 * types.
	 */
	public static Object setObjectIndex(Object obj, double dblIndex, Object value, Context cx, Scriptable scope) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefWriteError(obj, String.valueOf(dblIndex), value);
		}

		int index = (int) dblIndex;
		if (index == dblIndex) {
			return setObjectIndex(cx, sobj, index, value);
		}
		String s = toString(dblIndex);
		return setObjectProp(cx, sobj, s, value);
	}

	public static Object setObjectIndex(Context cx, Scriptable obj, int index, Object value) {
		ScriptableObject.putProperty(cx, obj, index, value);
		return value;
	}

	public static boolean deleteObjectElem(Context cx, Scriptable target, Object elem) {
		if (isSymbol(elem)) {
			SymbolScriptable so = ScriptableObject.ensureSymbolScriptable(target);
			Symbol s = (Symbol) elem;
			so.delete(cx, s);
			return !so.has(cx, s, target);
		}
		StringIdOrIndex s = toStringIdOrIndex(cx, elem);
		if (s.stringId == null) {
			target.delete(cx, s.index);
			return !target.has(cx, s.index, target);
		}
		target.delete(cx, s.stringId);
		return !target.has(cx, s.stringId, target);
	}

	public static boolean hasObjectElem(Context cx, Scriptable target, Object elem) {
		boolean result;

		if (isSymbol(elem)) {
			result = ScriptableObject.hasProperty(cx, target, (Symbol) elem);
		} else {
			StringIdOrIndex s = toStringIdOrIndex(cx, elem);
			if (s.stringId == null) {
				result = ScriptableObject.hasProperty(cx, target, s.index);
			} else {
				result = ScriptableObject.hasProperty(cx, target, s.stringId);
			}
		}

		return result;
	}

	public static Object refGet(Ref ref, Context cx) {
		return ref.get(cx);
	}

	public static Object refSet(Ref ref, Object value, Context cx, Scriptable scope) {
		return ref.set(cx, scope, value);
	}

	public static Object refDel(Ref ref, Context cx) {
		return wrapBoolean(ref.delete(cx));
	}

	static boolean isSpecialProperty(String s) {
		return s.equals("__proto__") || s.equals("__parent__");
	}

	public static Ref specialRef(Object obj, String specialProperty, Context cx, Scriptable scope) {
		return SpecialRef.createSpecial(cx, scope, obj, specialProperty);
	}

	/**
	 * The delete operator
	 * <p>
	 * See ECMA 11.4.1
	 * <p>
	 * In ECMA 0.19, the description of the delete operator (11.4.1)
	 * assumes that the [[Delete]] method returns a value. However,
	 * the definition of the [[Delete]] operator (8.6.2.5) does not
	 * define a return value. Here we assume that the [[Delete]]
	 * method doesn't return a value.
	 */
	public static Object delete(Object obj, Object id, Context cx, Scriptable scope, boolean isName) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			if (isName) {
				return Boolean.TRUE;
			}
			throw undefDeleteError(obj, id);
		}
		boolean result = deleteObjectElem(cx, sobj, id);
		return wrapBoolean(result);
	}

	/**
	 * Looks up a name in the scope chain and returns its value.
	 */
	public static Object name(Context cx, Scriptable scope, String name) {
		Scriptable parent = scope.getParentScope();
		if (parent == null) {
			Object result = topScopeName(cx, scope, name);
			if (result == Scriptable.NOT_FOUND) {
				throw notFoundError(scope, name);
			}
			return result;
		}

		return nameOrFunction(cx, scope, parent, name, false);
	}

	private static Object nameOrFunction(Context cx, Scriptable scope, Scriptable parentScope, String name, boolean asFunctionCall) {
		Object result;
		Scriptable thisObj = scope; // It is used only if asFunctionCall==true.

		while (true) {
			if (scope instanceof NativeWith) {
				Scriptable withObj = scope.getPrototype(cx);
				result = ScriptableObject.getProperty(cx, withObj, name);
				if (result != Scriptable.NOT_FOUND) {
					// function this should be the target object of with
					thisObj = withObj;
					break;
				}
			} else if (scope instanceof NativeCall) {
				// NativeCall does not prototype chain and Scriptable.get
				// can be called directly.
				result = scope.get(cx, name, scope);
				if (result != Scriptable.NOT_FOUND) {
					if (asFunctionCall) {
						// ECMA 262 requires that this for nested funtions
						// should be top scope
						thisObj = ScriptableObject.getTopLevelScope(parentScope);
					}
					break;
				}
			} else {
				// Can happen if Rhino embedding decided that nested
				// scopes are useful for what ever reasons.
				result = ScriptableObject.getProperty(cx, scope, name);
				if (result != Scriptable.NOT_FOUND) {
					thisObj = scope;
					break;
				}
			}
			scope = parentScope;
			parentScope = parentScope.getParentScope();
			if (parentScope == null) {
				result = topScopeName(cx, scope, name);
				if (result == Scriptable.NOT_FOUND) {
					throw notFoundError(scope, name);
				}
				// For top scope thisObj for functions is always scope itself.
				thisObj = scope;
				break;
			}
		}

		if (asFunctionCall) {
			if (!(result instanceof Callable)) {
				throw notFunctionError(result, name);
			}
			storeScriptable(cx, thisObj);
		}

		return result;
	}

	private static Object topScopeName(Context cx, Scriptable scope, String name) {
		if (cx.useDynamicScope) {
			scope = checkDynamicScope(cx, cx.topCallScope, scope);
		}
		return ScriptableObject.getProperty(cx, scope, name);
	}


	/**
	 * Returns the object in the scope chain that has a given property.
	 * <p>
	 * The order of evaluation of an assignment expression involves
	 * evaluating the lhs to a reference, evaluating the rhs, and then
	 * modifying the reference with the rhs value. This method is used
	 * to 'bind' the given name to an object containing that property
	 * so that the side effects of evaluating the rhs do not affect
	 * which property is modified.
	 * Typically used in conjunction with setName.
	 * <p>
	 * See ECMA 10.1.4
	 */
	public static Scriptable bind(Context cx, Scriptable scope, String id) {
		Scriptable firstXMLObject = null;
		Scriptable parent = scope.getParentScope();
		childScopesChecks:
		if (parent != null) {
			// Check for possibly nested "with" scopes first
			while (scope instanceof NativeWith) {
				Scriptable withObj = scope.getPrototype(cx);
				if (ScriptableObject.hasProperty(cx, withObj, id)) {
					return withObj;
				}

				scope = parent;
				parent = parent.getParentScope();
				if (parent == null) {
					break childScopesChecks;
				}
			}
			for (; ; ) {
				if (ScriptableObject.hasProperty(cx, scope, id)) {
					return scope;
				}
				scope = parent;
				parent = parent.getParentScope();
				if (parent == null) {
					break childScopesChecks;
				}
			}
		}
		// scope here is top scope
		if (cx.useDynamicScope) {
			scope = checkDynamicScope(cx, cx.topCallScope, scope);
		}
		if (ScriptableObject.hasProperty(cx, scope, id)) {
			return scope;
		}
		// Nothing was found, but since XML objects always bind
		// return one if found
		return firstXMLObject;
	}

	public static Object setName(Scriptable bound, Object value, Context cx, Scriptable scope, String id) {
		if (bound != null) {
			// TODO: we used to special-case XMLObject here, but putProperty
			// seems to work for E4X and it's better to optimize  the common case
			ScriptableObject.putProperty(cx, bound, id, value);
		} else {
			// "newname = 7;", where 'newname' has not yet
			// been defined, creates a new property in the
			// top scope unless strict mode is specified.
			if (cx.hasFeature(Context.FEATURE_STRICT_MODE) || cx.hasFeature(Context.FEATURE_STRICT_VARS)) {
				Context.reportWarning(cx, getMessage1("msg.assn.create.strict", id));
			}
			// Find the top scope by walking up the scope chain.
			bound = ScriptableObject.getTopLevelScope(scope);
			if (cx.useDynamicScope) {
				bound = checkDynamicScope(cx, cx.topCallScope, bound);
			}
			bound.put(cx, id, bound, value);
		}
		return value;
	}

	public static Object strictSetName(Context cx, Scriptable bound, Object value, Scriptable scope, String id) {
		if (bound != null) {
			// TODO: The LeftHandSide also may not be a reference to a
			// data property with the attribute value {[[Writable]]:false},
			// to an accessor property with the attribute value
			// {[[Put]]:undefined}, nor to a non-existent property of an
			// object whose [[Extensible]] internal property has the value
			// false. In these cases a TypeError exception is thrown (11.13.1).
			// TODO: we used to special-case XMLObject here, but putProperty
			// seems to work for E4X and we should optimize  the common case
			ScriptableObject.putProperty(cx, bound, id, value);
			return value;
		}
		// See ES5 8.7.2
		String msg = "Assignment to undefined \"" + id + "\" in strict mode";
		throw constructError("ReferenceError", msg);
	}

	public static Object setConst(Context cx, Scriptable bound, Object value, String id) {
		ScriptableObject.putConstProperty(cx, bound, id, value);
		return value;
	}

	public static Scriptable toIterator(Context cx, Scriptable scope, Scriptable obj, boolean keyOnly) {
		if (ScriptableObject.hasProperty(cx, obj, NativeIterator.ITERATOR_PROPERTY_NAME)) {
			Object v = ScriptableObject.getProperty(cx, obj, NativeIterator.ITERATOR_PROPERTY_NAME);
			if (!(v instanceof Callable f)) {
				throw typeError0("msg.invalid.iterator");
			}
			Object[] args = new Object[]{keyOnly ? Boolean.TRUE : Boolean.FALSE};
			v = f.call(cx, scope, obj, args);
			if (!(v instanceof Scriptable)) {
				throw typeError0("msg.iterator.primitive");
			}
			return (Scriptable) v;
		}
		return null;
	}

	public static final int ENUMERATE_KEYS = 0;
	public static final int ENUMERATE_VALUES = 1;
	public static final int ENUMERATE_ARRAY = 2;
	public static final int ENUMERATE_KEYS_NO_ITERATOR = 3;
	public static final int ENUMERATE_VALUES_NO_ITERATOR = 4;
	public static final int ENUMERATE_ARRAY_NO_ITERATOR = 5;
	public static final int ENUMERATE_VALUES_IN_ORDER = 6;

	public static IdEnumeration enumInit(Context cx, Object value, Scriptable scope, int enumType) {
		IdEnumeration x = new IdEnumeration();
		x.obj = toObjectOrNull(cx, value, scope);
		// "for of" loop
		if (enumType == ENUMERATE_VALUES_IN_ORDER) {
			x.enumType = enumType;
			x.iterator = null;
			return enumInitInOrder(cx, x);
		}
		if (x.obj == null) {
			// null or undefined do not cause errors but rather lead to empty
			// "for in" loop
			return x;
		}
		x.enumType = enumType;
		x.iterator = null;
		if (enumType != ENUMERATE_KEYS_NO_ITERATOR && enumType != ENUMERATE_VALUES_NO_ITERATOR && enumType != ENUMERATE_ARRAY_NO_ITERATOR) {
			x.iterator = toIterator(cx, x.obj.getParentScope(), x.obj, enumType == ENUMERATE_KEYS);
		}
		if (x.iterator == null) {
			// enumInit should read all initial ids before returning
			// or "for (a.i in a)" would wrongly enumerate i in a as well
			x.changeObject(cx);
		}

		return x;
	}

	private static IdEnumeration enumInitInOrder(Context cx, IdEnumeration x) {
		Object iterator = x.obj instanceof SymbolScriptable ? ScriptableObject.getProperty(cx, x.obj, SymbolKey.ITERATOR) : null;

		if (!(iterator instanceof Callable f)) {
			if (iterator instanceof IdEnumerationIterator) {
				x.iterator = (IdEnumerationIterator) iterator;
				return x;
			}

			throw typeError1("msg.not.iterable", toString(x.obj));
		}

		Scriptable scope = x.obj.getParentScope();
		Object v = f.call(cx, scope, x.obj, EMPTY_ARGS);

		if (!(v instanceof Scriptable)) {
			if (v instanceof IdEnumerationIterator) {
				x.iterator = (IdEnumerationIterator) v;
				return x;
			}

			throw typeError1("msg.not.iterable", toString(x.obj));
		}

		x.iterator = (Scriptable) v;
		return x;
	}

	/**
	 * Prepare for calling name(...): return function corresponding to
	 * name and make current top scope available
	 * as lastStoredScriptable() for consumption as thisObj.
	 * The caller must call lastStoredScriptable() immediately
	 * after calling this method.
	 */
	public static Callable getNameFunctionAndThis(String name, Context cx, Scriptable scope) {
		Scriptable parent = scope.getParentScope();
		if (parent == null) {
			Object result = topScopeName(cx, scope, name);
			if (!(result instanceof Callable)) {
				if (result == Scriptable.NOT_FOUND) {
					throw notFoundError(scope, name);
				}
				throw notFunctionError(result, name);
			}
			// Top scope is not NativeWith or NativeCall => thisObj == scope
			Scriptable thisObj = scope;
			storeScriptable(cx, thisObj);
			return (Callable) result;
		}

		// name will call storeScriptable(cx, thisObj);
		return (Callable) nameOrFunction(cx, scope, parent, name, true);
	}

	/**
	 * Prepare for calling obj[id](...): return function corresponding to
	 * obj[id] and make obj properly converted to Scriptable available
	 * as lastStoredScriptable() for consumption as thisObj.
	 * The caller must call lastStoredScriptable() immediately
	 * after calling this method.
	 */
	public static Callable getElemFunctionAndThis(Object obj, Object elem, Context cx, Scriptable scope) {
		Scriptable thisObj;
		Object value;

		if (isSymbol(elem)) {
			thisObj = toObjectOrNull(cx, obj, scope);
			if (thisObj == null) {
				throw undefCallError(obj, String.valueOf(elem));
			}
			value = ScriptableObject.getProperty(cx, thisObj, (Symbol) elem);

		} else {
			StringIdOrIndex s = toStringIdOrIndex(cx, elem);
			if (s.stringId != null) {
				return getPropFunctionAndThis(obj, s.stringId, cx, scope);
			}

			thisObj = toObjectOrNull(cx, obj, scope);
			if (thisObj == null) {
				throw undefCallError(obj, String.valueOf(elem));
			}

			value = ScriptableObject.getProperty(cx, thisObj, s.index);
		}

		if (!(value instanceof Callable)) {
			throw notFunctionError(value, elem);
		}

		storeScriptable(cx, thisObj);
		return (Callable) value;
	}

	/**
	 * Prepare for calling obj.property(...): return function corresponding to
	 * obj.property and make obj properly converted to Scriptable available
	 * as lastStoredScriptable() for consumption as thisObj.
	 * The caller must call lastStoredScriptable() immediately
	 * after calling this method.
	 */
	public static Callable getPropFunctionAndThis(Object obj, String property, Context cx, Scriptable scope) {
		Scriptable thisObj = toObjectOrNull(cx, obj, scope);
		return getPropFunctionAndThisHelper(obj, property, cx, thisObj);
	}

	private static Callable getPropFunctionAndThisHelper(Object obj, String property, Context cx, Scriptable thisObj) {
		if (thisObj == null) {
			throw undefCallError(obj, property);
		}

		Object value = ScriptableObject.getProperty(cx, thisObj, property);
		if (!(value instanceof Callable)) {
			Object noSuchMethod = ScriptableObject.getProperty(cx, thisObj, "__noSuchMethod__");
			if (noSuchMethod instanceof Callable) {
				value = new NoSuchMethodShim((Callable) noSuchMethod, property);
			}
		}

		if (!(value instanceof Callable)) {
			throw notFunctionError(thisObj, value, property);
		}

		storeScriptable(cx, thisObj);
		return (Callable) value;
	}

	/**
	 * Prepare for calling &lt;expression&gt;(...): return function corresponding to
	 * &lt;expression&gt; and make parent scope of the function available
	 * as lastStoredScriptable() for consumption as thisObj.
	 * The caller must call lastStoredScriptable() immediately
	 * after calling this method.
	 */
	public static Callable getValueFunctionAndThis(Object value, Context cx) {
		if (!(value instanceof Callable f)) {
			throw notFunctionError(value);
		}

		Scriptable thisObj = null;
		if (f instanceof Scriptable) {
			thisObj = ((Scriptable) f).getParentScope();
		}
		if (thisObj == null) {
			if (cx.topCallScope == null) {
				throw new IllegalStateException();
			}
			thisObj = cx.topCallScope;
		}
		if (thisObj.getParentScope() != null) {
			if (thisObj instanceof NativeWith) {
				// functions defined inside with should have with target
				// as their thisObj
			} else if (thisObj instanceof NativeCall) {
				// nested functions should have top scope as their thisObj
				thisObj = ScriptableObject.getTopLevelScope(thisObj);
			}
		}
		storeScriptable(cx, thisObj);
		return f;
	}

	/**
	 * Given an object, get the "Symbol.iterator" element, throw a TypeError if it
	 * is not present, then call the result, (throwing a TypeError if the result is
	 * not a function), and return that result, whatever it is.
	 */
	public static Object callIterator(Object obj, Context cx, Scriptable scope) {
		final Callable getIterator = getElemFunctionAndThis(obj, SymbolKey.ITERATOR, cx, scope);
		final Scriptable iterable = lastStoredScriptable(cx);
		return getIterator.call(cx, scope, iterable, EMPTY_ARGS);
	}

	/**
	 * Given an iterator result, return true if and only if there is a "done"
	 * property that's true.
	 */
	public static boolean isIteratorDone(Context cx, Object result) {
		if (!(result instanceof Scriptable)) {
			return false;
		}
		final Object prop = getObjectProp(cx, (Scriptable) result, ES6Iterator.DONE_PROPERTY);
		return toBoolean(prop);
	}

	/**
	 * Perform function call in reference context. Should always
	 * return value that can be passed to
	 * {@link #refGet(Ref, Context)} or {@link #refSet(Ref, Object, Context, Scriptable)}
	 * arbitrary number of times.
	 * The args array reference should not be stored in any object that is
	 * can be GC-reachable after this method returns. If this is necessary,
	 * store args.clone(), not args array itself.
	 */
	public static Ref callRef(Callable function, Scriptable thisObj, Object[] args, Context cx) {
		if (function instanceof RefCallable rfunction) {
			Ref ref = rfunction.refCall(cx, thisObj, args);
			if (ref == null) {
				throw new IllegalStateException(rfunction.getClass().getName() + ".refCall() returned null");
			}
			return ref;
		}
		// No runtime support for now
		String msg = getMessage1("msg.no.ref.from.function", toString(function));
		throw constructError("ReferenceError", msg);
	}

	/**
	 * Operator new.
	 * <p>
	 * See ECMA 11.2.2
	 */
	public static Scriptable newObject(Context cx, Object fun, Scriptable scope, Object[] args) {
		if (!(fun instanceof Function function)) {
			throw notFunctionError(fun);
		}
		return function.construct(cx, scope, args);
	}

	public static Object callSpecial(Context cx, Callable fun, Scriptable thisObj, Object[] args, Scriptable scope, Scriptable callerThis, int callType, String filename, int lineNumber) {
		if (callType == Node.SPECIALCALL_EVAL) {
			if (thisObj.getParentScope() == null && NativeGlobal.isEvalFunction(fun)) {
				return evalSpecial(cx, scope, callerThis, args, filename, lineNumber);
			}
		} else if (callType == Node.SPECIALCALL_WITH) {
			if (NativeWith.isWithFunction(fun)) {
				throw Context.reportRuntimeError1(cx, "msg.only.from.new", "With");
			}
		} else {
			throw Kit.codeBug();
		}

		return fun.call(cx, scope, thisObj, args);
	}

	public static Object newSpecial(Context cx, Object fun, Object[] args, Scriptable scope, int callType) {
		if (callType == Node.SPECIALCALL_EVAL) {
			if (NativeGlobal.isEvalFunction(fun)) {
				throw typeError1("msg.not.ctor", "eval");
			}
		} else if (callType == Node.SPECIALCALL_WITH) {
			if (NativeWith.isWithFunction(fun)) {
				return NativeWith.newWithSpecial(cx, scope, args);
			}
		} else {
			throw Kit.codeBug();
		}

		return newObject(cx, fun, scope, args);
	}

	/**
	 * Function.prototype.apply and Function.prototype.call
	 * <p>
	 * See Ecma 15.3.4.[34]
	 */
	public static Object applyOrCall(Context cx, boolean isApply, Scriptable scope, Scriptable thisObj, Object[] args) {
		int L = args.length;
		Callable function = getCallable(cx, thisObj);

		Scriptable callThis = null;

		if (L != 0) {
			callThis = args[0] == Undefined.instance ? Undefined.SCRIPTABLE_UNDEFINED : toObjectOrNull(cx, args[0], scope);
		}

		Object[] callArgs;
		if (isApply) {
			// Follow Ecma 15.3.4.3
			callArgs = L <= 1 ? EMPTY_ARGS : getApplyArguments(cx, args[1]);
		} else {
			// Follow Ecma 15.3.4.4
			if (L <= 1) {
				callArgs = EMPTY_ARGS;
			} else {
				callArgs = new Object[L - 1];
				System.arraycopy(args, 1, callArgs, 0, L - 1);
			}
		}

		return function.call(cx, scope, callThis, callArgs);
	}

	/**
	 * @return true if the passed in Scriptable looks like an array
	 */
	private static boolean isArrayLike(Context cx, Scriptable obj) {
		return obj != null && (obj instanceof NativeJavaList || obj instanceof Arguments || ScriptableObject.hasProperty(cx, obj, "length"));
	}

	static Object[] getApplyArguments(Context cx, Object arg1) {
		if (arg1 == null || arg1 == Undefined.instance) {
			return EMPTY_ARGS;
		} else if (arg1 instanceof Scriptable && isArrayLike(cx, (Scriptable) arg1)) {
			return cx.getElements((Scriptable) arg1);
		} else if (arg1 instanceof ScriptableObject) {
			return EMPTY_ARGS;
		} else {
			throw typeError0("msg.arg.isnt.array");
		}
	}

	static Callable getCallable(Context cx, Scriptable thisObj) {
		Callable function;
		if (thisObj instanceof Callable) {
			function = (Callable) thisObj;
		} else {
			Object value = thisObj.getDefaultValue(cx, FunctionClass);
			if (!(value instanceof Callable)) {
				throw notFunctionError(value, thisObj);
			}
			function = (Callable) value;
		}
		return function;
	}

	/**
	 * The eval function property of the global object.
	 * <p>
	 * See ECMA 15.1.2.1
	 */
	public static Object evalSpecial(Context cx, Scriptable scope, Object thisArg, Object[] args, String filename, int lineNumber) {
		if (args.length < 1) {
			return Undefined.instance;
		}
		Object x = args[0];
		if (!(x instanceof CharSequence)) {
			if (cx.hasFeature(Context.FEATURE_STRICT_MODE) || cx.hasFeature(Context.FEATURE_STRICT_EVAL)) {
				throw Context.reportRuntimeError0(cx, "msg.eval.nonstring.strict");
			}
			String message = getMessage0("msg.eval.nonstring");
			Context.reportWarning(cx, message);
			return x;
		}
		if (filename == null) {
			int[] linep = new int[1];
			filename = Context.getSourcePositionFromStack(linep);
			if (filename != null) {
				lineNumber = linep[0];
			} else {
				filename = "";
			}
		}
		String sourceName = makeUrlForGeneratedScript(true, filename, lineNumber);

		ErrorReporter reporter;
		reporter = DefaultErrorReporter.forEval(cx.getErrorReporter());

		Evaluator evaluator = Context.createInterpreter();

		// Compile with explicit interpreter instance to force interpreter
		// mode.
		Script script = cx.compileString(x.toString(), evaluator, reporter, sourceName, 1, null);
		evaluator.setEvalScriptFlag(script);
		Callable c = (Callable) script;
		return c.call(cx, scope, (Scriptable) thisArg, EMPTY_ARGS);
	}

	/**
	 * The typeof operator
	 */
	public static MemberType typeof(Object value) {
		return MemberType.get(value);
	}

	/**
	 * The typeof operator that correctly handles the undefined case
	 */
	public static MemberType typeofName(Context cx, Scriptable scope, String id) {
		Scriptable val = bind(cx, scope, id);
		if (val == null) {
			return MemberType.UNDEFINED;
		}
		return typeof(getObjectProp(cx, val, id));
	}

	public static boolean isObject(Object value) {
		if (value == null) {
			return false;
		}
		if (Undefined.instance.equals(value)) {
			return false;
		}
		if (value instanceof ScriptableObject) {
			var type = ((ScriptableObject) value).getTypeOf();
			return type == MemberType.OBJECT || type == MemberType.FUNCTION;
		}
		if (value instanceof Scriptable) {
			return (!(value instanceof Callable));
		}
		return false;
	}

	// neg:
	// implement the '-' operator inline in the caller
	// as "-toNumber(val)"

	// not:
	// implement the '!' operator inline in the caller
	// as "!toBoolean(val)"

	// bitnot:
	// implement the '~' operator inline in the caller
	// as "~toInt32(val)"

	public static Object add(Object val1, Object val2, Context cx) {
		if (val1 instanceof Number && val2 instanceof Number) {
			return wrapNumber(((Number) val1).doubleValue() + ((Number) val2).doubleValue());
		}
		if ((val1 instanceof Symbol) || (val2 instanceof Symbol)) {
			throw typeError0("msg.not.a.number");
		}
		if (val1 instanceof Scriptable) {
			val1 = ((Scriptable) val1).getDefaultValue(cx, null);
		}
		if (val2 instanceof Scriptable) {
			val2 = ((Scriptable) val2).getDefaultValue(cx, null);
		}
		if (!(val1 instanceof CharSequence) && !(val2 instanceof CharSequence)) {
			if ((val1 instanceof Number) && (val2 instanceof Number)) {
				return wrapNumber(((Number) val1).doubleValue() + ((Number) val2).doubleValue());
			}
			return wrapNumber(toNumber(val1) + toNumber(val2));
		}
		return new ConsString(toCharSequence(val1), toCharSequence(val2));
	}

	public static CharSequence add(CharSequence val1, Object val2) {
		return new ConsString(val1, toCharSequence(val2));
	}

	public static CharSequence add(Object val1, CharSequence val2) {
		return new ConsString(toCharSequence(val1), val2);
	}

	public static Object nameIncrDecr(Scriptable scopeChain, String id, Context cx, int incrDecrMask) {
		Scriptable target;
		Object value;
		search:
		{
			do {
				if (cx.useDynamicScope && scopeChain.getParentScope() == null) {
					scopeChain = checkDynamicScope(cx, cx.topCallScope, scopeChain);
				}
				target = scopeChain;
				do {
					value = target.get(cx, id, scopeChain);
					if (value != Scriptable.NOT_FOUND) {
						break search;
					}
					target = target.getPrototype(cx);
				} while (target != null);
				scopeChain = scopeChain.getParentScope();
			} while (scopeChain != null);
			throw notFoundError(null, id);
		}
		return doScriptableIncrDecr(cx, target, id, scopeChain, value, incrDecrMask);
	}

	public static Object propIncrDecr(Object obj, String id, Context cx, Scriptable scope, int incrDecrMask) {
		Scriptable start = toObjectOrNull(cx, obj, scope);
		if (start == null) {
			throw undefReadError(obj, id);
		}

		Scriptable target = start;
		Object value;
		search:
		{
			do {
				value = target.get(cx, id, start);
				if (value != Scriptable.NOT_FOUND) {
					break search;
				}
				target = target.getPrototype(cx);
			} while (target != null);
			start.put(cx, id, start, NaNobj);
			return NaNobj;
		}
		return doScriptableIncrDecr(cx, target, id, start, value, incrDecrMask);
	}

	private static Object doScriptableIncrDecr(Context cx, Scriptable target, String id, Scriptable protoChainStart, Object value, int incrDecrMask) {
		final boolean post = (incrDecrMask & Node.POST_FLAG) != 0;
		double number;
		if (value instanceof Number) {
			number = ((Number) value).doubleValue();
		} else {
			number = toNumber(value);
			if (post) {
				// convert result to number
				value = wrapNumber(number);
			}
		}
		if ((incrDecrMask & Node.DECR_FLAG) == 0) {
			++number;
		} else {
			--number;
		}
		Number result = wrapNumber(number);
		target.put(cx, id, protoChainStart, result);
		if (post) {
			return value;
		}
		return result;
	}

	public static Object elemIncrDecr(Object obj, Object index, Context cx, Scriptable scope, int incrDecrMask) {
		Object value = getObjectElem(cx, obj, index, scope);
		final boolean post = (incrDecrMask & Node.POST_FLAG) != 0;
		double number;
		if (value instanceof Number) {
			number = ((Number) value).doubleValue();
		} else {
			number = toNumber(value);
			if (post) {
				// convert result to number
				value = wrapNumber(number);
			}
		}
		if ((incrDecrMask & Node.DECR_FLAG) == 0) {
			++number;
		} else {
			--number;
		}
		Number result = wrapNumber(number);
		setObjectElem(obj, index, result, cx, scope);
		if (post) {
			return value;
		}
		return result;
	}

	public static Object refIncrDecr(Ref ref, Context cx, Scriptable scope, int incrDecrMask) {
		Object value = ref.get(cx);
		boolean post = ((incrDecrMask & Node.POST_FLAG) != 0);
		double number;
		if (value instanceof Number) {
			number = ((Number) value).doubleValue();
		} else {
			number = toNumber(value);
			if (post) {
				// convert result to number
				value = wrapNumber(number);
			}
		}
		if ((incrDecrMask & Node.DECR_FLAG) == 0) {
			++number;
		} else {
			--number;
		}
		Number result = wrapNumber(number);
		ref.set(cx, scope, result);
		if (post) {
			return value;
		}
		return result;
	}

	public static Object toPrimitive(Context cx, Object val) {
		return toPrimitive(cx, val, null);
	}

	public static Object toPrimitive(Context cx, Object val, Class<?> typeHint) {
		if (!(val instanceof Scriptable s)) {
			return val;
		}
		Object result = s.getDefaultValue(cx, typeHint);
		if ((result instanceof Scriptable) && !isSymbol(result)) {
			throw typeError0("msg.bad.default.value");
		}
		return result;
	}

	/**
	 * Equality
	 * <p>
	 * See ECMA 11.9
	 */
	public static boolean eq(Context cx, Object x, Object y) {
		if (x == null || x == Undefined.instance) {
			if (y == null || y == Undefined.instance) {
				return true;
			}
			if (y instanceof ScriptableObject) {
				Object test = ((ScriptableObject) y).equivalentValues(x);
				if (test != Scriptable.NOT_FOUND) {
					return (Boolean) test;
				}
			}
			return false;
		} else if (x == y) {
			return true;
		}

		Object x1 = Wrapper.unwrapped(x);
		Object y1 = Wrapper.unwrapped(y);

		if (x1 == y1) {
			return true;
		} else if (SpecialEquality.checkSpecialEquality(cx, x1, y1, false)) {
			return true;
		} else if (SpecialEquality.checkSpecialEquality(cx, y1, x1, false)) {
			return true;
		} else if (x instanceof Number) {
			return eqNumber(cx, ((Number) x).doubleValue(), y);
		} else if (x instanceof CharSequence) {
			return eqString(cx, (CharSequence) x, y);
		} else if (x instanceof Boolean) {
			boolean b = (Boolean) x;
			if (y instanceof Boolean) {
				return b == (Boolean) y;
			}
			if (y instanceof ScriptableObject) {
				Object test = ((ScriptableObject) y).equivalentValues(x);
				if (test != Scriptable.NOT_FOUND) {
					return (Boolean) test;
				}
			}
			return eqNumber(cx, b ? 1.0 : 0.0, y);
		} else if (x instanceof Scriptable) {
			if (y instanceof Scriptable) {
				if (x instanceof ScriptableObject) {
					Object test = ((ScriptableObject) x).equivalentValues(y);
					if (test != Scriptable.NOT_FOUND) {
						return (Boolean) test;
					}
				}
				if (y instanceof ScriptableObject) {
					Object test = ((ScriptableObject) y).equivalentValues(x);
					if (test != Scriptable.NOT_FOUND) {
						return (Boolean) test;
					}
				}
				if (x instanceof Wrapper && y instanceof Wrapper) {
					// See bug 413838. Effectively an extension to ECMA for
					// the LiveConnect case.
					return x1 == y1 || (isPrimitive(x1) && isPrimitive(y1) && eq(cx, x1, y1));
				}
				return false;
			} else if (y instanceof Boolean) {
				if (x instanceof ScriptableObject) {
					Object test = ((ScriptableObject) x).equivalentValues(y);
					if (test != Scriptable.NOT_FOUND) {
						return (Boolean) test;
					}
				}
				double d = (Boolean) y ? 1.0 : 0.0;
				return eqNumber(cx, d, x);
			} else if (y instanceof Number) {
				return eqNumber(cx, ((Number) y).doubleValue(), x);
			} else if (y instanceof CharSequence) {
				return eqString(cx, (CharSequence) y, x);
			}
			// covers the case when y == Undefined.instance as well
			return false;
		} else {
			warnAboutNonJSObject(x);
			return x == y;
		}
	}

	/*
	 * Implement "SameValue" as in ECMA 7.2.9. This is not the same as "eq" because it handles
	 * signed zeroes and NaNs differently.
	 */
	public static boolean same(Context cx, Object x, Object y) {
		if (typeof(x) != typeof(y)) {
			return false;
		}
		if (x instanceof Number) {
			if (isNaN(x) && isNaN(y)) {
				return true;
			}
			return x.equals(y);
		}
		return eq(cx, x, y);
	}

	/**
	 * Implement "SameValueZero" from ECMA 7.2.9
	 */
	public static boolean sameZero(Context cx, Object x, Object y) {
		x = Wrapper.unwrapped(x);
		y = Wrapper.unwrapped(y);

		if (typeof(x) != typeof(y)) {
			return false;
		}
		if (x instanceof Number) {
			if (isNaN(x) && isNaN(y)) {
				return true;
			}
			final double dx = ((Number) x).doubleValue();
			if (y instanceof Number) {
				final double dy = ((Number) y).doubleValue();
				if (((dx == negativeZero) && (dy == 0.0)) || ((dx == 0.0) && dy == negativeZero)) {
					return true;
				}
			}
			return eqNumber(cx, dx, y);
		}
		return eq(cx, x, y);
	}

	public static boolean isNaN(Object n) {
		if (n instanceof Double) {
			return ((Double) n).isNaN();
		}
		if (n instanceof Float) {
			return ((Float) n).isNaN();
		}
		return false;
	}

	public static boolean isPrimitive(Object obj) {
		return obj == null || obj == Undefined.instance || (obj instanceof Number) || (obj instanceof String) || (obj instanceof Boolean);
	}

	static boolean eqNumber(Context cx, double x, Object y) {
		if (y == null || y == Undefined.instance) {
			return false;
		} else if (y instanceof Number) {
			return x == ((Number) y).doubleValue();
		} else if (y instanceof CharSequence) {
			return x == toNumber(y);
		} else if (y instanceof Boolean) {
			return x == ((Boolean) y ? 1.0 : +0.0);
		} else if (isSymbol(y)) {
			return false;
		} else if (y instanceof Scriptable) {
			if (y instanceof ScriptableObject) {
				Object xval = wrapNumber(x);
				Object test = ((ScriptableObject) y).equivalentValues(xval);
				if (test != Scriptable.NOT_FOUND) {
					return (Boolean) test;
				}
			}
			return eqNumber(cx, x, toPrimitive(cx, y));
		} else {
			warnAboutNonJSObject(y);
			return false;
		}
	}

	private static boolean eqString(Context cx, CharSequence x, Object y) {
		if (y == null || y == Undefined.instance) {
			return false;
		} else if (y instanceof CharSequence c) {
			return x.length() == c.length() && x.toString().equals(c.toString());
		} else if (y instanceof Number) {
			return toNumber(x.toString()) == ((Number) y).doubleValue();
		} else if (y instanceof Boolean) {
			return toNumber(x.toString()) == ((Boolean) y ? 1.0 : 0.0);
		} else if (isSymbol(y)) {
			return false;
		} else if (y instanceof Scriptable) {
			if (y instanceof ScriptableObject) {
				Object test = ((ScriptableObject) y).equivalentValues(x.toString());
				if (test != Scriptable.NOT_FOUND) {
					return (Boolean) test;
				}
			}
			return eqString(cx, x, toPrimitive(cx, y));
		} else {
			warnAboutNonJSObject(y);
			return false;
		}
	}

	public static boolean shallowEq(Context cx, Object x, Object y) {
		if (x == y) {
			if (!(x instanceof Number)) {
				return true;
			}
			// NaN check
			double d = ((Number) x).doubleValue();
			return !Double.isNaN(d);
		} else if (x == null || x == Undefined.instance || x == Undefined.SCRIPTABLE_UNDEFINED) {
			return (x == Undefined.instance && y == Undefined.SCRIPTABLE_UNDEFINED) || (x == Undefined.SCRIPTABLE_UNDEFINED && y == Undefined.instance);
		}

		Object x1 = Wrapper.unwrapped(x);
		Object y1 = Wrapper.unwrapped(y);

		if (x1 == y1) {
			return true;
		} else if (SpecialEquality.checkSpecialEquality(cx, x1, y1, true)) {
			return true;
		} else if (SpecialEquality.checkSpecialEquality(cx, y1, x1, true)) {
			return true;
		} else if (x1 instanceof Number) {
			if (y1 instanceof Number) {
				return ((Number) x1).doubleValue() == ((Number) y1).doubleValue();
			}
		} else if (x1 instanceof CharSequence) {
			return x1.toString().equals(String.valueOf(y1));
		} else if (y1 instanceof CharSequence) {
			return y1.toString().equals(String.valueOf(x1));
		} else if (x1 instanceof Boolean) {
			if (y1 instanceof Boolean) {
				return x1.equals(y1);
			}
		} else if (!(x1 instanceof Scriptable)) {
			warnAboutNonJSObject(x1);
		}

		return false;
	}

	/**
	 * The instanceof operator.
	 *
	 * @return a instanceof b
	 */
	public static boolean instanceOf(Context cx, Object a, Object b) {
		// Check RHS is an object
		if (!(b instanceof Scriptable)) {
			throw typeError0("msg.instanceof.not.object");
		}

		// for primitive values on LHS, return false
		if (!(a instanceof Scriptable)) {
			return false;
		}

		return ((Scriptable) b).hasInstance(cx, (Scriptable) a);
	}

	/**
	 * Delegates to
	 *
	 * @return true iff rhs appears in lhs' proto chain
	 */
	public static boolean jsDelegatesTo(Context cx, Scriptable lhs, Scriptable rhs) {
		Scriptable proto = lhs.getPrototype(cx);

		while (proto != null) {
			if (proto.equals(rhs)) {
				return true;
			}
			proto = proto.getPrototype(cx);
		}

		return false;
	}

	/**
	 * The in operator.
	 * <p>
	 * This is a new JS 1.3 language feature.  The in operator mirrors
	 * the operation of the for .. in construct, and tests whether the
	 * rhs has the property given by the lhs.  It is different from the
	 * for .. in construct in that:
	 * <BR> - it doesn't perform ToObject on the right hand side
	 * <BR> - it returns true for DontEnum properties.
	 *
	 * @param a the left hand operand
	 * @param b the right hand operand
	 * @return true if property name or element number a is a property of b
	 */
	public static boolean in(Context cx, Object a, Object b) {
		if (!(b instanceof Scriptable)) {
			throw typeError0("msg.in.not.object");
		}

		return hasObjectElem(cx, (Scriptable) b, a);
	}

	public static boolean cmp_LT(Context cx, Object val1, Object val2) {
		double d1, d2;
		if (val1 instanceof Number && val2 instanceof Number) {
			d1 = ((Number) val1).doubleValue();
			d2 = ((Number) val2).doubleValue();
		} else {
			if ((val1 instanceof Symbol) || (val2 instanceof Symbol)) {
				throw typeError0("msg.compare.symbol");
			}
			if (val1 instanceof Scriptable) {
				val1 = ((Scriptable) val1).getDefaultValue(cx, NumberClass);
			}
			if (val2 instanceof Scriptable) {
				val2 = ((Scriptable) val2).getDefaultValue(cx, NumberClass);
			}
			if (val1 instanceof CharSequence && val2 instanceof CharSequence) {
				return val1.toString().compareTo(val2.toString()) < 0;
			}
			d1 = toNumber(val1);
			d2 = toNumber(val2);
		}
		return d1 < d2;
	}

	public static boolean cmp_LE(Context cx, Object val1, Object val2) {
		double d1, d2;
		if (val1 instanceof Number && val2 instanceof Number) {
			d1 = ((Number) val1).doubleValue();
			d2 = ((Number) val2).doubleValue();
		} else {
			if ((val1 instanceof Symbol) || (val2 instanceof Symbol)) {
				throw typeError0("msg.compare.symbol");
			}
			if (val1 instanceof Scriptable) {
				val1 = ((Scriptable) val1).getDefaultValue(cx, NumberClass);
			}
			if (val2 instanceof Scriptable) {
				val2 = ((Scriptable) val2).getDefaultValue(cx, NumberClass);
			}
			if (val1 instanceof CharSequence && val2 instanceof CharSequence) {
				return val1.toString().compareTo(val2.toString()) <= 0;
			}
			d1 = toNumber(val1);
			d2 = toNumber(val2);
		}
		return d1 <= d2;
	}

	// ------------------
	// Statements
	// ------------------

	public static boolean hasTopCall(Context cx) {
		return (cx.topCallScope != null);
	}

	public static Scriptable getTopCallScope(Context cx) {
		Scriptable scope = cx.topCallScope;
		if (scope == null) {
			throw new IllegalStateException();
		}
		return scope;
	}

	public static Object doTopCall(Callable callable, Context cx, Scriptable scope, Scriptable thisObj, Object[] args, boolean isTopLevelStrict) {
		if (scope == null) {
			throw new IllegalArgumentException();
		}
		if (cx.topCallScope != null) {
			throw new IllegalStateException();
		}

		Object result;
		cx.topCallScope = ScriptableObject.getTopLevelScope(scope);
		cx.useDynamicScope = cx.hasFeature(Context.FEATURE_DYNAMIC_SCOPE);
		boolean previousTopLevelStrict = cx.isTopLevelStrict;
		cx.isTopLevelStrict = isTopLevelStrict;
		ContextFactory f = cx.getFactory();
		try {
			result = f.doTopCall(callable, cx, scope, thisObj, args);
		} finally {
			cx.topCallScope = null;
			// Cleanup cached references
			cx.isTopLevelStrict = previousTopLevelStrict;

			if (cx.currentActivationCall != null) {
				// Function should always call exitActivationFunction
				// if it creates activation record
				throw new IllegalStateException();
			}
		}
		return result;
	}

	/**
	 * Return <code>possibleDynamicScope</code> if <code>staticTopScope</code>
	 * is present on its prototype chain and return <code>staticTopScope</code>
	 * otherwise.
	 * Should only be called when <code>staticTopScope</code> is top scope.
	 */
	static Scriptable checkDynamicScope(Context cx, Scriptable possibleDynamicScope, Scriptable staticTopScope) {
		// Return cx.topCallScope if scope
		if (possibleDynamicScope == staticTopScope) {
			return possibleDynamicScope;
		}
		Scriptable proto = possibleDynamicScope;
		for (; ; ) {
			proto = proto.getPrototype(cx);
			if (proto == staticTopScope) {
				return possibleDynamicScope;
			}
			if (proto == null) {
				return staticTopScope;
			}
		}
	}

	public static void addInstructionCount(Context cx, int instructionsToAdd) {
		cx.instructionCount += instructionsToAdd;
		if (cx.instructionCount > cx.instructionThreshold) {
			cx.observeInstructionCount(cx.instructionCount);
			cx.instructionCount = 0;
		}
	}

	public static void initScript(Context cx, NativeFunction funObj, Scriptable thisObj, Scriptable scope, boolean evalScript) {
		if (cx.topCallScope == null) {
			throw new IllegalStateException();
		}

		int varCount = funObj.getParamAndVarCount();
		if (varCount != 0) {

			Scriptable varScope = scope;
			// Never define any variables from var statements inside with
			// object. See bug 38590.
			while (varScope instanceof NativeWith) {
				varScope = varScope.getParentScope();
			}

			for (int i = varCount; i-- != 0; ) {
				String name = funObj.getParamOrVarName(i);
				boolean isConst = funObj.getParamOrVarConst(i);
				// Don't overwrite existing def if already defined in object
				// or prototypes of object.
				if (!ScriptableObject.hasProperty(cx, scope, name)) {
					if (isConst) {
						ScriptableObject.defineConstProperty(cx, varScope, name);
					} else if (!evalScript) {
						if (!(funObj instanceof InterpretedFunction) || ((InterpretedFunction) funObj).hasFunctionNamed(name)) {
							// Global var definitions are supposed to be DONTDELETE
							ScriptableObject.defineProperty(cx, varScope, name, Undefined.instance, ScriptableObject.PERMANENT);
						}
					} else {
						varScope.put(cx, name, varScope, Undefined.instance);
					}
				} else {
					ScriptableObject.redefineProperty(cx, scope, name, isConst);
				}
			}
		}
	}

	public static Scriptable createFunctionActivation(Context cx, NativeFunction funObj, Scriptable scope, Object[] args, boolean isStrict) {
		return new NativeCall(cx, funObj, scope, args, false, isStrict);
	}

	public static Scriptable createArrowFunctionActivation(Context cx, NativeFunction funObj, Scriptable scope, Object[] args, boolean isStrict) {
		return new NativeCall(cx, funObj, scope, args, true, isStrict);
	}

	public static void enterActivationFunction(Context cx, Scriptable scope) {
		if (cx.topCallScope == null) {
			throw new IllegalStateException();
		}
		NativeCall call = (NativeCall) scope;
		call.parentActivationCall = cx.currentActivationCall;
		cx.currentActivationCall = call;
		call.defineAttributesForArguments(cx);
	}

	public static void exitActivationFunction(Context cx) {
		NativeCall call = cx.currentActivationCall;
		cx.currentActivationCall = call.parentActivationCall;
		call.parentActivationCall = null;
	}

	static NativeCall findFunctionActivation(Context cx, Function f) {
		NativeCall call = cx.currentActivationCall;
		while (call != null) {
			if (call.function == f) {
				return call;
			}
			call = call.parentActivationCall;
		}
		return null;
	}

	public static Scriptable newCatchScope(Throwable t, Scriptable lastCatchScope, String exceptionName, Context cx, Scriptable scope) {
		Object obj;
		boolean cacheObj;

		getObj:
		if (t instanceof JavaScriptException) {
			cacheObj = false;
			obj = ((JavaScriptException) t).getValue();
		} else {
			cacheObj = true;

			// Create wrapper object unless it was associated with
			// the previous scope object

			if (lastCatchScope != null) {
				NativeObject last = (NativeObject) lastCatchScope;
				obj = last.getAssociatedValue(t);
				if (obj == null) {
					throw Kit.codeBug();
				}
				break getObj;
			}

			RhinoException re;
			TopLevel.NativeErrors type;
			String errorMsg;
			Throwable javaException = null;

			if (t instanceof EcmaError ee) {
				re = ee;
				type = TopLevel.NativeErrors.valueOf(ee.getName());
				errorMsg = ee.getErrorMessage();
			} else if (t instanceof WrappedException we) {
				re = we;
				javaException = we.getWrappedException();
				type = TopLevel.NativeErrors.JavaException;
				errorMsg = javaException.getClass().getName() + ": " + javaException.getMessage();
			} else if (t instanceof EvaluatorException ee) {
				// Pure evaluator exception, nor WrappedException instance
				re = ee;
				type = TopLevel.NativeErrors.InternalError;
				errorMsg = ee.getMessage();
			} else if (cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS)) {
				// With FEATURE_ENHANCED_JAVA_ACCESS, scripts can catch
				// all exception types
				re = new WrappedException(t);
				type = TopLevel.NativeErrors.JavaException;
				errorMsg = t.toString();
			} else {
				// Script can catch only instances of JavaScriptException,
				// EcmaError and EvaluatorException
				throw Kit.codeBug();
			}

			String sourceUri = re.sourceName();
			if (sourceUri == null) {
				sourceUri = "";
			}
			int line = re.lineNumber();
			Object[] args;
			if (line > 0) {
				args = new Object[]{errorMsg, sourceUri, line};
			} else {
				args = new Object[]{errorMsg, sourceUri};
			}

			Scriptable errorObject = newNativeError(cx, scope, type, args);
			// set exception in Error objects to enable non-ECMA "stack" property
			if (errorObject instanceof NativeError) {
				((NativeError) errorObject).setStackProvider(re);
			}

			if (javaException != null && isVisible(cx, javaException, ClassShutter.TYPE_EXCEPTION)) {
				Object wrap = cx.getWrapFactory().wrap(cx, scope, javaException, null);
				ScriptableObject.defineProperty(cx, errorObject, "javaException", wrap, ScriptableObject.PERMANENT | ScriptableObject.READONLY | ScriptableObject.DONTENUM);
			}
			if (isVisible(cx, re, ClassShutter.TYPE_EXCEPTION)) {
				Object wrap = cx.getWrapFactory().wrap(cx, scope, re, null);
				ScriptableObject.defineProperty(cx, errorObject, "rhinoException", wrap, ScriptableObject.PERMANENT | ScriptableObject.READONLY | ScriptableObject.DONTENUM);
			}
			obj = errorObject;
		}

		NativeObject catchScopeObject = new NativeObject();
		// See ECMA 12.4
		catchScopeObject.defineProperty(cx, exceptionName, obj, ScriptableObject.PERMANENT);

		if (isVisible(cx, t, ClassShutter.TYPE_EXCEPTION)) {
			// Add special Rhino object __exception__ defined in the catch
			// scope that can be used to retrieve the Java exception associated
			// with the JavaScript exception (to get stack trace info, etc.)
			catchScopeObject.defineProperty(cx, "__exception__", Context.javaToJS(t, scope), ScriptableObject.PERMANENT | ScriptableObject.DONTENUM);
		}

		if (cacheObj) {
			catchScopeObject.associateValue(t, obj);
		}
		return catchScopeObject;
	}

	public static Scriptable wrapException(Throwable t, Scriptable scope, Context cx) {
		RhinoException re;
		String errorName;
		String errorMsg;
		Throwable javaException = null;

		if (t instanceof EcmaError ee) {
			re = ee;
			errorName = ee.getName();
			errorMsg = ee.getErrorMessage();
		} else if (t instanceof WrappedException we) {
			re = we;
			javaException = we.getWrappedException();
			errorName = "JavaException";
			errorMsg = javaException.getClass().getName() + ": " + javaException.getMessage();
		} else if (t instanceof EvaluatorException ee) {
			// Pure evaluator exception, nor WrappedException instance
			re = ee;
			errorName = "InternalError";
			errorMsg = ee.getMessage();
		} else if (cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS)) {
			// With FEATURE_ENHANCED_JAVA_ACCESS, scripts can catch
			// all exception types
			re = new WrappedException(t);
			errorName = "JavaException";
			errorMsg = t.toString();
		} else {
			// Script can catch only instances of JavaScriptException,
			// EcmaError and EvaluatorException
			throw Kit.codeBug();
		}

		String sourceUri = re.sourceName();
		if (sourceUri == null) {
			sourceUri = "";
		}
		int line = re.lineNumber();
		Object[] args;
		if (line > 0) {
			args = new Object[]{errorMsg, sourceUri, line};
		} else {
			args = new Object[]{errorMsg, sourceUri};
		}

		Scriptable errorObject = cx.newObject(scope, errorName, args);
		ScriptableObject.putProperty(cx, errorObject, "name", errorName);
		// set exception in Error objects to enable non-ECMA "stack" property
		if (errorObject instanceof NativeError) {
			((NativeError) errorObject).setStackProvider(re);
		}

		if (javaException != null && isVisible(cx, javaException, ClassShutter.TYPE_EXCEPTION)) {
			Object wrap = cx.getWrapFactory().wrap(cx, scope, javaException, null);
			ScriptableObject.defineProperty(cx, errorObject, "javaException", wrap, ScriptableObject.PERMANENT | ScriptableObject.READONLY | ScriptableObject.DONTENUM);
		}
		if (isVisible(cx, re, ClassShutter.TYPE_EXCEPTION)) {
			Object wrap = cx.getWrapFactory().wrap(cx, scope, re, null);
			ScriptableObject.defineProperty(cx, errorObject, "rhinoException", wrap, ScriptableObject.PERMANENT | ScriptableObject.READONLY | ScriptableObject.DONTENUM);
		}
		return errorObject;
	}

	private static boolean isVisible(Context cx, Object obj, int type) {
		ClassShutter shutter = cx.getClassShutter();
		return shutter == null || shutter.visibleToScripts(obj.getClass().getName(), type);
	}

	public static Scriptable enterWith(Object obj, Context cx, Scriptable scope) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw typeError1("msg.undef.with", toString(obj));
		}
		return new NativeWith(scope, sobj);
	}

	public static Scriptable leaveWith(Scriptable scope) {
		NativeWith nw = (NativeWith) scope;
		return nw.getParentScope();
	}

	public static Scriptable enterDotQuery(Object value, Scriptable scope) {
		throw notXmlError(value);
	}

	public static Object updateDotQuery(boolean value, Scriptable scope) {
		// Return null to continue looping
		NativeWith nw = (NativeWith) scope;
		return nw.updateDotQuery(value);
	}

	public static Scriptable leaveDotQuery(Scriptable scope) {
		NativeWith nw = (NativeWith) scope;
		return nw.getParentScope();
	}

	public static void setFunctionProtoAndParent(Context cx, BaseFunction fn, Scriptable scope) {
		setFunctionProtoAndParent(cx, fn, scope, false);
	}

	public static void setFunctionProtoAndParent(Context cx, BaseFunction fn, Scriptable scope, boolean es6GeneratorFunction) {
		fn.setParentScope(scope);
		if (es6GeneratorFunction) {
			fn.setPrototype(cx, ScriptableObject.getGeneratorFunctionPrototype(cx, scope));
		} else {
			fn.setPrototype(cx, ScriptableObject.getFunctionPrototype(cx, scope));
		}
	}

	public static void setObjectProtoAndParent(Context cx, ScriptableObject object, Scriptable scope) {
		// Compared with function it always sets the scope to top scope
		scope = ScriptableObject.getTopLevelScope(scope);
		object.setParentScope(scope);
		Scriptable proto = ScriptableObject.getClassPrototype(cx, scope, object.getClassName());
		object.setPrototype(cx, proto);
	}

	public static void setBuiltinProtoAndParent(Context cx, Scriptable object, Scriptable scope, TopLevel.Builtins type) {
		scope = ScriptableObject.getTopLevelScope(scope);
		object.setParentScope(scope);
		object.setPrototype(cx, TopLevel.getBuiltinPrototype(cx, scope, type));
	}


	public static void initFunction(Context cx, Scriptable scope, NativeFunction function, int type, boolean fromEvalCode) {
		if (type == FunctionNode.FUNCTION_STATEMENT) {
			String name = function.getFunctionName();
			if (name != null && name.length() != 0) {
				if (!fromEvalCode) {
					// ECMA specifies that functions defined in global and
					// function scope outside eval should have DONTDELETE set.
					ScriptableObject.defineProperty(cx, scope, name, function, ScriptableObject.PERMANENT);
				} else {
					scope.put(cx, name, scope, function);
				}
			}
		} else if (type == FunctionNode.FUNCTION_EXPRESSION_STATEMENT) {
			String name = function.getFunctionName();
			if (name != null && name.length() != 0) {
				// Always put function expression statements into initial
				// activation object ignoring the with statement to follow
				// SpiderMonkey
				while (scope instanceof NativeWith) {
					scope = scope.getParentScope();
				}
				scope.put(cx, name, scope, function);
			}
		} else {
			throw Kit.codeBug();
		}
	}

	public static Scriptable newArrayLiteral(Object[] objects, int[] skipIndices, Context cx, Scriptable scope) {
		final int SKIP_DENSITY = 2;
		int count = objects.length;
		int skipCount = 0;
		if (skipIndices != null) {
			skipCount = skipIndices.length;
		}
		int length = count + skipCount;
		if (length > 1 && skipCount * SKIP_DENSITY < length) {
			// If not too sparse, create whole array for constructor
			Object[] sparse;
			if (skipCount == 0) {
				sparse = objects;
			} else {
				sparse = new Object[length];
				int skip = 0;
				for (int i = 0, j = 0; i != length; ++i) {
					if (skip != skipCount && skipIndices[skip] == i) {
						sparse[i] = Scriptable.NOT_FOUND;
						++skip;
						continue;
					}
					sparse[i] = objects[j];
					++j;
				}
			}
			return cx.newArray(scope, sparse);
		}

		var array = cx.newArray(scope, length);

		int skip = 0;
		for (int i = 0, j = 0; i != length; ++i) {
			if (skip != skipCount && skipIndices[skip] == i) {
				++skip;
				continue;
			}
			array.put(cx, i, array, objects[j]);
			++j;
		}
		return array;
	}

	public static Scriptable newObjectLiteral(Object[] propertyIds, Object[] propertyValues, int[] getterSetters, Context cx, Scriptable scope) {
		Scriptable object = cx.newObject(scope);
		for (int i = 0, end = propertyIds.length; i != end; ++i) {
			Object id = propertyIds[i];
			int getterSetter = getterSetters == null ? 0 : getterSetters[i];
			Object value = propertyValues[i];
			if (id instanceof String) {
				if (getterSetter == 0) {
					if (isSpecialProperty((String) id)) {
						Ref ref = specialRef(object, (String) id, cx, scope);
						ref.set(cx, scope, value);
					} else {
						object.put(cx, (String) id, object, value);
					}
				} else {
					ScriptableObject so = (ScriptableObject) object;
					Callable getterOrSetter = (Callable) value;
					boolean isSetter = getterSetter == 1;
					so.setGetterOrSetter(cx, (String) id, 0, getterOrSetter, isSetter);
				}
			} else {
				int index = (Integer) id;
				object.put(cx, index, object, value);
			}
		}
		return object;
	}

	public static boolean isArrayObject(Object obj) {
		return obj instanceof NativeJavaList || obj instanceof Arguments;
	}

	public static Object[] getArrayElements(Scriptable object) {
		Context cx = Context.getContext();
		long longLen = getLengthProperty(cx, object, false);
		if (longLen > Integer.MAX_VALUE) {
			// arrays beyond  MAX_INT is not in Java in any case
			throw new IllegalArgumentException();
		}
		int len = (int) longLen;
		if (len == 0) {
			return EMPTY_ARGS;
		}
		Object[] result = new Object[len];
		for (int i = 0; i < len; i++) {
			Object elem = ScriptableObject.getProperty(cx, object, i);
			result[i] = (elem == Scriptable.NOT_FOUND) ? Undefined.instance : elem;
		}
		return result;
	}

	static void checkDeprecated(Context cx, String name) {
		throw Context.reportRuntimeError(cx, getMessage1("msg.deprec.ctor", name));
	}

	public static String getMessage0(String messageId) {
		return getMessage(messageId, null);
	}

	public static String getMessage1(String messageId, Object arg1) {
		Object[] arguments = {arg1};
		return getMessage(messageId, arguments);
	}

	public static String getMessage2(String messageId, Object arg1, Object arg2) {
		Object[] arguments = {arg1, arg2};
		return getMessage(messageId, arguments);
	}

	public static String getMessage3(String messageId, Object arg1, Object arg2, Object arg3) {
		Object[] arguments = {arg1, arg2, arg3};
		return getMessage(messageId, arguments);
	}

	public static String getMessage4(String messageId, Object arg1, Object arg2, Object arg3, Object arg4) {
		Object[] arguments = {arg1, arg2, arg3, arg4};
		return getMessage(messageId, arguments);
	}

	/**
	 * This is an interface defining a message provider. Create your
	 * own implementation to override the default error message provider.
	 *
	 * @author Mike Harm
	 */
	public interface MessageProvider {

		/**
		 * Returns a textual message identified by the given messageId,
		 * parameterized by the given arguments.
		 *
		 * @param messageId the identifier of the message
		 * @param arguments the arguments to fill into the message
		 */
		String getMessage(String messageId, Object[] arguments);
	}

	public static final MessageProvider messageProvider = new DefaultMessageProvider();

	public static String getMessage(String messageId, Object[] arguments) {
		return messageProvider.getMessage(messageId, arguments);
	}

	/* OPT there's a noticable delay for the first error!  Maybe it'd
	 * make sense to use a ListResourceBundle instead of a properties
	 * file to avoid (synchronized) text parsing.
	 */
	private static class DefaultMessageProvider implements MessageProvider {
		@Override
		public String getMessage(String messageId, Object[] arguments) {
			final String defaultResource = "dev.latvian.mods.rhino.resources.Messages";

			Context cx = Context.getCurrentContext();
			Locale locale = cx != null ? cx.getLocale() : Locale.getDefault();

			// ResourceBundle does caching.
			ResourceBundle rb = ResourceBundle.getBundle(defaultResource, locale);

			String formatString;
			try {
				formatString = rb.getString(messageId);
			} catch (java.util.MissingResourceException mre) {
				throw new RuntimeException("no message resource found for message property " + messageId);
			}

			/*
			 * It's OK to format the string, even if 'arguments' is null;
			 * we need to format it anyway, to make double ''s collapse to
			 * single 's.
			 */
			MessageFormat formatter = new MessageFormat(formatString);
			return formatter.format(arguments);
		}
	}

	public static EcmaError constructError(String error, String message) {
		int[] linep = new int[1];
		String filename = Context.getSourcePositionFromStack(linep);
		return constructError(error, message, filename, linep[0], null, 0);
	}

	public static EcmaError constructError(String error, String message, int lineNumberDelta) {
		int[] linep = new int[1];
		String filename = Context.getSourcePositionFromStack(linep);
		if (linep[0] != 0) {
			linep[0] += lineNumberDelta;
		}
		return constructError(error, message, filename, linep[0], null, 0);
	}

	public static EcmaError constructError(String error, String message, String sourceName, int lineNumber, String lineSource, int columnNumber) {
		return new EcmaError(error, message, sourceName, lineNumber, lineSource, columnNumber);
	}

	public static EcmaError rangeError(String message) {
		return constructError("RangeError", message);
	}

	public static EcmaError typeError(String message) {
		return constructError("TypeError", message);
	}

	public static EcmaError typeError0(String messageId) {
		String msg = getMessage0(messageId);
		return typeError(msg);
	}

	public static EcmaError typeError1(String messageId, Object arg1) {
		String msg = getMessage1(messageId, arg1);
		return typeError(msg);
	}

	public static EcmaError typeError2(String messageId, Object arg1, Object arg2) {
		String msg = getMessage2(messageId, arg1, arg2);
		return typeError(msg);
	}

	public static EcmaError typeError3(String messageId, String arg1, String arg2, String arg3) {
		String msg = getMessage3(messageId, arg1, arg2, arg3);
		return typeError(msg);
	}

	public static RuntimeException undefReadError(Object object, Object id) {
		return typeError2("msg.undef.prop.read", toString(object), toString(id));
	}

	public static RuntimeException undefCallError(Object object, Object id) {
		return typeError2("msg.undef.method.call", toString(object), toString(id));
	}

	public static RuntimeException undefWriteError(Object object, Object id, Object value) {
		return typeError3("msg.undef.prop.write", toString(object), toString(id), toString(value));
	}

	private static RuntimeException undefDeleteError(Object object, Object id) {
		throw typeError2("msg.undef.prop.delete", toString(object), toString(id));
	}

	public static RuntimeException notFoundError(Scriptable object, String property) {
		// XXX: use object to improve the error message
		String msg = getMessage1("msg.is.not.defined", property);
		throw constructError("ReferenceError", msg);
	}

	public static RuntimeException notFunctionError(Object value) {
		return notFunctionError(value, value);
	}

	public static RuntimeException notFunctionError(Object value, Object messageHelper) {
		// Use value for better error reporting
		String msg = (messageHelper == null) ? "null" : messageHelper.toString();
		if (value == Scriptable.NOT_FOUND) {
			return typeError1("msg.function.not.found", msg);
		}
		return typeError2("msg.isnt.function", msg, typeof(value));
	}

	public static RuntimeException notFunctionError(Object obj, Object value, String propertyName) {
		// Use obj and value for better error reporting
		String objString = toString(obj);
		if (obj instanceof NativeFunction) {
			// Omit function body in string representations of functions
			int paren = objString.indexOf(')');
			int curly = objString.indexOf('{', paren);
			if (curly > -1) {
				objString = objString.substring(0, curly + 1) + "...}";
			}
		}
		if (value == Scriptable.NOT_FOUND) {
			return typeError2("msg.function.not.found.in", propertyName, objString);
		}
		return typeError3("msg.isnt.function.in", propertyName, objString, typeof(value).toString());
	}

	private static RuntimeException notXmlError(Object value) {
		throw typeError1("msg.isnt.xml.object", toString(value));
	}

	private static void warnAboutNonJSObject(Object nonJSObject) {
		final String omitParam = getMessage0("params.omit.non.js.object.warning");
		if (!"true".equals(omitParam)) {
			String message = getMessage2("msg.non.js.object.warning", nonJSObject, nonJSObject.getClass().getName());
			Context.reportWarning(Context.getCurrentContext(), message);
			// Just to be sure that it would be noticed
			System.err.println(message);
		}
	}

	public static RegExp getRegExpProxy(Context cx) {
		return cx.getRegExp();
	}

	public static void setRegExpProxy(Context cx, RegExp proxy) {
		if (proxy == null) {
			throw new IllegalArgumentException();
		}
		cx.regExp = proxy;
	}

	public static Scriptable wrapRegExp(Context cx, Scriptable scope, Object compiled) {
		return cx.getRegExp().wrapRegExp(cx, scope, compiled);
	}

	public static Scriptable getTemplateLiteralCallSite(Context cx, Scriptable scope, Object[] strings, int index) {
		/* step 1 */
		Object callsite = strings[index];
		if (callsite instanceof Scriptable) {
			return (Scriptable) callsite;
		}
		assert callsite instanceof String[];
		String[] vals = (String[]) callsite;
		assert (vals.length & 1) == 0;
		// ScriptableObject // final int FROZEN = ScriptableObject.PERMANENT | ScriptableObject.READONLY;
		/* step 2-7 */
		var siteObj = cx.newArray(scope, vals.length >>> 1);
		var rawObj = cx.newArray(scope, vals.length >>> 1);
		for (int i = 0, n = vals.length; i < n; i += 2) {
			/* step 8 a-f */
			int idx = i >>> 1;
			siteObj.put(cx, idx, siteObj, vals[i]);
			// ScriptableObject // siteObj.setAttributes(cx, idx, FROZEN);
			rawObj.put(cx, idx, rawObj, vals[i + 1]);
			// ScriptableObject // rawObj.setAttributes(cx, idx, FROZEN);
		}
		/* step 9 */
		// TODO: call abstract operation FreezeObject
		// ScriptableObject // rawObj.setAttributes(cx, "length", FROZEN);
		// ScriptableObject // rawObj.preventExtensions();
		/* step 10 */
		siteObj.put(cx, "raw", siteObj, rawObj);
		// ScriptableObject // siteObj.setAttributes(cx, "raw", FROZEN | ScriptableObject.DONTENUM);
		/* step 11 */
		// TODO: call abstract operation FreezeObject
		// ScriptableObject // siteObj.setAttributes(cx, "length", FROZEN);
		// ScriptableObject // siteObj.preventExtensions();
		/* step 12 */
		strings[index] = siteObj;
		return siteObj;
	}

	private static void storeScriptable(Context cx, Scriptable value) {
		// The previously stored scratchScriptable should be consumed
		if (cx.scratchScriptable != null) {
			throw new IllegalStateException();
		}
		cx.scratchScriptable = value;
	}

	public static Scriptable lastStoredScriptable(Context cx) {
		Scriptable result = cx.scratchScriptable;
		cx.scratchScriptable = null;
		return result;
	}

	static String makeUrlForGeneratedScript(boolean isEval, String masterScriptUrl, int masterScriptLine) {
		if (isEval) {
			return masterScriptUrl + '#' + masterScriptLine + "(eval)";
		}
		return masterScriptUrl + '#' + masterScriptLine + "(Function)";
	}

	/**
	 * Not all "NativeSymbol" instances are actually symbols. So account for that here rather than just
	 * by using an "instanceof" check.
	 */
	static boolean isSymbol(Object obj) {
		return (((obj instanceof NativeSymbol) && ((NativeSymbol) obj).isSymbol())) || (obj instanceof SymbolKey);
	}

	public static RuntimeException errorWithClassName(Context cx, String msg, Object val) {
		return Context.reportRuntimeError1(cx, msg, val.getClass().getName());
	}


	/**
	 * Equivalent to executing "new $constructorName(message, sourceFileName, sourceLineNo)" from JavaScript.
	 *
	 * @param cx      the current context
	 * @param scope   the current scope
	 * @param message the message
	 * @return a JavaScriptException you should throw
	 */
	public static JavaScriptException throwCustomError(Context cx, Scriptable scope, String constructorName, String message) {
		int[] linep = {0};
		String filename = Context.getSourcePositionFromStack(linep);
		final Scriptable error = cx.newObject(scope, constructorName, new Object[]{message, filename, linep[0]});
		return new JavaScriptException(cx, error, filename, linep[0]);
	}

	/**
	 * Support for generic Array-ish objects.  Most of the Array
	 * functions try to be generic; anything that has a length
	 * property is assumed to be an array.
	 * getLengthProperty returns 0 if obj does not have the length property
	 * or its value is not convertible to a number.
	 */
	public static long getLengthProperty(Context cx, Scriptable obj, boolean throwIfTooLarge) {
		// These will both give numeric lengths within Uint32 range.
		if (obj instanceof NativeString n) {
			return n.getLength();
		} else if (obj instanceof NativeJavaList n) {
			return n.getLength();
		}

		Object len = ScriptableObject.getProperty(cx, obj, "length");
		if (len == ScriptableObject.NOT_FOUND) {
			// toUint32(undefined) == 0
			return 0;
		}

		double doubleLen = toNumber(len);
		if (doubleLen > NativeNumber.MAX_SAFE_INTEGER) {
			if (throwIfTooLarge) {
				String msg = getMessage0("msg.arraylength.bad");
				throw rangeError(msg);
			}
			return (int) NativeNumber.MAX_SAFE_INTEGER;
		}
		if (doubleLen < 0) {
			return 0;
		}
		return toUint32(len);
	}

	public static Object[] unwrapArgs(Object... args) {
		if (args.length == 0) {
			return EMPTY_ARGS;
		}

		boolean newArray = true;

		for (int i = 0; i < args.length; i++) {
			Object o = Wrapper.unwrapped(args[i]);

			if (args[i] != o) {
				if (newArray) {
					newArray = false;
					Object[] args2 = new Object[args.length];
					System.arraycopy(args, 0, args2, 0, args.length);
					args = args2;
				}

				args[i] = o;
			}
		}

		return args;
	}
}
