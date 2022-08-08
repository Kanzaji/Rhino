package dev.latvian.mods.rhino.util;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

public class NativeArrayWrapper extends AbstractList<Object> {
	public static List<Object> of(Object array) {
		if (array instanceof Object[] a) {
			return Arrays.asList(a);
		}

		return new NativeArrayWrapper(array);
	}

	public final Object array;
	public final int length;
	public final Class<?> type;

	private NativeArrayWrapper(Object o) {
		this.array = o;
		this.length = Array.getLength(o);
		this.type = array.getClass().getComponentType();
	}

	@Override
	public Object get(int index) {
		return Array.get(array, index);
	}

	@Override
	public Object set(int index, Object value) {
		Object o = get(index);
		Array.set(array, index, value);
		return o;
	}

	@Override
	public int size() {
		return length;
	}
}
