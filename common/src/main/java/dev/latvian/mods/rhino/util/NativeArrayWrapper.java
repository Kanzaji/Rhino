package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Wrapper;

import java.lang.reflect.Array;
import java.util.AbstractList;

public class NativeArrayWrapper extends AbstractList<Object> implements Wrapper {
	public static NativeArrayWrapper of(Object array) {
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
	public Object unwrap() {
		return array;
	}

	@Override
	public Object get(int index) {
		if (array instanceof Object[] a) {
			return a[index];
		}

		return Array.get(array, index);
	}

	@Override
	public Object set(int index, Object value) {
		if (array instanceof Object[] a) {
			var o = a[index];
			a[index] = value;
			return o;
		}

		Object o = get(index);
		Array.set(array, index, value);
		return o;
	}

	@Override
	public int size() {
		return length;
	}
}
