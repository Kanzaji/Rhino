package dev.latvian.mods.rhino.js;

import dev.latvian.mods.rhino.ContextJS;
import dev.latvian.mods.rhino.Wrapper;
import dev.latvian.mods.rhino.js.prototype.PrototypeJS;
import dev.latvian.mods.rhino.util.NativeArrayWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ArrayJS {
	public static final PrototypeJS PROTOTYPE = JavaObjectJS.PROTOTYPE.create(TypeJS.ARRAY, "Array")
			.constructor(ArrayJS::construct)
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
}
