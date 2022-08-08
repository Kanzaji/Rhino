package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.SharedContextData;
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
}
