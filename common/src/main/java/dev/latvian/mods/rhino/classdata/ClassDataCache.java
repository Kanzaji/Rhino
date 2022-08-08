package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.SharedContextData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassDataCache {
	public static <T> List<T> optimizeList(List<T> list) {
		return switch (list.size()) {
			case 0 -> List.of();
			case 1 -> List.of(list.get(0));
			case 2 -> List.of(list.get(0), list.get(1));
			default -> list;
		};
	}

	public final SharedContextData sharedData;
	private final Object lock;
	private final Map<Class<?>, ClassData> cache;
	final ClassData objectClassData;
	private final ClassData arrayClassData;
	private final ClassData classClassData;

	public ClassDataCache(SharedContextData cxd) {
		sharedData = cxd;
		lock = new Object();
		cache = new HashMap<>();
		objectClassData = new ClassData(this, Object.class);
		arrayClassData = new ClassData(this, Object[].class);
		classClassData = new ClassData(this, Class.class);
	}

	public ClassData of(Class<?> c) {
		if (c == null || c == Object.class) {
			return objectClassData;
		} else if (c == Class.class) {
			return classClassData;
		} else if (c.isArray()) {
			return arrayClassData;
		}

		synchronized (lock) {
			ClassData d = cache.get(c);

			if (d == null) {
				d = new ClassData(this, c);
				cache.put(c, d);
			}

			return d;
		}
	}
}
