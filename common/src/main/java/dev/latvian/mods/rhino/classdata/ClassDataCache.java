package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.SharedContextData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassDataCache {
	@SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
	public static <K, V> Map<K, V> optimizeMap(Map<K, V> map) {
		Map.Entry<K, V>[] entries = new Map.Entry[map.size()];
		System.arraycopy(map.entrySet().toArray(), 0, entries, 0, entries.length);
		return Map.ofEntries(entries);
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
		cache = new ConcurrentHashMap<>(16, 0.75f, 1);
		objectClassData = new ClassData(this, PublicClassData.OBJECT);
		arrayClassData = new ClassData(this, PublicClassData.OBJECT_ARRAY);
		classClassData = new ClassData(this, PublicClassData.CLASS);
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
				d = new ClassData(this, PublicClassData.of(c));
				cache.put(c, d);
			}

			return d;
		}
	}
}
