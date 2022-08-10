package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Function;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.SharedContextData;
import dev.latvian.mods.rhino.Undefined;
import dev.latvian.mods.rhino.Wrapper;
import org.jetbrains.annotations.Nullable;

public class MethodSignature {
	public static final MethodSignature EMPTY = new MethodSignature();
	public static final MethodSignature OBJECT = new MethodSignature(Object.class);
	public static final MethodSignature OBJECT_ARRAY = new MethodSignature(Object[].class);
	public static final MethodSignature STRING = new MethodSignature(String.class);
	public static final MethodSignature BYTE = new MethodSignature(byte.class);
	public static final MethodSignature SHORT = new MethodSignature(short.class);
	public static final MethodSignature INT = new MethodSignature(int.class);
	public static final MethodSignature LONG = new MethodSignature(long.class);
	public static final MethodSignature FLOAT = new MethodSignature(float.class);
	public static final MethodSignature DOUBLE = new MethodSignature(double.class);
	public static final MethodSignature BOOLEAN = new MethodSignature(boolean.class);
	public static final MethodSignature CHAR = new MethodSignature(char.class);

	@Nullable
	private static MethodSignature ofSingleType(Class<?> type) {
		if (type == Object.class) {
			return OBJECT;
		} else if (type == Object[].class) {
			return OBJECT_ARRAY;
		} else if (type == String.class) {
			return STRING;
		} else if (type == byte.class) {
			return BYTE;
		} else if (type == short.class) {
			return SHORT;
		} else if (type == int.class) {
			return INT;
		} else if (type == long.class) {
			return LONG;
		} else if (type == float.class) {
			return FLOAT;
		} else if (type == double.class) {
			return DOUBLE;
		} else if (type == boolean.class) {
			return BOOLEAN;
		} else if (type == char.class) {
			return CHAR;
		}

		return null;
	}

	public static MethodSignature of(Class<?>... types) {
		if (types.length == 0) {
			return EMPTY;
		} else if (types.length == 1) {
			var t = ofSingleType(types[0]);

			if (t != null) {
				return t;
			}
		}

		return new MethodSignature(types);
	}

	public static MethodSignature ofArgs(Object... args) {
		if (args.length == 0) {
			return EMPTY;
		} else if (args.length == 1) {
			var c = args[0].getClass();
			var t = ofSingleType(c);

			if (t != null) {
				return t;
			}

			return new MethodSignature(c);
		}

		Class<?>[] types = new Class<?>[args.length];

		for (int i = 0; i < args.length; i++) {
			types[i] = args[i].getClass();
		}

		return new MethodSignature(types);
	}

	public final Class<?>[] types;
	private String string = null;
	private int hashCode = 0;

	private MethodSignature(Class<?>... types) {
		this.types = types;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof MethodSignature o) {
			if (types.length != o.types.length) {
				return false;
			}

			for (int i = 0; i < types.length; i++) {
				if (types[i] != o.types[i]) {
					return false;
				}
			}

			return true;
		}

		return false;
	}

	@Override
	public String toString() {
		if (string == null) {
			StringBuilder sb = new StringBuilder();
			sb.append('(');

			for (var type : types) {
				sb.append(type.descriptorString());
			}

			sb.append(')');
			string = sb.toString();
		}

		return string;
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			int h = 1;

			for (var type : types) {
				h = 31 * h + type.hashCode();
			}

			hashCode = h == 0 ? 1 : h;
		}

		return hashCode;
	}

	public boolean isEmpty() {
		return types.length == 0;
	}

	public int matches(SharedContextData data, Object[] args, MethodSignature argsSig) {
		if (this == argsSig) {
			return args.length;
		}

		int exactMatches = -1;

		if (types.length == args.length) {
			for (int i = 0; i < args.length; i++) {
				if (types[i] == argsSig.types[i]) {
					if (exactMatches == -1) {
						exactMatches = 0;
					}

					exactMatches++;
				}

				var typeWrapper = data.hasTypeWrappers() ? data.getTypeWrappers().getWrapperFactory(data, types[i], args[i]) : null;

				if (typeWrapper != null) {
					if (exactMatches == -1) {
						exactMatches = 0;
					}
				}
			}
		}

		return exactMatches;
	}

	public static Object[] unwrapArgs(Context cx, Object[] args, Class<?>[] types) {
		if (args.length == 0) {
			return ScriptRuntime.EMPTY_OBJECTS;
		}

		Object[] origArgs = args;

		for (int i = 0; i < args.length; i++) {
			Object o = Context.jsToJava(cx, args[i], types[i]);

			if (args[i] != o) {
				if (args == origArgs) {
					args = args.clone();
				}

				args[i] = o;
			}
		}

		return args;
	}

	public static String javaSignature(Class<?> type) {
		if (!type.isArray()) {
			return type.getName();
		}
		int arrayDimension = 0;
		do {
			++arrayDimension;
			type = type.getComponentType();
		} while (type.isArray());
		String name = type.getName();
		String suffix = "[]";
		if (arrayDimension == 1) {
			return name.concat(suffix);
		}
		int length = name.length() + arrayDimension * suffix.length();
		StringBuilder sb = new StringBuilder(length);
		sb.append(name);
		while (arrayDimension != 0) {
			--arrayDimension;
			sb.append(suffix);
		}
		return sb.toString();
	}

	public static String liveConnectSignature(Class<?>[] argTypes) {
		int N = argTypes.length;
		if (N == 0) {
			return "()";
		}
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		for (int i = 0; i != N; ++i) {
			if (i != 0) {
				sb.append(',');
			}
			sb.append(javaSignature(argTypes[i]));
		}
		sb.append(')');
		return sb.toString();
	}

	public static String scriptSignature(Object[] values) {
		StringBuilder sig = new StringBuilder();
		for (int i = 0; i != values.length; ++i) {
			Object value = values[i];

			String s;
			if (value == null) {
				s = "null";
			} else if (value instanceof Boolean) {
				s = "boolean";
			} else if (value instanceof String) {
				s = "string";
			} else if (value instanceof Number) {
				s = "number";
			} else if (value instanceof Scriptable) {
				if (value instanceof Undefined) {
					s = "undefined";
				} else if (value instanceof Wrapper) {
					Object wrapped = ((Wrapper) value).unwrap();
					s = wrapped.getClass().getName();
				} else if (value instanceof Function) {
					s = "function";
				} else {
					s = "object";
				}
			} else {
				s = javaSignature(value.getClass());
			}

			if (i != 0) {
				sig.append(',');
			}
			sig.append(s);
		}
		return sig.toString();
	}
}
