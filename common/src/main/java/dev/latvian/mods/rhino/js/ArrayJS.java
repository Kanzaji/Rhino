package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.Wrapper;
import dev.latvian.mods.rhino.js.prototype.ListMemberFunctions;
import dev.latvian.mods.rhino.js.prototype.PrototypeJS;
import dev.latvian.mods.rhino.util.NativeArrayWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ArrayJS {
	public static final PrototypeJS PROTOTYPE = JavaObjectJS.PROTOTYPE.create(TypeJS.ARRAY, "Array")
			.constructor(ArrayJS::construct)
			.selfMembers(ListMemberFunctions.INSTANCE)
			.staticFunction("from", ArrayJS::unimpl)
			.staticFunction("isArray", ArrayJS::unimpl)
			.staticFunction("of", ArrayJS::unimpl)
			.property("length", ArrayJS::length)
			.function("push", ArrayJS::unimpl)
			.function("pop", ArrayJS::unimpl)
			.function("shift", ArrayJS::unimpl)
			.function("unshift", ArrayJS::unimpl)
			.function("concat", ArrayJS::unimpl)
			.function("join", ArrayJS::unimpl)
			.function("reverse", ArrayJS::unimpl)
			.function("slice", ArrayJS::unimpl)
			.function("splice", ArrayJS::unimpl)
			.function("every", ArrayJS::unimpl)
			.function("some", ArrayJS::unimpl)
			.function("filter", ArrayJS::unimpl)
			.function("map", ArrayJS::unimpl)
			.function("reduce", ArrayJS::unimpl)
			.function("reduceRight", ArrayJS::unimpl)
			.function("find", ArrayJS::unimpl)
			.function("findIndex", ArrayJS::unimpl)
			.function("findLast", ArrayJS::unimpl)
			.function("findLastIndex", ArrayJS::unimpl)
			.function("flatMap", ArrayJS::unimpl)
			.function("copyWithin", ArrayJS::unimpl)
			.function("includes", ArrayJS::unimpl)
			.function("fill", ArrayJS::unimpl);

	private static List<?> construct(ContextJS cx, Object[] args) {
		if (args.length == 0) {
			return new ArrayList<>(0);
		} else if (args[0] instanceof Collection<?> l) {
			return new ArrayList<>(l);
		}

		return new ArrayList<>(Arrays.asList(Wrapper.unwrappedArray(args)));
	}

	private static List<?> list(Object arg) {
		if (arg == null) {
			return List.of();
		} else if (arg instanceof List<?> l) {
			return l;
		} else if (arg instanceof Collection<?>) {
			return new ArrayList<>((Collection<?>) arg);
		} else if (arg instanceof Object[]) {
			return Arrays.asList((Object[]) arg);
		} else if (arg.getClass().isArray()) {
			return NativeArrayWrapper.of(arg);
		} else if (arg instanceof Iterable<?>) {
			var list = new ArrayList<>();

			for (var o : (Iterable<?>) arg) {
				list.add(o);
			}

			return list;
		}

		return List.of(arg);
	}

	private static int length(ContextJS cx, Object self) {
		return ((List<?>) self).size();
	}

	private static Object unimpl(ContextJS cx, Object[] args) {
		throw new IllegalStateException("This function is not yet implemented!");
	}

	private static Object unimpl(ContextJS cx, Object self) {
		throw new IllegalStateException("This function is not yet implemented!");
	}

	/*

	private int push(Context cx, Scriptable scope, Object[] args) {
		if (args.length == 1) {
			list.add(Context.jsToJava(cx, args[0], listType));
		} else if (args.length > 1) {
			Object[] args1 = new Object[args.length];

			for (int i = 0; i < args.length; i++) {
				args1[i] = Context.jsToJava(cx, args[i], listType);
			}

			list.addAll(Arrays.asList(args1));
		}

		return list.size();
	}

	private Object pop(Context cx, Scriptable scope) {
		if (list.isEmpty()) {
			return Undefined.instance;
		}

		return list.remove(list.size() - 1);
	}

	private Object shift(Context cx, Scriptable scope) {
		if (list.isEmpty()) {
			return Undefined.instance;
		}

		return list.remove(0);
	}

	private int unshift(Context cx, Scriptable scope, Object[] args) {
		for (int i = args.length - 1; i >= 0; i--) {
			list.add(0, Context.jsToJava(cx, args[i], listType));
		}

		return list.size();
	}

	private Object concat(Context cx, Scriptable scope, Object[] args) {
		List<Object> list1 = new ArrayList<>(list);

		if (args.length > 0 && args[0] instanceof List<?>) {
			list1.addAll((List<?>) args[0]);
		}

		return list1;
	}

	private String join(Context cx, Scriptable scope, Object[] args) {
		if (list.isEmpty()) {
			return "";
		} else if (list.size() == 1) {
			return ScriptRuntime.toString(cx, list.get(0));
		}

		String j = ScriptRuntime.toString(cx, args[0]);
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < list.size(); i++) {
			if (i > 0) {
				sb.append(j);
			}

			sb.append(ScriptRuntime.toString(cx, list.get(i)));
		}

		return sb.toString();
	}

	private NativeJavaList reverse(Context cx, Scriptable scope) {
		if (list.size() > 1) {
			Collections.reverse(list);
		}

		return this;
	}

	private Object slice(Context cx, Scriptable scope, Object[] args) {
		// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/slice
		throw new IllegalStateException("Not implemented yet!");
	}

	private Object splice(Context cx, Scriptable scope, Object[] args) {
		// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/splice
		throw new IllegalStateException("Not implemented yet!");
	}

	private Object every(Context cx, Scriptable scope, Object[] args) {
		Predicate predicate = (Predicate) args[0];

		for (Object o : list) {
			if (!predicate.test(o)) {
				return Boolean.FALSE;
			}
		}

		return Boolean.TRUE;
	}

	private Object some(Context cx, Scriptable scope, Object[] args) {
		Predicate predicate = (Predicate) args[0];

		for (Object o : list) {
			if (predicate.test(o)) {
				return Boolean.TRUE;
			}
		}

		return Boolean.FALSE;
	}

	private Object filter(Context cx, Scriptable scope, Object[] args) {
		if (list.isEmpty()) {
			return this;
		}

		Predicate predicate = (Predicate) args[0];
		List<Object> list1 = new ArrayList<>();

		for (Object o : list) {
			if (predicate.test(o)) {
				list1.add(o);
			}
		}

		return list1;
	}

	private Object map(Context cx, Scriptable scope, Object[] args) {
		if (list.isEmpty()) {
			return this;
		}

		Function function = (Function) args[0];

		List<Object> list1 = new ArrayList<>();

		for (Object o : list) {
			list1.add(function.apply(o));
		}

		return list1;
	}

	private Object reduce(Context cx, Scriptable scope, Object[] args) {
		if (list.isEmpty()) {
			return Undefined.instance;
		} else if (list.size() == 1) {
			return list.get(0);
		}

		BinaryOperator operator = (BinaryOperator) args[0];
		Object o = valueUnwrapper.unwrap(cx, this, list.get(0));

		for (int i = 1; i < list.size(); i++) {
			o = valueUnwrapper.unwrap(cx, this, operator.apply(o, valueUnwrapper.unwrap(cx, this, list.get(i))));
		}

		return o;
	}

	private Object reduceRight(Context cx, Scriptable scope, Object[] args) {
		if (list.isEmpty()) {
			return Undefined.instance;
		} else if (list.size() == 1) {
			return list.get(0);
		}

		BinaryOperator operator = (BinaryOperator) args[0];
		Object o = valueUnwrapper.unwrap(cx, this, list.get(0));

		for (int i = list.size() - 1; i >= 1; i--) {
			o = valueUnwrapper.unwrap(cx, this, operator.apply(o, valueUnwrapper.unwrap(cx, this, list.get(i))));
		}

		return o;
	}

	private Object find(Context cx, Scriptable scope, Object[] args) {
		if (list.isEmpty()) {
			return Undefined.instance;
		}

		Predicate predicate = (Predicate) args[0];

		for (Object o : list) {
			if (predicate.test(o)) {
				return o;
			}
		}

		return Undefined.instance;
	}

	private Object findIndex(Context cx, Scriptable scope, Object[] args) {
		if (list.isEmpty()) {
			return -1;
		}

		Predicate predicate = (Predicate) args[0];

		for (int i = 0; i < list.size(); i++) {
			if (predicate.test(list.get(i))) {
				return i;
			}
		}

		return -1;
	}

	private Object findLast(Context cx, Scriptable scope, Object[] args) {
		if (list.isEmpty()) {
			return Undefined.instance;
		}

		Predicate predicate = (Predicate) args[0];

		for (int i = list.size() - 1; i >= 0; i--) {
			var o = list.get(i);

			if (predicate.test(o)) {
				return o;
			}
		}

		return Undefined.instance;
	}

	private Object findLastIndex(Context cx, Scriptable scope, Object[] args) {
		if (list.isEmpty()) {
			return -1;
		}

		Predicate predicate = (Predicate) args[0];

		for (int i = list.size() - 1; i >= 0; i--) {
			if (predicate.test(list.get(i))) {
				return i;
			}
		}

		return -1;
	}

	private Object flatMap(Context cx, Scriptable scope, Object[] args) {
		// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/flatMap
		throw new IllegalStateException("Not implemented yet!");
	}

	private Object copyWithin(Context cx, Scriptable scope, Object[] args) {
		// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/copyWithin
		throw new IllegalStateException("Not implemented yet!");
	}

	private Object includes(Context cx, Scriptable scope, Object[] args) {
		// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/includes
		throw new IllegalStateException("Not implemented yet!");
	}

	private Object fill(Context cx, Scriptable scope, Object[] args) {
		// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/fill
		throw new IllegalStateException("Not implemented yet!");
	}
	*/
}
